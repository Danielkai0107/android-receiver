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
    // 使用 IO 線程處理掃描和 DB 寫入，避免與 UI 主線程競爭
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var uploadJob: Job? = null
    
    private var gatewayId: String? = null
    private var scannedCount = 0

    /** 追蹤中的目標 UUID 設備（UUID+Major+Minor），用於記錄「暫無訊號」 */
    data class DeviceKey(val uuid: String, val major: Int, val minor: Int)
    private val trackedDevices = mutableSetOf<DeviceKey>()
    
    companion object {
        private const val TAG = "BeaconScanService"
        private const val NOTIFICATION_ID = 1001
        const val NO_SIGNAL_RSSI = -999  // sentinel 值，表示「暫無訊號」
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "服務創建")
        
        beaconManager = BeaconManager.getInstanceForApplication(this)
        
        // 完全清除所有預設解析器
        beaconManager.beaconParsers.clear()
        Log.d(TAG, "✅ 已清除所有預設解析器")
        
        // 只添加 iBeacon 解析器（0215 是 iBeacon 的識別碼）
        val iBeaconParser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        beaconManager.beaconParsers.add(iBeaconParser)
        
        Log.d(TAG, "✅ 已設定 iBeacon 解析器")
        Log.d(TAG, "📋 解析器數量: ${beaconManager.beaconParsers.size}")
        beaconManager.beaconParsers.forEach { parser ->
            Log.d(TAG, "  - 解析器格式: ${parser.toString()}")
        }
        
        // 設置掃描參數：加長掃描時間、縮短間隔，提高偵測率
        beaconManager.foregroundScanPeriod = 2200L   // 前景掃描 2.2 秒（涵蓋多次 Beacon 廣播）
        beaconManager.foregroundBetweenScanPeriod = 2000L  // 間隔 2 秒
        
        // 背景參數設為與前景一致，確保 Activity 切換時不會降級
        // （此 App 以前景服務長期運行，不需要省電降級）
        beaconManager.backgroundScanPeriod = 2200L
        beaconManager.backgroundBetweenScanPeriod = 2000L
        
        Log.d(TAG, "掃描參數：前景/背景統一 → 掃描 2.2 秒 / 間隔 2 秒（不因 Activity 切換而改變）")
        
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

            // 載入追蹤設備列表（恢復之前追蹤的設備）
            loadTrackedDevices()

            // 啟動時執行一次過期數據清理
            val retentionDays = preferenceManager.getDataRetentionDays().first()
            val cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000L)
            val deletedCount = scannedBeaconDao.deleteOlderThanTimestamp(cutoff)
            Log.d(TAG, "🧹 已清理 $retentionDays 天前的掃描記錄")

            startUploadScheduler()
        }
        
        // 改為 START_NOT_STICKY：服務停止後不會自動重啟
        // 避免用戶未操作時服務自動運行
        return START_NOT_STICKY
    }
    
    override fun onBeaconServiceConnect() {
        Log.d(TAG, "🔗 Beacon 服務連接")
        Log.d(TAG, "📋 當前解析器數量: ${beaconManager.beaconParsers.size}")
        
        // 強制鎖定前景掃描模式，不因 Activity 生命週期切換而降級
        beaconManager.backgroundMode = false
        Log.d(TAG, "🔒 已鎖定前景掃描模式")
        
        beaconManager.removeAllRangeNotifiers()
        beaconManager.addRangeNotifier { beacons, region ->
            Log.d(TAG, "📡 掃描回調觸發 - Region: ${region?.uniqueId}, Beacons: ${beacons.size}")
            serviceScope.launch {
                handleBeacons(beacons.toList())
            }
        }
        
        // 開始掃描所有 Beacon
        try {
            val region = Region("all-beacons", null, null, null)
            beaconManager.startRangingBeacons(region)
            Log.d(TAG, "✅ 已啟動 Beacon 掃描，Region: ${region.uniqueId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 啟動掃描失敗", e)
        }
    }
    
    private suspend fun handleBeacons(beacons: List<org.altbeacon.beacon.Beacon>) {
        val now = System.currentTimeMillis()

        // 本輪掃到的目標設備
        val detectedTargetKeys = mutableSetOf<DeviceKey>()

        if (beacons.isNotEmpty()) {
            Log.d(TAG, "🎯 偵測到 ${beacons.size} 個 Beacon")
        } else {
            Log.d(TAG, "⚠️ 本次掃描週期沒有偵測到任何設備")
        }

        val location = locationService.getCurrentLocation()

        beacons.forEach { beacon ->
            val uuid = beacon.id1.toString()
            Log.d(TAG, "📍 原始 Beacon 數據 - UUID: $uuid, Major: ${beacon.id2}, Minor: ${beacon.id3}, RSSI: ${beacon.rssi}, Parser: ${beacon.parserIdentifier}")
            
            // 使用 Service UUID Repository 檢查是否為目標 UUID
            val isTargetUuid = serviceUuidRepository.isTargetUuid(uuid)
            
            // 記錄所有掃描到的 Beacon（用於顯示清單）
            val scannedBeaconEntity = com.safenet.receiver.data.local.entity.ScannedBeaconEntity(
                uuid = uuid,
                major = beacon.id2.toInt(),
                minor = beacon.id3.toInt(),
                rssi = beacon.rssi,
                distance = beacon.distance,
                isInWhitelist = isTargetUuid,
                scannedAt = now
            )
            scannedBeaconDao.insert(scannedBeaconEntity)
            scannedCount++
            
            // 處理目標 UUID 的 Beacon
            if (isTargetUuid) {
                val key = DeviceKey(uuid, beacon.id2.toInt(), beacon.id3.toInt())
                detectedTargetKeys.add(key)
                trackedDevices.add(key)  // 加入追蹤列表

                if (location != null) {
                    val domainBeacon = Beacon(
                        uuid = uuid,
                        major = beacon.id2.toInt(),
                        minor = beacon.id3.toInt(),
                        rssi = beacon.rssi,
                        distance = beacon.distance,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        scannedAt = now
                    )
                    beaconRepository.addToQueue(domainBeacon)
                }
                
                Log.d(TAG, "✅ 目標 UUID Beacon: $uuid, Major=${beacon.id2}, Minor=${beacon.id3}, RSSI=${beacon.rssi}, 距離=${String.format("%.2f", beacon.distance)}m")
            } else {
                Log.d(TAG, "⏭️ 非目標 UUID，僅記錄: $uuid")
            }
        }

        // 對追蹤列表中但本輪未掃到的目標設備，寫入「暫無訊號」記錄（rssi = -999）
        val missingDevices = trackedDevices - detectedTargetKeys
        if (missingDevices.isNotEmpty()) {
            Log.d(TAG, "📝 ${missingDevices.size} 個追蹤設備未偵測到，記錄暫無訊號")
            missingDevices.forEach { device ->
                val noSignalEntity = com.safenet.receiver.data.local.entity.ScannedBeaconEntity(
                    uuid = device.uuid,
                    major = device.major,
                    minor = device.minor,
                    rssi = NO_SIGNAL_RSSI,
                    distance = 0.0,
                    isInWhitelist = true,
                    scannedAt = now
                )
                scannedBeaconDao.insert(noSignalEntity)
                Log.d(TAG, "📝 暫無訊號: ${device.uuid} M:${device.major} m:${device.minor}")
            }
        }

        updateNotification()
    }

    /** 服務啟動時從 DB 載入所有曾掃到的目標設備，恢復追蹤列表 */
    private suspend fun loadTrackedDevices() {
        val entities = scannedBeaconDao.getDistinctTargetDevices()
        entities.forEach { entity ->
            trackedDevices.add(DeviceKey(entity.uuid, entity.major, entity.minor))
        }
        Log.d(TAG, "✅ 已載入 ${trackedDevices.size} 個追蹤設備")
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
        // 每次掃描回調都強制前景模式，防止 AltBeacon 自動切回背景
        if (beaconManager.backgroundMode) {
            beaconManager.backgroundMode = false
            Log.d(TAG, "🔒 重新鎖定前景掃描模式")
        }
        
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
