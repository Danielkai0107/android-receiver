package com.safenet.receiver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beacon_queue")
data class BeaconQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val latitude: Double,
    val longitude: Double,
    val scannedAt: Long,
    val uploadStatus: String, // PENDING, UPLOADING, UPLOADED, FAILED
    
    // HTTP 請求和響應資訊
    val uploadedAt: Long? = null,  // 上傳時間戳
    val requestUrl: String? = null,  // 請求 URL
    val requestBody: String? = null,  // 請求 Body (JSON)
    val requestHeaders: String? = null,  // 請求 Headers (JSON)
    val responseCode: Int? = null,  // 響應狀態碼
    val responseBody: String? = null,  // 響應 Body (JSON)
    val responseHeaders: String? = null,  // 響應 Headers (JSON)
    val responseDuration: Long? = null  // 響應時間 (ms)
)
