# 🔧 修改 Cloud Function 建議

## 問題分析

### 目前的設計問題

**當前邏輯**：
```
手機獲取白名單
  ↓
Gateway 必須已註冊
  ↓
未註冊 → 404 錯誤 + 空白名單
```

**問題**：
- ❌ 每個接收器手機都要預先註冊
- ❌ 部署麻煩
- ❌ 無法即裝即用

---

## 💡 建議的修改

### 方案：返回全局白名單

**修改 `getDeviceWhitelist` Cloud Function**：

```javascript
// functions/src/index.ts 或相應文件

export const getDeviceWhitelist = onRequest(
  { cors: true, timeoutSeconds: 60 },
  async (req, res) => {
    const gateway_id = req.query.gateway_id || req.body.gateway_id;
    
    if (!gateway_id) {
      return res.status(400).json({ 
        success: false, 
        error: "gateway_id is required" 
      });
    }

    try {
      // 1. 查詢 Gateway 是否已註冊
      const gatewaySnapshot = await admin.firestore()
        .collection('gateways')
        .where('imei', '==', gateway_id)
        .where('isActive', '==', true)
        .limit(1)
        .get();

      let tenantId = null;
      let gatewayInfo = null;

      if (!gatewaySnapshot.empty) {
        // Gateway 已註冊
        const gatewayDoc = gatewaySnapshot.docs[0];
        gatewayInfo = {
          id: gatewayDoc.id,
          name: gatewayDoc.data().name,
          tenantId: gatewayDoc.data().tenantId,
          type: gatewayDoc.data().type
        };
        tenantId = gatewayDoc.data().tenantId;
        console.log(`Gateway ${gateway_id} 已註冊，tenantId: ${tenantId}`);
      } else {
        console.log(`Gateway ${gateway_id} 未註冊，使用全局白名單`);
      }

      // 2. 查詢設備白名單
      let devicesQuery = admin.firestore().collection('devices');
      
      if (tenantId) {
        // 已註冊 → 返回該組織的設備
        devicesQuery = devicesQuery.where('tenantId', '==', tenantId);
      }
      // 未註冊 → 返回所有設備（全局白名單）
      
      const devicesSnapshot = await devicesQuery
        .where('isActive', '==', true)
        .get();

      const devices = devicesSnapshot.docs.map(doc => {
        const data = doc.data();
        return {
          uuid: data.uuid,
          major: data.major || 0,
          minor: data.minor || 0,
          deviceName: data.deviceName || doc.id,
          macAddress: data.macAddress || ''
        };
      });

      // 3. 總是返回 200 OK（即使 Gateway 未註冊）
      return res.status(200).json({
        success: true,
        gateway: gatewayInfo,
        devices: devices,
        count: devices.length,
        timestamp: Date.now(),
        message: gatewayInfo 
          ? `Using tenant whitelist (${tenantId})` 
          : "Using global whitelist (gateway not registered)"
      });

    } catch (error) {
      console.error('Error fetching whitelist:', error);
      return res.status(500).json({
        success: false,
        devices: [],
        count: 0,
        error: error.message,
        timestamp: Date.now()
      });
    }
  }
);
```

---

## 🎯 修改後的行為

### Gateway 未註冊（您的情況）

**請求**:
```
GET /getDeviceWhitelist?gateway_id=ANDROID-42ec6a54d319eb84
```

**回應**:
```json
HTTP 200 ✅
{
  "success": true,
  "gateway": null,
  "devices": [
    {
      "uuid": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
      "major": 100,
      "minor": 1,
      "deviceName": "設備-001",
      "macAddress": "AA:BB:CC:DD:EE:FF"
    },
    // ... 所有活躍設備
  ],
  "count": 10,
  "message": "Using global whitelist (gateway not registered)"
}
```

### Gateway 已註冊

**回應**:
```json
HTTP 200 ✅
{
  "success": true,
  "gateway": {
    "id": "xxx",
    "name": "社區門口",
    "tenantId": "MWsT3I62yzygKPYl520f",
    "type": "BOUNDARY"
  },
  "devices": [
    // ... 該組織的設備
  ],
  "count": 5,
  "message": "Using tenant whitelist (MWsT3I62yzygKPYl520f)"
}
```

---

## 🔄 部署修改

```bash
# 修改 Cloud Function 代碼後
firebase deploy --only functions:getDeviceWhitelist

# 或重新部署所有 functions
firebase deploy --only functions
```

---

## ✅ 修改的好處

1. **即裝即用** - 任何手機都可以立即作為接收器
2. **自動白名單** - 未註冊時使用全局白名單
3. **靈活管理** - 註冊後可限制為組織專屬
4. **向後兼容** - Android App 不需要改動
5. **減少維護** - 不需要為每個手機預先配置

---

## 🚀 現在測試掃描清單功能

**在 Android Studio 中**：
1. 點擊 Run (Ctrl+R)
2. 安裝到 Pixel 6a

**在手機上**：
1. 點「開始掃描」
2. 等待幾秒
3. 點「📋 查看掃描清單」
4. 看到所有掃描到的 Beacon！

**不管白名單是否為空，您都能看到所有掃描數據了！** 🎉