package com.safenet.receiver.data.remote.api

import com.safenet.receiver.data.remote.model.BeaconDataRequest
import com.safenet.receiver.data.remote.model.BeaconDataResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * 上傳 Beacon 資料的 API 接口
 */
interface UploadApi {

    /**
     * 上傳到指定 URL（本機儲存的 URL，可編輯）
     */
    @POST
    suspend fun uploadBeaconDataToUrl(
        @Url fullUrl: String,
        @Body request: BeaconDataRequest
    ): BeaconDataResponse

    /**
     * 上傳 Beacon 掃描資料到 Cloud Functions（固定 base URL）
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
