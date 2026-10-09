package com.openterface.AOS.webrtc;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests to reproduce WebRTC server startup crash scenarios.
 * These tests verify that the server handles various failure conditions gracefully.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebRtcServerServiceCrashTest {

    @Mock
    private Context mockContext;

    @Mock
    private ApplicationInfo mockApplicationInfo;

    private WebRtcServerService service;

    @Before
    public void setUp() {
        // Setup mock context
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getApplicationInfo()).thenReturn(mockApplicationInfo);
        when(mockContext.getPackageName()).thenReturn("com.openterface.AOS");

        // Create service instance with mock context
        service = new WebRtcServerService();
        try {
            java.lang.reflect.Field contextField = android.app.Service.class.getDeclaredField("mBase");
            contextField.setAccessible(true);
            contextField.set(service, mockContext);
        } catch (Exception e) {
            // If reflection fails, tests will still run but may need adjustment
            System.err.println("Warning: Could not inject mock context: " + e.getMessage());
        }
    }

    /**
     * Test 1: Normal startup flow - verify server can start under ideal conditions
     */
    @Test
    public void testNormalStartup() {
        System.out.println("=== Test 1: Normal Startup ===");

        try {
            // Try to start server with valid parameters
            boolean result = service.startServer(
                8080,  // valid port
                "stun:stun.l.google.com:19302",  // valid STUN server
                1280,  // width
                720,   // height
                30,    // fps
                0      // rotation
            );

            System.out.println("Start result: " + result);

            // If it fails, we should get false but not crash
            if (!result) {
                System.out.println("Server failed to start (expected in test environment)");
            } else {
                System.out.println("Server started successfully");
                // Clean up
                service.stopServer();
            }

            System.out.println("Test 1 PASSED: No crash occurred");

        } catch (Exception e) {
            System.err.println("Test 1 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Normal startup should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 2: Port conflict - simulate signaling port already in use
     */
    @Test
    public void testPortConflict() {
        System.out.println("=== Test 2: Port Conflict ===");

        try {
            // Try to start with invalid port (0)
            boolean result1 = service.startServer(
                0,  // invalid port
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );

            System.out.println("Start with port 0 result: " + result1);
            assertFalse("Should fail with invalid port", result1);

            // Try negative port
            boolean result2 = service.startServer(
                -1,  // invalid port
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );

            System.out.println("Start with port -1 result: " + result2);
            assertFalse("Should fail with negative port", result2);

            // Try port > 65535
            boolean result3 = service.startServer(
                70000,  // invalid port
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );

            System.out.println("Start with port 70000 result: " + result3);
            assertFalse("Should fail with port > 65535", result3);

            System.out.println("Test 2 PASSED: Invalid ports handled gracefully");

        } catch (Exception e) {
            System.err.println("Test 2 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Port conflict test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 3: Invalid video parameters
     */
    @Test
    public void testInvalidVideoParameters() {
        System.out.println("=== Test 3: Invalid Video Parameters ===");

        try {
            // Zero width
            boolean result1 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                0,    // invalid width
                720,
                30,
                0
            );
            System.out.println("Start with width=0 result: " + result1);

            // Zero height
            boolean result2 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280,
                0,    // invalid height
                30,
                0
            );
            System.out.println("Start with height=0 result: " + result2);

            // Negative FPS
            boolean result3 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280,
                720,
                -1,   // invalid fps
                0
            );
            System.out.println("Start with fps=-1 result: " + result3);

            System.out.println("Test 3 PASSED: Invalid parameters handled");

        } catch (Exception e) {
            System.err.println("Test 3 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Invalid parameters test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 4: Invalid STUN server URL
     */
    @Test
    public void testInvalidStunServer() {
        System.out.println("=== Test 4: Invalid STUN Server ===");

        try {
            // Null STUN server
            boolean result1 = service.startServer(
                8080,
                null,  // null STUN server
                1280,
                720,
                30,
                0
            );
            System.out.println("Start with null STUN result: " + result1);

            // Empty STUN server
            boolean result2 = service.startServer(
                8080,
                "",  // empty STUN server
                1280,
                720,
                30,
                0
            );
            System.out.println("Start with empty STUN result: " + result2);

            // Invalid STUN server format
            boolean result3 = service.startServer(
                8080,
                "invalid-stun-url",  // invalid format
                1280,
                720,
                30,
                0
            );
            System.out.println("Start with invalid STUN result: " + result3);

            System.out.println("Test 4 PASSED: Invalid STUN servers handled");

        } catch (Exception e) {
            System.err.println("Test 4 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Invalid STUN test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 5: Multiple start attempts
     */
    @Test
    public void testMultipleStartAttempts() {
        System.out.println("=== Test 5: Multiple Start Attempts ===");

        try {
            // First start attempt
            boolean result1 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );
            System.out.println("First start result: " + result1);

            // Second start attempt (should fail if first succeeded)
            boolean result2 = service.startServer(
                8081,  // different port
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );
            System.out.println("Second start result: " + result2);

            // If first succeeded, second should fail (server already running)
            if (result1) {
                assertFalse("Second start should fail when server is already running", result2);
                service.stopServer();
            }

            System.out.println("Test 5 PASSED: Multiple start attempts handled");

        } catch (Exception e) {
            System.err.println("Test 5 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Multiple start test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 6: Start and stop lifecycle
     */
    @Test
    public void testStartStopLifecycle() {
        System.out.println("=== Test 6: Start/Stop Lifecycle ===");

        try {
            // Start server
            boolean startResult = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );
            System.out.println("Start result: " + startResult);

            // Stop server (should work regardless of start result)
            service.stopServer();
            System.out.println("Stop completed");

            // Stop again (should be idempotent)
            service.stopServer();
            System.out.println("Second stop completed");

            System.out.println("Test 6 PASSED: Lifecycle handled correctly");

        } catch (Exception e) {
            System.err.println("Test 6 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Lifecycle test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 7: WebRtcSignalingServer creation with invalid parameters
     */
    @Test
    public void testSignalingServerCreation() {
        System.out.println("=== Test 7: Signaling Server Creation ===");

        try {
            // Valid parameters
            WebRtcSignalingServer server1 = new WebRtcSignalingServer(8080, null);
            System.out.println("Created server with port 8080");
            assertNotNull("Server should be created", server1);

            // Invalid port
            WebRtcSignalingServer server2 = new WebRtcSignalingServer(0, null);
            System.out.println("Created server with port 0");
            assertNotNull("Server should be created even with invalid port", server2);

            // Negative port
            WebRtcSignalingServer server3 = new WebRtcSignalingServer(-1, null);
            System.out.println("Created server with port -1");
            assertNotNull("Server should be created even with negative port", server3);

            System.out.println("Test 7 PASSED: Signaling server creation handled");

        } catch (Exception e) {
            System.err.println("Test 7 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Signaling server creation test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 8: WebRtcFrameCapturer creation with invalid parameters
     */
    @Test
    public void testFrameCapturerCreation() {
        System.out.println("=== Test 8: Frame Capturer Creation ===");

        try {
            // Valid parameters
            WebRtcFrameCapturer capturer1 = new WebRtcFrameCapturer(1280, 720, 30);
            System.out.println("Created capturer 1280x720@30");
            assertNotNull("Capturer should be created", capturer1);

            // Zero dimensions
            WebRtcFrameCapturer capturer2 = new WebRtcFrameCapturer(0, 0, 30);
            System.out.println("Created capturer 0x0@30");
            assertNotNull("Capturer should be created even with zero dimensions", capturer2);

            // Negative FPS (should default to 30)
            WebRtcFrameCapturer capturer3 = new WebRtcFrameCapturer(1280, 720, -1);
            System.out.println("Created capturer with negative FPS");
            assertNotNull("Capturer should be created even with negative FPS", capturer3);

            // Zero FPS (should default to 30)
            WebRtcFrameCapturer capturer4 = new WebRtcFrameCapturer(1280, 720, 0);
            System.out.println("Created capturer with zero FPS");
            assertNotNull("Capturer should be created even with zero FPS", capturer4);

            System.out.println("Test 8 PASSED: Frame capturer creation handled");

        } catch (Exception e) {
            System.err.println("Test 8 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Frame capturer creation test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 9: WebRtcFrameCapture creation with invalid parameters
     */
    @Test
    public void testFrameCaptureCreation() {
        System.out.println("=== Test 9: Frame Capture Creation ===");

        try {
            // Valid FPS
            WebRtcFrameCapture capture1 = new WebRtcFrameCapture(30);
            System.out.println("Created capture with FPS=30");
            assertNotNull("Capture should be created", capture1);

            // Zero FPS (should default to 30)
            WebRtcFrameCapture capture2 = new WebRtcFrameCapture(0);
            System.out.println("Created capture with FPS=0");
            assertNotNull("Capture should be created even with zero FPS", capture2);

            // Negative FPS (should default to 30)
            WebRtcFrameCapture capture3 = new WebRtcFrameCapture(-1);
            System.out.println("Created capture with FPS=-1");
            assertNotNull("Capture should be created even with negative FPS", capture3);

            System.out.println("Test 9 PASSED: Frame capture creation handled");

        } catch (Exception e) {
            System.err.println("Test 9 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Frame capture creation test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 10: Callback management
     */
    @Test
    public void testCallbackManagement() {
        System.out.println("=== Test 10: Callback Management ===");

        try {
            // Set null callback
            service.setCallback(null);
            System.out.println("Set null callback");

            // Set valid callback
            WebRtcServerService.WebRtcCallback callback = new WebRtcServerService.WebRtcCallback() {
                @Override
                public void onClientConnected(String clientInfo) {}

                @Override
                public void onClientDisconnected(String clientInfo) {}

                @Override
                public void onServerError(String error) {}

                @Override
                public void onMouseEvent(int buttonMask, int x, int y, boolean pressed) {}

                @Override
                public void onKeyboardEvent(int keysym, boolean down, int modifier) {}

                @Override
                public void onConnectionStateChanged(String state) {}
            };

            service.setCallback(callback);
            System.out.println("Set valid callback");

            // Set null again
            service.setCallback(null);
            System.out.println("Set null callback again");

            System.out.println("Test 10 PASSED: Callback management handled");

        } catch (Exception e) {
            System.err.println("Test 10 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Callback management test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 11: onUvcFrame with null buffer
     */
    @Test
    public void testOnUvcFrameNullBuffer() {
        System.out.println("=== Test 11: onUvcFrame with Null Buffer ===");

        try {
            // This should not crash even with null buffer
            service.onUvcFrame(null, 1280, 720, System.nanoTime(), 0);
            System.out.println("onUvcFrame with null buffer completed");

            System.out.println("Test 11 PASSED: Null buffer handled");

        } catch (Exception e) {
            System.err.println("Test 11 FAILED: Exception occurred");
            e.printStackTrace();
            fail("onUvcFrame with null buffer should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 12: State verification
     */
    @Test
    public void testStateVerification() {
        System.out.println("=== Test 12: State Verification ===");

        try {
            // Initial state - should not be running
            assertFalse("Server should not be running initially", service.isRunning());

            // Try to start
            boolean startResult = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280, 720, 30, 0
            );

            System.out.println("Start result: " + startResult);

            // If start succeeded, verify running state
            if (startResult) {
                assertTrue("Server should be running after successful start", service.isRunning());
                service.stopServer();
            }

            // After stop, should not be running
            assertFalse("Server should not be running after stop", service.isRunning());

            System.out.println("Test 12 PASSED: State management correct");

        } catch (Exception e) {
            System.err.println("Test 12 FAILED: Exception occurred");
            e.printStackTrace();
            fail("State verification test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 13: WebRtcConfig with mock context
     */
    @Test
    public void testWebRtcConfig() {
        System.out.println("=== Test 13: WebRtcConfig ===");

        try {
            // Create config with mock context
            WebRtcConfig config = new WebRtcConfig(mockContext);
            assertNotNull("Config should be created", config);

            // Get default values
            int port = config.getSignallingPort();
            System.out.println("Default port: " + port);

            int width = config.getVideoWidth();
            System.out.println("Default width: " + width);

            int height = config.getVideoHeight();
            System.out.println("Default height: " + height);

            int fps = config.getVideoFps();
            System.out.println("Default FPS: " + fps);

            String stun = config.getStunServer();
            System.out.println("Default STUN: " + stun);

            System.out.println("Test 13 PASSED: Config handled");

        } catch (Exception e) {
            System.err.println("Test 13 FAILED: Exception occurred");
            e.printStackTrace();
            fail("WebRtcConfig test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 14: Edge case - extremely large video dimensions
     */
    @Test
    public void testExtremeVideoDimensions() {
        System.out.println("=== Test 14: Extreme Video Dimensions ===");

        try {
            // Very large dimensions
            boolean result1 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                10000,  // very large width
                10000,  // very large height
                30,
                0
            );
            System.out.println("Start with 10000x10000 result: " + result1);

            // Very small dimensions
            boolean result2 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1,  // very small width
                1,  // very small height
                30,
                0
            );
            System.out.println("Start with 1x1 result: " + result2);

            System.out.println("Test 14 PASSED: Extreme dimensions handled");

        } catch (Exception e) {
            System.err.println("Test 14 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Extreme dimensions test should not throw exception: " + e.getMessage());
        }
    }

    /**
     * Test 15: Edge case - extremely high FPS
     */
    @Test
    public void testExtremeFPS() {
        System.out.println("=== Test 15: Extreme FPS ===");

        try {
            // Very high FPS
            boolean result1 = service.startServer(
                8080,
                "stun:stun.l.google.com:19302",
                1280,
                720,
                1000,  // very high FPS
                0
            );
            System.out.println("Start with FPS=1000 result: " + result1);

            System.out.println("Test 15 PASSED: Extreme FPS handled");

        } catch (Exception e) {
            System.err.println("Test 15 FAILED: Exception occurred");
            e.printStackTrace();
            fail("Extreme FPS test should not throw exception: " + e.getMessage());
        }
    }
}
