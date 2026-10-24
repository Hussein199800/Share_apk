package com.example.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.PreInstallValidationInfo
import com.example.data.repository.ApkRepository
import com.example.util.FileNameAndHashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class PackageInstallerManager(
    private val context: Context,
    private val apkRepository: ApkRepository
) {
    private val packageManager: PackageManager = context.packageManager

    fun canRequestPackageInstalls(): Boolean {
        return packageManager.canRequestPackageInstalls()
    }

    fun createUnknownSourcesSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Pre-Install Security & Validation Inspector:
     * 1. Verifies ZIP header integrity (PK\x03\x04)
     * 2. Rejects any ZIP entry containing path traversal ("../", "..\\", or leading "/")
     * 3. Detects whether the URI is a single .apk or a multi-APK .apks/.zip bundle
     * 4. Parses metadata via PackageManager.getPackageArchiveInfo and computes SHA-256
     */
    suspend fun inspectExternalPackage(uri: Uri): PreInstallValidationInfo = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "external_package.apk"
        val tempInspectDir = File(context.cacheDir, "inspect_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        // Copy URI to a temporary file to verify header, compute SHA-256, and inspect entries
        val rawTempFile = File(tempInspectDir, "raw_input.bin")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(rawTempFile).use { output ->
                input.copyTo(output, 32 * 1024)
            }
        } ?: return@withContext invalidPackage(uri, displayName, "Unable to open input stream for URI")

        val fileSize = rawTempFile.length()
        if (fileSize < 4L) {
            return@withContext invalidPackage(uri, displayName, "File is empty or truncated (< 4 bytes)")
        }

        // 1. Verify ZIP magic header (0x50 0x4B 0x03 0x04)
        val header = ByteArray(4)
        FileInputStream(rawTempFile).use { it.read(header) }
        val isZipHeader = header[0] == 0x50.toByte() &&
            header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() &&
            header[3] == 0x04.toByte()

        if (!isZipHeader) {
            return@withContext invalidPackage(
                uri,
                displayName,
                "Invalid ZIP/APK header signature (Expected PK\\x03\\x04)"
            )
        }

        val sha256 = FileNameAndHashUtils.computeFileSha256(rawTempFile)

        // 2. Scan ZIP entries for path traversal and detect single APK vs .apks bundle
        var hasAndroidManifestAtRoot = false
        val apkEntries = mutableListOf<String>()

        ZipInputStream(BufferedInputStream(FileInputStream(rawTempFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                // Strict path traversal rejection
                if (name.contains("../") || name.contains("..\\") || name.startsWith("/")) {
                    return@withContext invalidPackage(
                        uri = uri,
                        fileName = displayName,
                        reason = "Security Alert: Rejected archive entry with path traversal ($name)",
                        sha256 = sha256,
                        size = fileSize
                    )
                }
                if (name == "AndroidManifest.xml") {
                    hasAndroidManifestAtRoot = true
                }
                if (!entry.isDirectory && name.lowercase().endsWith(".apk")) {
                    apkEntries.add(name)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val isApksBundle = !hasAndroidManifestAtRoot && apkEntries.isNotEmpty()
        if (!hasAndroidManifestAtRoot && apkEntries.isEmpty()) {
            return@withContext invalidPackage(
                uri = uri,
                fileName = displayName,
                reason = "Archive contains neither AndroidManifest.xml nor any .apk split entries",
                sha256 = sha256,
                size = fileSize
            )
        }

        // 3. Extract base.apk (if .apks bundle) or use rawTempFile (if single .apk) for getPackageArchiveInfo
        val baseApkForParsing = if (isApksBundle) {
            val extractedBase = File(tempInspectDir, "extracted_base.apk")
            val targetEntryName = apkEntries.firstOrNull {
                it.equals("base.apk", ignoreCase = true) || it.endsWith("/base.apk", ignoreCase = true)
            } ?: apkEntries.first()

            ZipInputStream(BufferedInputStream(FileInputStream(rawTempFile))).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    if (e.name == targetEntryName) {
                        FileOutputStream(extractedBase).use { out ->
                            zis.copyTo(out, 32 * 1024)
                        }
                        break
                    }
                    zis.closeEntry()
                    e = zis.nextEntry
                }
            }
            extractedBase
        } else {
            rawTempFile
        }

        val archiveInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                baseApkForParsing.absolutePath,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(baseApkForParsing.absolutePath, 0)
        }

        val appInfo = archiveInfo?.applicationInfo
        if (archiveInfo == null || appInfo == null) {
            return@withContext invalidPackage(
                uri = uri,
                fileName = displayName,
                reason = "Unable to parse Android package metadata from archive",
                sha256 = sha256,
                size = fileSize
            )
        }

        // Point sourceDir & publicSourceDir to the file so PackageManager can load icon & label
        appInfo.sourceDir = baseApkForParsing.absolutePath
        appInfo.publicSourceDir = baseApkForParsing.absolutePath

        val appLabel = runCatching {
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(archiveInfo.packageName)

        val iconBitmap = runCatching {
            apkRepository.drawableToBitmap(packageManager.getApplicationIcon(appInfo), 112)
        }.getOrNull()

        PreInstallValidationInfo(
            uri = uri,
            fileName = displayName,
            isApksBundle = isApksBundle,
            appLabel = appLabel,
            packageName = archiveInfo.packageName,
            versionName = archiveInfo.versionName ?: "1.0",
            versionCode = archiveInfo.longVersionCode,
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion,
            fileSizeBytes = fileSize,
            sha256Hash = sha256,
            iconBitmap = iconBitmap,
            splitEntryNames = if (isApksBundle) apkEntries.map { File(it).name } else listOf("base.apk"),
            isValid = true,
            securityWarning = null
        )
    }

    /**
     * Commits a PackageInstaller.Session for either a single .apk or a multi-entry .apks bundle.
     */
    suspend fun installFromValidatedUri(validation: PreInstallValidationInfo) = withContext(Dispatchers.IO) {
        val packageInstaller = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(validation.packageName)
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            if (!validation.isApksBundle) {
                context.contentResolver.openInputStream(validation.uri)?.use { input ->
                    session.openWrite("base.apk", 0, validation.fileSizeBytes).use { output ->
                        input.copyTo(output, 32 * 1024)
                        session.fsync(output)
                    }
                } ?: throw IllegalStateException("Cannot open .apk URI stream")
            } else {
                // Stream base.apk and every split_*.apk entry from the .apks ZIP into the SAME session
                context.contentResolver.openInputStream(validation.uri)?.use { rawInput ->
                    ZipInputStream(BufferedInputStream(rawInput)).use { zis ->
                        var entry = zis.nextEntry
                        var splitCounter = 0
                        while (entry != null) {
                            val name = entry.name
                            if (name.contains("../") || name.contains("..\\") || name.startsWith("/")) {
                                throw SecurityException("Rejected path traversal entry: $name")
                            }
                            if (!entry.isDirectory && name.lowercase().endsWith(".apk")) {
                                val safeEntryName = FileNameAndHashUtils.sanitizeFileName(
                                    File(name).name,
                                    64
                                ).ifBlank { "split_${splitCounter++}.apk" }
                                val entrySize = if (entry.size > 0) entry.size else -1L
                                session.openWrite(safeEntryName, 0, entrySize).use { out ->
                                    zis.copyTo(out, 32 * 1024)
                                    session.fsync(out)
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } ?: throw IllegalStateException("Cannot open .apks URI stream")
            }

            commitSessionWithReceiver(session, sessionId, validation.packageName)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
        }
    }

    /**
     * Installs an already-installed app's base + split APKs or a backed-up DocumentFile folder
     * through a single PackageInstaller.Session.
     */
    suspend fun installFromBackupAppFolder(
        appFolder: DocumentFile,
        packageName: String
    ) = withContext(Dispatchers.IO) {
        val apkFiles = appFolder.listFiles().filter {
            it.isFile && (it.name?.lowercase()?.endsWith(".apk") == true)
        }
        if (apkFiles.isEmpty()) {
            throw IllegalStateException("No .apk files found in backup folder for $packageName")
        }

        val packageInstaller = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
        }

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            apkFiles.forEachIndexed { idx, docFile ->
                val rawName = docFile.name ?: "split_$idx.apk"
                val safeName = FileNameAndHashUtils.sanitizeFileName(rawName, 64)
                val length = if (docFile.length() > 0) docFile.length() else -1L
                context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                    session.openWrite(safeName, 0, length).use { output ->
                        input.copyTo(output, 32 * 1024)
                        session.fsync(output)
                    }
                }
            }
            commitSessionWithReceiver(session, sessionId, packageName)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
        }
    }

    private fun commitSessionWithReceiver(
        session: PackageInstaller.Session,
        sessionId: Int,
        packageName: String
    ) {
        val callbackIntent = Intent(context, InstallStatusReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_PACKAGE_NAME, packageName)
        }
        // PackageInstaller requires a mutable PendingIntent so the system can populate EXTRA_STATUS
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            callbackIntent,
            pendingFlags
        )
        session.commit(pendingIntent.intentSender)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment
        }.getOrNull()
    }

    private fun invalidPackage(
        uri: Uri,
        fileName: String,
        reason: String,
        sha256: String = "",
        size: Long = 0L
    ) = PreInstallValidationInfo(
        uri = uri,
        fileName = fileName,
        isApksBundle = fileName.lowercase().endsWith(".apks") || fileName.lowercase().endsWith(".zip"),
        appLabel = fileName,
        packageName = "unknown",
        versionName = "-",
        versionCode = 0L,
        minSdk = 31,
        targetSdk = 35,
        fileSizeBytes = size,
        sha256Hash = sha256,
        iconBitmap = null,
        splitEntryNames = emptyList(),
        isValid = false,
        securityWarning = reason
    )
}
