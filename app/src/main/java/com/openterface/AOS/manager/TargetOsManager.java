package com.openterface.AOS.manager;

import android.content.Context;
import android.content.SharedPreferences;

import com.openterface.AOS.R;

/**
 * Target OS Manager - manages the target operating system configuration
 * Controls how the Win/Cmd/Super key is labeled and behaves
 */
public class TargetOsManager {

    public enum TargetOS {
        WINDOWS("Windows", "Win", R.drawable.ic_os_windows),
        MACOS("macOS", "Cmd", R.drawable.ic_os_macos),
        LINUX("Linux", "Super", R.drawable.ic_os_linux);

        private final String name;
        private final String keyLabel;
        private final int iconResId;

        TargetOS(String name, String keyLabel, int iconResId) {
            this.name = name;
            this.keyLabel = keyLabel;
            this.iconResId = iconResId;
        }

        public String getName() {
            return name;
        }

        public String getKeyLabel() {
            return keyLabel;
        }

        public int getIconResId() {
            return iconResId;
        }
    }

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_TARGET_OS = "target_os";
    private static final String DEFAULT_OS = "windows";

    private static TargetOsManager instance;
    private TargetOS currentOs;

    private TargetOsManager() {
        // Private constructor for singleton
    }

    public static synchronized TargetOsManager getInstance() {
        if (instance == null) {
            instance = new TargetOsManager();
        }
        return instance;
    }

    /**
     * Initialize with saved preference
     */
    public void initialize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String osName = prefs.getString(KEY_TARGET_OS, DEFAULT_OS);

        try {
            currentOs = TargetOS.valueOf(osName.toUpperCase());
        } catch (IllegalArgumentException e) {
            currentOs = TargetOS.WINDOWS;
        }
    }

    /**
     * Get current target OS
     */
    public TargetOS getCurrentOs() {
        return currentOs != null ? currentOs : TargetOS.WINDOWS;
    }

    /**
     * Set target OS and save to preferences
     */
    public void setCurrentOs(Context context, TargetOS os) {
        this.currentOs = os;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_TARGET_OS, os.name().toLowerCase())
            .apply();
    }

    /**
     * Get the key label for current OS (Win/Cmd/Super)
     */
    public String getKeyLabel() {
        return getCurrentOs().getKeyLabel();
    }

    /**
     * Get the icon resource ID for current OS
     */
    public int getIconResId() {
        return getCurrentOs().getIconResId();
    }

    /**
     * Get all available target OS options
     */
    public TargetOS[] getAllOptions() {
        return TargetOS.values();
    }
}
