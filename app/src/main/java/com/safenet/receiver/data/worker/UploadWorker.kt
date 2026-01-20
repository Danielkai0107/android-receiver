package com.safenet.receiver.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safenet.receiver.data.repository.BeaconRepository
import com.safenet.receiver.data.repository.UploadRepository
import com.safenet.receiver.service.LocationService
import com.safenet.receiver.utils.PreferenceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val beaconRepository: BeaconRepository,
    private val uploadRepository: UploadRepository,
    private val locationService: LocationService,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UploadWorker"
        const val WORK_NAME = "upload_beacons"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "開始背景上傳任務")
        
        val gatewayId = preferenceManager.getGatewayId().first()
        if (gatewayId == null) {
            Log.w(TAG, "Gateway ID 未設定")
            return Result.failure()
        }
        
        // 上傳前先整合相同的 Beacon，只保留信號最強的
        beaconRepository.consolidatePendingBeacons()
        
        val pendingBeacons = beaconRepository.getPendingBeacons()
        if (pendingBeacons.isEmpty()) {
            Log.d(TAG, "沒有待上傳的 Beacon")
            return Result.success()
        }
        
        Log.d(TAG, "準備上傳 ${pendingBeacons.size} 個不同的 Beacon")
        
        val location = locationService.getCurrentLocation()
        if (location == null) {
            Log.w(TAG, "無法獲取位置")
            return Result.retry()
        }
        
        val result = uploadRepository.uploadBeacons(
            gatewayId = gatewayId,
            beacons = pendingBeacons,
            latitude = location.latitude,
            longitude = location.longitude
        )
        
        return if (result.isSuccess) {
            val ids = pendingBeacons.map { it.id }
            // 更新狀態為 UPLOADED，不刪除記錄
            beaconRepository.updateStatus(ids, com.safenet.receiver.domain.model.UploadStatus.UPLOADED)
            Log.d(TAG, "背景上傳成功: ${pendingBeacons.size} 筆，已更新狀態為 UPLOADED")
            Result.success()
        } else {
            Log.e(TAG, "背景上傳失敗: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }
}
