package com.safenet.receiver.data.repository

import android.util.Log
import com.safenet.receiver.data.local.dao.WhitelistDeviceDao
import com.safenet.receiver.data.local.entity.WhitelistDeviceEntity
import com.safenet.receiver.data.remote.api.CloudFunctionApi
import com.safenet.receiver.domain.model.WhitelistDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistRepository @Inject constructor(
    @com.safenet.receiver.di.WhitelistRetrofit private val whitelistApi: CloudFunctionApi,
    private val whitelistDeviceDao: WhitelistDeviceDao
) {
    
    companion object {
        private const val TAG = "WhitelistRepository"
    }
    
    fun getAllDevicesFlow(): Flow<List<WhitelistDevice>> {
        return whitelistDeviceDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getDeviceCount(): Int {
        return whitelistDeviceDao.getCount()
    }
    
    suspend fun isInWhitelist(uuid: String): Boolean {
        return whitelistDeviceDao.getByUuid(uuid) != null
    }
    
    /**
     * 從 Cloud Function API 同步白名單（全局白名單，不需要 gateway_id）
     */
    suspend fun syncWhitelist(gatewayId: String = ""): Result<Int> {
        return try {
            Log.d(TAG, "同步全局白名單...")
            
            val response = whitelistApi.getDeviceWhitelist()
            
            if (response.success && response.devices.isNotEmpty()) {
                Log.d(TAG, "API 回應成功: count=${response.count}")
                
                // 清空舊資料
                whitelistDeviceDao.deleteAll()
                
                // 插入新資料
                val entities = response.devices.map { device ->
                    WhitelistDeviceEntity(
                        uuid = device.uuid,
                        major = device.major,
                        minor = device.minor,
                        deviceName = device.deviceName,
                        macAddress = device.macAddress,  // 直接使用 API 返回的值（可能為 null）
                        syncedAt = System.currentTimeMillis()
                    )
                }
                
                whitelistDeviceDao.insertAll(entities)
                Log.d(TAG, "全局白名單同步成功: ${entities.size} 個設備")
                
                Result.success(response.count)
            } else {
                // 白名單為空
                Log.w(TAG, "白名單為空，將掃描所有 Beacon")
                
                // 清空舊白名單
                whitelistDeviceDao.deleteAll()
                
                // 返回成功，count = 0
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "白名單同步異常", e)
            Result.failure(e)
        }
    }
    
    fun searchDevices(query: String): Flow<List<WhitelistDevice>> {
        return whitelistDeviceDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    private fun WhitelistDeviceEntity.toDomain() = WhitelistDevice(
        uuid = uuid,
        major = major,
        minor = minor,
        deviceName = deviceName,
        macAddress = macAddress,
        syncedAt = syncedAt
    )
}
