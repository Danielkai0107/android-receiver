package com.safenet.receiver.presentation.scans

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.safenet.receiver.R
import com.safenet.receiver.databinding.ActivityScansBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScansActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityScansBinding
    private val viewModel: ScansViewModel by viewModels()
    private lateinit var adapter: ScansAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScansBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "掃描清單"
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = ScansAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScansActivity)
            adapter = this@ScansActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.scannedBeacons.collect { beacons ->
                adapter.submitList(beacons)
                binding.tvEmpty.visibility = if (beacons.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.totalCount.collect { count ->
                binding.tvTotalCount.text = "總掃描數: $count"
            }
        }
        
        lifecycleScope.launch {
            viewModel.whitelistCount.collect { count ->
                binding.tvWhitelistCount.text = "白名單數: $count"
            }
        }
        
        lifecycleScope.launch {
            viewModel.uniqueDeviceCount.collect { count ->
                binding.tvUniqueCount.text = "不重複設備: $count"
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scans, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                showClearConfirmDialog()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除記錄")
            .setMessage("確定要清除所有掃描記錄嗎？")
            .setPositiveButton("確定") { _, _ ->
                viewModel.clearAllScans()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
