package com.safenet.receiver.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "LocationService"
        private const val MIN_DISTANCE_METERS = 50f  // 位置變化小於 50 米時重用
    }
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    private var lastLocation: Location? = null
    
    /**
     * 獲取當前位置（如果位置變化小於 50 米則重用）
     */
    suspend fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "沒有位置權限")
            return null
        }
        
        return try {
            // 使用 await() 等待異步任務完成
            val lastKnownLocation = fusedLocationClient.lastLocation.await()
            
            if (lastKnownLocation != null) {
                val distance = lastLocation?.distanceTo(lastKnownLocation) ?: Float.MAX_VALUE
                
                if (distance < MIN_DISTANCE_METERS && lastLocation != null) {
                    Log.d(TAG, "重用舊位置 (距離: ${distance}m)")
                    return lastLocation
                }
                
                lastLocation = lastKnownLocation
                Log.d(TAG, "獲取位置: lat=${lastKnownLocation.latitude}, lng=${lastKnownLocation.longitude}")
                return lastKnownLocation
            }
            
            Log.w(TAG, "lastLocation 為 null")
            null
        } catch (e: Exception) {
            Log.e(TAG, "獲取位置失敗", e)
            null
        }
    }
    
    /**
     * 持續監聽位置更新
     */
    fun getLocationUpdates(intervalMillis: Long = 120000L): Flow<Location> = callbackFlow {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "沒有位置權限")
            close()
            return@callbackFlow
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateIntervalMillis(intervalMillis / 2)
            setMaxUpdateDelayMillis(intervalMillis * 2)
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "位置更新: lat=${location.latitude}, lng=${location.longitude}")
                    lastLocation = location
                    trySend(location)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
