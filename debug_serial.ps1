# Openterface Serial Debug Script
# This PowerShell script helps debug serial connection issues

Write-Host "========================================" -ForegroundColor Green
Write-Host "    OPENTERFACE SERIAL DEBUG HELPER" -ForegroundColor Green  
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Function to check if ADB is available
function Check-ADB {
    try {
        $adbVersion = adb version 2>$null
        if ($adbVersion) {
            Write-Host "✅ ADB is available" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "❌ ADB not found or not in PATH" -ForegroundColor Red
        Write-Host "   Please install Android SDK Platform Tools" -ForegroundColor Yellow
        return $false
    }
}

# Function to list connected devices
function Get-ConnectedDevices {
    Write-Host "--- CONNECTED ANDROID DEVICES ---" -ForegroundColor Cyan
    
    $devices = adb devices
    Write-Host $devices
    
    $deviceLines = $devices -split "`n" | Where-Object { $_ -match "\t" -and $_ -notmatch "List of devices" }
    
    if ($deviceLines.Count -eq 0) {
        Write-Host "❌ No Android devices connected" -ForegroundColor Red
        Write-Host "   Please connect your Android device and enable USB debugging" -ForegroundColor Yellow
        return $false
    } else {
        Write-Host "✅ Found $($deviceLines.Count) connected device(s)" -ForegroundColor Green
        return $true
    }
}

# Function to enable USB debugging logs
function Enable-UsbLogs {
    Write-Host "--- ENABLING USB DEBUG LOGS ---" -ForegroundColor Cyan
    
    try {
        # Enable USB debug logging
        adb shell "setprop log.tag.USB D"
        adb shell "setprop log.tag.UsbDeviceManager D" 
        adb shell "setprop log.tag.UsbManager D"
        adb shell "setprop log.tag.UsbSerial D"
        adb shell "setprop log.tag.SerialDebug D"
        adb shell "setprop log.tag.OpenterfaceSerial D"
        
        Write-Host "✅ USB debug logging enabled" -ForegroundColor Green
    } catch {
        Write-Host "❌ Failed to enable USB logging" -ForegroundColor Red
    }
}

# Function to start continuous log monitoring
function Start-LogMonitoring {
    Write-Host "--- STARTING LOG MONITORING ---" -ForegroundColor Cyan
    Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
    Write-Host ""
    
    # Clear existing logs first
    adb logcat -c
    
    # Monitor logs with USB-related tags
    adb logcat -s USB:* UsbDeviceManager:* UsbManager:* UsbSerial:* SerialDebug:* OpenterfaceSerial:* Ch34xSerialDriver:*
}

# Function to capture USB device list from Android
function Get-AndroidUsbDevices {
    Write-Host "--- ANDROID USB DEVICES ---" -ForegroundColor Cyan
    
    try {
        # Get USB devices from Android system
        $usbDevices = adb shell "cat /proc/bus/usb/devices" 2>$null
        
        if ($usbDevices) {
            Write-Host "USB Devices from /proc/bus/usb/devices:" -ForegroundColor Green
            Write-Host $usbDevices
        } else {
            Write-Host "Could not read /proc/bus/usb/devices (may require root)" -ForegroundColor Yellow
        }
        
        # Alternative: Try lsusb if available
        $lsusb = adb shell "lsusb" 2>$null
        if ($lsusb) {
            Write-Host "`nUSB Devices from lsusb:" -ForegroundColor Green
            Write-Host $lsusb
        }
        
    } catch {
        Write-Host "❌ Could not retrieve USB device information" -ForegroundColor Red
    }
}

# Function to install APK with debug activity
function Install-DebugApk {
    Write-Host "--- INSTALLING DEBUG APK ---" -ForegroundColor Cyan
    
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    
    if (Test-Path $apkPath) {
        Write-Host "Installing APK with debug features..." -ForegroundColor Green
        adb install -r $apkPath
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ APK installed successfully" -ForegroundColor Green
            Write-Host "   You can now run the serial debug activity" -ForegroundColor Yellow
        } else {
            Write-Host "❌ APK installation failed" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ APK not found at: $apkPath" -ForegroundColor Red
        Write-Host "   Please build the project first: ./gradlew assembleDebug" -ForegroundColor Yellow
    }
}

# Function to display troubleshooting tips
function Show-TroubleshootingTips {
    Write-Host ""
    Write-Host "--- TROUBLESHOOTING TIPS ---" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Common Serial Communication Issues:" -ForegroundColor Yellow
    Write-Host "1. USB Permission not granted - Check app permissions" -ForegroundColor White
    Write-Host "2. Incorrect baudrate - Try 115200 or 9600" -ForegroundColor White  
    Write-Host "3. Device not properly connected - Check USB cable" -ForegroundColor White
    Write-Host "4. USB driver not loaded - Device may need specific drivers" -ForegroundColor White
    Write-Host "5. Hardware flow control issues - Check DTR/RTS settings" -ForegroundColor White
    Write-Host "6. Device in use by another app - Close other serial apps" -ForegroundColor White
    Write-Host "7. USB hub power issues - Try direct connection" -ForegroundColor White
    Write-Host "8. Cable/connection problems - Try different USB cable" -ForegroundColor White
    Write-Host ""
    Write-Host "Debug Steps to Try:" -ForegroundColor Yellow
    Write-Host "1. Run this script to monitor logs" -ForegroundColor White
    Write-Host "2. Connect Openterface device" -ForegroundColor White
    Write-Host "3. Launch your app and try serial communication" -ForegroundColor White
    Write-Host "4. Check logs for errors and connection status" -ForegroundColor White
    Write-Host "5. Try different baudrates and flow control settings" -ForegroundColor White
    Write-Host ""
    Write-Host "Expected Openterface Device IDs:" -ForegroundColor Yellow
    Write-Host "- Serial: 1A86:7523 or 1A86:FE0C (CH340/CH341)" -ForegroundColor White
    Write-Host "- UVC/HID: 534D:2109 or 345F:2132" -ForegroundColor White
}

# Main execution
Write-Host "Starting Openterface serial debug session..." -ForegroundColor Green
Write-Host ""

# Check prerequisites
if (-not (Check-ADB)) {
    exit 1
}

if (-not (Get-ConnectedDevices)) {
    exit 1
}

Write-Host ""
Show-TroubleshootingTips
Write-Host ""

# Menu loop
do {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Select an option:" -ForegroundColor Cyan
    Write-Host "1. Enable USB debug logging" -ForegroundColor White
    Write-Host "2. Monitor USB logs in real-time" -ForegroundColor White  
    Write-Host "3. Show Android USB devices" -ForegroundColor White
    Write-Host "4. Install debug APK" -ForegroundColor White
    Write-Host "5. Show troubleshooting tips" -ForegroundColor White
    Write-Host "6. Exit" -ForegroundColor White
    Write-Host ""
    
    $choice = Read-Host "Enter choice (1-6)"
    
    switch ($choice) {
        "1" { 
            Enable-UsbLogs 
            Write-Host "Press any key to continue..."
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        }
        "2" { Start-LogMonitoring }
        "3" { 
            Get-AndroidUsbDevices 
            Write-Host "Press any key to continue..."
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        }
        "4" { 
            Install-DebugApk 
            Write-Host "Press any key to continue..."
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        }
        "5" { 
            Show-TroubleshootingTips 
            Write-Host "Press any key to continue..."
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        }
        "6" { 
            Write-Host "Exiting debug session..." -ForegroundColor Green
            exit 0 
        }
        default { 
            Write-Host "Invalid choice. Please select 1-6." -ForegroundColor Red
        }
    }
    Write-Host ""
} while ($true)