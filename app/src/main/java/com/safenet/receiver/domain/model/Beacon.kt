package com.safenet.receiver.domain.model

data class Beacon(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double,
    val latitude: Double,
    val longitude: Double,
    val scannedAt: Long
)
