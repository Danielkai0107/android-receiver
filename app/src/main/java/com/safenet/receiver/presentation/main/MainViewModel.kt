package com.safenet.receiver.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.safenet.receiver.data.local.dao.ScannedBeaconDao
import com.safenet.receiver.data.repository.BeaconRepository
import com.safenet.receiver.data.repository.ServiceUuidRepository
import com.safenet.receiver.data.repository.WhitelistRepository
import com.safenet.receiver.di.WorkerModule
import com.safenet.receiver.utils.DeviceUtil
import com.safenet.receiver.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class MainViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val beaconRepository: BeaconRepository,
    private val serviceUuidRepository: ServiceUuidRepository,
    private val scannedBeaconDao: ScannedBeaconDao,
    private val preferenceManager: PreferenceManager,
    private val workManager: WorkManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        observeData()
        WorkerModule.schedulePeriodicUpload(workManager)
    }
    
    private fun observeData() {
        viewModelScope.launch {
            combine(
                preferenceManager.getGatewayId(),
                beaconRepository.getPendingCountFlow(),
                beaconRepository.getUploadedCountFlow(),
                beaconRepository.maxDistance,
                scannedBeaconDao.getTotalCountFlow(),
                serviceUuidRepository.serviceUuids
            ) { values: Array<*> ->
                val gatewayId = values[0] as? String
                val pending = values[1] as? Int ?: 0
                val uploaded = values[2] as? Int ?: 0
                val maxDistance = values[3] as? Double ?: 0.0
                val scanned = values[4] as? Int ?: 0
                @Suppress("UNCHECKED_CAST")
                val serviceUuidsSet = values[5] as? Set<String> ?: emptySet()
                
                _uiState.update { it.copy(
                    gatewayId = gatewayId ?: "未設定",
                    scannedCount = scanned,
                    uploadedCount = uploaded,
                    pendingCount = pending,
                    maxDistance = maxDistance,
                    serviceUuidCount = serviceUuidsSet.size,
                    serviceUuids = serviceUuidsSet.toList()
                ) }
            }.collect()
        }
        
        viewModelScope.launch {
            whitelistRepository.getAllDevicesFlow().collect { devices ->
                _uiState.update { it.copy(whitelistCount = devices.size) }
            }
        }
    }
    
    fun resetMaxDistance() {
        beaconRepository.resetMaxDistance()
    }
    
    fun initializeGatewayId(context: android.content.Context) {
        viewModelScope.launch {
            val currentId = preferenceManager.getGatewayId().first()
            if (currentId == null) {
                val imei = DeviceUtil.getDeviceIMEI(context)
                preferenceManager.saveGatewayId(imei)
                Log.d(TAG, "Gateway ID 初始化: $imei")
            }
        }
    }
    
    fun syncServiceUuid() {
        // 防止重複同步（如果正在同步中）
        if (_uiState.value.isSyncing) {
            Log.d(TAG, "服務 UUID 正在同步中，跳過")
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            Log.d(TAG, "開始同步服務 UUID")
            val result = serviceUuidRepository.syncServiceUuids()
            
            _uiState.update { state ->
                if (result.isSuccess) {
                    val uuids = result.getOrNull() ?: emptySet()
                    val message = "已同步 ${uuids.size} 個服務 UUID"
                    Log.d(TAG, message)
                    state.copy(
                        isSyncing = false,
                        lastSyncTime = System.currentTimeMillis(),
                        syncError = null
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "同步失敗"
                    Log.e(TAG, "同步失敗: $errorMsg")
                    state.copy(
                        isSyncing = false,
                        syncError = "同步服務 UUID 失敗: $errorMsg"
                    )
                }
            }
        }
    }
    
    fun syncWhitelist() {
        // 防止重複同步（如果正在同步中）
        if (_uiState.value.isSyncing) {
            Log.d(TAG, "白名單正在同步中，跳過")
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            
            val gatewayId = preferenceManager.getGatewayId().first()
            if (gatewayId == null) {
                _uiState.update { it.copy(
                    isSyncing = false,
                    syncError = "Gateway ID 未設定"
                ) }
                return@launch
            }
            
            Log.d(TAG, "自動同步白名單: gateway_id=$gatewayId")
            val result = whitelistRepository.syncWhitelist(gatewayId)
            
            _uiState.update { state ->
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    val message = if (count == 0) {
                        "Gateway 未註冊，將掃描所有 Beacon"
                    } else {
                        "已同步 $count 個白名單設備"
                    }
                    state.copy(
                        isSyncing = false,
                        lastSyncTime = System.currentTimeMillis(),
                        syncError = message
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "同步失敗"
                    state.copy(
                        isSyncing = false,
                        syncError = "同步失敗: $errorMsg"
                    )
                }
            }
        }
    }
    
    fun clearSyncError() {
        _uiState.update { it.copy(syncError = null) }
    }
    
    fun updatePermissionsState(context: android.content.Context) {
        val permissions = PermissionsState(
            location = hasLocationPermission(context),
            bluetooth = hasBluetoothPermission(context),
            phoneState = hasPhoneStatePermission(context),
            notification = hasNotificationPermission(context),
            backgroundLocation = com.safenet.receiver.utils.PermissionUtil.hasBackgroundLocationPermission(context)
        )
        _uiState.update { it.copy(permissions = permissions) }
    }
    
    private fun hasLocationPermission(context: android.content.Context): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED == 
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
    }
    
    private fun hasBluetoothPermission(context: android.content.Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.content.pm.PackageManager.PERMISSION_GRANTED == 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                )
        } else {
            true
        }
    }
    
    private fun hasPhoneStatePermission(context: android.content.Context): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED == 
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            )
    }
    
    private fun hasNotificationPermission(context: android.content.Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.content.pm.PackageManager.PERMISSION_GRANTED == 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
        } else {
            true
        }
    }
}

data class MainUiState(
    val gatewayId: String = "",
    val whitelistCount: Int = 0,  // 保留以兼容性
    val scannedCount: Int = 0,      // 已掃描數量（scanned_beacons 表總數）
    val uploadedCount: Int = 0,     // 已上傳數量（beacon_queue UPLOADED 狀態）
    val pendingCount: Int = 0,      // 待上傳數量（beacon_queue PENDING 狀態）
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncError: String? = null,
    val isScanning: Boolean = false,
    val permissions: PermissionsState = PermissionsState(),
    val maxDistance: Double = 0.0,  // 最遠距離（米）
    val serviceUuidCount: Int = 0,  // 服務 UUID 數量
    val serviceUuids: List<String> = emptyList()  // Service UUID 列表
)

data class PermissionsState(
    val location: Boolean = false,
    val bluetooth: Boolean = false,
    val phoneState: Boolean = false,
    val notification: Boolean = false,
    val backgroundLocation: Boolean = false
)
