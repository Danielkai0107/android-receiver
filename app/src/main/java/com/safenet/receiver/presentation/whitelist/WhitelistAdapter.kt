package com.safenet.receiver.presentation.whitelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safenet.receiver.databinding.ItemScannedBeaconBinding
import com.safenet.receiver.domain.model.ScannedBeacon
import java.text.SimpleDateFormat
import java.util.*

class WhitelistAdapter : ListAdapter<ScannedBeacon, WhitelistAdapter.ViewHolder>(DiffCallback()) {
    
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
    
    class ViewHolder(
        private val binding: ItemScannedBeaconBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(beacon: ScannedBeacon) {
            binding.apply {
                // 顯示完整 UUID
                tvUuid.text = beacon.uuid
                tvMajorMinor.text = "Major: ${beacon.major} | Minor: ${beacon.minor}"
                tvRssi.text = "RSSI: ${beacon.rssi}"
                tvDistance.text = "距離: ${String.format("%.2f", beacon.distance)}m"
                tvStatus.text = if (beacon.isInWhitelist) "✅ 已上傳" else "⏭️ 僅記錄"
                
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(beacon.scannedAt))
                tvTime.text = time
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<ScannedBeacon>() {
        override fun areItemsTheSame(oldItem: ScannedBeacon, newItem: ScannedBeacon): Boolean {
            return oldItem.uuid == newItem.uuid && 
                   oldItem.major == newItem.major && 
                   oldItem.minor == newItem.minor &&
                   oldItem.scannedAt == newItem.scannedAt
        }
        
        override fun areContentsTheSame(oldItem: ScannedBeacon, newItem: ScannedBeacon): Boolean {
            return oldItem == newItem
        }
    }
}
