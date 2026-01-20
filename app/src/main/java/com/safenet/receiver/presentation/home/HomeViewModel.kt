package com.safenet.receiver.presentation.home

import com.safenet.receiver.presentation.main.MainViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    whitelistRepository: com.safenet.receiver.data.repository.WhitelistRepository,
    beaconRepository: com.safenet.receiver.data.repository.BeaconRepository,
    serviceUuidRepository: com.safenet.receiver.data.repository.ServiceUuidRepository,
    scannedBeaconDao: com.safenet.receiver.data.local.dao.ScannedBeaconDao,
    preferenceManager: com.safenet.receiver.utils.PreferenceManager,
    workManager: androidx.work.WorkManager
) : MainViewModel(whitelistRepository, beaconRepository, serviceUuidRepository, scannedBeaconDao, preferenceManager, workManager)
