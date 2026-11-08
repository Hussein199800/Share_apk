package com.example.backup

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.BackupAppEntry
import com.example.data.model.BackupFileEntry
import com.example.data.model.BackupManifest
import com.example.data.model.InstalledAppInfo
import com.example.data.model.RestoreAppStatus
import com.example.data.model.RestoreCandidateApp
import com.example.util.FileNameAndHashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SpaceCheckResult(
    val availableBytes: Long,
    val requiredBytes: Long,
    val isSufficient: Boolean
)

data class BackupVerificationReport(
    val totalFilesChecked: Int,
    val verifiedMatchCount: Int,
    val corruptOrMissingFiles: List<String>
) {
    val isAllVerified: Boolean
        get() = corruptOrMissingFiles.isEmpty() && totalFilesChecked > 0
}

class PreFlashBackupManager(private val context: Context) {

    /**
     * Checks available free space at the selected SAF destination (or fallback internal storage StatFs)
     * before starting a full Pre-Flash Backup.
     */
    fun checkDestinationFreeSpace(treeUri: Uri?, requiredBytes: Long): SpaceCheckResult {
        val available = runCatching {
            if (treeUri != null) {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                context.contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
                    val stats = android.system.Os.fstatvfs(pfd.fileDescriptor)
                    stats.f_bavail * stats.f_frsize
                }
            } else null
        }.getOrNull() ?: runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes
        }.getOrDefault(Long.MAX_VALUE)

        // Add 5% safety buffer
        val neededWithBuffer = (requiredBytes * 1.05).toLong()
        return SpaceCheckResult(
            availableBytes = available,
            requiredBytes = requiredBytes,
            isSufficient = available >= neededWithBuffer
        )
    }

    /**
     * Executes the full Pre-Flash Backup inside the user-selected SAF DocumentFile tree:
     * - Creates or reuses `ShareAPK_Backup_YYYY-MM-DD_HHmm/` (or existing folder if resuming)
     * - Copies ShareAPK's own base APK as `00_Install_ShareAPK_First.apk`
     * - Writes `<packageName>/icon.png`, `base.apk`, and all `split_*.apk` files
     * - If resuming, skips any file that already exists in `<packageName>/` with matching SHA-256
     * - Writes `manifest.json` and self-contained `apps_list.html`
     */
    suspend fun executePreFlashBackup(
        rootTreeUri: Uri,
        appsToBackup: List<InstalledAppInfo>,
        resumeFolderName: String? = null,
        compress: Boolean = false,
        onProgress: (currentIdx: Int, totalCount: Int, currentApp: String, skippedCount: Int) -> Unit
    ): Pair<DocumentFile, BackupManifest> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, rootTreeUri)
            ?: throw IllegalStateException("Cannot access selected destination folder via SAF")

        val folderName = resumeFolderName ?: run {
            val ts = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
            "ShareAPK_Backup_$ts"
        }

        val backupDir = rootDoc.findFile(folderName)?.takeIf { it.isDirectory }
            ?: rootDoc.createDirectory(folderName)
            ?: throw IllegalStateException("Unable to create $folderName inside destination")

        // 1. Copy ShareAPK's own APK as 00_Install_ShareAPK_First.apk in root of backup folder
        val selfSourceDir = context.applicationInfo.sourceDir
        if (!selfSourceDir.isNullOrBlank()) {
            val selfFile = File(selfSourceDir)
            if (selfFile.exists()) {
                val selfTargetName = "00_Install_ShareAPK_First.apk"
                val existingSelf = backupDir.findFile(selfTargetName)
                if (existingSelf == null || existingSelf.length() != selfFile.length()) {
                    existingSelf?.delete()
                    val createdSelf = backupDir.createFile(
                        "application/vnd.android.package-archive",
                        selfTargetName
                    )
                    if (createdSelf != null) {
                        copyLocalFileToDocFile(selfFile, createdSelf)
                    }
                }
            }
        }

        val manifestEntries = mutableListOf<BackupAppEntry>()
        var skippedFilesTotal = 0

        if (compress) {
            // A ZIP can't be resumed half-written: an interrupted archive is rebuilt from scratch.
            backupDir.findFile(BackupArchive.NAME)?.delete()
            val zipDoc = backupDir.createFile("application/zip", BackupArchive.NAME)
                ?: throw IllegalStateException("Unable to create ${BackupArchive.NAME}")
            context.contentResolver.openOutputStream(zipDoc.uri)?.use { raw ->
                BackupArchive.open(java.io.BufferedOutputStream(raw, 256 * 1024)).use { zos ->
                    appsToBackup.forEachIndexed { index, app ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, appsToBackup.size, app.label, 0)
                        val folder = FileNameAndHashUtils.sanitizeFileName(app.packageName, 90)
                        app.iconBitmap?.let { bmp ->
                            val png = java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }.toByteArray()
                            BackupArchive.addEntry(zos, BackupArchive.entryName(folder, "icon.png"), png.inputStream())
                        }
                        val files = mutableListOf<BackupFileEntry>()
                        appSourceFiles(app).forEach { (name, src) ->
                            coroutineContext.ensureActive()
                            val (size, sha) = BufferedInputStream(FileInputStream(src), 256 * 1024).use {
                                BackupArchive.addEntry(zos, BackupArchive.entryName(folder, name), it)
                            }
                            files += BackupFileEntry(fileName = name, sizeBytes = size, sha256 = sha)
                        }
                        manifestEntries += appEntry(app, files)
                    }
                }
            } ?: throw IllegalStateException("Cannot open ${BackupArchive.NAME} for writing")
            val manifest = buildManifest(manifestEntries, BackupArchive.NAME)
            writeTextToDocFile(backupDir, "manifest.json", "application/json", serializeManifestToJson(manifest))
            writeTextToDocFile(backupDir, "apps_list.html", "text/html", buildSelfContainedHtmlReport(manifest))
            return@withContext Pair(backupDir, manifest)
        }

        // 2. Back up each selected application into <packageName>/
        appsToBackup.forEachIndexed { index, app ->
            coroutineContext.ensureActive()
            onProgress(index + 1, appsToBackup.size, app.label, skippedFilesTotal)

            val safePkgFolder = FileNameAndHashUtils.sanitizeFileName(app.packageName, 90)
            val appDir = backupDir.findFile(safePkgFolder)?.takeIf { it.isDirectory }
                ?: backupDir.createDirectory(safePkgFolder)
                ?: throw IllegalStateException("Failed to create directory for ${app.packageName}")

            // Save icon.png
            app.iconBitmap?.let { bmp ->
                if (appDir.findFile("icon.png") == null) {
                    appDir.createFile("image/png", "icon.png")?.let { iconDoc ->
                        context.contentResolver.openOutputStream(iconDoc.uri)?.use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }
                    }
                }
            }

            val fileEntries = mutableListOf<BackupFileEntry>()

            // Copy base.apk (with SHA-256 resume check)
            val baseSource = File(app.baseApkPath)
            if (baseSource.exists()) {
                val baseSha = FileNameAndHashUtils.computeFileSha256(baseSource)
                val wasSkipped = copyOrSkipIfMatchingSha(appDir, baseSource, "base.apk", baseSha)
                if (wasSkipped) skippedFilesTotal++
                fileEntries.add(
                    BackupFileEntry(
                        fileName = "base.apk",
                        sizeBytes = baseSource.length(),
                        sha256 = baseSha
                    )
                )
            }

            // Copy all split_*.apk files if split
            app.splitSourceDirs.forEachIndexed { splitIdx, splitPath ->
                coroutineContext.ensureActive()
                val splitFile = File(splitPath)
                if (splitFile.exists()) {
                    val splitName = splitFile.name.let { raw ->
                        if (raw.endsWith(".apk") && raw != "base.apk") {
                            FileNameAndHashUtils.sanitizeFileName(raw, 64)
                        } else {
                            "split_config_${splitIdx}.apk"
                        }
                    }
                    val splitSha = FileNameAndHashUtils.computeFileSha256(splitFile)
                    val wasSkipped = copyOrSkipIfMatchingSha(appDir, splitFile, splitName, splitSha)
                    if (wasSkipped) skippedFilesTotal++
                    fileEntries.add(
                        BackupFileEntry(
                            fileName = splitName,
                            sizeBytes = splitFile.length(),
                            sha256 = splitSha
                        )
                    )
                }
            }

            val playUrl = if (app.installerPackageName == "com.android.vending") {
                app.googlePlayUrl
            } else {
                app.googlePlayUrl
            }

            manifestEntries.add(
                BackupAppEntry(
                    label = app.label,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    versionCode = app.versionCode,
                    minSdk = app.minSdk,
                    targetSdk = app.targetSdk,
                    isSplitApk = app.isSplitApk,
                    supportedAbis = app.supportedAbis,
                    installerPackageName = app.installerPackageName,
                    googlePlayUrl = playUrl,
                    totalSizeBytes = app.totalSizeBytes,
                    files = fileEntries
                )
            )
        }

        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val manifest = BackupManifest(
            schemaVersion = 1,
            backupDateIso = isoNow,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidRelease = Build.VERSION.RELEASE ?: "12+",
            sdkInt = Build.VERSION.SDK_INT,
            appCount = manifestEntries.size,
            totalSizeBytes = manifestEntries.sumOf { it.totalSizeBytes },
            apps = manifestEntries
        )

        // 3. Write manifest.json
        val manifestJsonString = serializeManifestToJson(manifest)
        writeTextToDocFile(backupDir, "manifest.json", "application/json", manifestJsonString)

        // 4. Write apps_list.html
        val htmlReport = buildSelfContainedHtmlReport(manifest)
        writeTextToDocFile(backupDir, "apps_list.html", "text/html", htmlReport)

        Pair(backupDir, manifest)
    }

    /** (name inside the backup, source file) for base.apk and every split, as the folder backup names them. */
    private fun appSourceFiles(app: InstalledAppInfo): List<Pair<String, File>> {
        val out = mutableListOf<Pair<String, File>>()
        File(app.baseApkPath).takeIf { it.exists() }?.let { out += "base.apk" to it }
        app.splitSourceDirs.forEachIndexed { i, path ->
            val f = File(path)
            if (f.exists()) {
                val name = if (f.name.endsWith(".apk") && f.name != "base.apk") FileNameAndHashUtils.sanitizeFileName(f.name, 64) else "split_config_$i.apk"
                out += name to f
            }
        }
        return out
    }

    private fun appEntry(app: InstalledAppInfo, files: List<BackupFileEntry>) = BackupAppEntry(
        label = app.label, packageName = app.packageName, versionName = app.versionName, versionCode = app.versionCode,
        minSdk = app.minSdk, targetSdk = app.targetSdk, isSplitApk = app.isSplitApk, supportedAbis = app.supportedAbis,
        installerPackageName = app.installerPackageName, googlePlayUrl = app.googlePlayUrl,
        totalSizeBytes = app.totalSizeBytes, files = files
    )

    private fun buildManifest(entries: List<BackupAppEntry>, archive: String?) = BackupManifest(
        schemaVersion = 1,
        backupDateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidRelease = Build.VERSION.RELEASE ?: "12+",
        sdkInt = Build.VERSION.SDK_INT,
        appCount = entries.size,
        totalSizeBytes = entries.sumOf { it.totalSizeBytes },
        apps = entries,
        archive = archive
    )

    /**
     * Opens a compressed backup's archive. Uses the real file behind the SAF descriptor when there is
     * one (fast random access), otherwise falls back to streaming. Caller closes it.
     */
    fun openArchive(backupDir: DocumentFile, archiveName: String): BackupArchive.Reader {
        val doc = backupDir.findFile(archiveName) ?: throw IllegalStateException("$archiveName missing")
        val pfd = runCatching { context.contentResolver.openFileDescriptor(doc.uri, "r") }.getOrNull()
        val file = pfd?.let { File("/proc/self/fd/${it.fd}") }
        return BackupArchive.Reader.of(file, openStream = {
            context.contentResolver.openInputStream(doc.uri) ?: throw IllegalStateException("Cannot read $archiveName")
        }, onClose = { pfd?.close() })
    }

    /**
     * Resume helper: If `targetName` already exists in `parentDir`, has the exact same byte length,
     * and its SHA-256 matches `expectedSha256`, skips re-copying and returns true!
     */
    private fun copyOrSkipIfMatchingSha(
        parentDir: DocumentFile,
        sourceFile: File,
        targetName: String,
        expectedSha256: String
    ): Boolean {
        val existing = parentDir.findFile(targetName)
        if (existing != null && existing.length() == sourceFile.length()) {
            val existingSha = runCatching {
                context.contentResolver.openInputStream(existing.uri)?.use { input ->
                    FileNameAndHashUtils.computeStreamSha256(input)
                }
            }.getOrNull()
            if (existingSha != null && existingSha.equals(expectedSha256, ignoreCase = true)) {
                return true // Skipped!
            }
            existing.delete()
        }

        val created = parentDir.createFile("application/vnd.android.package-archive", targetName)
            ?: throw IllegalStateException("Cannot create $targetName in backup folder")
        copyLocalFileToDocFile(sourceFile, created)
        return false
    }

    private fun copyLocalFileToDocFile(sourceFile: File, destDoc: DocumentFile) {
        context.contentResolver.openOutputStream(destDoc.uri)?.use { out ->
            BufferedInputStream(FileInputStream(sourceFile)).use { input ->
                input.copyTo(out, 32 * 1024)
            }
        } ?: throw IllegalStateException("Cannot open output stream for ${destDoc.name}")
    }

    private fun writeTextToDocFile(
        parentDir: DocumentFile,
        fileName: String,
        mimeType: String,
        content: String
    ) {
        parentDir.findFile(fileName)?.delete()
        val doc = parentDir.createFile(mimeType, fileName)
            ?: throw IllegalStateException("Unable to create $fileName")
        context.contentResolver.openOutputStream(doc.uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }

    /**
     * Post-Write SHA-256 Verification:
     * Re-reads every written APK file in the backup directory and verifies its SHA-256 against manifest.json.
     */
    suspend fun verifyBackupFolderSha256(
        backupDir: DocumentFile,
        manifest: BackupManifest,
        onFileProgress: (checkedCount: Int, currentFile: String) -> Unit
    ): BackupVerificationReport = withContext(Dispatchers.IO) {
        var totalChecked = 0
        var matched = 0
        val corruptList = mutableListOf<String>()

        manifest.archive?.let { archive ->
            // Re-read the whole archive: every entry's CRC-32, size and SHA-256 must match.
            val expected = manifest.apps.flatMap { app ->
                val folder = FileNameAndHashUtils.sanitizeFileName(app.packageName, 90)
                app.files.map { BackupArchive.entryName(folder, it.fileName) to (it.sizeBytes to it.sha256) }
            }.toMap()
            val doc = backupDir.findFile(archive)
            val problems = if (doc == null) listOf("$archive (missing)") else
                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                    BackupArchive.verify(java.io.BufferedInputStream(input, 256 * 1024), expected) { name ->
                        totalChecked++
                        onFileProgress(totalChecked, name)
                    }
                } ?: listOf("$archive (unreadable)")
            return@withContext BackupVerificationReport(
                totalFilesChecked = expected.size,
                verifiedMatchCount = expected.size - problems.count { it.contains('/') },
                corruptOrMissingFiles = problems
            )
        }

        for (appEntry in manifest.apps) {
            coroutineContext.ensureActive()
            val safeFolder = FileNameAndHashUtils.sanitizeFileName(appEntry.packageName, 90)
            val appDocDir = backupDir.findFile(safeFolder)
            if (appDocDir == null || !appDocDir.isDirectory) {
                corruptList.add("${appEntry.packageName} (missing directory)")
                continue
            }
            for (fileEntry in appEntry.files) {
                coroutineContext.ensureActive()
                totalChecked++
                onFileProgress(totalChecked, "${appEntry.label} / ${fileEntry.fileName}")
                val fileDoc = appDocDir.findFile(fileEntry.fileName)
                if (fileDoc == null || !fileDoc.exists()) {
                    corruptList.add("${appEntry.packageName}/${fileEntry.fileName} (missing)")
                    continue
                }
                val actualSha = runCatching {
                    context.contentResolver.openInputStream(fileDoc.uri)?.use { input ->
                        FileNameAndHashUtils.computeStreamSha256(input)
                    }
                }.getOrNull()

                if (actualSha != null && actualSha.equals(fileEntry.sha256, ignoreCase = true)) {
                    matched++
                } else {
                    corruptList.add("${appEntry.packageName}/${fileEntry.fileName} (SHA-256 mismatch)")
                }
            }
        }

        BackupVerificationReport(
            totalFilesChecked = totalChecked,
            verifiedMatchCount = matched,
            corruptOrMissingFiles = corruptList
        )
    }

    /**
     * Flow B: Parses `manifest.json` from a selected `ShareAPK_Backup_*` folder (or child folder)
     * and compares each backed-up app against currently installed packages on the device.
     */
    suspend fun parseBackupFolderForRestore(
        selectedTreeUri: Uri
    ): Triple<DocumentFile, BackupManifest, List<RestoreCandidateApp>> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, selectedTreeUri)
            ?: throw IllegalStateException("Cannot open selected backup directory")

        // Support picking either the ShareAPK_Backup_* directory directly OR its parent directory
        val targetBackupDir = if (rootDoc.findFile("manifest.json") != null) {
            rootDoc
        } else {
            rootDoc.listFiles()
                .filter { it.isDirectory && (it.name?.startsWith("ShareAPK_Backup_") == true) }
                .maxByOrNull { it.lastModified() }
                ?.takeIf { it.findFile("manifest.json") != null }
                ?: throw IllegalStateException("manifest.json not found in selected folder")
        }

        val manifestDoc = targetBackupDir.findFile("manifest.json")
            ?: throw IllegalStateException("manifest.json missing in ${targetBackupDir.name}")

        val rawJson = context.contentResolver.openInputStream(manifestDoc.uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Failed to read manifest.json")

        val manifest = parseManifestFromJson(rawJson)
        val pm = context.packageManager
        val deviceAbis = Build.SUPPORTED_ABIS.toSet()

        // Compressed backup: list the archive once (sizes come from its central directory when available).
        val archiveSizes: Map<String, Long>? = manifest.archive?.let { name ->
            runCatching {
                openArchive(targetBackupDir, name).use { r -> r.names().associateWith { n -> r.size(n) ?: -1L } }
            }.getOrDefault(emptyMap())
        }

        val candidates = manifest.apps.map { entry ->
            val safeFolder = FileNameAndHashUtils.sanitizeFileName(entry.packageName, 90)
            val filesExist = if (archiveSizes != null) {
                entry.files.all { f ->
                    val size = archiveSizes[BackupArchive.entryName(safeFolder, f.fileName)]
                    size != null && (size < 0 || size == f.sizeBytes)
                }
            } else {
                val appFolder = targetBackupDir.findFile(safeFolder)
                appFolder != null && entry.files.all { f ->
                    val doc = appFolder.findFile(f.fileName)
                    doc != null && doc.length() == f.sizeBytes
                }
            }

            val installedInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(entry.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(entry.packageName, 0)
                }
            }.getOrNull()

            val sdkIncompatible = Build.VERSION.SDK_INT < entry.minSdk
            val abiIncompatible = entry.supportedAbis.isNotEmpty() &&
                entry.supportedAbis.none { it in deviceAbis || it.startsWith("arm") || it.startsWith("x86") }

            val status: RestoreAppStatus
            val incompatibilityReason: String?

            when {
                sdkIncompatible -> {
                    status = RestoreAppStatus.INCOMPATIBLE
                    incompatibilityReason = "Requires Android SDK ${entry.minSdk} (Device is SDK ${Build.VERSION.SDK_INT})"
                }
                abiIncompatible -> {
                    status = RestoreAppStatus.INCOMPATIBLE
                    incompatibilityReason = "ABI mismatch (${entry.supportedAbis.joinToString()})"
                }
                installedInfo == null -> {
                    status = RestoreAppStatus.NOT_INSTALLED
                    incompatibilityReason = null
                }
                installedInfo.longVersionCode < entry.versionCode -> {
                    status = RestoreAppStatus.OLDER_VERSION_INSTALLED
                    incompatibilityReason = null
                }
                else -> {
                    status = RestoreAppStatus.ALREADY_INSTALLED
                    incompatibilityReason = null
                }
            }

            val selectedByDefault = (status == RestoreAppStatus.NOT_INSTALLED ||
                status == RestoreAppStatus.OLDER_VERSION_INSTALLED) && filesExist

            RestoreCandidateApp(
                entry = entry,
                status = status,
                installedVersionName = installedInfo?.versionName,
                installedVersionCode = installedInfo?.longVersionCode,
                isSelected = selectedByDefault,
                integrityOk = filesExist,
                incompatibilityReason = incompatibilityReason
            )
        }

        Triple(targetBackupDir, manifest, candidates)
    }

    fun serializeManifestToJson(manifest: BackupManifest): String {
        val appsJson = JSONArray()
        manifest.apps.forEach { app ->
            val filesJson = JSONArray()
            app.files.forEach { file ->
                filesJson.put(JSONObject().apply {
                    put("fileName", file.fileName)
                    put("sizeBytes", file.sizeBytes)
                    put("sha256", file.sha256)
                })
            }
            val abisJson = JSONArray(app.supportedAbis)
            appsJson.put(JSONObject().apply {
                put("label", app.label)
                put("packageName", app.packageName)
                put("versionName", app.versionName)
                put("versionCode", app.versionCode)
                put("minSdk", app.minSdk)
                put("targetSdk", app.targetSdk)
                put("isSplitApk", app.isSplitApk)
                put("supportedAbis", abisJson)
                put("installerPackageName", app.installerPackageName ?: JSONObject.NULL)
                put("googlePlayUrl", app.googlePlayUrl ?: JSONObject.NULL)
                put("totalSizeBytes", app.totalSizeBytes)
                put("files", filesJson)
            })
        }

        return JSONObject().apply {
            put("schemaVersion", manifest.schemaVersion)
            put("backupDateIso", manifest.backupDateIso)
            put("deviceModel", manifest.deviceModel)
            put("androidRelease", manifest.androidRelease)
            put("sdkInt", manifest.sdkInt)
            put("appCount", manifest.appCount)
            put("totalSizeBytes", manifest.totalSizeBytes)
            manifest.archive?.let { put("archive", it) }
            put("apps", appsJson)
        }.toString(2)
    }

    fun parseManifestFromJson(jsonString: String): BackupManifest {
        val root = JSONObject(jsonString)
        val appsArray = root.optJSONArray("apps") ?: JSONArray()
        val apps = mutableListOf<BackupAppEntry>()

        for (i in 0 until appsArray.length()) {
            val obj = appsArray.getJSONObject(i)
            val filesArray = obj.optJSONArray("files") ?: JSONArray()
            val files = mutableListOf<BackupFileEntry>()
            for (j in 0 until filesArray.length()) {
                val f = filesArray.getJSONObject(j)
                files.add(
                    BackupFileEntry(
                        fileName = f.optString("fileName", "base.apk"),
                        sizeBytes = f.optLong("sizeBytes", 0L),
                        sha256 = f.optString("sha256", "")
                    )
                )
            }
            val abisArray = obj.optJSONArray("supportedAbis") ?: JSONArray()
            val abis = (0 until abisArray.length()).map { abisArray.getString(it) }

            apps.add(
                BackupAppEntry(
                    label = obj.optString("label", "App"),
                    packageName = obj.optString("packageName", ""),
                    versionName = obj.optString("versionName", "1.0"),
                    versionCode = obj.optLong("versionCode", 1L),
                    minSdk = obj.optInt("minSdk", 31),
                    targetSdk = obj.optInt("targetSdk", 35),
                    isSplitApk = obj.optBoolean("isSplitApk", files.size > 1),
                    supportedAbis = abis,
                    installerPackageName = obj.optString("installerPackageName").takeIf { it.isNotBlank() && it != "null" },
                    googlePlayUrl = obj.optString("googlePlayUrl").takeIf { it.isNotBlank() && it != "null" },
                    totalSizeBytes = obj.optLong("totalSizeBytes", files.sumOf { it.sizeBytes }),
                    files = files
                )
            )
        }

        return BackupManifest(
            schemaVersion = root.optInt("schemaVersion", 1),
            backupDateIso = root.optString("backupDateIso", ""),
            deviceModel = root.optString("deviceModel", "Android Device"),
            androidRelease = root.optString("androidRelease", "12+"),
            sdkInt = root.optInt("sdkInt", 31),
            appCount = root.optInt("appCount", apps.size),
            totalSizeBytes = root.optLong("totalSizeBytes", apps.sumOf { it.totalSizeBytes }),
            apps = apps,
            archive = root.optString("archive").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun buildSelfContainedHtmlReport(manifest: BackupManifest): String {
        val rows = manifest.apps.joinToString("\n") { app ->
            val typeBadge = if (app.isSplitApk) "Split (.apks - ${app.files.size} files)" else "Single (.apk)"
            val shaFirst = app.files.firstOrNull()?.sha256?.take(16) ?: "-"
            """
            <tr>
              <td><strong>${app.label}</strong></td>
              <td class="ltr">${app.packageName}</td>
              <td class="ltr">v${app.versionName} (${app.versionCode})</td>
              <td>$typeBadge</td>
              <td class="ltr">${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)}</td>
              <td class="ltr">$shaFirst...</td>
              <td><a href="${app.googlePlayUrl ?: "#"}" target="_blank">Google Play</a></td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="utf-8" />
          <title>تقرير النسخة الاحتياطية — شارك أبك | ShareAPK Pre-Flash Report</title>
          <style>
            body { background:#2B1B14; color:#FFF8F0; font-family:system-ui,sans-serif; padding:24px; }
            h1 { color:#C9A227; margin-bottom:4px; }
            .summary { background:#38241C; padding:16px; border-radius:12px; border:1px solid #6D4C41; margin:16px 0; }
            table { width:100%; border-collapse:collapse; background:#38241C; border-radius:12px; overflow:hidden; }
            th, td { padding:12px; border-bottom:1px solid rgba(215,204,200,0.18); text-align:right; }
            th { background:#4E342E; color:#C9A227; }
            .ltr { direction:ltr; unicode-bidi:embed; font-family:monospace; }
            a { color:#C9A227; }
          </style>
        </head>
        <body>
          <h1>تقرير النسخة الاحتياطية الشاملة — شارك أبك | ShareAPK</h1>
          <div class="summary">
            <p><strong>تاريخ النسخ / Date:</strong> <span class="ltr">${manifest.backupDateIso}</span></p>
            <p><strong>الجهاز / Device:</strong> <span class="ltr">${manifest.deviceModel} (Android ${manifest.androidRelease}, SDK ${manifest.sdkInt})</span></p>
            <p><strong>عدد التطبيقات / Apps Count:</strong> <span class="ltr">${manifest.appCount}</span> • <strong>الحجم الكلي / Total Size:</strong> <span class="ltr">${FileNameAndHashUtils.formatBytes(manifest.totalSizeBytes)}</span></p>
            <p><strong>ملاحظة:</strong> ثبّت ملف <code>00_Install_ShareAPK_First.apk</code> أولاً بعد الفورمات لاسترجاع جميع التطبيقات تلقائياً.</p>
          </div>
          <table>
            <thead>
              <tr>
                <th>التطبيق / App</th>
                <th>الحزمة / Package</th>
                <th>الإصدار / Version</th>
                <th>النوع / Type</th>
                <th>الحجم / Size</th>
                <th>SHA-256</th>
                <th>المتجر / Store</th>
              </tr>
            </thead>
            <tbody>
              $rows
            </tbody>
          </table>
        </body>
        </html>
        """.trimIndent()
    }
}
