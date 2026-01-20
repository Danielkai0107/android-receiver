package com.safenet.receiver.presentation.scans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.data.local.dao.ScannedBeaconDao
import com.safenet.receiver.domain.model.ScannedBeacon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScansViewModel @Inject constructor(
    private val scannedBeaconDao: ScannedBeaconDao
) : ViewModel() {
    
    val scannedBeacons: StateFlow<List<ScannedBeacon>> = scannedBeaconDao.getRecentScansFlow()
        .map { entities ->
            entities.map { entity ->
                ScannedBeacon(
                    uuid = entity.uuid,
                    major = entity.major,
                    minor = entity.minor,
                    rssi = entity.rssi,
                    distance = entity.distance,
                    isInWhitelist = entity.isInWhitelist,
                    scannedAt = entity.scannedAt,
                    scanCount = entity.scanCount
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    val totalCount: StateFlow<Int> = scannedBeaconDao.getTotalCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    
    val whitelistCount: StateFlow<Int> = scannedBeaconDao.getWhitelistCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    
    val uniqueDeviceCount: StateFlow<Int> = scannedBeaconDao.getUniqueDeviceCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    
    fun clearAllScans() {
        viewModelScope.launch {
            scannedBeaconDao.deleteAll()
        }
    }
}
