package com.safenet.receiver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_devices")
data class WhitelistDeviceEntity(
    @PrimaryKey val uuid: String,
    val major: Int,
    val minor: Int,
    val deviceName: String,
    val macAddress: String? = null,  // 改為可選，預設為 null
    val syncedAt: Long
)
