package com.example.installer

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.InputStream
import kotlin.concurrent.thread

enum class ShizukuStatus { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY }

/**
 * Optional Shizuku support: when the user runs Shizuku and grants access, installs and removals
 * happen silently through `pm` with shell rights. Everything still works without it, with
 * Android's normal confirmation dialogs.
 */
object ShizukuBridge {
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 7301

    private val _status = MutableStateFlow(ShizukuStatus.NOT_RUNNING)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile private var service: IShellService? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { refresh() }
            Shizuku.addBinderDeadListener { service = null; refresh() }
            Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
        }
        refresh()
    }

    fun refresh() {
        val ctx = appContext
        _status.value = when {
            ctx != null && !isInstalled(ctx) -> ShizukuStatus.NOT_INSTALLED
            !runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> ShizukuStatus.NOT_RUNNING
            runCatching { Shizuku.checkSelfPermission() }.getOrDefault(-1) != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NO_PERMISSION
            else -> ShizukuStatus.READY
        }
    }

    fun isReady() = _status.value == ShizukuStatus.READY

    fun requestPermission() {
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
        refresh()
    }

    private fun isInstalled(ctx: Context) =
        runCatching { ctx.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0); true }.getOrDefault(false)

    private suspend fun shell(): IShellService {
        service?.takeIf { it.asBinder().pingBinder() }?.let { return it }
        val ctx = appContext ?: error("Shizuku not initialised")
        val ready = CompletableDeferred<IShellService>()
        val args = Shizuku.UserServiceArgs(ComponentName(ctx.packageName, ShellService::class.java.name))
            .daemon(false).processNameSuffix("shell").debuggable(false).version(1)
        Shizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                binder?.let { ready.complete(IShellService.Stub.asInterface(it)) }
            }
            override fun onServiceDisconnected(name: ComponentName?) { service = null }
        })
        return withTimeout(10_000) { ready.await() }.also { service = it }
    }

    suspend fun run(vararg cmd: String, stdin: InputStream? = null): ShellCommands.Result = withContext(Dispatchers.IO) {
        val s = shell()
        if (stdin == null) return@withContext ShellCommands.parse(s.run(arrayOf(*cmd), null))
        // Feed stdin through a pipe; a damaged backup file throws in the writer and is reported.
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        var feedError: Throwable? = null
        val feeder = thread(isDaemon = true) {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(write).use { out -> stdin.use { it.copyTo(out, 64 * 1024) } }
            } catch (t: Throwable) {
                feedError = t
                runCatching { write.closeWithError(t.message ?: "read failed") }
            }
        }
        val raw = try { s.run(arrayOf(*cmd), read) } finally { runCatching { read.close() } }
        feeder.join()
        feedError?.let { throw it }
        ShellCommands.parse(raw)
    }

    /**
     * Installs one app's files silently: create a session, stream each file into it, commit.
     * Returns null on success, else the reason. A failed write abandons the session.
     */
    suspend fun install(packageName: String, files: List<Triple<String, Long, () -> InputStream>>): String? {
        val create = run(*ShellCommands.installCreate(files.sumOf { it.second }, packageName))
        val session = ShellCommands.sessionId(create.output) ?: return create.reason
        try {
            for ((name, size, open) in files) {
                val w = run(*ShellCommands.installWrite(session, size, name), stdin = open())
                if (!w.ok) { run(*ShellCommands.installAbandon(session)); return w.reason }
            }
        } catch (e: Exception) {
            runCatching { run(*ShellCommands.installAbandon(session)) }
            return e.message ?: e.javaClass.simpleName
        }
        val commit = run(*ShellCommands.installCommit(session))
        return if (commit.ok) null else commit.reason
    }
}
