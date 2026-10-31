package com.example.receive

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.receive.protocol.ProtocolJson
import com.example.receive.protocol.ShareApkMetaDto
import java.io.File
import java.util.zip.ZipFile

/** A received package on disk, with what we could learn about it. */
data class ReceivedItem(
    val file: File,
    val label: String,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val status: VersionStatus?,
    val icon: Bitmap?,
    val receivedAt: Long
)

/** Lists and inspects packages under `filesDir/received`. */
class ReceivedLibrary(private val context: Context) {
    val root: File get() = File(context.filesDir, "received")

    fun list(): List<ReceivedItem> =
        root.walkTopDown()
            .filter { it.isFile && ReceiveSessionManager.isAppPackage(it.name) }
            .map(::inspect)
            .sortedByDescending { it.receivedAt }
            .toList()

    /** Stores the sender-supplied ShareAPK metadata next to the file (used for bundles we can't parse cheaply). */
    fun saveMeta(file: File, meta: ShareApkMetaDto) {
        runCatching { metaFile(file).writeText(ProtocolJson.encode(meta)) }
    }

    fun delete(item: ReceivedItem) {
        item.file.delete()
        metaFile(item.file).delete()
        item.file.parentFile?.takeIf { it != root && it.listFiles().isNullOrEmpty() }?.delete()
    }

    fun inspect(file: File): ReceivedItem {
        val pm = context.packageManager
        val apk = if (file.extension.equals("apk", true)) file else extractBaseApk(file)
        val info = apk?.let { runCatching { pm.getPackageArchiveInfo(it.path, 0) }.getOrNull() }
        info?.applicationInfo?.let { it.sourceDir = apk.path; it.publicSourceDir = apk.path }
        val meta = runCatching { ProtocolJson.decode<ShareApkMetaDto>(metaFile(file).readText()) }.getOrNull()
        val pkg = info?.packageName ?: meta?.packageName
        val code = info?.longVersionCode ?: meta?.versionCode
        val installed = pkg?.let {
            runCatching { pm.getPackageInfo(it, 0).longVersionCode }.getOrNull()
        }
        val item = ReceivedItem(
            file = file,
            label = info?.applicationInfo?.loadLabel(pm)?.toString() ?: file.nameWithoutExtension,
            packageName = pkg,
            versionName = info?.versionName ?: meta?.versionName,
            versionCode = code,
            status = code?.let { compareWithInstalled(it, installed) },
            icon = info?.applicationInfo?.let { runCatching { it.loadIcon(pm).toBitmap(96, 96) }.getOrNull() },
            receivedAt = file.lastModified()
        )
        if (apk != null && apk != file) apk.delete()
        return item
    }

    /** For .apks/.xapk/.apkm: copy base.apk (or the first .apk) to cache so PackageManager can read it. */
    private fun extractBaseApk(bundle: File): File? = runCatching {
        ZipFile(bundle).use { zip ->
            val entries = zip.entries().toList().filter { !it.isDirectory && it.name.endsWith(".apk", true) }
            val base = entries.firstOrNull { it.name.substringAfterLast('/').equals("base.apk", true) }
                ?: entries.firstOrNull { !it.name.substringAfterLast('/').startsWith("split", true) && !it.name.contains("config.", true) }
                ?: entries.firstOrNull()
                ?: return null
            val out = File.createTempFile("received_base", ".apk", context.cacheDir)
            zip.getInputStream(base).use { input -> out.outputStream().use { input.copyTo(it) } }
            out
        }
    }.getOrNull()

    private fun metaFile(file: File) = File(file.parentFile, ".${file.name}.meta.json")
}
