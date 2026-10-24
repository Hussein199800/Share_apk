package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.backup.BackupVerificationReport
import com.example.backup.PreFlashBackupManager
import com.example.data.model.BackupManifest
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupServiceProgressState(
    val isRunning: Boolean = false,
    val isVerifying: Boolean = false,
    val currentAppIndex: Int = 0,
    val totalAppsCount: Int = 0,
    val currentAppLabel: String = "",
    val skippedFilesCount: Int = 0,
    val completedManifest: BackupManifest? = null,
    val completedFolderName: String? = null,
    val verificationReport: BackupVerificationReport? = null,
    val awaitingSafetyChecklist: Boolean = false,
    val isFullyCompleted: Boolean = false,
    val errorMessage: String? = null
)

object BackupJobOrchestrator {
    private val _state = MutableStateFlow(BackupServiceProgressState())
    val state: StateFlow<BackupServiceProgressState> = _state.asStateFlow()

    var pendingApps: List<InstalledAppInfo> = emptyList()
    var pendingTreeUri: Uri? = null
    var pendingResumeFolderName: String? = null

    fun updateState(transform: (BackupServiceProgressState) -> BackupServiceProgressState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = BackupServiceProgressState()
    }
}

class BackupForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeBackupJob: Job? = null

    companion object {
        const val CHANNEL_ID = "shareapk_preflash_backup_channel"
        const val NOTIFICATION_ID = 4201
        const val ACTION_START_BACKUP = "com.shareapk.app.action.START_BACKUP"
        const val ACTION_CANCEL_BACKUP = "com.shareapk.app.action.CANCEL_BACKUP"

        fun startBackupService(
            context: Context,
            treeUri: Uri,
            apps: List<InstalledAppInfo>,
            resumeFolderName: String? = null
        ) {
            BackupJobOrchestrator.pendingApps = apps
            BackupJobOrchestrator.pendingTreeUri = treeUri
            BackupJobOrchestrator.pendingResumeFolderName = resumeFolderName

            val intent = Intent(context, BackupForegroundService::class.java).apply {
                action = ACTION_START_BACKUP
            }
            context.startForegroundService(intent)
        }

        fun cancelBackupService(context: Context) {
            val intent = Intent(context, BackupForegroundService::class.java).apply {
                action = ACTION_CANCEL_BACKUP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_BACKUP -> {
                activeBackupJob?.cancel()
                BackupJobOrchestrator.updateState {
                    it.copy(
                        isRunning = false,
                        isVerifying = false,
                        errorMessage = "Backup cancelled by user / تم إلغاء النسخ الاحتياطي"
                    )
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_BACKUP -> {
                val treeUri = BackupJobOrchestrator.pendingTreeUri
                val apps = BackupJobOrchestrator.pendingApps
                val resumeFolder = BackupJobOrchestrator.pendingResumeFolderName

                if (treeUri == null || apps.isEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                val initialNotification = buildProgressNotification(
                    current = 0,
                    total = apps.size,
                    appTitle = "جاري تجهيز النسخ الاحتياطي الشامل..."
                )

                // On Android 14+ (API 34+), startForeground must specify FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }

                runBackupWorkflow(treeUri, apps, resumeFolder)
            }
        }
        return START_NOT_STICKY
    }

    private fun runBackupWorkflow(
        treeUri: Uri,
        apps: List<InstalledAppInfo>,
        resumeFolderName: String?
    ) {
        activeBackupJob?.cancel()
        activeBackupJob = serviceScope.launch {
            val manager = PreFlashBackupManager(applicationContext)
            try {
                BackupJobOrchestrator.updateState {
                    BackupServiceProgressState(
                        isRunning = true,
                        isVerifying = false,
                        currentAppIndex = 0,
                        totalAppsCount = apps.size,
                        currentAppLabel = apps.firstOrNull()?.label.orEmpty()
                    )
                }

                val (backupDir, manifest) = manager.executePreFlashBackup(
                    rootTreeUri = treeUri,
                    appsToBackup = apps,
                    resumeFolderName = resumeFolderName
                ) { idx, total, appLabel, skipped ->
                    BackupJobOrchestrator.updateState {
                        it.copy(
                            isRunning = true,
                            currentAppIndex = idx,
                            totalAppsCount = total,
                            currentAppLabel = appLabel,
                            skippedFilesCount = skipped
                        )
                    }
                    updateNotification(idx, total, appLabel)
                }

                // Post-Write SHA-256 Verification
                BackupJobOrchestrator.updateState {
                    it.copy(
                        isVerifying = true,
                        currentAppLabel = "SHA-256 Verification..."
                    )
                }

                val report = manager.verifyBackupFolderSha256(backupDir, manifest) { count, fileLabel ->
                    BackupJobOrchestrator.updateState {
                        it.copy(
                            isVerifying = true,
                            currentAppLabel = "SHA-256 (#$count): $fileLabel"
                        )
                    }
                }

                BackupJobOrchestrator.updateState {
                    it.copy(
                        isRunning = false,
                        isVerifying = false,
                        completedManifest = manifest,
                        completedFolderName = backupDir.name ?: "ShareAPK_Backup",
                        verificationReport = report,
                        awaitingSafetyChecklist = true,
                        isFullyCompleted = false
                    )
                }
            } catch (_: CancellationException) {
                BackupJobOrchestrator.updateState {
                    it.copy(
                        isRunning = false,
                        isVerifying = false,
                        errorMessage = "تم إيقاف النسخ الاحتياطي (يمكنك الاستئناف لاحقاً)"
                    )
                }
            } catch (e: Exception) {
                BackupJobOrchestrator.updateState {
                    it.copy(
                        isRunning = false,
                        isVerifying = false,
                        errorMessage = e.localizedMessage ?: "Backup failed"
                    )
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ShareAPK Pre-Flash Backup",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows live progress during Pre-Flash full application backup"
        }
        nm.createNotificationChannel(channel)
    }

    private fun updateNotification(current: Int, total: Int, appTitle: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID, buildProgressNotification(current, total, appTitle))
    }

    private fun buildProgressNotification(current: Int, total: Int, appTitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, BackupForegroundService::class.java).apply {
            action = ACTION_CANCEL_BACKUP
        }
        val cancelPending = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("شارك أبك | النسخ الاحتياطي الشامل ($current / $total)")
            .setContentText(appTitle)
            .setProgress(total.coerceAtLeast(1), current, false)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إلغاء / Cancel",
                cancelPending
            )
            .build()
    }
}
