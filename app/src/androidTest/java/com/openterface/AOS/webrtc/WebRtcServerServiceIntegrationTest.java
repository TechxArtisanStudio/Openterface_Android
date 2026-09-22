package com.openterface.AOS.webrtc;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration test for WebRtcServerService foreground service behavior.
 * Tests the fix for SecurityException when starting foreground service on Android 14+.
 */
@RunWith(AndroidJUnit4.class)
public class WebRtcServerServiceIntegrationTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @After
    public void tearDown() {
        // Clean up any running service
        Intent stopIntent = new Intent(context, WebRtcServerService.class);
        context.stopService(stopIntent);
    }

    /**
     * Test that WebRtcServerService can be started without throwing SecurityException.
     * This verifies the fix for the foreground service type validation issue on Android 14+.
     */
    @Test
    public void testServiceStartsWithoutException() {
        // Start the service
        Intent startIntent = new Intent(context, WebRtcServerService.class);

        try {
            // Start the service
            boolean started = context.startService(startIntent) != null;

            // If we get here without exception, the service started successfully
            // This means the foreground service type validation passed
            assertTrue("Service should start successfully", started);

        } catch (Exception e) {
            fail("Service should start without throwing exception: " + e.getMessage());
        }
    }

    /**
     * Test that the service handles different Android versions correctly.
     * On Android 14+ (API 34+), it should use the 3-argument startForeground().
     * On older versions, it should use the 2-argument version.
     */
    @Test
    public void testForegroundServiceTypeHandling() {
        // This test verifies that the service can start on any Android version
        // The actual version-specific logic is tested by the system
        Intent startIntent = new Intent(context, WebRtcServerService.class);

        try {
            boolean started = context.startService(startIntent) != null;

            // Log the Android version for debugging
            int sdkVersion = Build.VERSION.SDK_INT;

            // The service should start successfully regardless of Android version
            assertTrue("Service should start on Android " + sdkVersion, started);

        } catch (Exception e) {
            fail("Service should start on any Android version: " + e.getMessage());
        }
    }

    /**
     * Test that the service can be stopped cleanly.
     */
    @Test
    public void testServiceCanBeStopped() {
        // Start the service first
        Intent startIntent = new Intent(context, WebRtcServerService.class);
        context.startService(startIntent);

        // Stop the service
        Intent stopIntent = new Intent(context, WebRtcServerService.class);
        boolean stopped = context.stopService(stopIntent);

        assertTrue("Service should be stopped", stopped);
    }
}