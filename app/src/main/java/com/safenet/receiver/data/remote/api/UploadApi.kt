package com.safenet.receiver.data.remote.api

import com.safenet.receiver.data.remote.model.BeaconDataRequest
import com.safenet.receiver.data.remote.model.BeaconDataResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 上傳 Beacon 資料的 API 接口
 */
interface UploadApi {
    
    /**
     * 上傳 Beacon 掃描資料到 Cloud Functions
     * @param request Beacon 資料請求
     */
    @POST("receiveBeaconData")
    suspend fun uploadBeaconData(
        @Body request: BeaconDataRequest
    ): BeaconDataResponse
}

/**
 * 備用上傳 API (Cloud Run)
 */
interface FallbackUploadApi {
    
    /**
     * 上傳 Beacon 掃描資料到 Cloud Run
     * @param request Beacon 資料請求
     */
    @POST("/")
    suspend fun uploadBeaconData(
        @Body request: BeaconDataRequest
    ): BeaconDataResponse
}
