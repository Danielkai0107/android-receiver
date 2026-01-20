#!/bin/bash

echo "════════════════════════════════════════════════"
echo "  SafeNet Android 接收器"
echo "  使用 Android Studio 構建和測試"
echo "════════════════════════════════════════════════"
echo ""
echo "✅ 您的 Pixel 6a 已連接"
echo ""
echo "📱 請按照以下步驟操作："
echo ""
echo "步驟 1: Android Studio 會自動打開"
echo "步驟 2: 等待 Gradle 同步完成（5-10 分鐘）"
echo "步驟 3: 頂部選擇 'Pixel 6a'"
echo "步驟 4: 點擊綠色 ▶️ Run 按鈕"
echo "步驟 5: 完成！"
echo ""
echo "⏳ 正在啟動 Android Studio..."
echo ""

# 打開 Android Studio
if open -a "Android Studio" /Users/danielkai/Desktop/safe-net-app/android-receiver 2>/dev/null; then
    echo "✅ Android Studio 已啟動"
    echo ""
    echo "📋 在 Android Studio 中："
    echo "   1. 等待底部顯示 'Gradle build finished'"
    echo "   2. 確認頂部設備選擇器顯示 'Pixel 6a'"
    echo "   3. 點擊綠色 ▶️ Run 按鈕"
    echo ""
    echo "💡 提示：首次同步會下載依賴，需要幾分鐘"
    echo ""
else
    echo "❌ 找不到 Android Studio"
    echo ""
    echo "請手動操作："
    echo "1. 打開 Android Studio"
    echo "2. File → Open"
    echo "3. 選擇: /Users/danielkai/Desktop/safe-net-app/android-receiver"
    echo "4. 等待同步完成後點擊 Run"
    echo ""
fi

echo "════════════════════════════════════════════════"
