package com.openterface.AOS.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.openterface.AOS.target.MouseMode;

/**
 * Mouse mode preference persistence.
 * Uses clean keys, no legacy migration.
 */
public class MouseModePreferences {
    private static final String PREFS_NAME = "settings";
    private static final String KEY_MODE = "mouse_mode";
    private static final String KEY_TRACKPAD = "trackpad_sensitivity";

    /**
     * Save mouse mode and sensitivity settings.
     */
    public static void save(Context context, MouseMode mode, boolean trackpadSensitivity) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name().toLowerCase())
            .putBoolean(KEY_TRACKPAD, trackpadSensitivity)
            .apply();
    }

    /**
     * Load mouse mode from preferences.
     * Returns ABSOLUTE as default if not found.
     */
    public static MouseMode loadMode(Context context) {
        String modeStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, "absolute");
        return "relative".equals(modeStr) ? MouseMode.RELATIVE : MouseMode.ABSOLUTE;
    }

    /**
     * Load trackpad sensitivity setting from preferences.
     * Returns false as default if not found.
     */
    public static boolean loadTrackpadSensitivity(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TRACKPAD, false);
    }
}
