# ✅ HTTP 404 錯誤處理修復

## 🐛 問題分析

### 錯誤日誌
```
I  <-- END HTTP (144-byte body)
E  上傳異常
   retrofit2.HttpException: HTTP 404
E  ❌ 上傳失敗: HTTP 404
```

### 根本原因

**API 端點本身是正常的**,但後端使用 **HTTP 404 狀態碼**來表示業務邏輯錯誤(如 Gateway 未註冊),而不是返回 HTTP 200 + `success: false`。

#### 實際的 API 回應

```bash
curl -X POST https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/ \
  -H "Content-Type: application/json" \
  -d '{"gateway_id":"TEST",...}'
```

**回應**:
```
HTTP/2 404
{
  "success": false,
  "error": "Gateway TEST is not registered or inactive. Please register it in the Gateway Management system."
}
```

#### 原本的問題

1. **Retrofit 處理方式**: 當收到 404 狀態碼時,直接拋出 `HttpException`,不會執行正常的響應處理邏輯
2. **原本的代碼**: 只有一個 `catch (e: Exception)`,無法提取 404 響應中的具體錯誤訊息
3. **結果**: 只顯示 "HTTP 404",用戶不知道具體原因

---

## 🔧 修復方案

### 1. 更新 BeaconDataResponse 模型

**檔案**: `app/src/main/java/com/safenet/receiver/data/remote/model/BeaconDataResponse.kt`

**新增 `error` 欄位**:
```kotlin
data class BeaconDataResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("error")  // ← 新增
    val error: String?
)
```

### 2. 改進 UploadRepository 的錯誤處理

**檔案**: `app/src/main/java/com/safenet/receiver/data/repository/UploadRepository.kt`

**新增專門的 HttpException 處理**:
```kotlin
} catch (e: retrofit2.HttpException) {
    // 處理 HTTP 錯誤(如 404),嘗試提取錯誤訊息
    val errorBody = try {
        e.response()?.errorBody()?.string()
    } catch (ex: Exception) {
        null
    }
    
    // 嘗試解析錯誤訊息
    val errorMessage = if (errorBody != null) {
        try {
            val gson = com.google.gson.Gson()
            val errorResponse = gson.fromJson(errorBody, BeaconDataResponse::class.java)
            errorResponse.message ?: errorResponse.error ?: "HTTP ${e.code()}"
        } catch (ex: Exception) {
            errorBody
        }
    } else {
        "HTTP ${e.code()}"
    }
    
    Log.e(TAG, "上傳失敗 (HTTP ${e.code()}): $errorMessage")
    Result.failure(Exception(errorMessage))
} catch (e: Exception) {
    Log.e(TAG, "上傳異常", e)
    Result.failure(e)
}
```

---

## 📊 修復效果對比

### 修復前
```
E  上傳異常
   retrofit2.HttpException: HTTP 404
E  ❌ 上傳失敗: HTTP 404
```
❌ 只知道是 404 錯誤,不知道具體原因

### 修復後
```
E  上傳失敗 (HTTP 404): Gateway ANDROID-xxx is not registered or inactive. Please register it in the Gateway Management system.
E  ❌ 上傳失敗: Gateway ANDROID-xxx is not registered or inactive...
```
✅ 清楚知道是 Gateway 未註冊的問題

---

## 🧪 測試步驟

### 1. 安裝新版本
```bash
adb install /Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

### 2. 啟動應用並開始掃描

### 3. 監控日誌
```bash
adb logcat | grep -E "UploadRepository|BeaconScanService.*上傳"
```

### 4. 預期結果

#### 如果 Gateway 未註冊
```
D  上傳 Beacon 資料: gateway=ANDROID-42ec6a54d319eb84, count=3
E  上傳失敗 (HTTP 404): Gateway ANDROID-xxx is not registered or inactive. Please register it in the Gateway Management system.
E  ❌ 上傳失敗: Gateway ANDROID-xxx is not registered...
```

#### 如果 Gateway 已註冊
```
D  上傳 Beacon 資料: gateway=ANDROID-42ec6a54d319eb84, count=3
D  上傳成功
D  ✅ 上傳成功: 3 筆,已更新狀態為 UPLOADED
```

---

## 💡 為什麼會有這個問題?

### 後端設計問題
理想的 RESTful API 設計應該是:
- **200 OK** + `{"success": false, "error": "..."}` → 業務邏輯錯誤
- **404 Not Found** → 端點不存在(URL 錯誤)

但目前的後端使用 **404** 來表示業務邏輯錯誤(Gateway 未註冊),這導致客戶端需要額外處理。

### 解決方案選擇
1. **修改後端** (理想但耗時): 改為返回 200 + `success: false`
2. **修改客戶端** (快速可行): 捕獲 HttpException 並解析錯誤訊息

我們選擇了方案 2,這樣可以立即解決問題,不需要等待後端修改。

---

## ✅ 驗證清單

安裝新版本後確認:

- [ ] 沒有 `retrofit2.HttpException` 未處理的錯誤
- [ ] 如果 Gateway 未註冊,錯誤訊息清楚說明原因
- [ ] 如果 Gateway 已註冊,上傳成功
- [ ] 錯誤日誌中顯示完整的錯誤訊息,不只是 "HTTP 404"

---

## 📱 APK 位置

```
/Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 後續建議

如果您有權限修改後端,建議將 API 改為:

**目前**:
```
HTTP/2 404
{"success": false, "error": "Gateway xxx is not registered..."}
```

**建議**:
```
HTTP/2 200 OK
{"success": false, "error": "Gateway xxx is not registered..."}
```

這樣更符合 RESTful API 的最佳實踐,客戶端處理起來也更簡單。

---

## 🎉 完成

**HTTP 404 錯誤處理已改進!** 🚀

現在應用可以正確提取和顯示 404 響應中的具體錯誤訊息,讓調試和問題排查更加容易。
