package com.safenet.receiver.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safenet.receiver.data.local.dao.BeaconQueueDao
import com.safenet.receiver.data.local.dao.ScannedBeaconDao
import com.safenet.receiver.data.local.dao.WhitelistDeviceDao
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import com.safenet.receiver.data.local.entity.ScannedBeaconEntity
import com.safenet.receiver.data.local.entity.WhitelistDeviceEntity

@Database(
    entities = [
        WhitelistDeviceEntity::class,
        BeaconQueueEntity::class,
        ScannedBeaconEntity::class
    ],
    version = 4,  // 升級版本以支援 scanCount 統計欄位
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun whitelistDeviceDao(): WhitelistDeviceDao
    abstract fun beaconQueueDao(): BeaconQueueDao
    abstract fun scannedBeaconDao(): ScannedBeaconDao
    
    companion object {
        const val DATABASE_NAME = "safenet_receiver.db"
    }
}
