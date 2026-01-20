# ⚠️ 重要說明

## Gateway ID 使用方式

### ✅ 簡化模式（當前實現）

**手機端不需要預先註冊 Gateway ID**

- App 自動獲取 IMEI 或 Android ID 作為 Gateway ID
- 直接使用這個 ID 進行白名單同步和 Beacon 上傳
- **無需**在 Firebase 中預先註冊 Gateway
- **無需**驗證 Gateway 是否存在

### 📡 工作流程

```
1. App 啟動
   ↓
2. 自動獲取 IMEI/Android ID
   ↓
3. 作為 gateway_id 使用
   ↓
4. 調用白名單 API（可能返回空列表）
   ↓
5. 掃描 Beacon
   ↓
6. 上傳到 Cloud Function（使用 gateway_id）
   ↓
7. Cloud Function 處理（不驗證 gateway 是否註冊）
```

### 🔄 兩種使用場景

#### 場景 1: 無白名單模式
- Gateway ID 未在後台註冊
- 白名單 API 返回空列表或錯誤
- **App 仍正常運行**
- 掃描到的**所有** Beacon 都會上傳
- Cloud Function 接收並處理所有數據

#### 場景 2: 白名單模式（可選）
- 在 Firebase `gateways` 集合中註冊 Gateway
- 在 Firebase `devices` 集合中添加需要監控的設備
- 白名單 API 返回設備列表
- App **只上傳**白名單中的 Beacon
- 節省流量和處理時間

### 💡 建議使用方式

**開發/測試階段**:
- 直接使用，不需要註冊 Gateway
- 所有 Beacon 都會被掃描和上傳
- 方便快速測試

**生產環境**:
- 建議註冊 Gateway 並配置白名單
- 只監控指定的設備
- 減少無用數據上傳

### 📝 白名單同步說明

當調用白名單 API 時：

**成功情況**:
```json
{
  "success": true,
  "devices": [...],
  "count": 5
}
```
- App 會使用白名單過濾
- 只上傳白名單中的 Beacon

**失敗情況**:
```json
{
  "success": false,
  "devices": [],
  "count": 0,
  "error": "Gateway not registered"
}
```
- App 顯示友好提示
- **繼續正常運行**
- 掃描到的 Beacon 仍會上傳
- Cloud Function 仍會接收數據

### ⚙️ Cloud Function 端說明

**白名單 API** (`getDeviceWhitelist`):
- 檢查 Gateway 是否註冊
- 未註冊時返回錯誤，但**不影響手機 App 運行**

**上傳 API** (`receivebeacondata`):
- **不檢查** Gateway 是否註冊
- 接收所有上傳的 Beacon 數據
- 直接處理並保存到數據庫

### 🎯 總結

**關鍵點**:
1. ✅ 手機端無需預先註冊 Gateway
2. ✅ 自動獲取並使用 IMEI/Android ID
3. ✅ 白名單為空時仍正常運行
4. ✅ 所有 Beacon 數據都會上傳
5. ✅ Cloud Function 處理所有數據
6. ✅ 白名單功能為可選增強功能

**這種設計的優點**:
- 📱 手機端部署簡單
- 🚀 即裝即用
- 🔧 靈活配置
- 📊 完整數據收集
- 🎛️ 可選的白名單過濾

---

*更新日期: 2026-01-20*
