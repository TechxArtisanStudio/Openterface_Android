#!/bin/bash
set -e
cd "$(dirname "$0")"
echo "Building APK..."
./gradlew assembleDebug --console=plain
echo "Installing to device..."
# Get first connected device
DEVICE=$(/Users/txa/Library/Android/sdk/platform-tools/adb devices | grep -v "List" | grep "device" | head -1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "No device found!"
    exit 1
fi
echo "Installing to device: $DEVICE"
/Users/txa/Library/Android/sdk/platform-tools/adb -s "$DEVICE" install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk
echo "Done!"
