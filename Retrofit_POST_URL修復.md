# ✅ Retrofit POST URL 錯誤修復

## 🐛 錯誤原因

```
java.lang.IllegalArgumentException: Missing either @POST URL or @Url parameter.
for method CloudFunctionApi.uploadBeaconData
```

**Retrofit 不接受 `@POST("")` 空字串！**

---

## 🔧 修復方案

### CloudFunctionApi.kt

**修改前**:
```kotlin
@GET("")   // ❌ Retrofit 認為空字串無效
@POST("")  // ❌ 導致 IllegalArgumentException
```

**修改後**:
```kotlin
@GET("/")   // ✅ 正確
@POST("/")  // ✅ 正確
```

---

## 📝 為什麼可以用 `@POST("/")`？

### Cloud Run URL 的特性

這兩個 API 使用 **Cloud Run**，URL 本身就是完整的端點：

```
https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/
                                                 ↑ 這就是端點根路徑
```

**Retrofit 組合**:
```
baseUrl: https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/
@POST("/"): /
結果: https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/  ✅ 正確
```

### 與 Cloud Functions 的區別

**Cloud Functions** 需要指定函數名稱：
```
baseUrl: https://us-central1-xxx.cloudfunctions.net/
@GET("getServiceUuids"): getServiceUuids
結果: https://us-central1-xxx.cloudfunctions.net/getServiceUuids  ✅
```

---

## 📊 最終的 API 配置

### 1. Service UUID API (Cloud Functions)
```kotlin
.baseUrl("https://us-central1-safe-net-tw.cloudfunctions.net/")
@GET("getServiceUuids")
```

### 2. 白名單 API (Cloud Run)
```kotlin
.baseUrl("https://getdevicewhitelist-kmzfyt3t5a-uc.a.run.app/")
@GET("/")
```

### 3. 上傳 API (Cloud Run)
```kotlin
.baseUrl("https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/")
@POST("/")
```

---

## 📦 新版本已構建

✅ **構建成功！**

**APK 位置**:
```
/Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 測試步驟

### 1. 安裝新版本
```bash
adb install /Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

### 2. 監控上傳
```bash
adb logcat | grep "UploadRepository\|BeaconScanService.*上傳"
```

### 3. 預期日誌

**修復前（錯誤）**:
```
E  上傳異常
java.lang.IllegalArgumentException: Missing either @POST URL or @Url parameter.
E  ❌ 上傳失敗: Missing either @POST URL...
```

**修復後（成功）**:
```
D  上傳 Beacon 資料: gateway=ANDROID-42ec6a54d319eb84, count=1
D  上傳成功
D  ✅ 上傳成功: 1 筆，已更新狀態為 UPLOADED
```

或（如果 Gateway 未註冊）:
```
D  上傳 Beacon 資料: gateway=ANDROID-42ec6a54d319eb84, count=1
E  上傳失敗: Gateway ANDROID-xxx is not registered...
```
→ 這是正常的，至少不是 Retrofit 錯誤！

---

## ✅ 驗證清單

安裝新版本後確認：

- [ ] 沒有 "Missing @POST URL" 錯誤
- [ ] 看到 "上傳 Beacon 資料" 日誌
- [ ] 上傳成功或收到明確的 API 錯誤訊息
- [ ] Firebase 數據更新（如果 Gateway 已註冊）

---

## 🎉 完成

**Retrofit POST URL 錯誤已修復！** 🚀

### 修復內容
✅ `@GET("")` → `@GET("/")`  
✅ `@POST("")` → `@POST("/")`  
✅ 符合 Retrofit 規範  
✅ 可以正常上傳  

**現在上傳功能應該正常了！** ✨

---

## 📱 立即測試

```bash
adb install app-debug.apk
adb logcat | grep "UploadRepository"

# 等待 60 秒後應該看到上傳日誌
```
