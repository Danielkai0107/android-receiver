package com.safenet.receiver.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.safenet.receiver.R
import com.safenet.receiver.ReceiverApplication
import com.safenet.receiver.data.repository.BeaconRepository
import com.safenet.receiver.data.repository.ServiceUuidRepository
import com.safenet.receiver.data.repository.UploadRepository
import com.safenet.receiver.data.repository.WhitelistRepository
import com.safenet.receiver.domain.model.Beacon
import com.safenet.receiver.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.altbeacon.beacon.*
import javax.inject.Inject

@AndroidEntryPoint
class BeaconScanService : Service(), BeaconConsumer {
    
    @Inject
    lateinit var whitelistRepository: WhitelistRepository
    
    @Inject
    lateinit var beaconRepository: BeaconRepository
    
    @Inject
    lateinit var uploadRepository: UploadRepository
    
    @Inject
    lateinit var locationService: LocationService
    
    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    @Inject
    lateinit var scannedBeaconDao: com.safenet.receiver.data.local.dao.ScannedBeaconDao
    
    @Inject
    lateinit var serviceUuidRepository: ServiceUuidRepository
    
    private lateinit var beaconManager: BeaconManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var uploadJob: Job? = null
    
    private var gatewayId: String? = null
    private var scannedCount = 0
    
    companion object {
        private const val TAG = "BeaconScanService"
        private const val NOTIFICATION_ID = 1001
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服務創建")
        
        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(
            BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        )
        
        // 設置掃描參數，避免 "scanning too frequently" 錯誤
        // foregroundScanPeriod: 掃描時間（毫秒）
        // foregroundBetweenScanPeriod: 兩次掃描之間的間隔（毫秒）
        beaconManager.foregroundScanPeriod = 1100L  // 掃描 1.1 秒
        beaconManager.foregroundBetweenScanPeriod = 5000L  // 間隔 5 秒（符合預設值）
        
        // 背景掃描參數（如果需要）
        beaconManager.backgroundScanPeriod = 1100L
        beaconManager.backgroundBetweenScanPeriod = 10000L  // 背景間隔更長，節省電量
        
        Log.d(TAG, "掃描參數：掃描 1.1 秒，間隔 5 秒")
        
        beaconManager.bind(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "服務啟動")
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        serviceScope.launch {
            gatewayId = preferenceManager.getGatewayId().first()
            if (gatewayId == null) {
                Log.e(TAG, "❌ Gateway ID 為 null！無法上傳數據")
                Log.e(TAG, "請確保已授予 READ_PHONE_STATE 權限")
            } else {
                Log.d(TAG, "✅ Gateway ID 已設定: $gatewayId")
            }
            startUploadScheduler()
        }
        
        // 改為 START_NOT_STICKY：服務停止後不會自動重啟
        // 避免用戶未操作時服務自動運行
        return START_NOT_STICKY
    }
    
    override fun onBeaconServiceConnect() {
        Log.d(TAG, "Beacon 服務連接")
        
        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, _ ->
            serviceScope.launch {
                handleBeacons(beacons.toList())
            }
        }
        
