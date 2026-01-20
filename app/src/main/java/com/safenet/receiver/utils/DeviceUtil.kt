package com.safenet.receiver.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import java.util.*

object DeviceUtil {
    
    @Suppress("HardwareIds", "MissingPermission")
    fun getDeviceIMEI(context: Context): String {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return generateFallbackId(context)
            }
            
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            
            val imei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager?.imei
            } else {
                @Suppress("DEPRECATION")
                telephonyManager?.deviceId
            }
            
            imei?.takeIf { it.isNotBlank() } ?: generateFallbackId(context)
        } catch (e: Exception) {
            e.printStackTrace()
            generateFallbackId(context)
        }
    }
    
    private fun generateFallbackId(context: Context): String {
        // 使用 Android ID 作為備用
        @Suppress("HardwareIds")
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        return "ANDROID-${androidId}"
    }
    
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE}"
    }
}
