# 🚀 快速啟動指南 - Android 實體機測試

## 📱 準備工作

### 1. 檢查專案狀態

專案已經清理乾淨，不包含：
- ❌ build 目錄（構建時自動生成）
- ❌ .gradle 目錄（Gradle 快取）
- ❌ .idea 目錄（IDE 配置）
- ❌ local.properties（本地配置）
- ❌ *.iml 文件（IDE 模組文件）

✅ 專案處於最乾淨的狀態，可以直接使用！

---

## 🔌 連接 Android 實體機

### 步驟 1: 準備手機

#### 在 Android 手機上開啟開發者選項

1. **進入設定** → **關於手機**
2. 連續點擊 **「版本號碼」** 或 **「Build Number」** 7 次
3. 螢幕會顯示「您已成為開發人員」

#### 開啟 USB 調試

1. **返回設定** → **系統** → **開發人員選項**
2. 開啟 **「USB 調試」**
3. 開啟 **「安裝透過 USB 的應用程式」**（可選）

### 步驟 2: 連接手機到電腦

1. 使用 USB 線連接手機和 Mac
2. 手機會彈出 **「允許 USB 調試」** 提示
3. 勾選 **「一律允許這台電腦」**
4. 點擊 **「確定」**

### 步驟 3: 驗證連接

打開終端機，執行：

```bash
# 如果沒有 adb，先安裝 Android SDK Platform Tools
# 或使用 Android Studio 內建的 adb

# 檢查設備是否連接
adb devices
```

**預期輸出：**
```
List of devices attached
XXXXXXXXXX    device    ← 你的手機序號
```

如果顯示 `unauthorized`，請檢查手機是否授權 USB 調試。

---

## 🏗️ 使用 Android Studio 構建和運行

### 方法 1: 通過 Android Studio（推薦）

#### 1. 打開專案

```bash
cd /Users/danielkai/Desktop/safe-net-app/android-receiver
# 右鍵點擊目錄，選擇「用 Android Studio 打開」
# 或在 Android Studio 中選擇 File → Open → 選擇 android-receiver 目錄
```

#### 2. 等待 Gradle 同步

- 首次打開會自動下載依賴（需要網路）
- 等待下方的 Gradle Build 完成
- 如果有錯誤，點擊 **「Sync Now」** 重試

#### 3. 選擇運行設備

- 在 Android Studio 頂部工具欄
- 點擊設備下拉選單
- 選擇你的實體機（會顯示型號名稱）

#### 4. 運行 App

點擊綠色的 **▶️ Run** 按鈕，或按快捷鍵：
- **Mac**: `Control + R` 或 `Shift + F10`
- **Windows**: `Shift + F10`

#### 5. 首次安裝

- App 會自動安裝到手機
- 安裝完成後自動啟動
- 可能需要在手機上授予安裝權限

---

## 🛠️ 使用命令行構建和運行

### 方法 2: 通過 Gradle 命令行

#### 1. 進入專案目錄

```bash
cd /Users/danielkai/Desktop/safe-net-app/android-receiver
```

#### 2. 構建 Debug APK

```bash
# 如果沒有執行權限，先添加
chmod +x gradlew

# 清理並構建
./gradlew clean assembleDebug
```

**APK 位置：**
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 3. 安裝到手機

