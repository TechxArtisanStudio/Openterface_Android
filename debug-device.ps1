# Quick Debug Commands for Openterface Android
# Device: 192.168.100.40:36111

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  OPENTERFACE ANDROID - DEBUG HELPER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$DEVICE = "192.168.100.40:36111"

function Show-Menu {
    Write-Host "Available Commands:" -ForegroundColor Yellow
    Write-Host "  1. Monitor ALL logs" -ForegroundColor White
    Write-Host "  2. Monitor Openterface logs only" -ForegroundColor White
    Write-Host "  3. Monitor USB/Serial logs" -ForegroundColor White
    Write-Host "  4. Monitor Baudrate feature" -ForegroundColor White
    Write-Host "  5. Clear logs and start fresh" -ForegroundColor White
    Write-Host "  6. Restart the app" -ForegroundColor White
    Write-Host "  7. Reinstall and restart" -ForegroundColor White
    Write-Host "  8. Show device info" -ForegroundColor White
    Write-Host "  9. Check USB devices" -ForegroundColor White
    Write-Host "  0. Exit" -ForegroundColor White
    Write-Host ""
}

function Monitor-AllLogs {
    Write-Host "📊 Monitoring ALL logs (Ctrl+C to stop)..." -ForegroundColor Green
    adb -s $DEVICE logcat
}

function Monitor-OpenterfaceLogs {
    Write-Host "📊 Monitoring Openterface logs only (Ctrl+C to stop)..." -ForegroundColor Green
    adb -s $DEVICE logcat | Select-String "Openterface|com.openterface"
}

function Monitor-UsbLogs {
    Write-Host "📊 Monitoring USB/Serial logs (Ctrl+C to stop)..." -ForegroundColor Green
    adb -s $DEVICE logcat | Select-String "USB|Serial|UVC|CH340|UsbDevice"
}

function Monitor-BaudrateLogs {
    Write-Host "📊 Monitoring Baudrate feature (Ctrl+C to stop)..." -ForegroundColor Green
    adb -s $DEVICE logcat | Select-String "Baudrate|BaudrateDialog|DrawerLayoutDeal"
}

function Clear-LogsAndMonitor {
    Write-Host "🧹 Clearing logs..." -ForegroundColor Yellow
    adb -s $DEVICE logcat -c
    Write-Host "✅ Logs cleared!" -ForegroundColor Green
    Write-Host "📊 Starting fresh monitoring (Ctrl+C to stop)..." -ForegroundColor Green
    adb -s $DEVICE logcat | Select-String "Openterface"
}

function Restart-App {
    Write-Host "🔄 Stopping app..." -ForegroundColor Yellow
    adb -s $DEVICE shell am force-stop com.openterface.AOS
    Start-Sleep -Seconds 1
    Write-Host "▶️ Starting app..." -ForegroundColor Green
    adb -s $DEVICE shell am start -n com.openterface.AOS/.activity.MainActivity
    Write-Host "✅ App restarted!" -ForegroundColor Green
}

function Reinstall-App {
    Write-Host "📦 Rebuilding APK..." -ForegroundColor Yellow
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    .\gradlew assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Build successful!" -ForegroundColor Green
        Write-Host "📲 Installing on device..." -ForegroundColor Yellow
        adb -s $DEVICE install -r app\build\outputs\apk\debug\OpenterfaceAndroid-debug.apk
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Installation successful!" -ForegroundColor Green
            Write-Host "▶️ Starting app..." -ForegroundColor Green
            adb -s $DEVICE shell am start -n com.openterface.AOS/.activity.MainActivity
        } else {
            Write-Host "❌ Installation failed!" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Build failed!" -ForegroundColor Red
    }
}

function Show-DeviceInfo {
    Write-Host "📱 Device Information:" -ForegroundColor Cyan
    Write-Host "  Device: $DEVICE" -ForegroundColor White
    
    $model = adb -s $DEVICE shell getprop ro.product.model
    $android = adb -s $DEVICE shell getprop ro.build.version.release
    $sdk = adb -s $DEVICE shell getprop ro.build.version.sdk
    
    Write-Host "  Model: $model" -ForegroundColor White
    Write-Host "  Android: $android (API $sdk)" -ForegroundColor White
}

function Check-UsbDevices {
    Write-Host "🔌 USB Devices on Android:" -ForegroundColor Cyan
    adb -s $DEVICE shell "ls -l /dev/bus/usb/*"
    Write-Host ""
    Write-Host "📋 USB Device List:" -ForegroundColor Cyan
    adb -s $DEVICE shell "dumpsys usb"
}

# Main loop
while ($true) {
    Show-Menu
    $choice = Read-Host "Select option"
    
    switch ($choice) {
        "1" { Monitor-AllLogs }
        "2" { Monitor-OpenterfaceLogs }
        "3" { Monitor-UsbLogs }
        "4" { Monitor-BaudrateLogs }
        "5" { Clear-LogsAndMonitor }
        "6" { Restart-App }
        "7" { Reinstall-App }
        "8" { Show-DeviceInfo }
        "9" { Check-UsbDevices }
        "0" { 
            Write-Host "👋 Goodbye!" -ForegroundColor Green
            exit 
        }
        default { Write-Host "❌ Invalid option!" -ForegroundColor Red }
    }
    
    Write-Host ""
    Write-Host "Press any key to continue..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    Clear-Host
}
