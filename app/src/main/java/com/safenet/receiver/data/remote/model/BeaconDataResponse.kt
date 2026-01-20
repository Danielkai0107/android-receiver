package com.safenet.receiver.data.remote.model

import com.google.gson.annotations.SerializedName

data class BeaconDataResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("error")
    val error: String?
)
