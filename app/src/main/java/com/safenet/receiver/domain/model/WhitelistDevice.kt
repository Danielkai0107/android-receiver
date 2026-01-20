package com.safenet.receiver.domain.model

data class WhitelistDevice(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val deviceName: String,
    val macAddress: String? = null,  // 改為可選，預設為 null
    val syncedAt: Long
)
