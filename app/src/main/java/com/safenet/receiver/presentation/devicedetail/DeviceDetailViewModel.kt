package com.safenet.receiver.presentation.devicedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.data.local.dao.ScannedBeaconDao
import com.safenet.receiver.data.local.entity.ScannedBeaconEntity
import com.safenet.receiver.service.BeaconScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RssiDetailState(
    val currentRssi: Int? = null,
    val previousRssi: Int? = null,     // 前一次有訊號的 RSSI
    val rssiDelta: Int? = null,        // 即時變化量 = 最新 - 前次（正=增強，負=減弱）
    val minRssi: Int? = null,
    val maxRssi: Int? = null,
    val sampleCount: Int = 0,          // 總筆數（含暫無訊號）
    val validSignalCount: Int = 0,     // 有訊號的筆數
    val noSignalCount: Int = 0,        // 暫無訊號的筆數
    val records: List<ScannedBeaconEntity> = emptyList()
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scannedBeaconDao: ScannedBeaconDao
) : ViewModel() {

    private val uuid: String = savedStateHandle.get<String>("uuid") ?: ""
    private val major: Int = savedStateHandle.get<Int>("major") ?: 0
    private val minor: Int = savedStateHandle.get<Int>("minor") ?: 0

    val rssiDetailState: StateFlow<RssiDetailState> = scannedBeaconDao
        .getRssiHistoryFlow(uuid, major, minor)
        .map { list ->
            if (list.isEmpty()) {
                RssiDetailState(sampleCount = 0, records = emptyList())
            } else {
                // 只統計有訊號的記錄
                val validRecords = list.filter { it.rssi != BeaconScanService.NO_SIGNAL_RSSI }
                val noSignalCount = list.size - validRecords.size

                if (validRecords.isEmpty()) {
                    // 所有記錄都是暫無訊號
                    RssiDetailState(
                        currentRssi = null,
                        sampleCount = list.size,
                        validSignalCount = 0,
                        noSignalCount = noSignalCount,
                        records = list
                    )
                } else {
                    val rssis = validRecords.map { it.rssi }
                    val min = rssis.minOrNull()!!
                    val max = rssis.maxOrNull()!!
                    // 看最新一筆是否有訊號，用來決定即時顯示
                    val latestRecord = list.first()
                    val isCurrentNoSignal = latestRecord.rssi == BeaconScanService.NO_SIGNAL_RSSI
                    val currentRssi = if (isCurrentNoSignal) null else validRecords.first().rssi

                    // 取前一次有訊號的 RSSI 來算即時變化量
                    val previousValid = if (validRecords.size >= 2) validRecords[1] else null
                    val previousRssi = previousValid?.rssi
                    val rssiDelta = if (currentRssi != null && previousRssi != null) {
                        currentRssi - previousRssi
                    } else null

                    RssiDetailState(
                        currentRssi = currentRssi,
                        previousRssi = previousRssi,
                        rssiDelta = rssiDelta,
                        minRssi = min,
                        maxRssi = max,
                        sampleCount = list.size,
                        validSignalCount = validRecords.size,
                        noSignalCount = noSignalCount,
                        records = list
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RssiDetailState())

    fun getDeviceTitle(): String = "UUID: $uuid (M:$major m:$minor)"
}
