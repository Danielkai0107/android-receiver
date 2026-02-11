package com.safenet.receiver.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    
    val scanFrequency: StateFlow<Int> = preferenceManager.getScanFrequency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_SCAN_FREQUENCY)
    
    val uploadInterval: StateFlow<Int> = preferenceManager.getUploadInterval()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_UPLOAD_INTERVAL)
    
    val whitelistSyncInterval: StateFlow<Int> = preferenceManager.getWhitelistSyncInterval()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_WHITELIST_SYNC_INTERVAL)
    
    val gpsUpdateFrequency: StateFlow<Int> = preferenceManager.getGpsUpdateFrequency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_GPS_UPDATE_FREQUENCY)
    
    val offlineCacheLimit: StateFlow<Int> = preferenceManager.getOfflineCacheLimit()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_OFFLINE_CACHE_LIMIT)

    val dataRetentionDays: StateFlow<Int> = preferenceManager.getDataRetentionDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_DATA_RETENTION_DAYS)

    val uploadUrl: StateFlow<String> = preferenceManager.getUploadUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PreferenceManager.DEFAULT_UPLOAD_URL)

    fun updateScanFrequency(seconds: Int) {
        viewModelScope.launch {
            preferenceManager.saveScanFrequency(seconds)
        }
    }
    
    fun updateUploadInterval(seconds: Int) {
        viewModelScope.launch {
            preferenceManager.saveUploadInterval(seconds)
        }
    }
    
    fun updateWhitelistSyncInterval(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.saveWhitelistSyncInterval(minutes)
        }
    }
    
    fun updateGpsUpdateFrequency(minutes: Int) {
        viewModelScope.launch {
            preferenceManager.saveGpsUpdateFrequency(minutes)
        }
    }
    
    fun updateOfflineCacheLimit(limit: Int) {
        viewModelScope.launch {
            preferenceManager.saveOfflineCacheLimit(limit)
        }
    }

    fun updateDataRetentionDays(days: Int) {
        viewModelScope.launch {
            preferenceManager.saveDataRetentionDays(days)
        }
    }

    fun saveUploadUrl(url: String) {
        viewModelScope.launch {
            preferenceManager.saveUploadUrl(url)
        }
    }
}
