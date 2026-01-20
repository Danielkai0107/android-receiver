package com.safenet.receiver.presentation.whitelist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.data.local.dao.BeaconQueueDao
import com.safenet.receiver.domain.model.ScannedBeacon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val beaconQueueDao: BeaconQueueDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "UploadRecordsViewModel"
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 獲取所有已上傳的記錄（從 beacon_queue 表，status = UPLOADED）
    val uploadRecords: StateFlow<List<ScannedBeacon>> = _searchQuery
        .flatMapLatest { query ->
            beaconQueueDao.getUploadedBeaconsFlow()
        }
        .map { entities ->
            entities.map { entity ->
                ScannedBeacon(
                    uuid = entity.uuid,
                    major = entity.major,
                    minor = entity.minor,
                    rssi = entity.rssi,
                    distance = 0.0,  // beacon_queue 沒有 distance 欄位
                    isInWhitelist = true,  // UPLOADED 的都是目標 UUID
                    scannedAt = entity.scannedAt
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    val totalCount: StateFlow<Int> = beaconQueueDao.getUploadedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
