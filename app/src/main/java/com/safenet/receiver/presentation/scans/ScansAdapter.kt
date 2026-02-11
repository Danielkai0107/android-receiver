package com.safenet.receiver.presentation.scans

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safenet.receiver.databinding.ItemScannedBeaconBinding
import com.safenet.receiver.domain.model.ScannedBeacon
import java.text.SimpleDateFormat
import java.util.*

class ScansAdapter : ListAdapter<ScannedBeacon, ScansAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((ScannedBeacon) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScannedBeaconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemScannedBeaconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(beacon: ScannedBeacon) {
            binding.root.setOnClickListener { onItemClick?.invoke(beacon) }
            binding.apply {
                // 顯示完整 UUID
                tvUuid.text = beacon.uuid
                
                // Major/Minor
                tvMajorMinor.text = "Major: ${beacon.major} | Minor: ${beacon.minor}"
                
                // RSSI 和距離
                tvRssi.text = "RSSI: ${beacon.rssi} dBm"
                tvDistance.text = String.format("距離: %.2f m", beacon.distance)
                
                // 時間
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(beacon.scannedAt))
                tvTime.text = time
                
                // 掃描次數
                tvScanCount.text = "次數: ${beacon.scanCount}"
                
                // 白名單狀態
                if (beacon.isInWhitelist) {
                    tvStatus.text = "✅ 白名單"
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    cardView.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                } else {
                    tvStatus.text = "⏭️ 非白名單"
                    tvStatus.setTextColor(Color.parseColor("#FF9800"))
                    cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                
                // RSSI 信號強度顏色
                val rssiColor = when {
                    beacon.rssi >= -60 -> Color.parseColor("#4CAF50")  // 強
                    beacon.rssi >= -75 -> Color.parseColor("#FF9800")  // 中
                    else -> Color.parseColor("#F44336")  // 弱
                }
                tvRssi.setTextColor(rssiColor)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ScannedBeacon>() {
        override fun areItemsTheSame(oldItem: ScannedBeacon, newItem: ScannedBeacon): Boolean {
            return oldItem.scannedAt == newItem.scannedAt && oldItem.uuid == newItem.uuid
        }
        
        override fun areContentsTheSame(oldItem: ScannedBeacon, newItem: ScannedBeacon): Boolean {
            return oldItem == newItem
        }
    }
}
