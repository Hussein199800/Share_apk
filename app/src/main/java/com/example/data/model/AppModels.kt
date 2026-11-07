package com.example.data.model

import android.graphics.Bitmap
import android.net.Uri

enum class AppFilter {
    USER_APPS,
    SYSTEM_APPS,
    SPLIT_ONLY,
    FAVORITES
}

enum class AppSortOption {
    NAME_ASC,
    SIZE_DESC,
    INSTALLED_DATE,
    RECENTLY_UPDATED
}

enum class FileNamingFormat {
    ORIGINAL_NAME,
    LATIN_TRANSLITERATION,
    PACKAGE_NAME
}

enum class ThemePreference {
    DARK,
    LIGHT,
    SYSTEM
}

enum class LanguagePreference {
    ARABIC,
    ENGLISH,
    SYSTEM
}

enum class MainNavTab {
    APPS,
    FILES,
    RECEIVE,
    SCREEN,
    MORE
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val isSplitApk: Boolean,
    val baseApkPath: String,
    val splitSourceDirs: List<String>,
    val splitNames: List<String>,
    val totalSizeBytes: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerPackageName: String?,
    val installerDisplayName: String,
    val signingSha256: String,
    val supportedAbis: List<String>,
    val iconBitmap: Bitmap? = null,
    val isFavorite: Boolean = false
) {
    val fileExtension: String
        get() = if (isSplitApk) "apks" else "apk"

    val googlePlayUrl: String
        get() = "https://play.google.com/store/apps/details?id=$packageName"
}

data class PreInstallValidationInfo(
    val uri: Uri,
    val fileName: String,
    val isApksBundle: Boolean,
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSizeBytes: Long,
    val sha256Hash: String,
    val iconBitmap: Bitmap?,
    val splitEntryNames: List<String>,
    val isValid: Boolean,
    val securityWarning: String? = null
)

enum class RestoreAppStatus {
    NOT_INSTALLED,
    OLDER_VERSION_INSTALLED,
    ALREADY_INSTALLED,
    INCOMPATIBLE
}

data class BackupFileEntry(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String
)

data class BackupAppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val isSplitApk: Boolean,
    val supportedAbis: List<String>,
    val installerPackageName: String?,
    val googlePlayUrl: String?,
    val totalSizeBytes: Long,
    val files: List<BackupFileEntry>
)

data class BackupManifest(
    val schemaVersion: Int = 1,
    val backupDateIso: String,
    val deviceModel: String,
    val androidRelease: String,
    val sdkInt: Int,
    val appCount: Int,
    val totalSizeBytes: Long,
    val apps: List<BackupAppEntry>
)

data class RestoreCandidateApp(
    val entry: BackupAppEntry,
    val status: RestoreAppStatus,
    val installedVersionName: String?,
    val installedVersionCode: Long?,
    val isSelected: Boolean,
    val integrityOk: Boolean,
    val incompatibilityReason: String? = null
)

data class BatchRestoreSummary(
    val installedCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val details: List<String>
)