```bash
# 確保手機已連接
adb devices

# 安裝 APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 如果已安裝，使用 -r 參數重新安裝
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 4. 啟動 App

```bash
# 啟動 MainActivity
adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity
```

---

## ✅ 測試清單

### 1. 首次啟動測試

#### 步驟：
1. **啟動 App**
2. **授予權限**：
   - ✅ 位置權限（精確位置）
   - ✅ 藍牙權限
   - ✅ 讀取手機狀態權限
   - ✅ 通知權限（Android 13+）

#### 驗證：
- [ ] Gateway ID 已顯示（IMEI 或 ANDROID-XXX）
- [ ] 所有統計數字顯示為 0
- [ ] 沒有崩潰或錯誤

### 2. 白名單同步測試

#### 步驟：
1. 點擊 **「同步白名單」** 按鈕
2. 等待幾秒鐘

#### 驗證：
- [ ] 白名單數量更新（例如：白名單設備: 5）
- [ ] 最後同步時間更新
- [ ] 沒有錯誤提示

#### 如果出現錯誤：
```
錯誤: Gateway XX:XX:XX:XX:XX:XX is not registered or inactive
```
**說明**: 這個提示表示 Gateway 未在系統中註冊

**無需擔心**:
- ✅ 這不影響功能，App 仍可正常掃描和上傳
- ✅ 白名單可能為空，但不影響 Beacon 掃描
- ✅ 掃描到的 Beacon 會正常上傳到 Cloud Function

**如需完整功能** (可選):
1. 記下 App 顯示的 Gateway ID
2. 在 Firebase Console 的 `gateways` 集合中註冊這個 Gateway
3. 在 `devices` 集合中添加需要監控的設備
4. 重新同步白名單

### 3. Beacon 掃描測試

#### 步驟：
1. 點擊 **「開始掃描」** 按鈕
2. 通知欄會顯示「SafeNet 接收器 - Beacon 掃描服務運行中」

#### 驗證：
- [ ] 通知正常顯示
- [ ] 按鈕文字變成「停止掃描」
- [ ] 沒有崩潰

#### 掃描 Beacon（需要實際 Beacon 設備）：
- [ ] 已掃描數量增加
- [ ] 如果 Beacon 在白名單中，待上傳數量增加
- [ ] 如果 Beacon 不在白名單中，被忽略

### 4. 自動上傳測試

#### 步驟：
1. 確保有網路連線
2. 等待 60 秒（預設上傳間隔）

#### 驗證：
- [ ] 已上傳數量增加
- [ ] 待上傳數量減少
- [ ] 沒有錯誤

### 5. 離線快取測試

#### 步驟：
1. 關閉手機的網路連線（Wi-Fi 和行動數據）
2. 繼續掃描 Beacon
3. 等待 60 秒
4. 開啟網路連線

#### 驗證：
- [ ] 離線時，待上傳數量持續增加
- [ ] 網路恢復後，自動開始上傳
- [ ] 已上傳數量增加

### 6. UI 功能測試

#### 查看白名單：
1. 點擊 **「查看白名單」**
2. 查看設備列表

#### 驗證：
- [ ] 顯示所有白名單設備
- [ ] 每個設備顯示：UUID, Major, Minor, MAC, 同步時間
- [ ] 可以正常返回

#### 查看設定：
1. 點擊 **「設定」**
2. 查看配置參數

#### 驗證：
- [ ] 顯示所有設定值
- [ ] 掃描頻率: 5 秒
- [ ] 上傳間隔: 60 秒
- [ ] 白名單同步間隔: 10 分鐘
- [ ] GPS 更新頻率: 2 分鐘
- [ ] 離線快取上限: 1000 筆

---

## 🐛 常見問題排除

### 問題 1: adb devices 找不到設備

**解決方案**:
```bash
# 重啟 adb server
adb kill-server
adb start-server
adb devices
```

**或者**:
- 檢查 USB 線是否正常
- 嘗試不同的 USB 端口
- 重新授權 USB 調試

### 問題 2: Gradle 同步失敗

**解決方案**:
```bash
# 清理 Gradle 快取
./gradlew clean

# 刪除 .gradle 目錄
rm -rf .gradle

# 重新同步
./gradlew build
```

### 問題 3: App 安裝失敗

**錯誤**: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**解決方案**:
```bash
# 先卸載舊版本
adb uninstall com.safenet.receiver

# 重新安裝
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 問題 4: 權限請求沒有彈出

**解決方案**:
1. 卸載 App
2. 重新安裝
3. 首次啟動會自動請求權限

**或手動授權**:
```bash
adb shell pm grant com.safenet.receiver android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.safenet.receiver android.permission.BLUETOOTH_SCAN
adb shell pm grant com.safenet.receiver android.permission.READ_PHONE_STATE
```

