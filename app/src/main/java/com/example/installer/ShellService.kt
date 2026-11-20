package com.example.installer

import android.os.ParcelFileDescriptor
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/** Shizuku UserService: lives in Shizuku's shell-uid process and runs one command at a time. */
class ShellService : IShellService.Stub() {
    override fun run(cmd: Array<String>, stdin: ParcelFileDescriptor?): String {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val feeder = stdin?.let { pfd ->
            thread(isDaemon = true) {
                runCatching {
                    ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input -> p.outputStream.use { input.copyTo(it, 64 * 1024) } }
                }
                runCatching { p.outputStream.close() }
            }
        } ?: run { runCatching { p.outputStream.close() }; null }
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        feeder?.join(5_000)
        return "$code\n$out"
    }

    override fun destroy() {
        exitProcess(0)
    }
}
