package com.safenet.receiver.data.remote.model

import com.google.gson.annotations.SerializedName

data class BeaconDataRequest(
    @SerializedName("gateway_id")
    val gatewayId: String,
    
    @SerializedName("lat")
    val lat: Double,
    
    @SerializedName("lng")
    val lng: Double,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("beacons")
    val beacons: List<BeaconData>
)

data class BeaconData(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("major")
    val major: Int,
    
    @SerializedName("minor")
    val minor: Int,
    
    @SerializedName("rssi")
    val rssi: Int
)
