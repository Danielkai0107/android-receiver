package com.safenet.receiver.presentation.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.safenet.receiver.databinding.FragmentPermissionsBinding
import com.safenet.receiver.utils.PermissionUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PermissionsFragment : Fragment() {
    
    private var _binding: FragmentPermissionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PermissionsViewModel by viewModels()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.updatePermissionsState(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        viewModel.updatePermissionsState(requireContext())
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.updatePermissionsState(requireContext())
    }
    
    private fun setupViews() {
        binding.apply {
            btnRequestLocation.setOnClickListener {
                requestLocationPermission()
            }
            
            btnRequestBluetooth.setOnClickListener {
                requestBluetoothPermission()
            }
            
            btnRequestPhoneState.setOnClickListener {
                requestPhoneStatePermission()
            }
            
            btnRequestNotification.setOnClickListener {
                requestNotificationPermission()
            }
            
            btnRequestBackgroundLocation.setOnClickListener {
                requestBackgroundLocationPermission()
            }
            
            btnOpenSettings.setOnClickListener {
                openAppSettings()
            }
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.permissionsState.collect { permissions ->
                updatePermissionsUI(permissions)
            }
        }
    }
    
    private fun updatePermissionsUI(permissions: com.safenet.receiver.presentation.main.PermissionsState) {
        binding.apply {
            // 位置權限
            updatePermissionItem(
                tvLocationStatus,
                btnRequestLocation,
                permissions.location
            )
            
            // 藍牙權限
            updatePermissionItem(
                tvBluetoothStatus,
                btnRequestBluetooth,
                permissions.bluetooth
            )
            
            // 手機狀態
            updatePermissionItem(
                tvPhoneStateStatus,
                btnRequestPhoneState,
                permissions.phoneState
            )
            
            // 通知權限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                layoutNotification.visibility = View.VISIBLE
                updatePermissionItem(
                    tvNotificationStatus,
                    btnRequestNotification,
                    permissions.notification
                )
            } else {
                layoutNotification.visibility = View.GONE
            }
            
            // 背景位置
            updatePermissionItem(
                tvBackgroundLocationStatus,
                btnRequestBackgroundLocation,
                permissions.backgroundLocation
            )
            
            // 整體狀態
            val allGranted = permissions.location && permissions.bluetooth && permissions.phoneState
            tvOverallStatus.text = if (allGranted) {
                "✅ 所有必要權限已授予"
            } else {
                "⚠️ 請授予所有必要權限"
            }
            tvOverallStatus.setTextColor(
                requireContext().getColor(
                    if (allGranted) android.R.color.holo_green_dark 
                    else android.R.color.holo_orange_dark
                )
            )
        }
    }
    
    private fun updatePermissionItem(
        statusTextView: android.widget.TextView,
        button: android.widget.Button,
        granted: Boolean
    ) {
        statusTextView.text = if (granted) "✅ 已授予" else "❌ 未授予"
        statusTextView.setTextColor(
            requireContext().getColor(
                if (granted) android.R.color.holo_green_dark 
                else android.R.color.holo_red_dark
            )
        )
        button.isEnabled = !granted
    }
    
    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }
    
    private fun requestPhoneStatePermission() {
        permissionLauncher.launch(
            arrayOf(android.Manifest.permission.READ_PHONE_STATE)
        )
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }
    
    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(requireContext())
                .setTitle("背景位置權限")
                .setMessage("需要「一律允許」位置權限\n\n請在設定中選擇：\n「一律允許」或「Allow all the time」")
                .setPositiveButton("前往設定") { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton("取消", null)
                .show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
