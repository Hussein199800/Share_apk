package com.example.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InstallSessionResult(
    val sessionId: Int,
    val packageName: String?,
    val status: Int,
    val statusTitleAr: String,
    val statusTitleEn: String,
    val messageAr: String,
    val messageEn: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

object InstallStatusEventBus {
    private val _latestResult = MutableStateFlow<InstallSessionResult?>(null)
    val latestResult: StateFlow<InstallSessionResult?> = _latestResult.asStateFlow()

    fun emitResult(result: InstallSessionResult) {
        _latestResult.value = result
    }

    fun clearResult() {
        _latestResult.value = null
    }
}

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val pkgName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val extraMsg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Android 13+ (API 33+) uses typed getParcelableExtra(name, Class)
            val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            if (confirmIntent != null) {
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(confirmIntent) }
            }
            return
        }

        val result = when (status) {
            PackageInstaller.STATUS_SUCCESS -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "تم تثبيت التطبيق بنجاح ✓",
                statusTitleEn = "Package Installed Successfully ✓",
                messageAr = "اكتملت جلسة التثبيت بنجاح للحزمة ${pkgName.orEmpty()}.",
                messageEn = "PackageInstaller session completed successfully for ${pkgName.orEmpty()}."
            )
            PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "تعارض في التوقيع (STATUS_FAILURE_CONFLICT)",
                statusTitleEn = "Signature Conflict (STATUS_FAILURE_CONFLICT)",
                messageAr = "التطبيق مثبت مسبقاً بتوقيع رقمي مختلف. يرجى إلغاء تثبيت النسخة الحالية أولاً ثم إعادة المحاولة. $extraMsg",
                messageEn = "An existing version with a different signing certificate is already installed. Uninstall it first. $extraMsg"
            )
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "غير متوافق مع الجهاز (STATUS_FAILURE_INCOMPATIBLE)",
                statusTitleEn = "Incompatible Package (STATUS_FAILURE_INCOMPATIBLE)",
                messageAr = "هذه الحزمة غير متوافقة مع إصدار أندرويد أو معمارية المعالج (ABI) الخاصة بجهازك. $extraMsg",
                messageEn = "This APK/APKS is incompatible with your device's CPU ABI or Android SDK version. $extraMsg"
            )
            PackageInstaller.STATUS_FAILURE_STORAGE -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "مساحة التخزين غير كافية (STATUS_FAILURE_STORAGE)",
                statusTitleEn = "Insufficient Storage (STATUS_FAILURE_STORAGE)",
                messageAr = "لا توجد مساحة فارغة كافية على الجهاز لإكمال تثبيت هذه الحزمة. $extraMsg",
                messageEn = "Not enough free storage space on the device to install this package. $extraMsg"
            )
            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "تم إلغاء التثبيت (STATUS_FAILURE_ABORTED)",
                statusTitleEn = "Installation Cancelled (STATUS_FAILURE_ABORTED)",
                messageAr = "قام المستخدم بإلغاء عملية التثبيت قبل اكتمالها.",
                messageEn = "The installation was cancelled by the user."
            )
            else -> InstallSessionResult(
                sessionId = sessionId,
                packageName = pkgName,
                status = status,
                statusTitleAr = "تعذر إكمال التثبيت (STATUS_FAILURE)",
                statusTitleEn = "Installation Failed (STATUS_FAILURE)",
                messageAr = "حدث خطأ أثناء تثبيت الحزمة: $extraMsg",
                messageEn = "An error occurred while committing the install session: $extraMsg"
            )
        }

        InstallStatusEventBus.emitResult(result)
    }
}
