#!/bin/bash
# Beacon 掃描測試腳本

echo "================================"
echo "   Beacon 掃描診斷工具"
echo "================================"
echo ""

# 1. 檢查設備連接
echo "1️⃣  檢查設備連接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 沒有連接的設備"
    exit 1
fi
echo "✅ 設備已連接"
echo ""

# 2. 檢查位置模式
echo "2️⃣  檢查位置設定..."
LOCATION_MODE=$(adb shell settings get secure location_mode)
echo "   位置模式: $LOCATION_MODE (3=高精確度)"
if [ "$LOCATION_MODE" != "3" ]; then
    echo "⚠️  警告：位置模式不是高精確度"
fi
echo ""

# 3. 檢查應用權限
echo "3️⃣  檢查應用權限..."
adb shell dumpsys package com.safenet.receiver | grep -A 1 "ACCESS_FINE_LOCATION\|BLUETOOTH_SCAN" | grep "granted=true" > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ 關鍵權限已授予"
else
    echo "❌ 權限未完整授予"
fi
echo ""

# 4. 檢查服務狀態
echo "4️⃣  檢查掃描服務..."
if adb shell dumpsys activity services | grep -q "BeaconScanService"; then
    echo "✅ 掃描服務正在運行"
else
    echo "⚠️  掃描服務未運行"
fi
echo ""

# 5. 清空日誌並開始監控
echo "5️⃣  清空日誌，準備監控..."
adb logcat -c
echo "✅ 日誌已清空"
echo ""

echo "================================"
echo "請在手機上操作："
echo "1. 確認通知欄位置圖標已開啟"
echo "2. 確認 iBeacon 模擬器在廣播"
echo "3. 點擊「開始掃描」按鈕"
echo "4. 等待 15 秒..."
echo "================================"
echo ""

# 等待 15 秒
for i in {15..1}; do
    echo -ne "\r⏰ 倒數：$i 秒...  "
    sleep 1
done
echo -e "\n"

# 6. 檢查日誌
echo "6️⃣  檢查掃描日誌..."
echo ""

# 檢查位置拒絕錯誤
if adb logcat -d | grep "location deny.*safenet" | tail -1 | grep -q "safenet"; then
    echo "❌ 位置拒絕錯誤仍然存在！"
    echo ""
    echo "🔧 請檢查："
    echo "   1. 設定 → 位置 → 確認開關開啟"
    echo "   2. 應用權限 → 位置 → 選擇「始終允許」"
    echo "   3. 關閉省電模式"
    echo ""
else
    echo "✅ 沒有位置拒絕錯誤"
fi

# 檢查是否偵測到 Beacon
BEACON_LOGS=$(adb logcat -d | grep "BeaconScanService.*偵測到")
if [ ! -z "$BEACON_LOGS" ]; then
    echo "✅ 已偵測到 Beacon："
    echo "$BEACON_LOGS" | tail -5
else
    echo "⚠️  尚未偵測到任何 Beacon"
    echo ""
    echo "🔍 可能原因："
    echo "   1. iBeacon 模擬器沒有在廣播"
    echo "   2. UUID 不匹配後端設定"
    echo "   3. 兩支手機距離太遠（建議 < 1 公尺）"
    echo "   4. 模擬器格式不是 iBeacon"
fi

echo ""
echo "================================"
echo "📊 服務狀態："
adb logcat -d | grep "BeaconScanService" | tail -8
echo "================================"
