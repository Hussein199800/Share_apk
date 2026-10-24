package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shareapk_history.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory = historyDao.getAllHistory()

    suspend fun logOperation(
        actionType: String,
        appLabel: String,
        packageName: String,
        versionName: String,
        fileFormat: String,
        fileSizeBytes: Long,
        sha256Hash: String
    ) {
        historyDao.insertHistory(
            HistoryEntity(
                actionType = actionType,
                appLabel = appLabel,
                packageName = packageName,
                versionName = versionName,
                fileFormat = fileFormat,
                fileSizeBytes = fileSizeBytes,
                sha256Hash = sha256Hash
            )
        )
    }

    suspend fun deleteById(id: Long) = historyDao.deleteById(id)

    suspend fun clearAll() = historyDao.clearAll()
}
