package com.safenet.receiver.presentation.uploadhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safenet.receiver.data.local.entity.BeaconQueueEntity
import com.safenet.receiver.databinding.ItemUploadHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class UploadHistoryAdapter : ListAdapter<BeaconQueueEntity, UploadHistoryAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUploadHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemUploadHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        fun bind(beacon: BeaconQueueEntity) {
            binding.apply {
                tvUuid.text = "UUID: ${beacon.uuid}"
                tvMajorMinor.text = "Major: ${beacon.major} | Minor: ${beacon.minor}"
                tvRssi.text = "RSSI: ${beacon.rssi} dBm"
                tvLocation.text = "位置: ${String.format("%.6f", beacon.latitude)}, ${String.format("%.6f", beacon.longitude)}"
                tvTime.text = "掃描時間: ${dateFormat.format(Date(beacon.scannedAt))}"
                
                // 如果有上傳時間，顯示上傳時間
                if (beacon.uploadedAt != null) {
                    tvUploadTime.text = "上傳時間: ${dateFormat.format(Date(beacon.uploadedAt))}"
                    tvUploadTime.visibility = android.view.View.VISIBLE
                } else {
                    tvUploadTime.visibility = android.view.View.GONE
                }
                
                tvStatus.text = "狀態: ${beacon.uploadStatus}"
                
                // 顯示完整的 HTTP 請求和響應資訊
                if (beacon.requestUrl != null && beacon.responseCode != null) {
                    val httpLog = buildHttpLog(beacon)
                    tvHttpLog.text = httpLog
                } else {
                    // 如果沒有 HTTP 詳情，顯示基本資訊
                    val basicInfo = """
                        Request Body:
                        {
                          "uuid": "${beacon.uuid}",
                          "major": ${beacon.major},
                          "minor": ${beacon.minor},
                          "rssi": ${beacon.rssi},
                          "latitude": ${beacon.latitude},
                          "longitude": ${beacon.longitude},
                          "scannedAt": ${beacon.scannedAt}
                        }
                    """.trimIndent()
                    tvHttpLog.text = basicInfo
                }
            }
        }
        
        private fun buildHttpLog(beacon: BeaconQueueEntity): String {
            val sb = StringBuilder()
            
            // Request
            sb.append("--> POST ${beacon.requestUrl}\n")
            
            // Request Headers
            if (!beacon.requestHeaders.isNullOrEmpty()) {
                try {
                    val headers = com.google.gson.Gson().fromJson(
                        beacon.requestHeaders,
                        Map::class.java
                    ) as Map<*, *>
                    headers.forEach { (key, value) ->
                        sb.append("$key: $value\n")
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
            
            // Request Body
            sb.append("\n${beacon.requestBody}\n")
            sb.append("--> END POST\n\n")
            
            // Response
            sb.append("<-- ${beacon.responseCode} ${beacon.requestUrl}")
            if (beacon.responseDuration != null) {
                sb.append(" (${beacon.responseDuration}ms)\n")
            } else {
                sb.append("\n")
            }
            
            // Response Headers
            if (!beacon.responseHeaders.isNullOrEmpty()) {
                try {
                    val headers = com.google.gson.Gson().fromJson(
                        beacon.responseHeaders,
                        Map::class.java
                    ) as Map<*, *>
                    headers.forEach { (key, value) ->
                        sb.append("$key: $value\n")
                    }
                } catch (e: Exception) {
                    // Ignore parsing error
                }
            }
            
            // Response Body
            sb.append("\n${beacon.responseBody}\n")
            sb.append("<-- END HTTP")
            
            return sb.toString()
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<BeaconQueueEntity>() {
        override fun areItemsTheSame(oldItem: BeaconQueueEntity, newItem: BeaconQueueEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BeaconQueueEntity, newItem: BeaconQueueEntity): Boolean {
            return oldItem == newItem
        }
    }
}
