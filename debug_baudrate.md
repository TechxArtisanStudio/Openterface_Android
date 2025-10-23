# Debug Baudrate Feature Specifically

# 1. Monitor baudrate selection dialog
adb logcat | findstr "BaudrateDialog\|showBaudrateDialog"

# 2. Monitor UsbDeviceManager baudrate changes
adb logcat | findstr "UsbDeviceManager.*baudrate\|Serial.*baudrate"

# 3. Monitor connection attempts
adb logcat | findstr "Serial connection\|Failed to connect\|connectionSuccessful"

# 4. Monitor the drawer layout clicks
adb logcat | findstr "DrawerLayoutDeal\|action_baudrate"

# 5. Full debug session for baudrate testing
adb logcat -c
adb logcat | findstr "UsbDeviceManager\|BaudrateDialog\|Serial.*port\|baudrate"

# 6. Test sequence:
# - Install app: ./gradlew installDebug
# - Start logging: adb logcat | findstr "baudrate"
# - Open app and test baudrate dialog
# - Check logs for baudrate changes