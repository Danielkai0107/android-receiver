package com.safenet.receiver.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.safenet.receiver.data.local.dao.ScannedBeaconDao
import com.safenet.receiver.service.BeaconScanService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val scannedBeaconDao: ScannedBeaconDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExportRepository"
    }

    /**
     * 匯出掃描記錄為 CSV
     * @param uuid 若指定則只匯出該 UUID 的記錄，否則匯出全部
     */
    suspend fun exportToCSV(uuid: String? = null): Result<Uri> {
        return try {
            val records = if (uuid != null) {
                scannedBeaconDao.getByUuid(uuid)
            } else {
                scannedBeaconDao.getAllScans()
            }

            if (records.isEmpty()) {
                return Result.failure(Exception("沒有可匯出的記錄"))
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // UTF-8 with BOM 以支援 Excel 開啟中文
            val csv = buildString {
                append("\uFEFF")  // BOM
                appendLine("timestamp,datetime,uuid,major,minor,rssi,distance,hasSignal,isTargetUuid")
                records.forEach { record ->
                    val hasSignal = record.rssi != BeaconScanService.NO_SIGNAL_RSSI
                    val rssiDisplay = if (hasSignal) record.rssi.toString() else ""
                    val distDisplay = if (hasSignal) String.format("%.2f", record.distance) else ""
                    appendLine("${record.scannedAt},${dateFormat.format(Date(record.scannedAt))},${record.uuid},${record.major},${record.minor},$rssiDisplay,$distDisplay,$hasSignal,${record.isInWhitelist}")
                }
            }

            val fileName = if (uuid != null) {
                "beacon_${uuid.take(8)}_$fileTimestamp.csv"
            } else {
                "beacon_all_$fileTimestamp.csv"
            }

            val exportDir = File(context.getExternalFilesDir(null), "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)
            file.writeText(csv)

            Log.d(TAG, "CSV 已匯出: ${file.absolutePath}, ${records.size} 筆記錄")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "匯出 CSV 失敗", e)
            Result.failure(e)
        }
    }
}
