package com.safenet.receiver.presentation.devicedetail

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safenet.receiver.data.local.entity.ScannedBeaconEntity
import com.safenet.receiver.databinding.ItemRssiRecordBinding
import com.safenet.receiver.service.BeaconScanService
import java.text.SimpleDateFormat
import java.util.*

class RssiRecordAdapter : ListAdapter<ScannedBeaconEntity, RssiRecordAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRssiRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        // 列表按時間 DESC 排列：position+1 是時間上的「前一筆」
        // 用前一筆（較舊）的 RSSI 來計算此筆的變化量
        val olderRecord = if (position + 1 < itemCount) getItem(position + 1) else null
        holder.bind(record, olderRecord)
    }

    class ViewHolder(
        private val binding: ItemRssiRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

        fun bind(record: ScannedBeaconEntity, olderRecord: ScannedBeaconEntity?) {
            binding.tvTime.text = timeFormat.format(Date(record.scannedAt))

            if (record.rssi == BeaconScanService.NO_SIGNAL_RSSI) {
                // 暫無訊號
                binding.tvRssi.text = "無訊號"
                binding.tvRssi.setTextColor(Color.GRAY)
                binding.tvDelta.text = ""
                binding.tvDistance.text = "--"
            } else {
                // 正常訊號
                binding.tvRssi.text = "${record.rssi}"
                binding.tvRssi.setTextColor(rssiColor(record.rssi))
                binding.tvDistance.text = String.format("%.1fm", record.distance)

                // 計算與前一筆有訊號記錄的變化量
                if (olderRecord != null && olderRecord.rssi != BeaconScanService.NO_SIGNAL_RSSI) {
                    val delta = record.rssi - olderRecord.rssi
                    val sign = if (delta >= 0) "+" else ""
                    binding.tvDelta.text = "$sign$delta"
                    binding.tvDelta.setTextColor(when {
                        delta > 0 -> Color.parseColor("#4CAF50")   // 增強 = 綠
                        delta < 0 -> Color.parseColor("#F44336")   // 減弱 = 紅
                        else -> Color.GRAY                          // 無變化
                    })
                } else {
                    binding.tvDelta.text = ""
                }
            }
        }

        private fun rssiColor(rssi: Int): Int = when {
            rssi >= -60 -> Color.parseColor("#4CAF50")
            rssi >= -75 -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ScannedBeaconEntity>() {
        override fun areItemsTheSame(a: ScannedBeaconEntity, b: ScannedBeaconEntity) =
            a.id == b.id

        override fun areContentsTheSame(a: ScannedBeaconEntity, b: ScannedBeaconEntity) =
            a == b
    }
}
