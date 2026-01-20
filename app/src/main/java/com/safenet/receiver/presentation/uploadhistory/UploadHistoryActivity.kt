package com.safenet.receiver.presentation.uploadhistory

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.safenet.receiver.R
import com.safenet.receiver.databinding.ActivityUploadHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityUploadHistoryBinding
    private val viewModel: UploadHistoryViewModel by viewModels()
    private lateinit var adapter: UploadHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "上傳記錄"
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        adapter = UploadHistoryAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UploadHistoryActivity)
            adapter = this@UploadHistoryActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uploadHistory.collect { history ->
                adapter.submitList(history)
                binding.tvEmpty.visibility = if (history.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                binding.tvTotalCount.text = "總上傳數: ${history.size}"
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_upload_history, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                showClearConfirmDialog()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除記錄")
            .setMessage("確定要清除所有上傳記錄嗎？")
            .setPositiveButton("確定") { _, _ ->
                viewModel.clearAllUploadHistory()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
