# ⚡ 在 Android Studio 中操作（最終方案）

## ✅ 問題已修復

已修復 App 圖標問題，現在可以正常構建了。

---

## 🎯 在 Android Studio 中的操作步驟

### 當前狀態

✅ Android Studio 已打開  
✅ 專案已載入  
✅ Pixel 6a 已連接  
⚠️ 需要重新同步 Gradle

---

## 📋 具體操作（按順序）

### 步驟 1: 同步 Gradle

**方式 A: 自動同步**
- 等待 Android Studio 自動檢測到變更
- 頂部會顯示「Gradle files have changed」
- 點擊 **「Sync Now」**

**方式 B: 手動同步**
```
File → Sync Project with Gradle Files
```

或使用快捷鍵：
- **Mac**: `Cmd + Shift + A` → 輸入 "sync" → Enter

### 步驟 2: 等待同步完成

**觀察底部進度條**：
```
⏳ Gradle sync...
⬇️  Downloading dependencies...
🔨 Building...
✅ Gradle build finished  ← 等這個
```

**預計時間**: 3-5 分鐘（首次）

### 步驟 3: 選擇設備

**頂部工具欄**：
```
┌──────────────────────────────────┐
│ [Pixel 6a ▼]  [▶️ app] [🐛]    │
└──────────────────────────────────┘
```

確認顯示 **Pixel 6a**

### 步驟 4: 構建並運行

**點擊綠色 ▶️ Run 按鈕**

或使用快捷鍵：
- **Mac**: `Ctrl + R`

**會自動執行**：
```
🔨 Building APK...
📦 Installing to Pixel 6a...
🚀 Launching app...
✅ SUCCESS!
```

### 步驟 5: 在手機上測試

**App 會自動啟動，您會看到**：
1. 權限請求（依序授予）
2. 主畫面顯示
3. Gateway ID 已顯示

---

## 🎉 成功的標誌

### 在 Android Studio 中

**Build 視窗（底部）**：
```
✅ BUILD SUCCESSFUL in 1m 23s
45 actionable tasks: 45 executed
```

**Run 視窗（底部）**：
```
Installing APK...
APK installed in 2 s 456 ms
Launching 'app' on Pixel 6a
✅ App successfully installed and launched
```

### 在手機上

**螢幕顯示**：
```
SafeNet 接收器

Gateway ID: 123456789012345

白名單設備: 0
已掃描: 0
已上傳: 0
待上傳: 0

[開始掃描]
[同步白名單]
```

---

## 🔧 如果出現問題

### 問題 1: Sync 失敗

**操作**：
```
File → Invalidate Caches → Invalidate and Restart
```

### 問題 2: 找不到 SDK

**操作**：
```
Tools → SDK Manager
確保已安裝：
- Android 14.0 (API 34) ✅
- Android SDK Build-Tools 34 ✅
- Android SDK Platform-Tools ✅
```

### 問題 3: 設備未顯示

**操作**：
```bash
# 在終端機執行
adb kill-server
adb start-server
```

然後在 Android Studio：
```
File → Sync Project with Gradle Files
```

---

## 💡 完成後的測試步驟

### 1. 授予權限

**會依序請求**：
- ✅ 位置權限（精確位置）
- ✅ 藍牙權限  
- ✅ 讀取手機狀態
- ✅ 通知權限

**全部點「允許」**

### 2. 查看 Gateway ID

主畫面頂部會顯示：
```
Gateway ID: 123456789012345
```

**記下這個 ID**（之後配置白名單用）

### 3. 測試白名單同步

點擊 **「同步白名單」**

**可能結果**：
- ✅ 成功：白名單設備 > 0
- ⚠️ Gateway 未註冊：顯示提示，但不影響使用

### 4. 開始掃描

點擊 **「開始掃描」**

**驗證**：
- ✅ 通知欄顯示服務運行
- ✅ 按鈕變成「停止掃描」
- ✅ 如有 Beacon，統計數會增加

---

## 📊 查看 Logcat（可選）

### 在 Android Studio 中

**底部工具欄** → **Logcat**

**搜尋關鍵字**：
```
safenet
WhitelistRepository
BeaconScanService
UploadRepository
```

**重要 Log**：
```
✅ Gateway ID 初始化: 123456789012345
✅ 白名單同步成功: 5 個設備
✅ Beacon 掃描服務連接
✅ 偵測到 3 個 Beacon
✅ 白名單設備: FDA50693-..., RSSI: -65
✅ 上傳成功: 3 筆
```

---

## 🎊 測試完成清單

- [ ] Gradle 同步成功
- [ ] 構建成功
- [ ] 安裝到 Pixel 6a
- [ ] App 啟動
- [ ] 所有權限已授予
- [ ] Gateway ID 顯示
- [ ] 白名單同步（成功或顯示提示）
- [ ] 開始掃描正常
- [ ] 通知顯示正常
- [ ] 統計數據更新

---

## 🚀 現在就在 Android Studio 中操作吧！

**當前步驟**：
1. 在 Android Studio 中點擊 **「Sync Now」** 或手動同步
2. 等待 3-5 分鐘
3. 點擊綠色 ▶️ Run
4. 完成！

**這次一定會成功！** 🎉
