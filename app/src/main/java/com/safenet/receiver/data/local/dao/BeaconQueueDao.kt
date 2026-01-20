package com.safenet.receiver.data.local.dao

import androidx.room.*
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeaconQueueDao {
    
    // 按 UUID+Major+Minor 分組，每個設備只取最新的 PENDING 記錄（去重）
    @Query("""
        SELECT * FROM beacon_queue 
        WHERE uploadStatus = 'PENDING' 
        AND id IN (
            SELECT MAX(id) 
            FROM beacon_queue 
            WHERE uploadStatus = 'PENDING'
            GROUP BY uuid, major, minor
        )
        ORDER BY scannedAt ASC
    """)
    suspend fun getPendingBeacons(): List<BeaconQueueEntity>
    
    // 按 UUID+Major+Minor 分組，只顯示每個設備的最新上傳記錄（去重）
    @Query("""
        SELECT * FROM beacon_queue 
        WHERE uploadStatus = 'UPLOADED' 
        AND id IN (
            SELECT MAX(id) 
            FROM beacon_queue 
            WHERE uploadStatus = 'UPLOADED'
            GROUP BY uuid, major, minor
        )
        ORDER BY scannedAt DESC 
        LIMIT 100
    """)
    fun getUploadedBeaconsFlow(): Flow<List<BeaconQueueEntity>>
    
    @Query("SELECT * FROM beacon_queue ORDER BY scannedAt DESC")
    fun getAllFlow(): Flow<List<BeaconQueueEntity>>
    
    // 統計總記錄數（顯示總次數）
    @Query("""
        SELECT COUNT(*) 
        FROM beacon_queue 
        WHERE uploadStatus = 'PENDING'
    """)
    fun getPendingCountFlow(): Flow<Int>
    
    @Query("""
        SELECT COUNT(*) 
        FROM beacon_queue 
        WHERE uploadStatus = 'UPLOADED'
    """)
    fun getUploadedCountFlow(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beacon: BeaconQueueEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(beacons: List<BeaconQueueEntity>)
    
    @Update
    suspend fun update(beacon: BeaconQueueEntity)
    
    @Query("UPDATE beacon_queue SET uploadStatus = :status WHERE id IN (:ids)")
    suspend fun updateStatus(ids: List<Long>, status: String)
    
    @Delete
    suspend fun delete(beacon: BeaconQueueEntity)
    
    @Query("DELETE FROM beacon_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    // 刪除同一個 Beacon 的舊 PENDING 記錄
    @Query("DELETE FROM beacon_queue WHERE uuid = :uuid AND major = :major AND minor = :minor AND uploadStatus = 'PENDING'")
    suspend fun deleteOldPending(uuid: String, major: Int, minor: Int)
    
    // 查詢同一個 Beacon 是否已有 PENDING 記錄
    @Query("SELECT * FROM beacon_queue WHERE uuid = :uuid AND major = :major AND minor = :minor AND uploadStatus = 'PENDING' LIMIT 1")
    suspend fun getPendingBeacon(uuid: String, major: Int, minor: Int): BeaconQueueEntity?
    
    @Query("DELETE FROM beacon_queue WHERE uploadStatus = 'UPLOADED' AND scannedAt < :timestamp")
    suspend fun deleteOldUploaded(timestamp: Long)
    
    @Query("DELETE FROM beacon_queue WHERE id IN (SELECT id FROM beacon_queue ORDER BY scannedAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
    
    @Query("SELECT COUNT(*) FROM beacon_queue")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM beacon_queue")
    suspend fun deleteAll()
    
    // 整合相同的 PENDING 記錄，只保留信號最強的
    @Query("""
        DELETE FROM beacon_queue 
        WHERE uploadStatus = 'PENDING'
        AND id NOT IN (
            SELECT id FROM beacon_queue 
            WHERE uploadStatus = 'PENDING'
            AND (uuid, major, minor, rssi) IN (
                SELECT uuid, major, minor, MAX(rssi)
                FROM beacon_queue 
                WHERE uploadStatus = 'PENDING'
                GROUP BY uuid, major, minor
            )
        )
    """)
    suspend fun consolidatePendingBeacons()
}
