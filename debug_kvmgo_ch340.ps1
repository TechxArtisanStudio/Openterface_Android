# Debug KVMGO with CH340 (1A86:FE0C)
# This script helps test and debug KVMGO devices with CH340 chipsets

# Clear logs
Write-Host "Clearing Android logs..."
adb logcat -c

# Launch the app
Write-Host "Launching Openterface app..."
adb shell am start -n com.openterface.AOS/.activity.MainActivity

# Monitor logs with filtering for USB detection events
Write-Host "Monitoring logs for USB detection events..."
Write-Host "Press Ctrl+C to stop monitoring logs"
adb logcat -v time | Select-String -Pattern "UsbDeviceManager|KVMGO|FE0C|CH34x|detectOpenterface|baudrate"