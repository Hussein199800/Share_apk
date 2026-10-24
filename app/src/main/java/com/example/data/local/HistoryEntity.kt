package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_logs")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // SHARE, SAVE_DOWNLOADS, INSTALL, BACKUP, WIFI_SHARE
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val fileFormat: String, // .apk, .apks, .zip
    val fileSizeBytes: Long,
    val sha256Hash: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
