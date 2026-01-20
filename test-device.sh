#!/bin/bash
# SafeNet Android 接收器 - 實體機測試腳本

echo "🚀 SafeNet Android 接收器 - 實體機測試"
echo "========================================"
echo ""

# 檢查設備連接
echo "📱 檢查 Android 設備連接..."
DEVICES=$(adb devices | grep -w "device" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ 錯誤: 沒有檢測到 Android 設備"
    echo ""
    echo "請確認："
    echo "  1. 手機已開啟 USB 調試"
    echo "  2. USB 線已連接"
    echo "  3. 手機已授權此電腦"
    echo ""
    echo "執行 'adb devices' 查看設備狀態"
    exit 1
fi

echo "✅ 已檢測到 $DEVICES 個設備"
echo ""

# 顯示設備信息
echo "📋 設備信息："
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
echo ""

# 選擇操作
echo "請選擇操作："
echo "  1) 構建並安裝 (推薦首次使用)"
echo "  2) 僅安裝 (APK 已存在)"
echo "  3) 僅啟動 App"
echo "  4) 查看實時 Log"
echo "  5) 卸載 App"
echo ""
read -p "請輸入選項 [1-5]: " choice

case $choice in
    1)
        echo ""
        echo "🏗️  開始構建..."
        chmod +x gradlew
        ./gradlew clean assembleDebug
        
        if [ $? -eq 0 ]; then
            echo "✅ 構建成功"
            echo ""
            echo "📦 開始安裝..."
            adb install -r app/build/outputs/apk/debug/app-debug.apk
            
            if [ $? -eq 0 ]; then
                echo "✅ 安裝成功"
                echo ""
                echo "🚀 啟動 App..."
                adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity
                echo ""
                echo "✅ App 已啟動！"
                echo ""
                echo "💡 提示: 執行 './test-device.sh' 選擇 4 查看實時 Log"
            else
                echo "❌ 安裝失敗"
                exit 1
            fi
        else
            echo "❌ 構建失敗"
            exit 1
        fi
        ;;
        
    2)
        echo ""
        echo "📦 開始安裝..."
        
        if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
            echo "❌ APK 文件不存在，請先構建"
            echo "   執行選項 1 或運行: ./gradlew assembleDebug"
            exit 1
        fi
        
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        
        if [ $? -eq 0 ]; then
            echo "✅ 安裝成功"
            echo ""
            echo "🚀 啟動 App..."
            adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity
            echo "✅ App 已啟動！"
        else
            echo "❌ 安裝失敗"
            exit 1
        fi
        ;;
        
    3)
        echo ""
        echo "🚀 啟動 App..."
        adb shell am start -n com.safenet.receiver/.presentation.main.MainActivity
        echo "✅ App 已啟動！"
        ;;
        
    4)
        echo ""
        echo "📊 查看實時 Log (按 Ctrl+C 停止)..."
        echo "========================================"
        adb logcat -c  # 清除舊 log
        adb logcat | grep -i --color=always -E "safenet|WhitelistRepository|BeaconScan|Upload|Location"
        ;;
        
    5)
        echo ""
        echo "🗑️  卸載 App..."
        adb uninstall com.safenet.receiver
        
        if [ $? -eq 0 ]; then
            echo "✅ 卸載成功"
        else
            echo "❌ 卸載失敗 (可能尚未安裝)"
        fi
        ;;
        
    *)
        echo "❌ 無效選項"
        exit 1
        ;;
esac

echo ""
echo "🎉 操作完成！"
