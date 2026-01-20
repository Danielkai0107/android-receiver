package com.safenet.receiver.data.remote.model

import com.google.gson.annotations.SerializedName

data class WhitelistResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("gateway")
    val gateway: GatewayInfo?,
    
    @SerializedName("devices")
    val devices: List<WhitelistDeviceDto>,
    
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("error")
    val error: String?
)

data class GatewayInfo(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("tenantId")
    val tenantId: String,
    
    @SerializedName("type")
    val type: String
)

data class WhitelistDeviceDto(
    @SerializedName("uuid")
    val uuid: String,
    
    @SerializedName("major")
    val major: Int,
    
    @SerializedName("minor")
    val minor: Int,
    
    @SerializedName("deviceName")
    val deviceName: String,
    
    @SerializedName("macAddress")
    val macAddress: String? = null  // 改為可選，預設為 null
)
