package com.safenet.receiver.data.local.dao

import androidx.room.*
import com.safenet.receiver.data.local.entity.ScannedBeaconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedBeaconDao {
    
    // 按 UUID+Major+Minor 分組，取每個設備的「最新一筆」記錄及出現次數
    @Query("""
        SELECT 
            s.id,
            s.uuid,
            s.major,
            s.minor,
            s.rssi,
            s.distance,
            s.isInWhitelist,
            s.scannedAt,
            c.scanCount
        FROM scanned_beacons s
        INNER JOIN (
            SELECT uuid, major, minor, MAX(scannedAt) as maxTime, COUNT(*) as scanCount
            FROM scanned_beacons
            GROUP BY uuid, major, minor
        ) c ON s.uuid = c.uuid AND s.major = c.major AND s.minor = c.minor AND s.scannedAt = c.maxTime
        ORDER BY s.scannedAt DESC
        LIMIT 100
    """)
    fun getRecentScansFlow(): Flow<List<ScannedBeaconEntity>>

    // 僅符合目標 UUID（白名單）的裝置，取每個設備的「最新一筆」記錄及出現次數
    @Query("""
        SELECT 
            s.id,
            s.uuid,
            s.major,
            s.minor,
            s.rssi,
            s.distance,
            s.isInWhitelist,
            s.scannedAt,
            c.scanCount
        FROM scanned_beacons s
        INNER JOIN (
            SELECT uuid, major, minor, MAX(scannedAt) as maxTime, COUNT(*) as scanCount
            FROM scanned_beacons
            WHERE isInWhitelist = 1
            GROUP BY uuid, major, minor
        ) c ON s.uuid = c.uuid AND s.major = c.major AND s.minor = c.minor AND s.scannedAt = c.maxTime
        WHERE s.isInWhitelist = 1
        ORDER BY s.scannedAt DESC
        LIMIT 100
    """)
    fun getRecentTargetUuidScansFlow(): Flow<List<ScannedBeaconEntity>>

    // 單一裝置的 RSSI 歷史，用於即時 RSSI 詳情頁（最近 1000 筆）
    @Query("""
        SELECT * FROM scanned_beacons 
        WHERE uuid = :uuid AND major = :major AND minor = :minor 
        ORDER BY scannedAt DESC 
        LIMIT 1000
    """)
    fun getRssiHistoryFlow(uuid: String, major: Int, minor: Int): Flow<List<ScannedBeaconEntity>>

    // 刪除此裝置早於某時間的記錄（用於每 100 筆清空刷新）
    @Query("""
        DELETE FROM scanned_beacons 
        WHERE uuid = :uuid AND major = :major AND minor = :minor AND scannedAt < :cutoffScannedAt
    """)
    suspend fun deleteOlderThanForDevice(uuid: String, major: Int, minor: Int, cutoffScannedAt: Long)
    
    @Query("SELECT * FROM scanned_beacons ORDER BY scannedAt DESC")
    suspend fun getAllScans(): List<ScannedBeaconEntity>

    // 取得指定 UUID 的所有記錄（用於匯出）
    @Query("SELECT * FROM scanned_beacons WHERE uuid = :uuid ORDER BY scannedAt DESC")
    suspend fun getByUuid(uuid: String): List<ScannedBeaconEntity>

    // 取得所有不重複的目標 UUID 設備（用於服務啟動時恢復追蹤列表）
    @Query("""
        SELECT MIN(id) as id, uuid, major, minor, 0 as rssi, 0.0 as distance, 
               1 as isInWhitelist, MAX(scannedAt) as scannedAt, 1 as scanCount
        FROM scanned_beacons 
        WHERE isInWhitelist = 1
        GROUP BY uuid, major, minor
    """)
    suspend fun getDistinctTargetDevices(): List<ScannedBeaconEntity>
    
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

    // 刪除超過指定時間戳的記錄（數據保留用），返回刪除行數
    @Query("DELETE FROM scanned_beacons WHERE scannedAt < :cutoffTimestamp")
    suspend fun deleteOlderThanTimestamp(cutoffTimestamp: Long): Int
    
    @Query("DELETE FROM scanned_beacons")
    suspend fun deleteAll()
    
    @Query("DELETE FROM scanned_beacons WHERE id IN (SELECT id FROM scanned_beacons ORDER BY scannedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
    
    @Query("SELECT COUNT(*) FROM scanned_beacons")
    suspend fun getCount(): Int
}
