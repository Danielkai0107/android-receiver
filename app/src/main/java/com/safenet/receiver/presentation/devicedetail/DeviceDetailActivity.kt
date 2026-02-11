package com.safenet.receiver.presentation.devicedetail

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.safenet.receiver.databinding.ActivityDeviceDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceDetailBinding
    private val viewModel: DeviceDetailViewModel by viewModels()
    private lateinit var rssiRecordAdapter: RssiRecordAdapter
    private var lastRssi: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "RSSI 即時"

        binding.tvUuid.text = viewModel.getDeviceTitle()

        rssiRecordAdapter = RssiRecordAdapter()
        binding.recyclerRssiRecords.apply {
            layoutManager = LinearLayoutManager(this@DeviceDetailActivity)
            adapter = rssiRecordAdapter
        }

        lifecycleScope.launch {
            viewModel.rssiDetailState.collect { state ->
                if (state.sampleCount == 0) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.tvRecordsTitle.visibility = android.view.View.GONE
                    binding.recyclerRssiRecords.visibility = android.view.View.GONE
                    binding.tvRssi.text = "--"
                    binding.tvRssiPrevious.text = "--"
                    binding.tvRssiDelta.text = "--"
                    binding.tvRssiDelta.setTextColor(Color.GRAY)
                    binding.tvRssiMin.text = "--"
                    binding.tvRssiMax.text = "--"
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.tvRecordsTitle.visibility = android.view.View.VISIBLE
                    binding.recyclerRssiRecords.visibility = android.view.View.VISIBLE

                    // 即時 RSSI 顯示
                    if (state.currentRssi != null) {
                        val current = state.currentRssi
                        binding.tvRssi.text = "$current"
                        binding.tvRssi.setTextColor(rssiColor(current))

                        // 數值變化脈衝動畫
                        if (lastRssi != null && lastRssi != current) {
                            playRssiPulseAnimation()
                        }
                        lastRssi = current
                    } else {
                        // 目前暫無訊號
                        binding.tvRssi.text = "暫無"
                        binding.tvRssi.setTextColor(Color.GRAY)

                        if (lastRssi != null) {
                            playRssiPulseAnimation()
                        }
                        lastRssi = null
                    }

                    // 即時變化區塊
                    if (state.previousRssi != null) {
                        binding.tvRssiPrevious.text = "${state.previousRssi} dBm"
                    } else {
                        binding.tvRssiPrevious.text = "--"
                    }

                    if (state.rssiDelta != null) {
                        val delta = state.rssiDelta
                        val sign = if (delta >= 0) "+" else ""
                        binding.tvRssiDelta.text = "$sign$delta dBm"
                        // 正值（增強）= 綠色，負值（減弱）= 紅色，0 = 灰色
                        binding.tvRssiDelta.setTextColor(when {
                            delta > 0 -> Color.parseColor("#4CAF50")
                            delta < 0 -> Color.parseColor("#F44336")
                            else -> Color.GRAY
                        })
                    } else {
                        binding.tvRssiDelta.text = "--"
                        binding.tvRssiDelta.setTextColor(Color.GRAY)
                    }

                    // 歷史最小/最大
                    if (state.validSignalCount > 0) {
                        binding.tvRssiMin.text = "${state.minRssi} dBm"
                        binding.tvRssiMax.text = "${state.maxRssi} dBm"
                    } else {
                        binding.tvRssiMin.text = "--"
                        binding.tvRssiMax.text = "--"
                    }

                    // 下方每次掃描記錄
                    rssiRecordAdapter.submitList(state.records) {
                        // submitList 完成後自動捲到頂部，確保與即時 RSSI 同步
                        binding.recyclerRssiRecords.scrollToPosition(0)
                    }
                }
            }
        }
    }

    private fun playRssiPulseAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.tvRssi, "scaleX", 1f, 1.15f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvRssi, "scaleY", 1f, 1.15f, 1f)
        scaleX.duration = 150
        scaleY.duration = 150
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.start()
    }

    private fun rssiColor(rssi: Int): Int = when {
        rssi >= -60 -> Color.parseColor("#4CAF50")
        rssi >= -75 -> Color.parseColor("#FF9800")
        else -> Color.parseColor("#F44336")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