        // 開始掃描所有 Beacon
        try {
            beaconManager.startRangingBeacons(Region("all-beacons", null, null, null))
        } catch (e: Exception) {
            Log.e(TAG, "啟動掃描失敗", e)
        }
    }
    
    private suspend fun handleBeacons(beacons: List<org.altbeacon.beacon.Beacon>) {
        if (beacons.isEmpty()) return
        
        Log.d(TAG, "偵測到 ${beacons.size} 個 Beacon")
        
        val location = locationService.getCurrentLocation()
        if (location == null) {
            Log.w(TAG, "無法獲取 GPS 位置，跳過")
            return
        }
        
        beacons.forEach { beacon ->
            val uuid = beacon.id1.toString()
            
            // 使用 Service UUID Repository 檢查是否為目標 UUID
            val isTargetUuid = serviceUuidRepository.isTargetUuid(uuid)
            
            // 記錄所有掃描到的 Beacon（用於顯示清單）
            val scannedBeaconEntity = com.safenet.receiver.data.local.entity.ScannedBeaconEntity(
                uuid = uuid,
                major = beacon.id2.toInt(),
                minor = beacon.id3.toInt(),
                rssi = beacon.rssi,
                distance = beacon.distance,
                isInWhitelist = isTargetUuid,  // 標記是否為目標 UUID
                scannedAt = System.currentTimeMillis()
            )
            scannedBeaconDao.insert(scannedBeaconEntity)
            scannedCount++
            
            // 只處理目標 UUID 的 Beacon
            if (isTargetUuid) {
                val domainBeacon = Beacon(
                    uuid = uuid,
                    major = beacon.id2.toInt(),
                    minor = beacon.id3.toInt(),
                    rssi = beacon.rssi,
                    distance = beacon.distance,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    scannedAt = System.currentTimeMillis()
                )
                
                beaconRepository.addToQueue(domainBeacon)
                
                Log.d(TAG, "✅ 目標 UUID Beacon: $uuid, Major=${beacon.id2}, Minor=${beacon.id3}, RSSI=${beacon.rssi}, 距離=${String.format("%.2f", beacon.distance)}m")
            } else {
                Log.d(TAG, "⏭️ 非目標 UUID，僅記錄: $uuid")
            }
        }
        
        updateNotification()
    }
    
    private fun startUploadScheduler() {
        uploadJob?.cancel()
        uploadJob = serviceScope.launch {
            val interval = preferenceManager.getUploadInterval().first() * 1000L
            Log.d(TAG, "📤 上傳定時器已啟動，間隔：${interval/1000} 秒")
            
            while (isActive) {
                Log.d(TAG, "⏰ 等待 ${interval/1000} 秒後執行上傳...")
                delay(interval)
                Log.d(TAG, "🚀 開始執行上傳...")
                performUpload()
            }
        }
    }
    
    private suspend fun performUpload() {
        Log.d(TAG, "📤 performUpload() 被調用")
        
        if (gatewayId == null) {
            Log.e(TAG, "❌ Gateway ID 為 null，無法上傳")
            return
        }
        val gid = gatewayId!!
        Log.d(TAG, "✅ Gateway ID: $gid")
        
        // 上傳前先整合相同的 Beacon，只保留信號最強的
        Log.d(TAG, "🔄 開始整合 PENDING 記錄...")
        beaconRepository.consolidatePendingBeacons()
        
        val pendingBeacons = beaconRepository.getPendingBeacons()
        Log.d(TAG, "📊 待上傳的 Beacon 數量: ${pendingBeacons.size}")
        
        if (pendingBeacons.isEmpty()) {
            Log.d(TAG, "沒有待上傳的 Beacon")
            return
        }
        
        Log.d(TAG, "準備上傳 ${pendingBeacons.size} 個不同的 Beacon")
        pendingBeacons.forEach { beacon ->
            Log.d(TAG, "  - UUID: ${beacon.uuid}, Major: ${beacon.major}, Minor: ${beacon.minor}, RSSI: ${beacon.rssi}")
        }
        
        val location = locationService.getCurrentLocation()
        if (location == null) {
            Log.w(TAG, "❌ 無法獲取位置，延後上傳")
            return
        }
        Log.d(TAG, "✅ 位置: lat=${location.latitude}, lng=${location.longitude}")
        
        val result = uploadRepository.uploadBeacons(
            gatewayId = gid,
            beacons = pendingBeacons,
            latitude = location.latitude,
            longitude = location.longitude
        )
        
        if (result.isSuccess) {
            val ids = pendingBeacons.map { it.id }
            // 更新狀態為 UPLOADED，不刪除記錄
            beaconRepository.updateStatus(ids, com.safenet.receiver.domain.model.UploadStatus.UPLOADED)
            Log.d(TAG, "✅ 上傳成功: ${pendingBeacons.size} 筆，已更新狀態為 UPLOADED")
        } else {
            Log.e(TAG, "❌ 上傳失敗: ${result.exceptionOrNull()?.message}")
        }
    }
    
    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, ReceiverApplication.SCAN_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.scanning_service_running))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, ReceiverApplication.SCAN_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("已掃描: $scannedCount 個設備")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        Log.d(TAG, "服務銷毀")
        uploadJob?.cancel()
        serviceScope.cancel()
        beaconManager.unbind(this)
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
