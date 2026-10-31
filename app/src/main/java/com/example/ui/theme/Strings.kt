package com.example.ui.theme

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.text.BidiFormatter
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import com.example.data.model.LanguagePreference
import java.util.Locale

data class AppStrings(
    val isArabic: Boolean,
    val appName: String,
    val appSubtitle: String,
    val navApps: String,
    val navBackup: String,
    val navHistory: String,
    val navSettings: String,
    val installExternalBtn: String,
    val searchHint: String,
    val filterUserApps: String,
    val filterSystemApps: String,
    val filterSplitOnly: String,
    val filterFavorites: String,
    val sortByLabel: String,
    val sortName: String,
    val sortSize: String,
    val sortInstalled: String,
    val sortUpdated: String,
    val splitBadge: String,
    val singleApkBadge: String,
    val shareAction: String,
    val saveDownloadsAction: String,
    val wifiShareAction: String,
    val reinstallVerifyAction: String,
    val systemInfoAction: String,
    val openPlayStoreAction: String,
    val copyAction: String,
    val copiedToast: String,
    val savedToDownloadsMsg: String,
    val openDownloadsBtn: String,
    val splitWarningTitle: String,
    val splitWarningBody: String,
    val continueBtn: String,
    val cancelBtn: String,
    val selectedCountFormat: String,
    val shareSelectedBtn: String,
    val backupSelectedZipBtn: String,
    val clearSelectionBtn: String,
    val appDetailsTitle: String,
    val packageNameLabel: String,
    val versionLabel: String,
    val sdkRangeLabel: String,
    val installedDateLabel: String,
    val updatedDateLabel: String,
    val installerSourceLabel: String,
    val apkSizeLabel: String,
    val splitFilesLabel: String,
    val signingCertSha256Label: String,
    val apkFileSha256Label: String,
    val computingSha256: String,
    val calculateSha256Btn: String,
    val selectAppPrompt: String,
    val emptyAppsList: String,
    val refreshApps: String,
    // Built-in Installer
    val installerSheetTitle: String,
    val securityVerifiedBadge: String,
    val securityWarningBadge: String,
    val unknownSourcesTitle: String,
    val unknownSourcesBody: String,
    val grantPermissionBtn: String,
    val installNowBtn: String,
    val installingProgress: String,
    val installSuccessTitle: String,
    val installFailureTitle: String,
    // Wi-Fi Share
    val wifiServerTitle: String,
    val wifiServerSubtitle: String,
    val stopServerBtn: String,
    val autoStopCountdown: String,
    val scanQrOrOpenUrl: String,
    // Pre-Flash Backup & Restore
    val backupRestoreHeader: String,
    val tabCreateBackup: String,
    val tabRestoreBackup: String,
    val includeSystemAppsToggle: String,
    val selectAllBtn: String,
    val selectNoneBtn: String,
    val chooseDestinationBtn: String,
    val startFullBackupBtn: String,
    val resumeBackupBtn: String,
    val safetyChecklistTitle: String,
    val safetyCheck1: String,
    val safetyCheck2: String,
    val safetyCheck3: String,
    val confirmChecklistAndFinish: String,
    val verifyBackupIntegrityBtn: String,
    val backupVerifiedSuccess: String,
    val selectBackupFolderToRestore: String,
    val startBatchRestoreBtn: String,
    val statusNotInstalled: String,
    val statusUpdateAvailable: String,
    val statusAlreadyInstalled: String,
    val statusIncompatible: String,
    // History
    val historyTitle: String,
    val clearHistoryBtn: String,
    val emptyHistoryMsg: String,
    // Settings & About
    val settingsTitle: String,
    val themeSectionTitle: String,
    val themeDark: String,
    val themeLight: String,
    val themeSystem: String,
    val dynamicColorTitle: String,
    val dynamicColorSubtitle: String,
    val languageSectionTitle: String,
    val namingSectionTitle: String,
    val namingOriginal: String,
    val namingLatin: String,
    val namingPackage: String,
    val cacheSectionTitle: String,
    val clearCacheNowBtn: String,
    val autoClearCacheToggle: String,
    val aboutDeveloperHeader: String,
    val developerNamePrimary: String,
    val developerNameSecondary: String,
    val developerRoleBio: String,
    val offlinePrivacyBadge: String,
    val repoTransparencyLabel: String,
    val openSourceLicenses: String,
    // Onboarding
    val onboardingStep1Title: String,
    val onboardingStep1Body: String,
    val onboardingStep2Title: String,
    val onboardingStep2Body: String,
    val onboardingStep3Title: String,
    val onboardingStep3Body: String,
    val nextBtn: String,
    val getStartedBtn: String,
    val navBack: String
)

