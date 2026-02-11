package com.safenet.receiver.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.safenet.receiver.databinding.FragmentSettingsBinding
import com.safenet.receiver.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.apply {
            // 掃描頻率
            layoutScanFrequency.setOnClickListener {
                showScanFrequencyDialog()
            }
            
            // 上傳間隔
            layoutUploadInterval.setOnClickListener {
                showUploadIntervalDialog()
            }
            
            // 白名單同步間隔
            layoutWhitelistSync.setOnClickListener {
                showWhitelistSyncDialog()
            }
            
            // GPS 更新頻率
            layoutGpsFrequency.setOnClickListener {
                showGpsFrequencyDialog()
            }
            
            // 數據保留時間
            layoutDataRetention.setOnClickListener {
                showDataRetentionDialog()
            }

            // 離線快取上限
            layoutCacheLimit.setOnClickListener {
                showCacheLimitDialog()
            }

            // 上傳 URL 儲存
            btnSaveUploadUrl.setOnClickListener {
                val url = etUploadUrl.text?.toString()?.trim() ?: ""
                if (url.isNotEmpty()) {
                    viewModel.saveUploadUrl(url)
                    Toast.makeText(requireContext(), "上傳 URL 已儲存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "請輸入 URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanFrequency.collect { value ->
                binding.tvScanFrequency.text = "$value 秒"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadInterval.collect { value ->
                binding.tvUploadInterval.text = "$value 秒"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.whitelistSyncInterval.collect { value ->
                binding.tvWhitelistSyncInterval.text = "$value 分鐘"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.gpsUpdateFrequency.collect { value ->
                binding.tvGpsUpdateFrequency.text = "$value 分鐘"
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.offlineCacheLimit.collect { value ->
                binding.tvOfflineCacheLimit.text = "$value 筆"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dataRetentionDays.collect { value ->
                binding.tvDataRetention.text = "$value 天"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadUrl.take(1).collect { url ->
                binding.etUploadUrl.setText(url)
            }
        }
    }
    
    private fun showScanFrequencyDialog() {
        val options = arrayOf("3 秒", "5 秒（預設）", "7 秒", "10 秒")
        val values = arrayOf(3, 5, 7, 10)
        
        AlertDialog.Builder(requireContext())
            .setTitle("掃描頻率")
            .setItems(options) { _, which ->
                viewModel.updateScanFrequency(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showUploadIntervalDialog() {
        val options = arrayOf("30 秒", "60 秒（預設）", "90 秒", "120 秒")
        val values = arrayOf(30, 60, 90, 120)
        
        AlertDialog.Builder(requireContext())
            .setTitle("上傳間隔")
            .setItems(options) { _, which ->
                viewModel.updateUploadInterval(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showWhitelistSyncDialog() {
        val options = arrayOf("5 分鐘", "10 分鐘（預設）", "15 分鐘", "30 分鐘")
        val values = arrayOf(5, 10, 15, 30)
        
        AlertDialog.Builder(requireContext())
            .setTitle("白名單同步間隔")
            .setItems(options) { _, which ->
                viewModel.updateWhitelistSyncInterval(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showGpsFrequencyDialog() {
        val options = arrayOf("1 分鐘", "2 分鐘（預設）", "3 分鐘", "5 分鐘")
        val values = arrayOf(1, 2, 3, 5)
        
        AlertDialog.Builder(requireContext())
            .setTitle("GPS 更新頻率")
            .setItems(options) { _, which ->
                viewModel.updateGpsUpdateFrequency(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showDataRetentionDialog() {
        val options = arrayOf("7 天", "14 天", "30 天（預設）", "60 天", "90 天")
        val values = arrayOf(7, 14, 30, 60, 90)

        AlertDialog.Builder(requireContext())
            .setTitle("數據保留時間")
            .setItems(options) { _, which ->
                viewModel.updateDataRetentionDays(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showCacheLimitDialog() {
        val options = arrayOf("500 筆", "1000 筆（預設）", "1500 筆", "2000 筆")
        val values = arrayOf(500, 1000, 1500, 2000)
        
        AlertDialog.Builder(requireContext())
            .setTitle("離線快取上限")
            .setItems(options) { _, which ->
                viewModel.updateOfflineCacheLimit(values[which])
                Toast.makeText(requireContext(), "已設定為 ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun updatePermissionItem(
        statusView: android.widget.TextView,
        button: android.widget.Button,
        granted: Boolean
    ) {
        statusView.text = if (granted) "✅ 已授予" else "❌ 未授予"
        statusView.setTextColor(
            requireContext().getColor(
                if (granted) android.R.color.holo_green_dark 
                else android.R.color.holo_red_dark
            )
        )
        button.isEnabled = !granted
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
