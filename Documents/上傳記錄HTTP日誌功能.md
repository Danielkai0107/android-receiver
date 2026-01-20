# 上傳記錄 HTTP 日誌功能

**更新日期：** 2026-01-20

## 功能概述

在上傳記錄頁面顯示完整的 HTTP 請求和響應資訊，類似 OkHttp 的日誌輸出。

## 顯示內容

每筆上傳記錄會顯示：

### 1. 基本資訊
- UUID
- Major / Minor
- RSSI (信號強度)
- 位置（經緯度）
- 掃描時間
- 上傳時間
- 上傳狀態

### 2. HTTP 請求 (Request)
```
--> POST https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData
Content-Type: application/json; charset=UTF-8
Content-Length: 193

{"beacons":[{"major":1,"minor":0,"rssi":-36,"uuid":"00000000-0000-0000-0000-000000000000"}],"gateway_id":"ANDROID-42ec6a54d319eb84","lat":24.9810858,"lng":121.5386161,"timestamp":1768923390218}
--> END POST
```

### 3. HTTP 響應 (Response)
```
<-- 200 https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData (2479ms)
content-type: application/json; charset=utf-8
server: Google Frontend

{"success":true,"received":1,"updated":0,"ignored":1,"timestamp":1768923390218}
<-- END HTTP
```

## 技術實作

### 1. 資料庫變更

#### BeaconQueueEntity.kt
新增欄位：
```kotlin
val uploadedAt: Long? = null,  // 上傳時間戳
val requestUrl: String? = null,  // 請求 URL
val requestBody: String? = null,  // 請求 Body (JSON)
val requestHeaders: String? = null,  // 請求 Headers (JSON)
val responseCode: Int? = null,  // 響應狀態碼
val responseBody: String? = null,  // 響應 Body (JSON)
val responseHeaders: String? = null,  // 響應 Headers (JSON)
val responseDuration: Long? = null  // 響應時間 (ms)
```

#### 資料庫版本升級
- 從 version 4 升級到 version 5
- 使用 `fallbackToDestructiveMigration()` 處理遷移

### 2. 上傳邏輯修改

#### UploadDetails.kt (新增)
建立資料類來傳遞上傳詳情：
```kotlin
data class UploadDetails(
    val success: Boolean,
    val requestUrl: String,
    val requestBody: String,
    val requestHeaders: Map<String, String>,
    val responseCode: Int,
    val responseBody: String,
    val responseHeaders: Map<String, String>,
    val responseDuration: Long,
    val uploadedAt: Long = System.currentTimeMillis()
)
```

#### UploadRepository.kt
- 修改 `uploadBeacons()` 返回類型從 `Result<Unit>` 改為 `Result<UploadDetails>`
- 在 `tryUploadToPrimary()` 和 `tryUploadToFallback()` 中：
  - 記錄開始時間
  - 捕獲請求 URL、Headers、Body
  - 捕獲響應狀態碼、Headers、Body
  - 計算響應時間
  - 返回 `UploadDetails` 物件

#### UploadWorker.kt
- 接收 `UploadDetails`
- 為每個上傳的 Beacon 調用 `beaconRepository.updateUploadDetails()`
- 將 HTTP 資訊保存到資料庫

#### BeaconQueueDao.kt
新增方法：
```kotlin
@Query("""
    UPDATE beacon_queue 
    SET uploadStatus = 'UPLOADED',
        uploadedAt = :uploadedAt,
        requestUrl = :requestUrl,
        requestBody = :requestBody,
        requestHeaders = :requestHeaders,
        responseCode = :responseCode,
        responseBody = :responseBody,
        responseHeaders = :responseHeaders,
        responseDuration = :responseDuration
    WHERE id = :id
""")
suspend fun updateUploadDetails(...)
```

### 3. UI 顯示

#### item_upload_history.xml
- 新增 `tvUploadTime` 顯示上傳時間
- 新增 `tvHttpLog` 在 ScrollView 中顯示 HTTP 日誌
- 設置最大高度 300dp，可滾動查看

#### UploadHistoryAdapter.kt
- 新增 `buildHttpLog()` 方法格式化 HTTP 日誌
- 模擬 OkHttp 日誌格式：
  - `--> POST URL` 開始請求
  - 顯示 Request Headers
  - 顯示 Request Body
  - `--> END POST` 結束請求
  - `<-- CODE URL (duration)` 開始響應
  - 顯示 Response Headers
  - 顯示 Response Body
  - `<-- END HTTP` 結束響應

## 修改的檔案

### 新增
1. `UploadDetails.kt` - 上傳詳情資料類

### 修改
1. `BeaconQueueEntity.kt` - 新增 HTTP 欄位
2. `AppDatabase.kt` - 版本升級到 5
3. `BeaconQueueDao.kt` - 新增 `updateUploadDetails()`
4. `BeaconRepository.kt` - 新增 `updateUploadDetails()`
5. `UploadRepository.kt` - 捕獲並返回 HTTP 詳情
6. `UploadWorker.kt` - 保存 HTTP 詳情
7. `UploadHistoryAdapter.kt` - 格式化並顯示 HTTP 日誌
8. `item_upload_history.xml` - UI 佈局更新

## 測試要點

### 1. 上傳功能測試
- [ ] 執行掃描並等待自動上傳
- [ ] 檢查是否成功捕獲 HTTP 資訊

### 2. 顯示測試
- [ ] 打開上傳記錄頁面
- [ ] 檢查是否顯示完整的 HTTP 日誌
- [ ] 確認格式類似 OkHttp 日誌輸出
- [ ] 確認 Request URL 正確
- [ ] 確認 Request Headers 正確
- [ ] 確認 Request Body 格式正確
- [ ] 確認 Response Code 正確
- [ ] 確認 Response Body 正確
- [ ] 確認響應時間正確

### 3. 滾動測試
- [ ] 檢查長日誌是否可以滾動查看
- [ ] 確認最大高度限制生效

### 4. 資料庫測試
- [ ] 卸載並重新安裝 App
- [ ] 確認資料庫升級成功
- [ ] 執行上傳並檢查新欄位

## 範例輸出

```
--> POST https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData
Content-Type: application/json; charset=UTF-8
Content-Length: 193

{"beacons":[{"major":1,"minor":0,"rssi":-36,"uuid":"00000000-0000-0000-0000-000000000000"}],"gateway_id":"ANDROID-42ec6a54d319eb84","lat":24.9810858,"lng":121.5386161,"timestamp":1768923390218}
--> END POST

<-- 200 https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData (2479ms)
content-type: application/json; charset=utf-8
server: Google Frontend

{"success":true,"received":1,"updated":0,"ignored":1,"timestamp":1768923390218}
<-- END HTTP
```

## 構建狀態

✅ **構建成功** (無錯誤)

---

**實作者：** AI Assistant  
**版本：** v1.0.20260120-http-logs