val ArabicStrings = AppStrings(
    isArabic = true,
    appName = "شارك أبك | ShareAPK",
    appSubtitle = "استخراج ومشاركة وتثبيت ونسخ التطبيقات بدون إنترنت",
    navApps = "التطبيقات",
    navBackup = "النسخ والاسترجاع",
    navHistory = "السجل",
    navSettings = "الإعدادات",
    installExternalBtn = "تثبيت ملف خارجي",
    searchHint = "ابحث باسم التطبيق أو معرف الحزمة...",
    filterUserApps = "تطبيقات المستخدم",
    filterSystemApps = "تطبيقات النظام",
    filterSplitOnly = "مقسّم (.apks)",
    filterFavorites = "المفضلة ★",
    sortByLabel = "الترتيب:",
    sortName = "الاسم (أ–ي)",
    sortSize = "الحجم (الأكبر)",
    sortInstalled = "تاريخ التثبيت",
    sortUpdated = "آخر تحديث",
    splitBadge = "مقسّم (.apks)",
    singleApkBadge = "ملف (.apk)",
    shareAction = "مشاركة الملف",
    saveDownloadsAction = "حفظ في التنزيلات",
    wifiShareAction = "مشاركة عبر Wi-Fi",
    reinstallVerifyAction = "تثبيت / فحص الحزمة",
    systemInfoAction = "معلومات التطبيق بالنظام",
    openPlayStoreAction = "فتح في Google Play",
    copyAction = "نسخ",
    copiedToast = "تم النسخ إلى الحافظة",
    savedToDownloadsMsg = "تم الحفظ في مجلد التنزيلات بنجاح",
    openDownloadsBtn = "فتح التنزيلات",
    splitWarningTitle = "تطبيق مقسّم (Split APKS)",
    splitWarningBody = "يتكون هذا التطبيق من حزمة أساسية (base.apk) وملفات تقسيم إضافية. سيتم دمجها تلقائياً في أرشيف (.apks) واحد. يحتاج جهاز المستلم إلى مثبت \"شارك أبك\" المدمج أو تطبيق SAI لتثبيت الحزمة.",
    continueBtn = "متابعة",
    cancelBtn = "إلغاء",
    selectedCountFormat = "تم تحديد %d تطبيق (%s)",
    shareSelectedBtn = "مشاركة المحدد",
    backupSelectedZipBtn = "نسخ (.zip) للتنزيلات",
    clearSelectionBtn = "إلغاء التحديد",
    appDetailsTitle = "تفاصيل الحزمة التقنية",
    packageNameLabel = "معرف الحزمة (Package)",
    versionLabel = "الإصدار (Version)",
    sdkRangeLabel = "نطاق النظام (SDK)",
    installedDateLabel = "تاريخ التثبيت",
    updatedDateLabel = "آخر تحديث",
    installerSourceLabel = "مصدر التثبيت",
    apkSizeLabel = "إجمالي حجم الحزمة",
    splitFilesLabel = "أجزاء الحزمة المقسّمة (Splits)",
    signingCertSha256Label = "بصمة شهادة التوقيع (Cert SHA-256)",
    apkFileSha256Label = "بصمة الملف (APK SHA-256)",
    computingSha256 = "جاري حساب البصمة الرقمية...",
    calculateSha256Btn = "حساب SHA-256",
    selectAppPrompt = "اختر تطبيقاً من القائمة لعرض التفاصيل التقنية وخيارات المشاركة والفحص",
    emptyAppsList = "لا توجد تطبيقات مطابقة لخيارات البحث أو التصفية الحالية",
    refreshApps = "تحديث قائمة التطبيقات",
    installerSheetTitle = "فحص وتثبيت الحزمة الخارجية",
    securityVerifiedBadge = "تم التحقق من سلامة الأرشيف والتوقيع",
    securityWarningBadge = "تحذير أمني في بنية الأرشيف",
    unknownSourcesTitle = "إذن تثبيت التطبيقات مطلوب",
    unknownSourcesBody = "لتثبيت ملفات .apk و .apks مباشرة، يرجى منح تطبيق \"شارك أبك\" صلاحية تثبيت التطبيقات غير المعروفة من إعدادات النظام.",
    grantPermissionBtn = "فتح إعدادات الإذن",
    installNowBtn = "تثبيت الحزمة الآن",
    installingProgress = "جاري كتابة الجلسة إلى PackageInstaller...",
    installSuccessTitle = "تم تثبيت التطبيق بنجاح",
    installFailureTitle = "تعذر إكمال التثبيت",
    wifiServerTitle = "خادم المشاركة المحلي عبر Wi-Fi",
    wifiServerSubtitle = "امسح رمز QR أو افتح الرابط في المتصفح على أي جهاز متصل بنفس الشبكة أو نقطة الاتصال",
    stopServerBtn = "إيقاف الخادم فوراً",
    autoStopCountdown = "يتوقف تلقائياً خلال:",
    scanQrOrOpenUrl = "الرابط المحلي المباشر:",
    backupRestoreHeader = "النسخ الاحتياطي الشامل قبل الفورمات (Pre-Flash)",
    tabCreateBackup = "إنشاء نسخة شاملة",
    tabRestoreBackup = "استرجاع من مجلد",
    includeSystemAppsToggle = "تضمين تطبيقات النظام",
    selectAllBtn = "تحديد الكل",
    selectNoneBtn = "إلغاء الكل",
    chooseDestinationBtn = "اختيار مجلد الحفظ (USB / SD / ذاكرة)",
    startFullBackupBtn = "بدء النسخ الاحتياطي الشامل",
    resumeBackupBtn = "استئناف النسخ (تخطي المتطابق بـ SHA-256)",
    safetyChecklistTitle = "قائمة التحقق الإلزامية قبل الفورمات",
    safetyCheck1 = "أدرك أن النسخ الاحتياطي يحفظ ملفات تثبيت التطبيقات (APK/APKS) فقط ولا يشمل البيانات الداخلية أو محادثات واتساب أو الصور.",
    safetyCheck2 = "إذا تم الحفظ في الذاكرة الداخلية، يجب نقل مجلد النسخة إلى فلاشة USB OTG أو بطاقة SD أو كمبيوتر قبل مسح الجهاز.",
    safetyCheck3 = "تأكدت من أخذ نسخة احتياطية منفصلة لرموز التحقق الثنائي (2FA) وكلمات المرور.",
    confirmChecklistAndFinish = "تأكيد القائمة واعتماد النسخة الاحتياطية",
    verifyBackupIntegrityBtn = "فحص تطابق SHA-256 لجميع الملفات",
    backupVerifiedSuccess = "تم التحقق من سلامة النسخة الاحتياطية (100% متطابق مع manifest.json)",
    selectBackupFolderToRestore = "اختيار مجلد ShareAPK_Backup للاسترجاع",
    startBatchRestoreBtn = "بدء الاسترجاع المتسلسل للتطبيقات المحددة",
    statusNotInstalled = "غير مثبت (محدد افتراضياً)",
    statusUpdateAvailable = "يتوفر إصدار أحدث في النسخة",
    statusAlreadyInstalled = "مثبت مسبقاً (نفس الإصدار أو أحدث)",
    statusIncompatible = "غير متوافق (SDK أو معمارية المعالج)",
    historyTitle = "سجل العمليات والملفات",
    clearHistoryBtn = "مسح السجل",
    emptyHistoryMsg = "لا توجد عمليات مشاركة أو حفظ أو تثبيت مسجلة حتى الآن",
    settingsTitle = "الإعدادات وحول التطبيق",
    themeSectionTitle = "المظهر والألوان (المظهر البني الدافئ)",
    themeDark = "داكن (إسبريسو وذهبي)",
    themeLight = "فاتح (كريمي دافئ)",
    themeSystem = "تلقائي حسب النظام",
    dynamicColorTitle = "ألوان Material You الديناميكية",
    dynamicColorSubtitle = "استخدام ألوان خلفية الشاشة على أندرويد 12+ بدلاً من المظهر البني الكلاسيكي",
    languageSectionTitle = "لغة التطبيق (Language)",
    namingSectionTitle = "صيغة تسمية الملفات المستخرجة",
    namingOriginal = "الاسم الأصلي (AppName_v1.2.apk)",
    namingLatin = "أحرف لاتينية (AppName_Latin_v1.2.apk)",
    namingPackage = "اسم الحزمة (com.pkg_v1.2.apk)",
    cacheSectionTitle = "إدارة الذاكرة المؤقتة (cacheDir/shared)",
    clearCacheNowBtn = "تفريغ المؤقت الآن",
    autoClearCacheToggle = "تنظيف تلقائي عند الخروج من التطبيق",
    aboutDeveloperHeader = "بطاقة المطور ومعلومات الإصدار",
    developerNamePrimary = "المطور: حسين محمد علي الطويل",
    developerNameSecondary = "Developer: Hussein Mohammed Ali Al-Taweel",
    developerRoleBio = "هندسة وتطوير تطبيقات أندرويد الأصلية (Kotlin & Jetpack Compose) — يعمل 100% بدون إنترنت مع حماية كاملة للخصوصية.",
    offlinePrivacyBadge = "يعمل 100% أوفلاين • صفر صلاحيات تخزين",
    repoTransparencyLabel = "مستودع المشروع المفتوح للشفافية (GitHub Repository)",
    openSourceLicenses = "تراخيص البرمجيات مفتوحة المصدر: Android Jetpack, Compose Material 3, Room, DataStore (Apache License 2.0)",
    onboardingStep1Title = "مشاركة APK و Split APKS بلمسة واحدة",
    onboardingStep1Body = "يدعم التطبيق استخراج التطبيقات المفردة (.apk) والتطبيقات المقسّمة الحديثة (.apks) وتثبيتها مباشرة عبر المثبت المدمج.",
    onboardingStep2Title = "خصوصية تامة بدون صلاحيات تخزين",
    onboardingStep2Body = "تتم المشاركة عبر FileProvider والحفظ عبر MediaStore و SAF بدون طلب صلاحيات قراءة ملفاتك الشخصية.",
    onboardingStep3Title = "نسخ احتياطي شامل قبل الفورمات",
    onboardingStep3Body = "احفظ جميع تطبيقاتك مع ملفات الـ Split ونسخة من تطبيق شارك أبك وتقرير HTML وبصمات SHA-256 لاسترجاعها دفعة واحدة.",
    nextBtn = "التالي",
    getStartedBtn = "ابدأ الاستخدام",
    navBack = "رجوع"
)

