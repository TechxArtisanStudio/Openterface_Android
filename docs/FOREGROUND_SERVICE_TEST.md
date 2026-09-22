# Foreground Service SecurityException Fix - Test Documentation

## Overview

This document describes the tests added to verify the fix for the `SecurityException` that occurred when starting foreground services on Android 14+ (API 34+).

## Problem Description

Users were experiencing crashes with the following stack trace:

```
Exception java.lang.SecurityException:
  at android.app.Service.startForeground (Service.java:862)
  at com.openterface.AOS.vnc.VncServerService.startServer (VncServerService.java:167)
Caused by android.os.RemoteException: Remote stack trace:
  at com.android.server.am.ActiveServices.validateForegroundServiceType (ActiveServices.java:3037)
```

The issue was caused by a mismatch or validation failure between the foreground service type used in code (`ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`) and the type declared in `AndroidManifest.xml` (`connectedDevice`).

## Fix Implementation

The fix adds a try-catch block with fallback logic in both `VncServerService` and `WebRtcServerService`:

1. First, attempt to use the 3-argument `startForeground()` with explicit service type (Android 14+)
2. If `SecurityException` is thrown, fall back to the 2-argument version
3. The 2-argument version lets the system read the type from the manifest

## Test Coverage

### Unit Tests

Located in:
- `app/src/test/java/com/openterface/AOS/vnc/VncServerServiceUnitTest.java`
- `app/src/test/java/com/openterface/AOS/webrtc/WebRtcServerServiceUnitTest.java`

**What they test:**
- Verify that `ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` constant exists and has a valid value
- Test the logic for determining when to use explicit service type (Android 14+)
- Verify the fallback logic structure

**How to run:**
```bash
./gradlew :app:testDebugUnitTest
```

### Integration Tests

Located in:
- `app/src/androidTest/java/com/openterface/AOS/vnc/VncServerServiceIntegrationTest.java`
- `app/src/androidTest/java/com/openterface/AOS/webrtc/WebRtcServerServiceIntegrationTest.java`

**What they test:**
- Verify that services can be started without throwing `SecurityException`
- Test that services work correctly on different Android versions
- Verify that services can be stopped cleanly

**How to run:**
```bash
# Run on connected device/emulator
./gradlew :app:connectedDebugAndroidTest

# Or install and run manually
./gradlew :app:installDebugAndroidTest
adb shell am instrument -w com.openterface.AOS.test/androidx.test.runner.AndroidJUnitRunner
```

## Test Requirements

### Unit Tests
- No special requirements
- Run on the local JVM
- Use Mockito for mocking Android framework classes

### Integration Tests
- Require an Android device or emulator
- Test on Android 14+ (API 34+) to verify the fix
- Test on older Android versions to ensure backward compatibility

## Recommended Test Scenarios

### Scenario 1: Android 14+ Device
1. Install the app on an Android 14+ device
2. Run the integration tests
3. Verify that services start without crashing
4. Check logcat for any "Failed to start foreground service" errors

### Scenario 2: Older Android Device
1. Install the app on an Android 13 or older device
2. Run the integration tests
3. Verify that services start normally
4. Verify that the 2-argument `startForeground()` is used

### Scenario 3: Manual Testing
1. Open the VNC server settings dialog
2. Start the VNC server
3. Verify no crash occurs
4. Check that the notification appears
5. Repeat for WebRTC server

## Expected Behavior

### On Android 14+ (API 34+)
- Services should start successfully using the 3-argument `startForeground()`
- If the explicit type fails, the fallback should use the 2-argument version
- No `SecurityException` should be thrown

### On Android 13 and older
- Services should start using the 2-argument `startForeground()`
- The system reads the service type from the manifest
- No `SecurityException` should be thrown

## Troubleshooting

### If tests fail with SecurityException:
1. Check that `AndroidManifest.xml` has the correct permissions:
   ```xml
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
   ```

2. Verify that the service declaration includes the correct type:
   ```xml
   <service
       android:name="com.openterface.AOS.vnc.VncServerService"
       android:foregroundServiceType="connectedDevice" />
   ```

3. Check logcat for detailed error messages:
   ```bash
   adb logcat | grep -E "(VncServerService|WebRtcServerService|SecurityException)"
   ```

### If integration tests don't run:
1. Ensure a device or emulator is connected: `adb devices`
2. Enable USB debugging on the device
3. For emulators, ensure they're running and accessible

## Test Maintenance

When updating the foreground service logic:
1. Update the unit tests to reflect new logic
2. Run integration tests on multiple Android versions
3. Verify the fix still works on Android 14+ devices
4. Update this documentation if the test structure changes
