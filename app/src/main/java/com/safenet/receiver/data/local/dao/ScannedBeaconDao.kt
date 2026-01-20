package com.safenet.receiver.data.local.dao

import androidx.room.*
import com.safenet.receiver.data.local.entity.ScannedBeaconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedBeaconDao {
    
    // 按 UUID+Major+Minor 分組，獲取每個設備的最新記錄和出現次數
    @Query("""
        SELECT 
            MAX(id) as id,
            uuid,
            major,
            minor,
            rssi,
            distance,
            isInWhitelist,
            MAX(scannedAt) as scannedAt,
            COUNT(*) as scanCount
        FROM scanned_beacons 
        GROUP BY uuid, major, minor
        ORDER BY scannedAt DESC 
        LIMIT 100
    """)
    fun getRecentScansFlow(): Flow<List<ScannedBeaconEntity>>
    
    @Query("SELECT * FROM scanned_beacons ORDER BY scannedAt DESC")
    suspend fun getAllScans(): List<ScannedBeaconEntity>
    
    @Query("SELECT COUNT(*) FROM scanned_beacons")
    fun getTotalCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM scanned_beacons WHERE isInWhitelist = 1")
    fun getWhitelistCountFlow(): Flow<Int>
    
    @Query("""
        SELECT COUNT(DISTINCT uuid || '-' || major || '-' || minor) 
        FROM scanned_beacons
    """)
    fun getUniqueDeviceCountFlow(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beacon: ScannedBeaconEntity)
    
    @Query("DELETE FROM scanned_beacons WHERE scannedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM scanned_beacons")
    suspend fun deleteAll()
    
    @Query("DELETE FROM scanned_beacons WHERE id IN (SELECT id FROM scanned_beacons ORDER BY scannedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
    
    @Query("SELECT COUNT(*) FROM scanned_beacons")
    suspend fun getCount(): Int
}
