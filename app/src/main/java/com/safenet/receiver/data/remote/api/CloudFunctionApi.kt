package com.safenet.receiver.data.remote.api

import com.safenet.receiver.data.remote.model.BeaconDataRequest
import com.safenet.receiver.data.remote.model.BeaconDataResponse
import com.safenet.receiver.data.remote.model.WhitelistResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CloudFunctionApi {
    
    /**
     * 獲取白名單設備列表（全局白名單，所有活躍設備）
     * Cloud Run URL 完整，使用 "/" 即可
     */
    @GET("/")
    suspend fun getDeviceWhitelist(): WhitelistResponse
    
    /**
     * 上傳 Beacon 掃描資料
     * @param request Beacon 資料請求
     * Cloud Run URL 完整，使用 "/" 即可
     */
    @POST("/")
    suspend fun uploadBeaconData(
        @Body request: BeaconDataRequest
    ): BeaconDataResponse
}
