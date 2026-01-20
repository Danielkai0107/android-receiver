package com.safenet.receiver.presentation.whitelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.safenet.receiver.databinding.FragmentWhitelistBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class WhitelistFragment : Fragment() {
    
    private var _binding: FragmentWhitelistBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WhitelistViewModel by viewModels()
    private lateinit var adapter: WhitelistAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhitelistBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupViews() {
        // 移除同步按鈕
        binding.btnSync.visibility = View.GONE
    }
    
    private fun setupRecyclerView() {
        adapter = WhitelistAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WhitelistFragment.adapter
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadRecords.collect { records ->
                adapter.submitList(records)
                binding.tvEmpty.visibility = if (records.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // 顯示最新上傳時間
                if (records.isNotEmpty()) {
                    val latestTime = records.firstOrNull()?.scannedAt
                    if (latestTime != null) {
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(latestTime))
                        binding.tvSyncTime.text = "最新: $time"
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalCount.collect { count ->
                binding.tvCount.text = "已上傳記錄: $count"
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
