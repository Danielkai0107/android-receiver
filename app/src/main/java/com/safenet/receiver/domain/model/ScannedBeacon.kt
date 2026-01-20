package com.safenet.receiver.domain.model

data class ScannedBeacon(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double,
    val isInWhitelist: Boolean,
    val scannedAt: Long,
    val scanCount: Int = 1  // 掃描次數
)
