package com.example.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float?) : UpdateState
    data class Ready(val release: ReleaseInfo, val apk: File) : UpdateState
    /** Releases page not reachable: no releases yet, private repository, or blocked network. */
    data object Unavailable : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/** In-app updates from this repository's GitHub releases, with package/version/signature checks. */
class Updater(private val context: Context, private val repo: String = REPO) {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val prefs = context.getSharedPreferences("updater", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val myVersionName: String
        get() = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

    /** Automatic check at most every 12 hours; quiet on failure. */
    suspend fun autoCheck() {
        val last = prefs.getLong(KEY_LAST, 0)
        if (System.currentTimeMillis() - last < 12 * 3600_000L) return
        check(quiet = true)
    }

    suspend fun check(quiet: Boolean = false) {
        if (_state.value is UpdateState.Downloading) return
        if (!quiet) _state.value = UpdateState.Checking
        val result = withContext(Dispatchers.IO) {
            runCatching {
                http.newCall(
                    Request.Builder().url("https://api.github.com/repos/$repo/releases/latest")
                        .header("Accept", "application/vnd.github+json").build()
                ).execute().use { r ->
                    when {
                        r.code == 404 || r.code == 403 -> UpdateState.Unavailable
                        !r.isSuccessful -> UpdateState.Failed("HTTP ${r.code}")
                        else -> {
                            val info = UpdateLogic.parseLatest(r.body!!.string())
                            when {
                                info == null -> UpdateState.Unavailable
                                UpdateLogic.isNewer(info.version, myVersionName) -> UpdateState.Available(info)
                                else -> UpdateState.UpToDate
                            }
                        }
                    }
                }
            }.getOrElse { UpdateState.Failed(it.message ?: it.javaClass.simpleName) }
        }
        prefs.edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
        // A quiet check only surfaces good news.
        if (!quiet || result is UpdateState.Available) _state.value = result
    }

    /** Downloads and verifies the APK; on success the state becomes [UpdateState.Ready]. */
    suspend fun download(release: ReleaseInfo) {
        if (_state.value is UpdateState.Downloading) return // double tap
        _state.value = UpdateState.Downloading(release, null)
        val dir = File(context.cacheDir, "update").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "shareapk-${release.version}.apk")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                http.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { r ->
                    if (!r.isSuccessful) error("HTTP ${r.code}")
                    val body = r.body!!
                    val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024); var done = 0L; var last = 0L
                            while (true) {
                                val n = input.read(buf); if (n < 0) break
                                output.write(buf, 0, n); done += n
                                if (done - last > 512 * 1024) {
                                    last = done
                                    _state.value = UpdateState.Downloading(release, if (total > 0) done.toFloat() / total else null)
                                }
                            }
                        }
                    }
                }
            }
        }
        ok.exceptionOrNull()?.let { _state.value = UpdateState.Failed(it.message ?: "download failed"); return }
        when (val v = verify(out)) {
            Verdict.Ok -> _state.value = UpdateState.Ready(release, out)
            else -> { out.delete(); _state.value = UpdateState.Failed(v.name) }
        }
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }

    private fun verify(apk: File): Verdict {
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val candidate = pm.getPackageArchiveInfo(apk.path, flags) ?: return Verdict.Unreadable
        val mine = pm.getPackageInfo(context.packageName, flags)
        fun signers(info: android.content.pm.PackageInfo): Set<String> =
            info.signingInfo?.apkContentsSigners.orEmpty().map { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
            }.toSet()
        return UpdateLogic.verify(
            candidate.packageName, candidate.longVersionCode, signers(candidate),
            context.packageName, mine.longVersionCode, signers(mine)
        )
    }

    companion object {
        const val REPO = "Hussein199800/Share_apk"
        private const val KEY_LAST = "last_check"
    }
}
