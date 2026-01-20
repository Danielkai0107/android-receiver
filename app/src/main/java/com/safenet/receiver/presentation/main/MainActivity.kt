package com.safenet.receiver.presentation.main

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.safenet.receiver.R
import com.safenet.receiver.databinding.ActivityMainBinding
import com.safenet.receiver.presentation.home.HomeFragment
import com.safenet.receiver.presentation.permissions.PermissionsFragment
import com.safenet.receiver.presentation.settings.SettingsFragment
import com.safenet.receiver.utils.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "所有權限已授予")
            Toast.makeText(this, "權限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Log.w(TAG, "部分權限被拒絕")
            showPermissionDeniedDialog()
        }
        // 更新權限狀態
        viewModel.updatePermissionsState(this)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        // 預設顯示執行頁
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
        
        // 初始化 Gateway ID（必須在同步 UUID 之前）
        viewModel.initializeGatewayId(this)
        
        // 檢查權限
        checkAndRequestPermissions()
        
        // 啟動時同步 Service UUID
        syncServiceUuidOnStartup()
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE
        )
        
        // Android 12+ 需要藍牙權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        // Android 13+ 需要通知權限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // 檢查哪些權限未授予
        val notGrantedPermissions = permissions.filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (notGrantedPermissions.isNotEmpty()) {
            Log.d(TAG, "需要請求權限: ${notGrantedPermissions.joinToString()}")
            showPermissionRequestDialog(notGrantedPermissions)
        } else {
            Log.d(TAG, "所有必要權限已授予")
            viewModel.updatePermissionsState(this)
        }
    }
    
    private fun showPermissionRequestDialog(permissions: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("需要權限")
            .setMessage("應用需要以下權限才能正常運作：\n\n" +
                    "• 位置權限 - 藍牙掃描必須\n" +
                    "• 藍牙權限 - 掃描 Beacon 設備\n" +
                    "• 手機狀態 - 獲取設備識別碼\n" +
                    "• 通知權限 - 顯示前台服務通知\n\n" +
                    "請點擊「授予權限」以繼續")
            .setPositiveButton("授予權限") { _, _ ->
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("稍後") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "部分功能可能無法使用", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("權限被拒絕")
            .setMessage("部分權限未授予，應用可能無法正常運作。\n\n您可以前往「權限」頁面重新授予權限。")
            .setPositiveButton("前往權限頁面") { _, _ ->
                binding.bottomNavigation.selectedItemId = R.id.nav_permissions
            }
            .setNegativeButton("關閉", null)
            .show()
    }
    
    private fun syncServiceUuidOnStartup() {
        lifecycleScope.launch {
            Log.d(TAG, "📱 App 啟動，開始同步服務 UUID...")
            
            var retryCount = 0
            val maxRetries = 10  // 最多重試 10 次
            var success = false
            
            while (!success && retryCount < maxRetries) {
                // 檢查當前 UUID 數量
                val currentCount = viewModel.uiState.value.serviceUuidCount
                
                if (currentCount > 0) {
                    Log.d(TAG, "✅ UUID 已同步成功 ($currentCount 個)")
                    Toast.makeText(this@MainActivity, "✅ 已載入 $currentCount 個服務 UUID", Toast.LENGTH_SHORT).show()
                    success = true
                    break
                }
                
                retryCount++
                Log.d(TAG, "嘗試同步 UUID (第 $retryCount 次)...")
                
                if (retryCount == 1) {
                    Toast.makeText(this@MainActivity, "正在同步服務 UUID...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "重試中... ($retryCount/$maxRetries)", Toast.LENGTH_SHORT).show()
                }
                
                viewModel.syncServiceUuid()
                
                // 等待 2 秒讓同步完成
                kotlinx.coroutines.delay(2000)
                
                // 檢查是否成功
                val newCount = viewModel.uiState.value.serviceUuidCount
                if (newCount > 0) {
                    Log.d(TAG, "✅ UUID 同步成功！獲取 $newCount 個 UUID")
                    Toast.makeText(this@MainActivity, "✅ 已載入 $newCount 個服務 UUID", Toast.LENGTH_SHORT).show()
                    success = true
                } else if (retryCount < maxRetries) {
                    Log.w(TAG, "⚠️ 第 $retryCount 次同步失敗，3 秒後重試...")
                    kotlinx.coroutines.delay(3000)
                }
            }
            
            if (!success) {
                Log.e(TAG, "❌ UUID 同步失敗！已重試 $maxRetries 次，請檢查網絡")
                Toast.makeText(
                    this@MainActivity,
                    "❌ 無法載入服務 UUID\n請檢查網絡連接\n可在執行頁面手動同步",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                R.id.nav_permissions -> {
                    loadFragment(PermissionsFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
