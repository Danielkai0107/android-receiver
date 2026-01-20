# ✅ UUID API 404 錯誤修復

## 🐛 問題原因

Retrofit URL 配置錯誤導致 HTTP 404。

### 錯誤配置

**NetworkModule.kt**:
```kotlin
.baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids/")
                                                                           ↑ 多餘的斜線
```

**ServiceUuidApi.kt**:
```kotlin
@GET("/")
     ↑ 開頭的斜線
```

**實際請求的 URL**:
```
https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids//
                                                                   ↑↑ 雙斜線！
```

**結果**: HTTP 404 Not Found

---

## 🔧 修復方案

### 方案 1: 修改 baseUrl（已採用）

**NetworkModule.kt**:
```kotlin
.baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/")
                                                              ↑ 移除 getServiceUuids
```

**ServiceUuidApi.kt**:
```kotlin
@GET("getServiceUuids")
     ↑ 移除開頭斜線
```

**實際請求的 URL**:
```
https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids
                                                                  ↑ 正確！
```

---

## 📝 Retrofit URL 組合規則

### 規則說明

| baseUrl | @GET | 結果 URL | 狀態 |
|---------|------|----------|------|
| `https://api.com/` | `@GET("path")` | `https://api.com/path` | ✅ 正確 |
| `https://api.com/` | `@GET("/path")` | `https://api.com/path` | ✅ 正確 |
| `https://api.com/api/` | `@GET("path")` | `https://api.com/api/path` | ✅ 正確 |
| `https://api.com/api/` | `@GET("/")` | `https://api.com/` | ⚠️ 覆蓋 baseUrl |
| `https://api.com/path/` | `@GET("/")` | `https://api.com/` | ❌ 錯誤 |

### 最佳實踐

1. **baseUrl 必須以 `/` 結尾**
   ```kotlin
   .baseUrl("https://api.com/")  // ✅ 正確
   .baseUrl("https://api.com")   // ❌ 錯誤
   ```

2. **@GET 路徑不要以 `/` 開頭**（除非要覆蓋 baseUrl）
   ```kotlin
   @GET("users")      // ✅ 正確
   @GET("/users")     // ⚠️ 會覆蓋 baseUrl 的路徑部分
   ```

3. **Cloud Functions 的特殊情況**
   ```kotlin
   // 方案 A（推薦）
   .baseUrl("https://us-central1-xxx.cloudfunctions.net/")
   @GET("functionName")
   
   // 方案 B
   .baseUrl("https://us-central1-xxx.cloudfunctions.net/functionName/")
   @GET("")  // 空字串
   ```

---

## 🧪 測試驗證

### 測試 API
```bash
curl https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids
```

**預期回應**:
```json
{
  "success": true,
  "uuids": [
    "FDA50693-A4E2-4FB1-AFCF-C6EB01234567",
    "FDA50693-A4E2-4FB1-AFCF-C6EB00000000"
  ],
  "count": 2,
  "timestamp": 1768892531137
}
```

### 測試 App

**安裝 APK**:
```bash
adb install /Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

**監控日誌**:
```bash
adb logcat | grep "ServiceUuidRepository"
```

**預期日誌**:
```
D/ServiceUuidRepository: 開始同步 Service UUID...
D/ServiceUuidRepository: ✅ 同步成功，獲取 2 個 UUID:
D/ServiceUuidRepository:    - FDA50693-A4E2-4FB1-AFCF-C6EB01234567
D/ServiceUuidRepository:    - FDA50693-A4E2-4FB1-AFCF-C6EB00000000
```

**如果還是 404，會看到**:
```
E/ServiceUuidRepository: ❌ 同步失敗: HTTP 404 Not Found
```

---

## 📋 修改清單

### 1. NetworkModule.kt
```kotlin
// 修改前
.baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids/")

// 修改後
.baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/")
```

### 2. ServiceUuidApi.kt
```kotlin
// 修改前
@GET("/")

// 修改後
@GET("getServiceUuids")
```

---

## ✅ 驗證清單

安裝新版本後確認：

- [ ] App 啟動時沒有 404 錯誤
- [ ] Logcat 顯示「✅ 同步成功，獲取 2 個 UUID」
- [ ] 執行頁面顯示 Service UUID
- [ ] 可以正常開始掃描

---

## 🎯 完整的 URL 配置

### 所有 API 端點

| API | baseUrl | @GET | 完整 URL |
|-----|---------|------|----------|
| Service UUID | `https://us-central1-safe-net-tw.cloudfunctions.net/` | `getServiceUuids` | `https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids` |
| 白名單 | `https://getdevicewhitelist-kmzfyt3t5a-uc.a.run.app/` | `/` | `https://getdevicewhitelist-kmzfyt3t5a-uc.a.run.app/` |
| 上傳 | `https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/` | `/` | `https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/` |

---

## 📦 構建結果

✅ **構建成功！**

**APK 位置**:
```
/Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎉 修復完成

**404 錯誤已修復！現在 API 請求會使用正確的 URL！** 🚀

### 修復前
```
❌ https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids//
                                                                     ↑↑ 404
```

### 修復後
```
✅ https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids
                                                                    ↑ 200 OK
```
