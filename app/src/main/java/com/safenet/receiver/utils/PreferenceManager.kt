package com.safenet.receiver.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val GATEWAY_ID = stringPreferencesKey("gateway_id")
        val SCAN_FREQUENCY = intPreferencesKey("scan_frequency")  // 秒
        val UPLOAD_INTERVAL = intPreferencesKey("upload_interval")  // 秒
        val WHITELIST_SYNC_INTERVAL = intPreferencesKey("whitelist_sync_interval")  // 分鐘
        val GPS_UPDATE_FREQUENCY = intPreferencesKey("gps_update_frequency")  // 分鐘
        val OFFLINE_CACHE_LIMIT = intPreferencesKey("offline_cache_limit")  // 筆數
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")  // 是否首次啟動
        val UPLOAD_URL = stringPreferencesKey("upload_url")  // 上傳 API 完整 URL
        val UPLOAD_ENABLED = booleanPreferencesKey("upload_enabled")  // 上傳開關
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")  // 數據保留天數

        const val DEFAULT_SCAN_FREQUENCY = 5
        const val DEFAULT_DATA_RETENTION_DAYS = 30
        const val DEFAULT_UPLOAD_URL = "https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData"
        const val DEFAULT_UPLOAD_INTERVAL = 60
        const val DEFAULT_WHITELIST_SYNC_INTERVAL = 10
        const val DEFAULT_GPS_UPDATE_FREQUENCY = 2
        const val DEFAULT_OFFLINE_CACHE_LIMIT = 1000
    }

    suspend fun saveGatewayId(gatewayId: String) {
        dataStore.edit { preferences ->
            preferences[GATEWAY_ID] = gatewayId
        }
    }

    fun getGatewayId(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[GATEWAY_ID]
    }

    suspend fun saveScanFrequency(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[SCAN_FREQUENCY] = seconds
        }
    }

    fun getScanFrequency(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[SCAN_FREQUENCY] ?: DEFAULT_SCAN_FREQUENCY
    }

    suspend fun saveUploadInterval(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[UPLOAD_INTERVAL] = seconds
        }
    }

    fun getUploadInterval(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[UPLOAD_INTERVAL] ?: DEFAULT_UPLOAD_INTERVAL
    }

    suspend fun saveWhitelistSyncInterval(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[WHITELIST_SYNC_INTERVAL] = minutes
        }
    }

    fun getWhitelistSyncInterval(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[WHITELIST_SYNC_INTERVAL] ?: DEFAULT_WHITELIST_SYNC_INTERVAL
    }

    suspend fun saveGpsUpdateFrequency(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[GPS_UPDATE_FREQUENCY] = minutes
        }
    }

    fun getGpsUpdateFrequency(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[GPS_UPDATE_FREQUENCY] ?: DEFAULT_GPS_UPDATE_FREQUENCY
    }

    suspend fun saveOfflineCacheLimit(limit: Int) {
        dataStore.edit { preferences ->
            preferences[OFFLINE_CACHE_LIMIT] = limit
        }
    }

    fun getOfflineCacheLimit(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[OFFLINE_CACHE_LIMIT] ?: DEFAULT_OFFLINE_CACHE_LIMIT
    }

    suspend fun saveUploadUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[UPLOAD_URL] = url.trim().ifEmpty { null } ?: DEFAULT_UPLOAD_URL
        }
    }

    fun getUploadUrl(): Flow<String> = dataStore.data.map { preferences ->
        preferences[UPLOAD_URL] ?: DEFAULT_UPLOAD_URL
    }

    suspend fun saveUploadEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UPLOAD_ENABLED] = enabled
        }
    }

    fun getUploadEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[UPLOAD_ENABLED] ?: true
    }

    suspend fun saveDataRetentionDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[DATA_RETENTION_DAYS] = days
        }
    }

    fun getDataRetentionDays(): Flow<Int> = dataStore.data.map { preferences ->
        preferences[DATA_RETENTION_DAYS] ?: DEFAULT_DATA_RETENTION_DAYS
    }

    /**
     * 檢查是否首次啟動
     */
    suspend fun isFirstLaunch(): Boolean {
        var isFirst = false
        dataStore.edit { preferences ->
            isFirst = preferences[IS_FIRST_LAUNCH] ?: true
            if (isFirst) {
                preferences[IS_FIRST_LAUNCH] = false
            }
        }
        return isFirst
    }
}
