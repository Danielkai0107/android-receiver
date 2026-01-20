package com.safenet.receiver.presentation.main

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.safenet.receiver.R
import com.safenet.receiver.databinding.ActivityMainBinding
import com.safenet.receiver.presentation.home.HomeFragment
import com.safenet.receiver.presentation.permissions.PermissionsFragment
import com.safenet.receiver.presentation.settings.SettingsFragment
import com.safenet.receiver.presentation.whitelist.WhitelistFragment
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
        
        // 啟動時同步 Service UUID
        syncServiceUuidOnStartup()
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
                R.id.nav_whitelist -> {
                    loadFragment(WhitelistFragment())
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
