package com.safenet.receiver.di

import android.content.Context
import androidx.room.Room
import com.safenet.receiver.data.local.dao.BeaconQueueDao
import com.safenet.receiver.data.local.dao.WhitelistDeviceDao
import com.safenet.receiver.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideWhitelistDeviceDao(database: AppDatabase): WhitelistDeviceDao {
        return database.whitelistDeviceDao()
    }
    
    @Provides
    @Singleton
    fun provideBeaconQueueDao(database: AppDatabase): BeaconQueueDao {
        return database.beaconQueueDao()
    }
    
    @Provides
    @Singleton
    fun provideScannedBeaconDao(database: AppDatabase): com.safenet.receiver.data.local.dao.ScannedBeaconDao {
        return database.scannedBeaconDao()
    }
}
