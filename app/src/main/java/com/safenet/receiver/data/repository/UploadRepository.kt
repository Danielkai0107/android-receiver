package com.safenet.receiver.data.repository

import android.util.Log
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import com.safenet.receiver.data.remote.api.UploadApi
import com.safenet.receiver.data.remote.api.FallbackUploadApi
import com.safenet.receiver.data.remote.model.BeaconData
import com.safenet.receiver.data.remote.model.BeaconDataRequest
import com.safenet.receiver.data.remote.model.BeaconDataResponse
import com.safenet.receiver.di.PrimaryUploadRetrofit
import com.safenet.receiver.di.FallbackUploadRetrofit
import com.safenet.receiver.utils.NetworkUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    @PrimaryUploadRetrofit private val primaryUploadApi: UploadApi,
    @FallbackUploadRetrofit private val fallbackUploadApi: FallbackUploadApi,
    private val networkUtil: NetworkUtil
) {
    
    companion object {
        private const val TAG = "UploadRepository"
    }
    
    /**
     * 批次上傳 Beacon 資料（主要端點 + 備用端點）
     */
    suspend fun uploadBeacons(
        gatewayId: String,
        beacons: List<BeaconQueueEntity>,
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        if (!networkUtil.isNetworkAvailable()) {
            Log.w(TAG, "無網路連線")
            return Result.failure(Exception("無網路連線"))
        }
        
        // 按 UUID 分組，每個 UUID 只保留最強訊號
        val bestBeacons = beacons
            .groupBy { it.uuid }
            .mapValues { (_, list) -> list.maxByOrNull { it.rssi } }
            .values
            .filterNotNull()
        
        val request = BeaconDataRequest(
            gatewayId = gatewayId,
            lat = latitude,
            lng = longitude,
            timestamp = System.currentTimeMillis(),
            beacons = bestBeacons.map { beacon ->
                BeaconData(
                    uuid = beacon.uuid,
                    major = beacon.major,
                    minor = beacon.minor,
                    rssi = beacon.rssi
                )
            }
        )
        
        Log.d(TAG, "上傳 Beacon 資料: gateway=$gatewayId, count=${request.beacons.size}")
        
        // 1. 先嘗試主要端點 (Cloud Functions)
        Log.d(TAG, "嘗試主要端點 (Cloud Functions)...")
        val primaryResult = tryUploadToPrimary(request)
        
        if (primaryResult.isSuccess) {
            Log.d(TAG, "✅ 主要端點上傳成功")
            return primaryResult
        }
        
        // 2. 主要端點失敗，嘗試備用端點 (Cloud Run)
        Log.w(TAG, "主要端點失敗: ${primaryResult.exceptionOrNull()?.message}")
        Log.d(TAG, "嘗試備用端點 (Cloud Run)...")
        val fallbackResult = tryUploadToFallback(request)
        
        if (fallbackResult.isSuccess) {
            Log.d(TAG, "✅ 備用端點上傳成功")
            return fallbackResult
        }
        
        // 3. 兩個端點都失敗
        Log.e(TAG, "❌ 所有端點都失敗")
        Log.e(TAG, "  - 主要端點: ${primaryResult.exceptionOrNull()?.message}")
        Log.e(TAG, "  - 備用端點: ${fallbackResult.exceptionOrNull()?.message}")
        return fallbackResult
    }
    
    /**
     * 嘗試上傳到主要端點 (Cloud Functions)
     */
    private suspend fun tryUploadToPrimary(request: BeaconDataRequest): Result<Unit> {
        return try {
            val response = primaryUploadApi.uploadBeaconData(request)
            handleResponse(response, "主要端點")
        } catch (e: retrofit2.HttpException) {
            Result.failure(Exception(handleHttpException(e, "主要端點")))
        } catch (e: Exception) {
            Log.e(TAG, "主要端點異常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 嘗試上傳到備用端點 (Cloud Run)
     */
    private suspend fun tryUploadToFallback(request: BeaconDataRequest): Result<Unit> {
        return try {
            val response = fallbackUploadApi.uploadBeaconData(request)
            handleResponse(response, "備用端點")
        } catch (e: retrofit2.HttpException) {
            Result.failure(Exception(handleHttpException(e, "備用端點")))
        } catch (e: Exception) {
            Log.e(TAG, "備用端點異常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 處理 API 回應
     */
    private fun handleResponse(response: BeaconDataResponse, endpoint: String): Result<Unit> {
        return if (response.success) {
            Log.d(TAG, "$endpoint 上傳成功")
            Result.success(Unit)
        } else {
            val error = response.message ?: response.error ?: "Unknown error"
            Log.e(TAG, "$endpoint 上傳失敗: $error")
            Result.failure(Exception(error))
        }
    }
    
    /**
     * 處理 HTTP 異常
     */
    private fun handleHttpException(e: retrofit2.HttpException, endpoint: String): String {
        val errorBody = try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            null
        }
        
        val errorMessage = if (errorBody != null) {
            try {
                val gson = com.google.gson.Gson()
                val errorResponse = gson.fromJson(errorBody, BeaconDataResponse::class.java)
                errorResponse.message ?: errorResponse.error ?: "HTTP ${e.code()}"
            } catch (ex: Exception) {
                errorBody
            }
        } else {
            "HTTP ${e.code()}"
        }
        
        Log.e(TAG, "$endpoint 失敗 (HTTP ${e.code()}): $errorMessage")
        return errorMessage
    }
}
