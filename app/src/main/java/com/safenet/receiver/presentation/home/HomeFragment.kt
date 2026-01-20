package com.safenet.receiver.presentation.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.safenet.receiver.R
import com.safenet.receiver.databinding.FragmentHomeBinding
import com.safenet.receiver.presentation.scans.ScansActivity
import com.safenet.receiver.service.BeaconScanService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private var isServiceRunning = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        // 初次檢查服務狀態
        checkServiceStatus()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.updatePermissionsState(requireContext())
        // 檢查服務實際運行狀態並同步 UI
        checkServiceStatus()
        // 不再自動同步白名單（切換分頁時）
    }
    
    private fun setupViews() {
        binding.apply {
            btnStartScan.setOnClickListener {
                if (isServiceRunning) {
                    stopScanService()
                } else {
                    startScanService()
                }
            }
            
            btnSyncServiceUuid.setOnClickListener {
                viewModel.syncServiceUuid()
            }
            
            btnViewScans.setOnClickListener {
                startActivity(Intent(requireContext(), ScansActivity::class.java))
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }
    
    private fun updateUI(state: com.safenet.receiver.presentation.main.MainUiState) {
        binding.apply {
            tvGatewayId.text = "Gateway ID: ${state.gatewayId}"
            
            // 顯示完整 Service UUID
            if (state.serviceUuidCount > 0 && state.serviceUuids.isNotEmpty()) {
                val uuidDisplay = state.serviceUuids.joinToString("\n") { uuid ->
                    "• $uuid"  // 顯示完整 UUID
                }
                tvServiceUuids.text = "服務 UUID (${state.serviceUuidCount}):\n$uuidDisplay"
            } else {
                tvServiceUuids.text = "服務 UUID: 未同步"
            }
            
            // 統計資訊（移除重複的 uploadedHistory）
            tvScannedCount.text = getString(R.string.scanned_count, state.scannedCount)
            tvUploadedCount.text = getString(R.string.uploaded_count, state.uploadedCount)
            tvPendingCount.text = getString(R.string.pending_count, state.pendingCount)
            
            // 顯示最遠距離
            tvMaxDistance.text = String.format("📏 最遠距離: %.1f m", state.maxDistance)
            
            if (state.lastSyncTime != null) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(state.lastSyncTime))
                tvLastSync.text = "同步: $time (${state.serviceUuidCount} 個 UUID)"
            }
            
            btnSyncServiceUuid.isEnabled = !state.isSyncing
            
            if (state.syncError != null && !state.syncError.contains("未註冊")) {
                Toast.makeText(requireContext(), state.syncError, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startScanService() {
        lifecycleScope.launch {
            // 檢查是否已同步 Service UUID
            val currentState = viewModel.uiState.value
            if (currentState.serviceUuidCount == 0) {
                Toast.makeText(requireContext(), "請先同步服務 UUID", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Android 14+ 需要檢查位置權限（前台服務 location 類型必須）
            if (!currentState.permissions.location) {
                Toast.makeText(requireContext(), "需要位置權限才能啟動掃描服務", Toast.LENGTH_LONG).show()
                android.util.Log.e("HomeFragment", "缺少位置權限，無法啟動前台服務")
                return@launch
            }
            
            // 清空掃描清單和統計資料
            viewModel.clearAllData()
            android.util.Log.d("HomeFragment", "已清空所有資料，重新開始掃描")
            
            val intent = Intent(requireContext(), BeaconScanService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
            isServiceRunning = true
            updateScanButton()
            
            // 延遲檢查服務是否真的啟動了
            kotlinx.coroutines.delay(500) // 等待 500ms
            val actuallyRunning = isServiceActuallyRunning()
            if (!actuallyRunning) {
                android.util.Log.w("HomeFragment", "服務啟動命令已發送，但服務未運行")
                isServiceRunning = false
                updateScanButton()
                Toast.makeText(requireContext(), "服務啟動失敗，請檢查權限", Toast.LENGTH_LONG).show()
            } else {
                android.util.Log.d("HomeFragment", "服務已成功啟動")
                Toast.makeText(requireContext(), "掃描服務已啟動", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun stopScanService() {
        val intent = Intent(requireContext(), BeaconScanService::class.java)
        requireContext().stopService(intent)
        isServiceRunning = false
        updateScanButton()
        
        // 重置最遠距離
        viewModel.resetMaxDistance()
        
        // 延遲檢查服務是否真的停止了
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500) // 等待 500ms
            val stillRunning = isServiceActuallyRunning()
            if (stillRunning) {
                android.util.Log.w("HomeFragment", "服務停止命令已發送，但服務仍在運行")
                // 再次嘗試同步狀態
                checkServiceStatus()
            } else {
                android.util.Log.d("HomeFragment", "服務已成功停止")
            }
        }
        
        Toast.makeText(requireContext(), "掃描服務已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateScanButton() {
        binding.btnStartScan.text = if (isServiceRunning) {
            "⏹ 停止掃描"
        } else {
            "▶️ 開始掃描"
        }
    }
    
    /**
     * 檢查服務實際運行狀態
     * 解決服務被系統殺死或 Fragment 重建後狀態不同步的問題
     */
    private fun checkServiceStatus() {
        val actuallyRunning = isServiceActuallyRunning()
        if (isServiceRunning != actuallyRunning) {
            android.util.Log.d("HomeFragment", "檢測到狀態不同步！本地狀態: $isServiceRunning, 實際狀態: $actuallyRunning")
            isServiceRunning = actuallyRunning
            updateScanButton()
        }
    }
    
    /**
     * 檢查 BeaconScanService 是否真的在運行
     */
    @Suppress("DEPRECATION")
    private fun isServiceActuallyRunning(): Boolean {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        try {
            // 獲取正在運行的服務列表
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            // 檢查我們的服務是否在列表中
            for (service in runningServices) {
                if (BeaconScanService::class.java.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "檢查服務狀態時出錯", e)
        }
        
        return false
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
