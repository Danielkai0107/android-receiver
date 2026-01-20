package com.safenet.receiver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ReceiverApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val scanChannel = NotificationChannel(
                SCAN_CHANNEL_ID,
                "Beacon 掃描服務",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "顯示 Beacon 掃描服務運行狀態"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(scanChannel)
        }
    }

    companion object {
        const val SCAN_CHANNEL_ID = "beacon_scan_channel"
    }
}
