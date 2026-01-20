#!/bin/bash

# 雙重上傳端點測試腳本
# 用於測試主要端點和備用端點的自動切換功能

echo "=========================================="
echo "  雙重上傳端點功能測試"
echo "=========================================="
echo ""

# 檢查 ADB
if ! command -v adb &> /dev/null; then
    echo "❌ 錯誤: 找不到 adb 命令"
    echo "請確保已安裝 Android SDK 並將 adb 添加到 PATH"
    exit 1
fi

# 檢查設備連接
echo "1️⃣  檢查設備連接..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
if [ "$DEVICES" -eq 0 ]; then
    echo "❌ 錯誤: 沒有連接的設備"
    echo ""
    echo "請確保:"
    echo "  - 設備已通過 USB 連接"
    echo "  - 設備已啟用 USB 調試"
    echo "  - 已授權此電腦進行 USB 調試"
    exit 1
fi
echo "✅ 找到 $DEVICES 個設備"
echo ""

# APK 路徑
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# 檢查 APK
echo "2️⃣  檢查 APK..."
if [ ! -f "$APK_PATH" ]; then
    echo "❌ 錯誤: 找不到 APK 檔案"
    echo "路徑: $APK_PATH"
    echo ""
    echo "請先構建應用:"
    echo "  ./gradlew assembleDebug"
    exit 1
fi
echo "✅ APK 檔案存在"
echo ""

# 顯示端點資訊
echo "3️⃣  端點配置資訊"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🥇 主要端點 (Cloud Functions)"
echo "   URL: https://us-central1-safe-net-tw.cloudfunctions.net/receiveBeaconData"
echo ""
echo "🥈 備用端點 (Cloud Run)"
echo "   URL: https://receivebeacondata-kmzfyt3t5a-uc.a.run.app/"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 安裝 APK
echo "4️⃣  安裝應用..."
adb install -r "$APK_PATH"
if [ $? -ne 0 ]; then
    echo ""
    echo "❌ 安裝失敗"
    exit 1
fi
echo "✅ 安裝成功"
echo ""

# 啟動應用
echo "5️⃣  啟動應用..."
adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity
sleep 2
echo "✅ 應用已啟動"
echo ""

# 開始監控日誌
echo "6️⃣  開始監控日誌..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "監控上傳相關日誌 (Ctrl+C 停止)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 測試重點："
echo "  1. 觀察是否優先使用主要端點 (Cloud Functions)"
echo "  2. 如果主要端點失敗，是否自動切換到備用端點"
echo "  3. 日誌是否清楚顯示使用了哪個端點"
echo ""
echo "💡 提示："
echo "  - 在應用中授予所有必要權限"
echo "  - 點擊「開始掃描」"
echo "  - 等待約 60 秒(自動上傳週期)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 監控特定標籤
adb logcat -c  # 清除舊日誌
adb logcat | grep --line-buffered -E "UploadRepository|BeaconScanService.*(上傳|Gateway|端點)"

echo ""
echo "測試完成"
