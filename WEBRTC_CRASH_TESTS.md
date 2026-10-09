# WebRTC Server Crash Tests

## Overview

This document describes the unit tests created to test crash scenarios when starting the WebRTC server in the Openterface Android application.

## Test File

**Location**: `app/src/test/java/com/openterface/AOS/webrtc/WebRtcServerServiceCrashTest.java`

## Test Coverage

The test suite includes 19 unit tests that verify the WebRTC server handles various error conditions gracefully without crashing:

### WebRtcSignalingServer Tests (14 tests)

1. **testSignalingServerCreationWithValidPort** - Verifies server can be created with a valid port number
2. **testSignalingServerCreationWithInvalidPorts** - Tests handling of invalid ports (0, negative, >65535)
3. **testSignalingServerCreationWithNullCallback** - Verifies server handles null callback gracefully
4. **testSignalingServerStartAndStop** - Tests normal start/stop lifecycle
5. **testSignalingServerStopWhenNotStarted** - Verifies stopping a server that wasn't started doesn't crash
6. **testSignalingServerStopMultipleTimes** - Tests that multiple stop calls are handled gracefully
7. **testSignalingServerSetNullContext** - Verifies null context is handled without crashing
8. **testSignalingServerSetNullPendingAnswer** - Tests null pending answer handling
9. **testSignalingServerSetPendingAnswer** - Verifies SDP answer storage and retrieval
10. **testSignalingServerSetConnectionState** - Tests various connection state changes
11. **testSignalingServerSetNullConnectionState** - Verifies null state handling
12. **testSignalingServerClearIceCandidatesWhenEmpty** - Tests clearing empty candidate list
13. **testSignalingServerClearIceCandidatesMultipleTimes** - Verifies multiple clears don't crash
14. **testSignalingServerGetActualPortBeforeStart** - Tests port retrieval before server starts

### WebRtcServerService Tests (5 tests)

15. **testWebRtcServerServiceInstantiation** - Verifies service can be instantiated
16. **testWebRtcServerServiceInitialState** - Tests initial state (not running, not connected)
17. **testWebRtcServerServiceSetNullCallback** - Verifies null callback handling
18. **testWebRtcServerServiceSetCallback** - Tests setting a valid callback
19. **testWebRtcServerServiceGetDeviceIpAddresses** - Verifies IP address retrieval works

## Configuration Changes

To enable these tests to run in the JVM environment (without Android framework), the following configuration was added to `app/build.gradle`:

```gradle
testOptions {
    unitTests {
        includeAndroidResources = true
        returnDefaultValues = true
    }
}
```

This configuration:
- `includeAndroidResources = true`: Makes Android resources available in unit tests
- `returnDefaultValues = true`: Returns default values (0, false, null) for Android framework methods instead of throwing exceptions

## Running the Tests

### Run all unit tests:
```bash
./gradlew testDebugUnitTest
```

### Run only the crash tests:
```bash
./gradlew testDebugUnitTest --tests "com.openterface.AOS.webrtc.WebRtcServerServiceCrashTest"
```

### View test results:
Test results are available at: `app/build/reports/tests/testDebugUnitTest/index.html`

## Test Results

All 19 tests pass successfully:
- **Tests**: 19
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0

## What These Tests Verify

These tests ensure that:

1. **Error Handling**: The WebRTC server components handle invalid inputs gracefully
2. **Null Safety**: Null values are handled without causing NullPointerExceptions
3. **Lifecycle Management**: Start/stop operations work correctly in various scenarios
4. **State Management**: Connection states and pending answers are managed correctly
5. **Edge Cases**: Invalid ports, empty collections, and multiple operations are handled
6. **No Crashes**: The server doesn't crash under error conditions, allowing proper error logging and recovery

## Limitations

These are **unit tests** that run on the JVM and don't test:
- Actual Android Service lifecycle (requires instrumented tests)
- Real network operations (port binding, actual HTTP requests)
- WebRTC initialization with real Android context
- Foreground service behavior (requires Android framework)

For testing actual service behavior, refer to:
- `WebRtcServerServiceIntegrationTest.java` - Android instrumented tests

## Related Tests

- **WebRtcServerServiceUnitTest.java** - Tests foreground service type handling logic
- **WebRtcServerServiceIntegrationTest.java** - Integration tests for actual service behavior on Android
- **WebRtcConfigTest.java** - Tests for WebRTC configuration
- **WebRtcInputRouterTest.java** - Tests for input routing
- **AndroidHidInputSenderTest.java** - Tests for HID input sending

## Future Improvements

Potential enhancements to consider:

1. Add Robolectric for more comprehensive Android framework mocking
2. Add tests for concurrent access scenarios
3. Add tests for memory/resource cleanup
4. Add performance tests for high-load scenarios
5. Add tests for network connectivity changes
6. Add tests for configuration changes (rotation, locale, etc.)