val EnglishStrings = AppStrings(
    isArabic = false,
    appName = "ShareAPK | شارك أبك",
    appSubtitle = "Offline APK & Split APKS Extractor, Installer & Backup",
    navApps = "Apps",
    navBackup = "Backup & Restore",
    navHistory = "History",
    navSettings = "Settings",
    installExternalBtn = "Install APK / APKS",
    searchHint = "Search by app name or package ID...",
    filterUserApps = "User Apps",
    filterSystemApps = "System Apps",
    filterSplitOnly = "Split (.apks)",
    filterFavorites = "Favorites ★",
    sortByLabel = "Sort:",
    sortName = "Name (A–Z)",
    sortSize = "Size (Largest)",
    sortInstalled = "Installed Date",
    sortUpdated = "Recently Updated",
    splitBadge = "Split (.apks)",
    singleApkBadge = "Single (.apk)",
    shareAction = "Share APK",
    saveDownloadsAction = "Save to Downloads",
    wifiShareAction = "Share over Wi-Fi",
    reinstallVerifyAction = "Verify / Reinstall",
    systemInfoAction = "System App Info",
    openPlayStoreAction = "Open in Google Play",
    copyAction = "Copy",
    copiedToast = "Copied to clipboard",
    savedToDownloadsMsg = "Saved to Downloads successfully",
    openDownloadsBtn = "Open Downloads",
    splitWarningTitle = "Split APK Bundle (.apks)",
    splitWarningBody = "This app contains base.apk plus split configuration APKs. ShareAPK will bundle them into a single .apks archive. The receiver needs ShareAPK's built-in installer (or SAI) to install it.",
    continueBtn = "Continue",
    cancelBtn = "Cancel",
    selectedCountFormat = "%d selected (%s)",
    shareSelectedBtn = "Share Selected",
    backupSelectedZipBtn = "Backup (.zip) to Downloads",
    clearSelectionBtn = "Clear Selection",
    appDetailsTitle = "Technical Package Inspector",
    packageNameLabel = "Package Name",
    versionLabel = "Version",
    sdkRangeLabel = "SDK Compatibility",
    installedDateLabel = "First Installed",
    updatedDateLabel = "Last Updated",
    installerSourceLabel = "Installer Source",
    apkSizeLabel = "Total APK Size",
    splitFilesLabel = "Split APK Modules",
    signingCertSha256Label = "Signing Certificate (SHA-256)",
    apkFileSha256Label = "Base APK Checksum (SHA-256)",
    computingSha256 = "Computing SHA-256 digest...",
    calculateSha256Btn = "Compute SHA-256",
    selectAppPrompt = "Select an installed app from the list to inspect technical metadata, splits, checksums, and sharing actions",
    emptyAppsList = "No installed applications match your current search or filter",
    refreshApps = "Refresh installed packages",
    installerSheetTitle = "Pre-Install Security & Validation",
    securityVerifiedBadge = "ZIP Structure & Package Verified",
    securityWarningBadge = "Archive Security Warning",
    unknownSourcesTitle = "Install Unknown Apps Permission",
    unknownSourcesBody = "To install external .apk and .apks bundles, please allow ShareAPK to install unknown apps in Android Settings.",
    grantPermissionBtn = "Open Permission Settings",
    installNowBtn = "Commit Install Session",
    installingProgress = "Streaming package into PackageInstaller.Session...",
    installSuccessTitle = "Package Installed Successfully",
    installFailureTitle = "Installation Failed",
    wifiServerTitle = "Local Wi-Fi HTTP Share Server",
    wifiServerSubtitle = "Scan the QR code or open the URL in any browser on the same Wi-Fi network or hotspot",
    stopServerBtn = "Stop Server Now",
    autoStopCountdown = "Auto-stops in:",
    scanQrOrOpenUrl = "Direct Local LAN URL:",
    backupRestoreHeader = "Pre-Flash Full Backup & Batch Restore",
    tabCreateBackup = "Create Full Backup",
    tabRestoreBackup = "Restore from Folder",
    includeSystemAppsToggle = "Include System Apps",
    selectAllBtn = "Select All",
    selectNoneBtn = "Select None",
    chooseDestinationBtn = "Pick Destination Folder (SAF / USB / SD)",
    startFullBackupBtn = "Start Full Pre-Flash Backup",
    resumeBackupBtn = "Resume Backup (Skip Matching SHA-256)",
    safetyChecklistTitle = "Mandatory Pre-Flash Safety Checklist",
    safetyCheck1 = "I understand that APK backup saves app installers only and does NOT include internal app data, WhatsApp chats, or photos.",
    safetyCheck2 = "If saving to internal storage, I must move the backup folder to an external USB OTG, SD card, or PC before wiping the device.",
    safetyCheck3 = "I have backed up my account 2FA codes and passwords separately.",
    confirmChecklistAndFinish = "Confirm Safety Checklist & Complete",
    verifyBackupIntegrityBtn = "Re-Verify All Files via SHA-256",
    backupVerifiedSuccess = "Backup Verified: All APK files match manifest.json SHA-256 hashes",
    selectBackupFolderToRestore = "Select ShareAPK_Backup_* Folder",
    startBatchRestoreBtn = "Start Sequential Batch Restore",
    statusNotInstalled = "Not Installed (Selected)",
    statusUpdateAvailable = "Older Version Installed (Update)",
    statusAlreadyInstalled = "Already Installed (Same/Newer)",
    statusIncompatible = "Incompatible (minSdk / ABI)",
    historyTitle = "Activity & Checksum History",
    clearHistoryBtn = "Clear History",
    emptyHistoryMsg = "No shared, saved, installed, or backed-up packages logged yet",
    settingsTitle = "Settings & About",
    themeSectionTitle = "Appearance (Warm Brown Theme)",
    themeDark = "Dark (Espresso & Gold)",
    themeLight = "Light (Warm Cream)",
    themeSystem = "Follow System",
    dynamicColorTitle = "Material You Dynamic Color",
    dynamicColorSubtitle = "Use Android 12+ wallpaper colors instead of custom Warm Brown palette",
    languageSectionTitle = "App Language (اللغة)",
    namingSectionTitle = "Extracted File Naming Format",
    namingOriginal = "Original App Name (AppName_v1.2.apk)",
    namingLatin = "Latin Transliteration (AppName_Latin_v1.2.apk)",
    namingPackage = "Package Name (com.pkg_v1.2.apk)",
    cacheSectionTitle = "Shared APK Cache (cacheDir/shared)",
    clearCacheNowBtn = "Clear Cache Now",
    autoClearCacheToggle = "Auto-clear shared cache on exit",
    aboutDeveloperHeader = "Featured Developer & Build Info",
    developerNamePrimary = "المطور: حسين محمد علي الطويل",
    developerNameSecondary = "Developer: Hussein Mohammed Ali Al-Taweel",
    developerRoleBio = "Pure Native Android Engineering (Kotlin, Jetpack Compose & Material 3) — 100% Offline-First & Zero Storage Permissions.",
    offlinePrivacyBadge = "100% Offline • Zero Storage Permissions",
    repoTransparencyLabel = "Open Project Repository for Transparency",
    openSourceLicenses = "Open Source Licenses: AndroidX, Jetpack Compose Material 3, Room, DataStore (Apache 2.0)",
    onboardingStep1Title = "Share Single .APK & Split .APKS",
    onboardingStep1Body = "Automatically detects multi-part Split APKs, bundles them into standard .apks archives, and installs them with the built-in session installer.",
    onboardingStep2Title = "Zero-Permission Privacy Architecture",
    onboardingStep2Body = "Uses FileProvider, MediaStore.Downloads, and Storage Access Framework (SAF) with zero invasive storage permissions.",
    onboardingStep3Title = "Full Pre-Flash Backup & Restore",
    onboardingStep3Body = "Creates a complete offline folder with ShareAPK's own installer, manifest.json, HTML catalog, and SHA-256 verification.",
    nextBtn = "Next",
    getStartedBtn = "Get Started",
    navBack = "Back"
)

