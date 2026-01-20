# 📱 UI 優化測試指南

## 🚀 安裝 APK
```bash
adb install /Users/danielkai/Desktop/android-receiver/app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ 測試清單

### 1. App 啟動測試

**步驟**:
1. 打開 App
2. 等待 1-2 秒

**預期結果**:
```
執行頁面顯示：
━━━━━━━━━━━━━━━━━━━
Gateway ID: ANDROID-xxx
服務 UUID:
• FDA50693...
... 共 1 個 UUID
━━━━━━━━━━━━━━━━━━━
```

**檢查 Logcat**:
```bash
adb logcat | grep "MainActivity\|ServiceUuidRepository"
```

應該看到：
```
D/MainActivity: 📱 App 啟動，開始同步服務 UUID...
D/ServiceUuidRepository: ✅ 同步成功，獲取 1 個 UUID: [FDA50693-...]
```

---

### 2. Service UUID 顯示測試

**檢查項目**:
- ✅ Gateway ID 正確顯示
- ✅ Service UUID 顯示（如果已同步）
- ✅ 如果未同步顯示「服務 UUID: 未同步」
- ✅ UUID 顯示前 8 位加 "..."
- ✅ 如果多個 UUID，顯示前 2 個 + "... 共 X 個 UUID"

---

### 3. 開始掃描測試

#### 場景 A: 未同步 UUID（理論上不會發生，因為啟動時會自動同步）

**步驟**:
1. 如果 UUID 顯示為「未同步」
2. 點擊「開始掃描」

**預期結果**:
- ❌ 不啟動服務
- 📱 Toast: "請先同步服務 UUID"
- 🔄 建議點「同步服務 UUID」按鈕

#### 場景 B: 已同步 UUID（正常情況）

**步驟**:
1. 確認 Service UUID 已顯示
2. 點擊「▶️ 開始掃描」

**預期結果**:
- ✅ 直接啟動掃描服務（無延遲）
- ✅ 按鈕變為「⏹ 停止掃描」
- ✅ Toast: "掃描服務已啟動"
- ✅ 開始掃描 Beacon

**Logcat 檢查**:
```bash
adb logcat | grep "HomeFragment\|BeaconScanService"
```

應該看到：
```
D/HomeFragment: 服務已成功啟動
D/BeaconScanService: 服務創建
D/BeaconScanService: 服務啟動
D/BeaconScanService: Beacon 服務連接
```

---

### 4. 上傳記錄分頁測試

**步驟**:
1. 切換到底部導覽的第二個分頁（原「白名單」）
2. 開始掃描一段時間

**預期結果**:
```
━━━━━━━━━━━━━━━━━━━
已上傳記錄: 1,234
最新: 14:30:25
━━━━━━━━━━━━━━━━━━━

[記錄列表]
UUID: FDA50693...     ✅ 已上傳
Major: 1, Minor: 1001
RSSI: -65     距離: 2.50m     14:30:25

UUID: E2C56DB5...     ⏭️ 僅記錄
Major: 2, Minor: 2002
RSSI: -72     距離: 5.20m     14:29:58
```

**檢查項目**:
- ✅ 標題顯示「已上傳記錄: X」
- ✅ 顯示最新上傳時間「最新: XX:XX:XX」
- ✅ **沒有**同步按鈕
- ✅ 記錄顯示完整信息（UUID、Major/Minor、RSSI、距離、時間）
- ✅ 狀態標記：「✅ 已上傳」或「⏭️ 僅記錄」
- ✅ 如果沒有記錄，顯示「暫無上傳記錄」

---

### 5. 手動同步 UUID 測試

**步驟**:
1. 在執行頁面
2. 點擊「🔄 同步服務 UUID」按鈕
3. 等待 1 秒

**預期結果**:
- ✅ 按鈕變為禁用狀態
- ✅ Service UUID 區域更新
- ✅ 「同步: XX:XX:XX (X 個 UUID)」更新

---

### 6. 切換分頁後狀態保持測試

**步驟**:
1. 開始掃描（按鈕顯示「停止掃描」）
2. 切換到其他分頁
3. 切換回執行頁面

**預期結果**:
- ✅ 按鈕仍然顯示「⏹ 停止掃描」
- ✅ 統計數字持續更新
- ✅ Service UUID 顯示不變

---

## 🔍 完整 Logcat 監控

```bash
# 終端 1: 查看主要流程
adb logcat | grep "MainActivity\|HomeFragment\|BeaconScanService\|ServiceUuidRepository"

# 終端 2: 查看掃描詳情
adb logcat | grep "BeaconScanService"
```

---

## 📊 預期的完整流程

```
【打開 App】
  ↓
D/MainActivity: 📱 App 啟動，開始同步服務 UUID...
  ↓
D/ServiceUuidRepository: 開始同步 Service UUID...
  ↓
D/ServiceUuidRepository: ✅ 同步成功，獲取 1 個 UUID: [FDA50693-...]
  ↓
【執行頁面顯示 UUID】
服務 UUID:
• FDA50693...
... 共 1 個 UUID
  ↓
【用戶點「開始掃描」】
  ↓
D/HomeFragment: 服務已成功啟動
  ↓
D/BeaconScanService: 服務創建
D/BeaconScanService: 服務啟動
D/BeaconScanService: Beacon 服務連接
  ↓
【掃描 Beacon】
  ↓
D/BeaconScanService: 偵測到 3 個 Beacon
D/BeaconScanService: ✅ 目標 UUID Beacon: FDA50693..., Major=1, Minor=1001, RSSI=-65, 距離=2.50m
D/BeaconScanService: ⏭️ 非目標 UUID，僅記錄: E2C56DB5...
  ↓
【切換到上傳記錄分頁】
  ↓
看到記錄列表實時更新
```

---

## ⚠️ 可能的問題

### 問題 1: Service UUID 顯示「未同步」
**原因**: 網絡問題或 API 錯誤  
**解決**: 
1. 檢查網絡連接
2. 檢查 Logcat 錯誤
3. 手動點「同步服務 UUID」

### 問題 2: 開始掃描提示「請先同步服務 UUID」
**原因**: UUID 同步失敗  
**解決**: 
1. 點「同步服務 UUID」按鈕
2. 等待同步完成
3. 再點「開始掃描」

### 問題 3: 上傳記錄分頁為空
**原因**: 還沒掃描到目標 UUID 的 Beacon  
**解決**: 
1. 確保附近有目標 UUID 的 Beacon
2. 檢查 Logcat 是否有「✅ 目標 UUID Beacon」
3. 等待掃描一段時間

---

## 🎉 測試通過標準

✅ App 啟動自動同步 UUID（Logcat 確認）  
✅ 執行頁面正確顯示 Service UUID  
✅ 開始掃描無延遲（不再同步 UUID）  
✅ 上傳記錄分頁顯示記錄（不是白名單）  
✅ 上傳記錄分頁沒有同步按鈕  
✅ 切換分頁後狀態保持  
✅ 停止掃描功能正常

---

## 📞 測試報告

測試完成後，請確認：

1. ✅ UUID 顯示是否正確
2. ✅ 開始掃描是否快速（無延遲）
3. ✅ 上傳記錄是否實時顯示
4. ✅ UI 是否符合預期

如有問題，請提供：
- 截圖
- Logcat 輸出
- 具體錯誤描述
