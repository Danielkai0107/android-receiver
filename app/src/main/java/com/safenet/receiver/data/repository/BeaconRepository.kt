package com.safenet.receiver.data.repository

import android.util.Log
import com.safenet.receiver.data.local.dao.BeaconQueueDao
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import com.safenet.receiver.domain.model.Beacon
import com.safenet.receiver.domain.model.UploadStatus
import com.safenet.receiver.utils.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeaconRepository @Inject constructor(
    private val beaconQueueDao: BeaconQueueDao,
    private val preferenceManager: PreferenceManager
) {
    
    companion object {
        private const val TAG = "BeaconRepository"
    }
    
    private val _maxDistance = MutableStateFlow(0.0)
    val maxDistance: StateFlow<Double> = _maxDistance.asStateFlow()
    
    /**
     * 將掃描到的 Beacon 加入上傳佇列
     * 同一個 Beacon (UUID+Major+Minor) 只保留信號最強的 PENDING 記錄
     */
    suspend fun addToQueue(beacon: Beacon): Result<Long> {
        return try {
            // 檢查是否已有同一個 Beacon 的 PENDING 記錄
            val existingBeacon = beaconQueueDao.getPendingBeacon(beacon.uuid, beacon.major, beacon.minor)
            
            if (existingBeacon != null) {
                // 比較信號強度（RSSI 越高越好，-60 > -70）
                if (beacon.rssi > existingBeacon.rssi) {
                    // 新記錄信號更強，刪除舊記錄
                    beaconQueueDao.deleteOldPending(beacon.uuid, beacon.major, beacon.minor)
                    Log.d(TAG, "更新 Beacon（信號更強）: uuid=${beacon.uuid}, 舊 RSSI=${existingBeacon.rssi} → 新 RSSI=${beacon.rssi}")
                } else {
                    // 舊記錄信號更強，保留舊記錄，不插入新記錄
                    Log.d(TAG, "保留舊記錄（信號更強）: uuid=${beacon.uuid}, 舊 RSSI=${existingBeacon.rssi} ≥ 新 RSSI=${beacon.rssi}")
                    return Result.success(existingBeacon.id)
                }
            }
            
            val entity = BeaconQueueEntity(
                uuid = beacon.uuid,
                major = beacon.major,
                minor = beacon.minor,
                rssi = beacon.rssi,
                latitude = beacon.latitude,
                longitude = beacon.longitude,
                scannedAt = beacon.scannedAt,
                uploadStatus = UploadStatus.PENDING.name
            )
            
            val id = beaconQueueDao.insert(entity)
            Log.d(TAG, "Beacon 加入佇列: uuid=${beacon.uuid}, major=${beacon.major}, minor=${beacon.minor}, rssi=${beacon.rssi}, distance=${beacon.distance}m")
            
            // 更新最遠距離
            if (beacon.distance > _maxDistance.value) {
                _maxDistance.value = beacon.distance
                Log.d(TAG, "更新最遠距離: ${beacon.distance}m")
            }
            
            // 檢查快取上限
            checkCacheLimit()
            
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Beacon 加入佇列失敗", e)
            Result.failure(e)
        }
    }
    
    /**
     * 重置最遠距離
     */
    fun resetMaxDistance() {
        _maxDistance.value = 0.0
    }
    
    suspend fun getPendingBeacons(): List<BeaconQueueEntity> {
        return beaconQueueDao.getPendingBeacons()
    }
    
    suspend fun consolidatePendingBeacons() {
        beaconQueueDao.consolidatePendingBeacons()
    }
    
    suspend fun updateStatus(ids: List<Long>, status: UploadStatus) {
        beaconQueueDao.updateStatus(ids, status.name)
    }
    
    suspend fun deleteUploaded(ids: List<Long>) {
        beaconQueueDao.deleteByIds(ids)
    }
    
    fun getPendingCountFlow(): Flow<Int> {
        return beaconQueueDao.getPendingCountFlow()
    }
    
    fun getUploadedCountFlow(): Flow<Int> {
        return beaconQueueDao.getUploadedCountFlow()
    }
    
    /**
     * 檢查並清理超過上限的快取
     */
    private suspend fun checkCacheLimit() {
        val limit = preferenceManager.getOfflineCacheLimit().first()
        val count = beaconQueueDao.getCount()
        
        if (count > limit) {
            val deleteCount = count - limit
            beaconQueueDao.deleteOldest(deleteCount)
            Log.d(TAG, "清理舊快取: 刪除 $deleteCount 筆")
        }
    }
    
    /**
     * 清理舊的已上傳記錄（超過 24 小時）
     */
    suspend fun cleanOldUploaded() {
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        beaconQueueDao.deleteOldUploaded(oneDayAgo)
    }
}
