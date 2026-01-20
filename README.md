# SafeNet Android 接收器 App

一個高效能的 Android Beacon 接收器應用，用於掃描和上傳 Beacon 信號到 Firebase Cloud Function。

## 功能特色

- ✅ **Beacon 掃描**: 使用 AltBeacon Library 持續掃描 BLE Beacon
- ✅ **白名單過濾**: 只上傳在白名單中的設備
- ✅ **批次上傳**: 累積 60 秒批次上傳，節省流量
- ✅ **離線快取**: 無網路時保存到本地，網路恢復後自動上傳
- ✅ **GPS 定位**: 自動附加位置信息
- ✅ **省電設計**: 優化的掃描頻率和位置更新策略
- ✅ **前景服務**: 確保持續運行
- ✅ **Cloud Function 整合**: 與現有後端無縫整合

## 技術棧

- **語言**: Kotlin
- **最低 SDK**: Android 8.0 (API 26)
- **目標 SDK**: Android 14 (API 34)
- **架構**: MVVM + Repository Pattern
- **依賴注入**: Hilt
- **Beacon 掃描**: AltBeacon Library 2.20.4
- **網路**: Retrofit 2.9.0 + OkHttp 4.12.0
- **數據庫**: Room 2.6.1
- **協程**: Kotlin Coroutines 1.7.3
- **WorkManager**: 2.9.0

## API 端點

### 白名單 API
```
GET https://us-central1-safe-net-tw.cloudfunctions.net/getDeviceWhitelist?gateway_id={IMEI}
```

### Beacon 上傳 API
```
POST https://receivebeacondata-kmzfyt3t5a-uc.a.run.app
```

## 專案結構

```
com.safenet.receiver/
├── data/
│   ├── local/          # Room Database
│   ├── remote/         # Retrofit API
│   ├── repository/     # 數據倉庫
│   └── worker/         # WorkManager
├── domain/
│   └── model/          # 業務模型
├── presentation/
│   ├── main/           # 主畫面
│   ├── settings/       # 設定
│   └── whitelist/      # 白名單列表
├── service/
│   ├── BeaconScanService.kt  # Beacon 掃描服務
│   └── LocationService.kt    # GPS 定位服務
└── utils/              # 工具類
```

## 構建專案

### 前置要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2+

### 構建步驟

1. 克隆專案
```bash
cd /Users/danielkai/Desktop/safe-net-app/android-receiver
```

2. 打開 Android Studio，選擇 "Open" 打開專案

3. 等待 Gradle 同步完成

4. 連接 Android 設備或啟動模擬器

5. 點擊 Run (綠色三角形) 或按 Shift+F10

### 使用 Gradle 命令行構建

```bash
# 清理
./gradlew clean

# 構建 Debug APK
./gradlew assembleDebug

# 構建 Release APK
./gradlew assembleRelease

# 運行單元測試
./gradlew test

# 運行 Android 測試
./gradlew connectedAndroidTest
```

## 權限說明

應用需要以下權限：

- **藍牙**: 掃描 BLE Beacon
- **位置**: Android 要求藍牙掃描必須有位置權限
- **讀取手機狀態**: 獲取 IMEI 作為 Gateway ID
- **網路**: 上傳數據到 Cloud Function
- **前景服務**: 保持服務持續運行

## 使用說明

### 首次啟動

1. 授予所有必要權限
2. App 會自動獲取 IMEI 作為 Gateway ID
3. 點擊「同步白名單」獲取可掃描的設備列表
   - **注意**: 即使白名單為空或 Gateway 未註冊，App 仍可正常運行
   - 掃描到的所有 Beacon 都會上傳到 Cloud Function

### 開始掃描

1. 確保已同步白名單
2. 點擊「開始掃描」按鈕
3. 前景服務會持續運行並掃描 Beacon
4. 掃描到的白名單設備會自動加入上傳佇列
5. 每 60 秒批次上傳一次

### 查看狀態

主畫面顯示：
- Gateway ID
- 白名單設備數量
- 已掃描數量
- 已上傳數量
- 待上傳數量

### 設定調整

進入設定可查看當前配置：
- 掃描頻率: 5 秒
- 上傳間隔: 60 秒
- 白名單同步間隔: 10 分鐘
- GPS 更新頻率: 2 分鐘
- 離線快取上限: 1000 筆

## 省電優化

- 藍牙掃描: 前景 5 秒/次，背景 10 秒/次
- GPS 更新: 位置變化 < 50m 時重用舊位置
- 批次上傳: 累積後一次上傳，減少網路請求
- 白名單快取: 本地緩存，減少 API 調用

## 故障排除

### 無法掃描到 Beacon

1. 檢查藍牙是否開啟
2. 確認已授予位置權限
3. 確認 Beacon 設備正常運作
4. 檢查 Beacon 是否在白名單中

### 無法上傳數據

1. 檢查網路連線
2. 確認 Gateway ID 已設定
3. 查看白名單是否為空
4. 檢查 Cloud Function URL 是否正確

### 電池消耗快

1. 降低掃描頻率
2. 增加 GPS 更新間隔
3. 增加上傳間隔
4. 確保 Doze 模式未限制 App

## 開發計劃

- [ ] 添加更詳細的統計圖表
- [ ] 支援手動設定 Gateway ID
- [ ] 添加日誌導出功能
- [ ] 優化內存使用
- [ ] 添加更多測試

## 授權

Copyright © 2026 SafeNet Project
# android-receiver
