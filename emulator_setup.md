# Create and Start Android Emulator

# 1. List available AVDs
emulator -list-avds

# 2. Create new AVD (if needed)
# Open Android Studio → Tools → AVD Manager → Create Virtual Device

# 3. Start emulator
emulator -avd [AVD_NAME]

# 4. Install and debug
./gradlew installDebug
adb logcat -s "UsbDeviceManager"