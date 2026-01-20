package com.safenet.receiver.data.local.dao

import androidx.room.*
import com.safenet.receiver.data.local.entity.WhitelistDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDeviceDao {
    
    @Query("SELECT * FROM whitelist_devices ORDER BY deviceName ASC")
    fun getAllFlow(): Flow<List<WhitelistDeviceEntity>>
    
    @Query("SELECT * FROM whitelist_devices")
    suspend fun getAll(): List<WhitelistDeviceEntity>
    
    @Query("SELECT * FROM whitelist_devices WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): WhitelistDeviceEntity?
    
    @Query("SELECT COUNT(*) FROM whitelist_devices")
    suspend fun getCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<WhitelistDeviceEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: WhitelistDeviceEntity)
    
    @Query("DELETE FROM whitelist_devices")
    suspend fun deleteAll()
    
    @Delete
    suspend fun delete(device: WhitelistDeviceEntity)
    
    @Query("SELECT * FROM whitelist_devices WHERE deviceName LIKE '%' || :query || '%' OR uuid LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<WhitelistDeviceEntity>>
}
