package com.safenet.receiver.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_beacons")
data class ScannedBeaconEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double,
    val isInWhitelist: Boolean,
    val scannedAt: Long,
    val scanCount: Int = 1  // 掃描次數統計
)
