#!/bin/bash
# Test script for orientation changes

ADB="/Users/txa/Library/Android/sdk/platform-tools/adb"
DEVICE="192.168.100.83:34341"
APP_PACKAGE="com.openterface.AOS"

# Clear logcat
$ADB -s $DEVICE logcat -c

echo "=== Installing APK ==="
$ADB -s $DEVICE install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk

echo "=== Launching app ==="
$ADB -s $DEVICE shell am start -n $APP_PACKAGE/.activity.MainActivity

echo "=== Waiting 5 seconds for app to start ==="
sleep 5

echo "=== Checking if app is running ==="
$ADB -s $DEVICE shell pidof $APP_PACKAGE

echo "=== Waiting 3 seconds ==="
sleep 3

echo "=== Rotating to landscape (simulating orientation change) ==="
$ADB -s $DEVICE shell content insert --uri content://settings/system --bind name:s:user_rotation --bind value:i:3

echo "=== Waiting 3 seconds ==="
sleep 3

echo "=== Capturing logs ==="
$ADB -s $DEVICE logcat -d -s MainActivity:DEBUG *:E > /tmp/orientation_test.log

echo "=== Logcat output ==="
cat /tmp/orientation_test.log

echo "=== Checking for crashes ==="
$ADB -s $DEVICE shell pidof $APP_PACKAGE || echo "APP CRASHED!"
