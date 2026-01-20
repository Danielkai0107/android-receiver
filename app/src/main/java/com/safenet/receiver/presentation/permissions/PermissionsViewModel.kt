package com.safenet.receiver.presentation.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.receiver.presentation.main.PermissionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor() : ViewModel() {
    
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()
    
    fun updatePermissionsState(context: Context) {
        val permissions = PermissionsState(
            location = hasLocationPermission(context),
            bluetooth = hasBluetoothPermission(context),
            phoneState = hasPhoneStatePermission(context),
            notification = hasNotificationPermission(context),
            backgroundLocation = com.safenet.receiver.utils.PermissionUtil.hasBackgroundLocationPermission(context)
        )
        _permissionsState.value = permissions
    }
    
    private fun hasLocationPermission(context: Context): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED == 
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
    }
    
    private fun hasBluetoothPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.content.pm.PackageManager.PERMISSION_GRANTED == 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                )
        } else {
            true
        }
    }
    
    private fun hasPhoneStatePermission(context: Context): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED == 
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_STATE
            )
    }
    
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.content.pm.PackageManager.PERMISSION_GRANTED == 
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
        } else {
            true
        }
    }
}
