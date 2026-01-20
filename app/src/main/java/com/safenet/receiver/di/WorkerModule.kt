package com.safenet.receiver.di

import android.content.Context
import androidx.work.*
import com.safenet.receiver.data.worker.UploadWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    fun schedulePeriodicUpload(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadRequest = PeriodicWorkRequestBuilder<UploadWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15, TimeUnit.MINUTES
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            UploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            uploadRequest
        )
    }
}
