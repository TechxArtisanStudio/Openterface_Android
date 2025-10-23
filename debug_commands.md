# Android Debug Setup Commands

# 1. Check connected devices
adb devices

# 2. Install debug APK
./gradlew installDebug

# 3. View real-time logs
adb logcat | findstr "UsbDeviceManager\|DrawerLayoutDeal\|MainActivity"

# 4. View specific tag logs
adb logcat -s "UsbDeviceManager"

# 5. Clear logs and start fresh
adb logcat -c && adb logcat

# 6. Install and run app
./gradlew installDebug && adb shell am start -n com.openterface.AOS/.activity.MainActivity

# 7. View crash logs
adb logcat | findstr "AndroidRuntime"

# 8. Monitor baudrate changes specifically
adb logcat | findstr "baudrate\|Baudrate\|BAUDRATE"