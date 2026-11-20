package com.example.installer

/** `pm` command lines and output parsing, free of android.* so they are unit-tested. */
object ShellCommands {
    fun installCreate(totalSize: Long, packageName: String) =
        arrayOf("pm", "install-create", "-r", "-S", totalSize.toString(), "--pkg", packageName)

    fun installWrite(session: Int, size: Long, name: String) =
        arrayOf("pm", "install-write", "-S", size.toString(), session.toString(), name, "-")

    fun installCommit(session: Int) = arrayOf("pm", "install-commit", session.toString())
    fun installAbandon(session: Int) = arrayOf("pm", "install-abandon", session.toString())

    /** User apps are removed completely. */
    fun uninstall(pkg: String) = arrayOf("pm", "uninstall", pkg)
    /** System apps are only removed for this user: reversible with [restoreForUser]. */
    fun uninstallForUser(pkg: String) = arrayOf("pm", "uninstall", "-k", "--user", "0", pkg)
    fun disable(pkg: String) = arrayOf("pm", "disable-user", "--user", "0", pkg)
    fun enable(pkg: String) = arrayOf("pm", "enable", "--user", "0", pkg)
    fun restoreForUser(pkg: String) = arrayOf("cmd", "package", "install-existing", "--user", "0", pkg)

    data class Result(val code: Int, val output: String) {
        val ok get() = code == 0 && !output.contains("Failure", ignoreCase = true)
        /** pm's own reason, e.g. "INSTALL_FAILED_UPDATE_INCOMPATIBLE". */
        val reason get() = Regex("Failure \\[([^\\]]+)]").find(output)?.groupValues?.get(1) ?: output.trim().lines().lastOrNull().orEmpty()
    }

    fun parse(raw: String): Result {
        val code = raw.substringBefore('\n').trim().toIntOrNull() ?: -1
        return Result(code, raw.substringAfter('\n', ""))
    }

    /** "Success: created install session [1234]" → 1234. */
    fun sessionId(output: String): Int? = Regex("\\[(\\d+)]").find(output)?.groupValues?.get(1)?.toIntOrNull()

    /** Never offered for removal: removing these can stop the phone from booting or working. */
    val PROTECTED = setOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.phone", "com.android.providers.settings",
        "com.android.providers.telephony", "com.android.providers.contacts", "com.android.providers.media",
        "com.android.providers.downloads", "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller", "com.android.shell",
        "com.android.server.telecom", "com.android.bluetooth", "com.android.nfc", "com.android.networkstack",
        "com.google.android.networkstack", "com.android.inputmethod.latin", "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard", "com.android.launcher3", "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher", "com.miui.home", "com.android.vending", "com.google.android.gms",
        "com.google.android.gsf", "com.android.externalstorage", "com.android.documentsui", "com.google.android.documentsui",
        "com.android.webview", "com.google.android.webview", "com.android.se", "com.qualcomm.qti.telephonyservice",
        "moe.shizuku.privileged.api", "com.example", "com.shareapk.app"
    )

    fun isProtected(pkg: String) = pkg in PROTECTED || pkg.startsWith("com.android.providers.")
}
