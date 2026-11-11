package com.openterface.AOS.target;

import android.util.Log;

/**
 * Centralized mouse mode state management.
 * Single source of truth for current mouse mode and sensitivity settings.
 */
public class MouseModeManager {
    private static final String TAG = "OP-MOUSE-MGR";

    // Core state
    private static MouseMode currentMode = MouseMode.ABSOLUTE;
    private static boolean trackpadSensitivity = false;  // Sensitivity curve for RELATIVE mode

    // Private constructor - utility class
    private MouseModeManager() {}

    /**
     * Get current mouse mode.
     */
    public static MouseMode getMode() {
        return currentMode;
    }

    /**
     * Set mouse control mode.
     */
    public static void setMode(MouseMode mode) {
        if (mode == null) return;
        if (currentMode != mode) {
            currentMode = mode;
            Log.d(TAG, "Mouse mode changed to: " + mode);
        }
    }

    /**
     * Enable/disable trackpad-style sensitivity curve.
     * When enabled in RELATIVE mode, sensitivity is scaled by 1/zoomScale
     * for precise control when zoomed in.
     */
    public static void setTrackpadSensitivity(boolean enabled) {
        if (trackpadSensitivity != enabled) {
            trackpadSensitivity = enabled;
            Log.d(TAG, "Trackpad sensitivity curve: " + enabled);
        }
    }

    /**
     * Check if trackpad-style sensitivity curve is currently active.
     * Returns true only when mode is RELATIVE and trackpadSensitivity is enabled.
     */
    public static boolean isTrackpadSensitivityEnabled() {
        return trackpadSensitivity && currentMode == MouseMode.RELATIVE;
    }

    /**
     * Check if current mode is ABSOLUTE.
     */
    public static boolean isAbsoluteMode() {
        return currentMode == MouseMode.ABSOLUTE;
    }

    /**
     * Check if current mode is RELATIVE.
     */
    public static boolean isRelativeMode() {
        return currentMode == MouseMode.RELATIVE;
    }
}
