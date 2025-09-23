# Test Baudrate Feature Script
# Run this after connecting your Android device

Write-Host "🔍 Checking for connected devices..." -ForegroundColor Yellow
adb devices

$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ No devices connected. Please:" -ForegroundColor Red
    Write-Host "  1. Connect your Android device via USB" -ForegroundColor White
    Write-Host "  2. Enable USB Debugging in Developer Options" -ForegroundColor White
    Write-Host "  3. Allow USB debugging when prompted" -ForegroundColor White
    exit
}

Write-Host "✅ Device(s) found!" -ForegroundColor Green

Write-Host "📱 Installing Openterface app..." -ForegroundColor Yellow
adb install -r app\build\outputs\apk\debug\OpenterfaceAndroid-debug.apk

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ App installed successfully!" -ForegroundColor Green
    
    Write-Host "🚀 Starting app..." -ForegroundColor Yellow
    adb shell am start -n com.openterface.AOS/.activity.MainActivity
    
    Write-Host "📋 Starting log monitoring for baudrate feature..." -ForegroundColor Yellow
    Write-Host "💡 Now test your baudrate dialog in the app!" -ForegroundColor Cyan
    Write-Host "   Open app → Menu → Baudrate Setting" -ForegroundColor White
    Write-Host "   Try different options and watch the logs below:" -ForegroundColor White
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    # Monitor logs for baudrate feature
    adb logcat -c  # Clear previous logs
    adb logcat | findstr "UsbDeviceManager BaudrateDialog baudrate DrawerLayoutDeal action_baudrate Serial.*port"
} else {
    Write-Host "❌ Failed to install app" -ForegroundColor Red
}