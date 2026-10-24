package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.FileNamingFormat
import com.example.data.model.InstalledAppInfo
import com.example.util.FileNameAndHashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    val sharedCacheDir: File
        get() = File(context.cacheDir, "shared").apply { if (!exists()) mkdirs() }

    /**
     * Queries all installed packages on the device on Dispatchers.IO.
     * Uses PackageManager.PackageInfoFlags.of(...) on Android 13+ (API 33+)
     * and the standard Int flags overload on Android 12/12L (API 31-32).
     */
    suspend fun loadInstalledApps(favoritePackages: Set<String>): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val flags = PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }

            packages.mapNotNull { pkgInfo ->
                val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
                val baseSourceDir = appInfo.sourceDir ?: return@mapNotNull null
                val baseFile = File(baseSourceDir)
                if (!baseFile.exists()) return@mapNotNull null

                val splitDirs = appInfo.splitSourceDirs?.filter { File(it).exists() } ?: emptyList()
                val splitNames = appInfo.splitNames?.toList() ?: splitDirs.map {
                    File(it).nameWithoutExtension
                }
                val isSplit = splitDirs.isNotEmpty()
                val totalSize = baseFile.length() + splitDirs.sumOf { File(it).length() }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                val label = runCatching {
                    packageManager.getApplicationLabel(appInfo).toString().ifBlank { pkgInfo.packageName }
                }.getOrDefault(pkgInfo.packageName)

                val installerPkg = resolveInstallerPackage(pkgInfo.packageName)
                val installerDisplay = formatInstallerDisplay(installerPkg)
                val signingHash = extractSigningSha256(pkgInfo)
                val iconBmp = runCatching {
                    drawableToBitmap(packageManager.getApplicationIcon(appInfo), 96)
                }.getOrNull()

                val abis = buildList {
                    appInfo.nativeLibraryDir?.let { dir ->
                        val abiFolder = File(dir).name
                        if (abiFolder.isNotBlank()) add(abiFolder)
                    }
                    if (isEmpty()) addAll(Build.SUPPORTED_ABIS.take(2))
                }

                InstalledAppInfo(
                    packageName = pkgInfo.packageName,
                    label = label,
                    versionName = pkgInfo.versionName ?: "1.0",
                    versionCode = pkgInfo.longVersionCode,
                    minSdk = appInfo.minSdkVersion,
                    targetSdk = appInfo.targetSdkVersion,
                    isSystemApp = isSystem,
                    isSplitApk = isSplit,
                    baseApkPath = baseSourceDir,
                    splitSourceDirs = splitDirs,
                    splitNames = splitNames,
                    totalSizeBytes = totalSize,
                    firstInstallTime = pkgInfo.firstInstallTime,
                    lastUpdateTime = pkgInfo.lastUpdateTime,
                    installerPackageName = installerPkg,
                    installerDisplayName = installerDisplay,
                    signingSha256 = signingHash,
                    supportedAbis = abis,
                    iconBitmap = iconBmp,
                    isFavorite = favoritePackages.contains(pkgInfo.packageName)
                )
            }
        }

    private fun resolveInstallerPackage(packageName: String): String? {
        return runCatching {
            val sourceInfo = packageManager.getInstallSourceInfo(packageName)
            sourceInfo.installingPackageName ?: sourceInfo.initiatingPackageName
        }.getOrNull()
    }

    private fun formatInstallerDisplay(installerPkg: String?): String {
        return when (installerPkg) {
            "com.android.vending" -> "Google Play Store"
            "com.google.android.packageinstaller",
            "com.android.packageinstaller" -> "Package Installer (Sideload)"
            "com.shareapk.app", "com.example" -> "ShareAPK Built-in Installer"
            null -> "System / Preload / ADB"
            else -> installerPkg
        }
    }

    private fun extractSigningSha256(pkgInfo: PackageInfo): String {
        return runCatching {
            val signingInfo = pkgInfo.signingInfo ?: return@runCatching "N/A"
            val signers = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            val firstCert = signers?.firstOrNull()?.toByteArray() ?: return@runCatching "N/A"
            FileNameAndHashUtils.computeBytesSha256(firstCert)
                .chunked(2)
                .joinToString(":")
        }.getOrDefault("N/A")
    }

    fun drawableToBitmap(drawable: Drawable, maxDimension: Int = 112): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            if (b.width <= maxDimension && b.height <= maxDimension) return b
        }
        val width = drawable.intrinsicWidth.coerceIn(1, maxDimension)
        val height = drawable.intrinsicHeight.coerceIn(1, maxDimension)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Prepares a single app for sharing or local Wi-Fi transfer inside cacheDir/shared.
     * - Single APK: copies base.apk to a sanitized .apk file.
     * - Split APK: bundles base.apk + all split_*.apk into a single .apks ZIP archive.
     */
    suspend fun prepareAppInCache(
        app: InstalledAppInfo,
        namingFormat: FileNamingFormat
    ): File = withContext(Dispatchers.IO) {
        val fileName = FileNameAndHashUtils.buildExportFileName(app, namingFormat)
        val outFile = File(sharedCacheDir, fileName)

        if (!app.isSplitApk) {
            copyFileStream(File(app.baseApkPath), outFile)
        } else {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
                // Add base.apk
                val baseFile = File(app.baseApkPath)
                addFileToZip(zos, baseFile, "base.apk")
                // Add all split APKs
                app.splitSourceDirs.forEachIndexed { index, splitPath ->
                    val splitFile = File(splitPath)
                    if (splitFile.exists()) {
                        val entryName = splitFile.name.let { name ->
                            if (name.endsWith(".apk") && name != "base.apk") name
                            else "split_config_${index}.apk"
                        }
                        addFileToZip(zos, splitFile, entryName)
                    }
                }
            }
        }
        outFile
    }

    private fun copyFileStream(source: File, dest: File) {
        BufferedInputStream(FileInputStream(source)).use { input ->
            BufferedOutputStream(FileOutputStream(dest)).use { output ->
                input.copyTo(output, 32 * 1024)
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName).apply {
            time = file.lastModified()
            size = file.length()
        }
        zos.putNextEntry(entry)
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.copyTo(zos, 32 * 1024)
        }
        zos.closeEntry()
    }

    /**
     * Creates a zero-permission FileProvider share Intent (ACTION_SEND or ACTION_SEND_MULTIPLE)
     * with Intent.FLAG_GRANT_READ_URI_PERMISSION and MIME application/vnd.android.package-archive.
     */
    fun createShareChooserIntent(files: List<File>, chooserTitle: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(context, authority, it) })

        val shareIntent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/vnd.android.package-archive"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        return Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Saves a prepared APK or APKS file to MediaStore.Downloads using IS_PENDING = 1 -> 0.
     * Requires zero storage permissions on Android 12+ (minSdk 31).
     */
    suspend fun saveAppToDownloads(
        app: InstalledAppInfo,
        namingFormat: FileNamingFormat
    ): Pair<Uri, String> = withContext(Dispatchers.IO) {
        val preparedFile = prepareAppInCache(app, namingFormat)
        val sha256 = FileNameAndHashUtils.computeFileSha256(preparedFile)
        val mimeType = if (app.isSplitApk) {
            "application/octet-stream"
        } else {
            "application/vnd.android.package-archive"
        }

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, preparedFile.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ShareAPK")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore.Downloads entry")

        resolver.openOutputStream(itemUri)?.use { outputStream ->
            BufferedInputStream(FileInputStream(preparedFile)).use { inputStream ->
                inputStream.copyTo(outputStream, 32 * 1024)
            }
        } ?: throw IllegalStateException("Failed to open output stream for Downloads")

        val doneValues = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        resolver.update(itemUri, doneValues, null, null)
        Pair(itemUri, sha256)
    }

    /**
     * Multi-App Quick Backup to Downloads:
     * Bundles all selected apps into a single timestamped ZIP in MediaStore.Downloads
     * containing each app's .apk / split set in its own subfolder plus a manifest.json.
     */
    suspend fun createQuickMultiAppZipInDownloads(
        apps: List<InstalledAppInfo>,
        onProgress: (currentIndex: Int, totalCount: Int, currentAppLabel: String) -> Unit
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val zipFileName = "ShareAPK_MultiBackup_${timestamp}.zip"
        val tempZipFile = File(sharedCacheDir, zipFileName)

        val manifestAppsArray = JSONArray()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile))).use { zos ->
            apps.forEachIndexed { index, app ->
                coroutineContext.ensureActive()
                onProgress(index + 1, apps.size, app.label)

                val folderPrefix = FileNameAndHashUtils.sanitizeFileName(app.packageName, 70)
                val baseFile = File(app.baseApkPath)
                val baseSha = FileNameAndHashUtils.computeFileSha256(baseFile)

                addFileToZip(zos, baseFile, "$folderPrefix/base.apk")

                val filesJson = JSONArray().apply {
                    put(JSONObject().apply {
                        put("fileName", "base.apk")
                        put("sizeBytes", baseFile.length())
                        put("sha256", baseSha)
                    })
                }

                app.splitSourceDirs.forEachIndexed { splitIdx, splitPath ->
                    val splitFile = File(splitPath)
                    if (splitFile.exists()) {
                        val splitName = splitFile.name.let {
                            if (it.endsWith(".apk") && it != "base.apk") it else "split_$splitIdx.apk"
                        }
                        val splitSha = FileNameAndHashUtils.computeFileSha256(splitFile)
                        addFileToZip(zos, splitFile, "$folderPrefix/$splitName")
                        filesJson.put(JSONObject().apply {
                            put("fileName", splitName)
                            put("sizeBytes", splitFile.length())
                            put("sha256", splitSha)
                        })
                    }
                }

                manifestAppsArray.put(JSONObject().apply {
                    put("label", app.label)
                    put("packageName", app.packageName)
                    put("versionName", app.versionName)
                    put("versionCode", app.versionCode)
                    put("isSplitApk", app.isSplitApk)
                    put("files", filesJson)
                })
            }

            val rootManifest = JSONObject().apply {
                put("schemaVersion", 1)
                put("createdAt", timestamp)
                put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("appCount", apps.size)
                put("apps", manifestAppsArray)
            }
            val manifestBytes = rootManifest.toString(2).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestBytes)
            zos.closeEntry()
        }

        val zipSha256 = FileNameAndHashUtils.computeFileSha256(tempZipFile)
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipFileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ShareAPK")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Could not create ZIP in Downloads")

        resolver.openOutputStream(uri)?.use { out ->
            BufferedInputStream(FileInputStream(tempZipFile)).use { input ->
                input.copyTo(out, 32 * 1024)
            }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        tempZipFile.delete()

        Pair(zipFileName, zipSha256)
    }

    fun getSharedCacheSizeBytes(): Long {
        val dir = File(context.cacheDir, "shared")
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearSharedCache(): Long = withContext(Dispatchers.IO) {
        val before = getSharedCacheSizeBytes()
        val dir = File(context.cacheDir, "shared")
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
        before
    }
}
