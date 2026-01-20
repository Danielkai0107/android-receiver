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
    val uploadStatus: String // PENDING, UPLOADING, UPLOADED, FAILED
)
