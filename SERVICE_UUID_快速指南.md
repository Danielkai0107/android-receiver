# 🚀 Service UUID 功能 - 快速指南

## 📱 APK 位置
```
/Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 主要變更

### 1️⃣ 白名單 → Service UUID
**原來**: 同步完整的設備白名單（可能數百個設備）
**現在**: 只同步服務 UUID（通常只有 1-3 個）

### 2️⃣ 顯示變更
**原來**: 白名單設備: 10
**現在**: 📤 已上傳記錄: 1,234（顯示歷史累計）

### 3️⃣ 過濾邏輯
只掃描和上傳匹配 Service UUID 的 Beacon

---

## 🔄 使用流程

### 步驟 1: 安裝 APK
```bash
adb install app-debug.apk
```

### 步驟 2: 打開 App
- 自動生成 Gateway ID
- 顯示 `Gateway ID: ANDROID-xxxxxxxxx`

### 步驟 3: 開始掃描
1. 點擊 **「▶️ 開始掃描」**
2. App 會自動：
   - 同步服務 UUID（Toast: "正在同步服務 UUID..."）
   - 等待 1.5 秒
   - 啟動掃描服務
3. 按鈕變為 **「⏹ 停止掃描」**

### 步驟 4: 查看統計
```
📊 統計資訊
━━━━━━━━━━━━━━━
📤 已上傳記錄: 1,234  ← 總共上傳過的記錄
📡 已掃描: 45          ← 本次掃描的數量
✅ 已上傳: 12          ← 本次已上傳的數量
⏳ 待上傳: 3           ← 等待上傳的數量
📏 最遠距離: 15.3 m    ← 最遠掃描距離

同步: 14:30:25 (1 個 UUID)  ← 最後同步時間和 UUID 數量
```

---

## 🔍 Logcat 日誌

### 查看服務 UUID 同步
```
adb logcat | grep ServiceUuidRepository
```

**成功輸出**:
```
D/ServiceUuidRepository: 開始同步 Service UUID...
D/ServiceUuidRepository: ✅ 同步成功，獲取 1 個 UUID: [FDA50693-A4E2-4FB1-AFCF-C6EB01234567]
```

### 查看 Beacon 過濾
```
adb logcat | grep BeaconScanService
```

**匹配的 UUID**:
```
D/BeaconScanService: ✅ 目標 UUID Beacon: FDA50693-A4E2-4FB1-AFCF-C6EB01234567, Major=1, Minor=1001, RSSI=-65, 距離=2.50m
```

**不匹配的 UUID**:
```
D/BeaconScanService: ⏭️ 非目標 UUID，僅記錄: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0
```

---

## 🧪 測試檢查清單

### ✅ 基本功能
- [ ] App 啟動成功
- [ ] Gateway ID 自動生成
- [ ] 點擊「同步服務 UUID」按鈕有效
- [ ] 開始掃描前自動同步 UUID
- [ ] 按鈕狀態正確切換

### ✅ UUID 過濾
- [ ] Logcat 顯示「✅ 目標 UUID Beacon」
- [ ] Logcat 顯示「⏭️ 非目標 UUID」
- [ ] 只有目標 UUID 被上傳

### ✅ 統計顯示
- [ ] 「📤 已上傳記錄」數字增長
- [ ] 重啟 App 後數字保留
- [ ] 「同步: XX:XX:XX (X 個 UUID)」顯示正確

### ✅ 狀態同步
- [ ] 切換 App 後按鈕狀態正確
- [ ] 停止掃描功能正常
- [ ] 服務實際停止

---

## 🔧 手動同步 UUID

如果需要手動重新同步：

1. 點擊 **「🔄 同步服務 UUID」** 按鈕
2. 等待 Toast 提示
3. 查看「同步: XX:XX:XX (X 個 UUID)」更新

---

## 📊 API 測試

### 測試 Service UUID API
```bash
curl https://us-central1-safe-net-tw.cloudfunctions.net/getServiceUuids
```

**預期回應**:
```json
{
  "success": true,
  "uuids": [
    {
      "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB01234567",
      "name": "大愛社區",
      "description": ""
    }
  ],
  "count": 1,
  "timestamp": 1737890252165
}
```

---

## ⚠️ 疑難排解

### 問題 1: 沒有掃描到 Beacon
**檢查**:
1. 藍牙權限已授予
2. 位置權限已授予
3. Service UUID 已同步
4. Beacon UUID 是否匹配

### 問題 2: 已上傳記錄不增長
**檢查**:
1. 查看 Logcat 確認有「✅ 目標 UUID Beacon」
2. 檢查網絡連接
3. 確認 GPS 位置可用

### 問題 3: 按鈕狀態錯誤
**解決**:
1. 切換到其他分頁再回來
2. 或重啟 App
3. 新版本已修復狀態同步問題

---

## 📞 支持

如有問題，請查看：
- `SERVICE_UUID_IMPLEMENTATION.md` - 完整實施文檔
- Logcat 日誌
- API 回應

---

## 🎉 完成！

安裝 APK 並開始測試新功能！
