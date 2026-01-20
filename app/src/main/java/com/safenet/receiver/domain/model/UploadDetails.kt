package com.safenet.receiver.domain.model

/**
 * 上傳詳情 - 包含 HTTP 請求和響應資訊
 */
data class UploadDetails(
    val success: Boolean,
    val requestUrl: String,
    val requestBody: String,
    val requestHeaders: Map<String, String>,
    val responseCode: Int,
    val responseBody: String,
    val responseHeaders: Map<String, String>,
    val responseDuration: Long,  // 毫秒
    val uploadedAt: Long = System.currentTimeMillis()
)
