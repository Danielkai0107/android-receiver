package com.safenet.receiver.presentation.uploadhistory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.data.local.dao.BeaconQueueDao
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadHistoryViewModel @Inject constructor(
    private val beaconQueueDao: BeaconQueueDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "UploadHistoryViewModel"
    }
    
    private val _uploadHistory = MutableStateFlow<List<BeaconQueueEntity>>(emptyList())
    val uploadHistory: StateFlow<List<BeaconQueueEntity>> = _uploadHistory.asStateFlow()
    
    init {
        observeUploadHistory()
    }
    
    private fun observeUploadHistory() {
        viewModelScope.launch {
            // 獲取所有已上傳記錄（不去重）
            beaconQueueDao.getAllUploadedFlow().collect { uploaded ->
                _uploadHistory.value = uploaded
                Log.d(TAG, "上傳記錄更新: ${uploaded.size} 筆")
            }
        }
    }
    
    fun clearAllUploadHistory() {
        viewModelScope.launch {
            try {
                beaconQueueDao.deleteAllUploaded()
                Log.d(TAG, "已清除所有上傳記錄")
            } catch (e: Exception) {
                Log.e(TAG, "清除上傳記錄失敗", e)
            }
        }
    }
}
