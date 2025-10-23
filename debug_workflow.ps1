# Android Debugging Workflow for Openterface
# Your Android SDK is at: D:\androidSDK\platform-tools

# 1. Check connected devices
adb devices

# 2. Install your app
adb install app\build\outputs\apk\debug\OpenterfaceAndroid-debug.apk

# 3. Start the app
adb shell am start -n com.openterface.AOS/.activity.MainActivity

# 4. Monitor general logs
adb logcat

# 5. Monitor only your app's logs
adb logcat | findstr "com.openterface.AOS"

# 6. Monitor baudrate feature specifically
adb logcat | findstr "UsbDeviceManager\|BaudrateDialog\|baudrate"

# 7. Monitor drawer layout clicks
adb logcat | findstr "DrawerLayoutDeal\|action_baudrate"

# 8. Clear logs and start fresh monitoring
adb logcat -c
adb logcat | findstr "UsbDeviceManager"

# 9. Monitor serial port connections
adb logcat | findstr "Serial.*port\|connection.*successful\|Failed to connect"

# 10. Uninstall app (if needed)
adb uninstall com.openterface.AOS

# 11. Reinstall and restart for testing
adb install -r app\build\outputs\apk\debug\OpenterfaceAndroid-debug.apk
adb shell am start -n com.openterface.AOS/.activity.MainActivity