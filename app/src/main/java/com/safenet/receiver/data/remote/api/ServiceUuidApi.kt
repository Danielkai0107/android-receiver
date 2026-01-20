package com.safenet.receiver.data.remote.api

import retrofit2.http.GET

/**
 * Service UUID API 回應
 * API 返回格式：
 * {
 *   "success": true,
 *   "uuids": ["FDA50693-A4E2-4FB1-AFCF-C6EB01234567", "FDA50693-..."],
 *   "count": 2,
 *   "timestamp": 1768892046262
 * }
 */
data class ServiceUuidResponse(
    val success: Boolean,
    val uuids: List<String>,  // 直接是字串陣列
    val count: Int,
    val timestamp: Long
)

interface ServiceUuidApi {
    @GET("getServiceUuids")
    suspend fun getServiceUuids(): ServiceUuidResponse
}
