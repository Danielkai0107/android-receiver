package com.safenet.receiver.data.repository

import android.util.Log
import com.safenet.receiver.data.remote.api.ServiceUuidApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceUuidRepository @Inject constructor(
    private val serviceUuidApi: ServiceUuidApi
) {
    private val TAG = "ServiceUuidRepository"
    
    private val _serviceUuids = MutableStateFlow<Set<String>>(emptySet())
    val serviceUuids: StateFlow<Set<String>> = _serviceUuids
    
    suspend fun syncServiceUuids(): Result<Set<String>> {
        return try {
            Log.d(TAG, "開始同步 Service UUID...")
            val response = serviceUuidApi.getServiceUuids()
            
            if (response.success && response.uuids.isNotEmpty()) {
                // API 現在直接返回字串陣列
                val uuids = response.uuids.toSet()
                _serviceUuids.value = uuids
                Log.d(TAG, "✅ 同步成功，獲取 ${uuids.size} 個 UUID:")
                uuids.forEach { uuid ->
                    Log.d(TAG, "   - $uuid")
                }
                Result.success(uuids)
            } else if (response.success && response.uuids.isEmpty()) {
                Log.w(TAG, "⚠️ API 返回成功但 UUID 列表為空")
                _serviceUuids.value = emptySet()
                Result.success(emptySet())
            } else {
                Log.e(TAG, "❌ API 返回失敗")
                Result.failure(Exception("API 返回失敗"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 同步失敗: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun isTargetUuid(uuid: String): Boolean {
        val uuids = _serviceUuids.value
        return uuids.any { it.equals(uuid, ignoreCase = true) }
    }
}