### 問題 5: 無法獲取 IMEI

**原因**: Android 10+ 限制 IMEI 訪問

**解決方案**:
- App 會自動使用 Android ID 作為備用
- Gateway ID 格式會是 `ANDROID-XXXXXXXXX`
- 功能完全正常

### 問題 6: Beacon 掃描不到設備

**檢查清單**:
- [ ] 藍牙已開啟
- [ ] 已授予位置權限（必須）
- [ ] Beacon 設備正常工作
- [ ] Beacon 在掃描範圍內（< 10m）

### 問題 7: 白名單同步失敗

**可能原因**:
1. Gateway ID 未在後台註冊
2. 網路連線問題
3. Cloud Function API 異常

**檢查步驟**:
```bash
# 測試 Cloud Function API
curl "https://us-central1-safe-net-tw.cloudfunctions.net/getDeviceWhitelist?gateway_id=YOUR_GATEWAY_ID"
```

---

## 📊 查看 Log 輸出

### 使用 Android Studio

1. 打開 **Logcat** 視圖（底部工具欄）
2. 選擇你的設備
3. 搜尋 **"safenet"** 或 **"receiver"**

### 使用命令行

```bash
# 即時查看所有 Log
adb logcat | grep -i safenet

# 查看特定 TAG
adb logcat | grep "WhitelistRepository\|BeaconScanService\|UploadRepository"

# 清除舊 Log
adb logcat -c

# 保存 Log 到文件
adb logcat > app_log.txt
```

**關鍵 Log TAG**:
- `WhitelistRepository` - 白名單同步
- `BeaconScanService` - Beacon 掃描
- `UploadRepository` - 數據上傳
- `LocationService` - GPS 定位
- `MainViewModel` - UI 狀態

---

## 🎯 測試通過標準

### ✅ 基本功能

- [x] App 正常啟動
- [x] 所有權限已授予
- [x] Gateway ID 正常顯示
- [x] 白名單同步成功
- [x] Beacon 掃描正常運行
- [x] 自動上傳功能正常

### ✅ 性能測試

- [x] CPU 使用率 < 10%
- [x] 內存使用 < 100MB
- [x] 沒有 ANR (應用無響應)
- [x] 沒有崩潰

### ✅ 穩定性測試

- [x] 連續運行 1 小時無問題
- [x] 前景/背景切換正常
- [x] 鎖屏後繼續運行
- [x] 網路中斷恢復正常

---

## 📱 實體機測試檢查清單

### 啟動前

- [ ] 手機已開啟開發者選項
- [ ] USB 調試已啟用
- [ ] 手機已連接到電腦
- [ ] adb devices 能看到設備

### 構建

- [ ] Gradle 同步成功
- [ ] 構建沒有錯誤
- [ ] APK 生成成功

### 安裝

- [ ] App 成功安裝到手機
- [ ] 圖標出現在手機桌面
- [ ] 可以正常啟動

### 功能

- [ ] 所有權限已授予
- [ ] Gateway ID 顯示正確
- [ ] 白名單同步成功
- [ ] Beacon 掃描正常
- [ ] 自動上傳正常
- [ ] 離線快取正常
- [ ] UI 交互流暢

### 完成

- [ ] 所有功能測試通過
- [ ] 沒有發現嚴重 Bug
- [ ] Log 沒有異常錯誤
- [ ] 可以投入實際使用

---

## 🚀 開始測試吧！

**快速命令**:

```bash
# 1. 進入專案目錄
cd /Users/danielkai/Desktop/safe-net-app/android-receiver

# 2. 檢查設備連接
adb devices

# 3. 構建並安裝
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 啟動 App
adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity

# 5. 查看 Log
adb logcat | grep -i safenet
```

**或使用 Android Studio 一鍵運行**:
- 打開專案
- 選擇實體機
- 點擊 Run ▶️

---

🎉 **祝測試順利！**

如有問題，請參考本文檔的「常見問題排除」章節。
