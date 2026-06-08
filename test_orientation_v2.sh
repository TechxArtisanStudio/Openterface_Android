#!/bin/bash
# Test script for orientation changes

ADB="/Users/txa/Library/Android/sdk/platform-tools/adb"
DEVICE="192.168.100.83:34341"
APP_PACKAGE="com.openterface.AOS"
PASS=0
FAIL=0

echo "============================================"
echo "   Openterface Orientation Test Suite"
echo "============================================"

# Helper function
check_pass() {
    if [ $? -eq 0 ] && [ "$1" != "" ]; then
        echo "  ✅ PASS: $1"
        PASS=$((PASS+1))
    else
        echo "  ❌ FAIL: $1"
        FAIL=$((FAIL+1))
    fi
}

# Test 1: Install APK
echo ""
echo "Test 1: Install APK"
$ADB -s $DEVICE install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk > /dev/null 2>&1
check_pass "APK installed"

# Test 2: Launch app
echo ""
echo "Test 2: Launch app in portrait mode"
$ADB -s $DEVICE logcat -c > /dev/null 2>&1
$ADB -s $DEVICE shell am force-stop $APP_PACKAGE > /dev/null 2>&1
sleep 1
$ADB -s $DEVICE shell am start -n $APP_PACKAGE/.activity.MainActivity > /dev/null 2>&1
sleep 5
PID=$($ADB -s $DEVICE shell pidof $APP_PACKAGE 2>/dev/null | tr -d '\r\n ')
if [ -n "$PID" ]; then
    echo "  ✅ PASS: App started (PID: $PID)"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: App did not start"
    FAIL=$((FAIL+1))
fi

# Test 3: Check no crash on startup
echo ""
echo "Test 3: Check no crash on startup"
CRASH_LOG=$($ADB -s $DEVICE logcat -d -s AndroidRuntime:E 2>/dev/null | grep "FATAL EXCEPTION")
if [ -z "$CRASH_LOG" ]; then
    echo "  ✅ PASS: No crash on startup"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: Crash on startup"
    echo "  $CRASH_LOG"
    FAIL=$((FAIL+1))
fi

# Test 4: Check portrait mode logs
echo ""
echo "Test 4: Check portrait mode logs"
PORTRAIT_LOGS=$($ADB -s $DEVICE logcat -d -s MainActivity:D 2>/dev/null | grep "initPortraitZones: setting up portrait zone listeners")
if [ -n "$PORTRAIT_LOGS" ]; then
    echo "  ✅ PASS: Portrait mode initialized correctly"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: Portrait mode initialization missing"
    FAIL=$((FAIL+1))
fi

# Test 5: Orientation change - portrait to landscape
echo ""
echo "Test 5: Orientation change (portrait → landscape)"
$ADB -s $DEVICE logcat -c > /dev/null 2>&1
$ADB -s $DEVICE shell input tap 1100 2600 > /dev/null 2>&1
sleep 5
CRASH_LOG=$($ADB -s $DEVICE logcat -d -s AndroidRuntime:E 2>/dev/null | grep "FATAL EXCEPTION")
if [ -z "$CRASH_LOG" ]; then
    echo "  ✅ PASS: No crash on orientation change"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: Crash on orientation change"
    echo "  $CRASH_LOG"
    FAIL=$((FAIL+1))
fi

# Test 6: Check app still running after orientation change
echo ""
echo "Test 6: App still running after orientation change"
PID=$($ADB -s $DEVICE shell pidof $APP_PACKAGE 2>/dev/null | tr -d '\r\n ')
if [ -n "$PID" ]; then
    echo "  ✅ PASS: App still running (PID: $PID)"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: App crashed after orientation change"
    FAIL=$((FAIL+1))
fi

# Test 7: Check landscape mode logs
echo ""
echo "Test 7: Check landscape mode logs"
LAND_LOG=$($ADB -s $DEVICE logcat -d -s MainActivity:D 2>/dev/null | grep "reloadLayoutForOrientation: nowPortrait=false")
if [ -n "$LAND_LOG" ]; then
    echo "  ✅ PASS: Landscape mode initialized correctly"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: Landscape mode initialization missing"
    FAIL=$((FAIL+1))
fi

# Test 8: Orientation change - landscape to portrait
echo ""
echo "Test 8: Orientation change (landscape → portrait)"
$ADB -s $DEVICE logcat -c > /dev/null 2>&1
$ADB -s $DEVICE shell input tap 1100 2600 > /dev/null 2>&1
sleep 5
CRASH_LOG=$($ADB -s $DEVICE logcat -d -s AndroidRuntime:E 2>/dev/null | grep "FATAL EXCEPTION")
if [ -z "$CRASH_LOG" ]; then
    echo "  ✅ PASS: No crash on second orientation change"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: Crash on second orientation change"
    echo "  $CRASH_LOG"
    FAIL=$((FAIL+1))
fi

# Test 9: Check app still running after second orientation change
echo ""
echo "Test 9: App still running after second orientation change"
PID=$($ADB -s $DEVICE shell pidof $APP_PACKAGE 2>/dev/null | tr -d '\r\n ')
if [ -n "$PID" ]; then
    echo "  ✅ PASS: App still running after both orientation changes (PID: $PID)"
    PASS=$((PASS+1))
else
    echo "  ❌ FAIL: App crashed after second orientation change"
    FAIL=$((FAIL+1))
fi

# Summary
echo ""
echo "============================================"
echo "   Test Summary"
echo "============================================"
echo "   PASSED: $PASS"
echo "   FAILED: $FAIL"
echo "   TOTAL:  $((PASS+FAIL))"
echo "============================================"

# Exit with failure if any tests failed
if [ $FAIL -gt 0 ]; then
    exit 1
fi
exit 0