val LocalAppStrings = staticCompositionLocalOf { ArabicStrings }

object LocaleHelper {
    /**
     * Applies per-app language using LocaleManager on Android 13+ (API 33+)
     * and falls back to AppCompatDelegate.setApplicationLocales on Android 12 (API 31-32).
     */
    fun applyLanguage(context: Context, preference: LanguagePreference) {
        val tag = when (preference) {
            LanguagePreference.ARABIC -> "ar"
            LanguagePreference.ENGLISH -> "en"
            LanguagePreference.SYSTEM -> ""
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = if (tag.isEmpty()) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(tag)
                }
            } else {
                val localeList = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
        } catch (_: Throwable) {
            // Safe fallback in unit/Robolectric environments where LocaleManager service may be stubbed
        }
    }

    fun resolveStrings(preference: LanguagePreference): AppStrings {
        return when (preference) {
            LanguagePreference.ARABIC -> ArabicStrings
            LanguagePreference.ENGLISH -> EnglishStrings
            LanguagePreference.SYSTEM -> {
                val sysLang = Locale.getDefault().language
                if (sysLang.startsWith("ar")) ArabicStrings else ArabicStrings
            }
        }
    }

    /**
     * Formats technical strings (package names, versions, sizes, SHA-256 hashes)
     * strictly in LTR direction even inside an Arabic RTL paragraph.
     */
    fun ltr(value: String): String {
        return try {
            BidiFormatter.getInstance(false).unicodeWrap(value)
        } catch (_: Throwable) {
            "\u200E$value\u200E"
        }
    }
}

@Composable
fun LtrBox(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
    }
}
