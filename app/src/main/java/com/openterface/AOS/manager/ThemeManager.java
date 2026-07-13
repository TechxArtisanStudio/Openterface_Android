package com.openterface.AOS.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.openterface.AOS.R;

/**
 * Theme Manager - manages app theme color configuration
 * Supports 8 color themes and dark/light modes
 */
public class ThemeManager {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_THEME_FAMILY = "theme_color_family";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String DEFAULT_FAMILY = "orange";
    private static final String DEFAULT_MODE = "system";

    public static final String FAMILY_ORANGE = "orange";
    public static final String FAMILY_BLUE = "blue";
    public static final String FAMILY_GREEN = "green";
    public static final String FAMILY_PINK = "pink";
    public static final String FAMILY_PURPLE = "purple";
    public static final String FAMILY_RED = "red";
    public static final String FAMILY_TEAL = "teal";
    public static final String FAMILY_INDIGO = "indigo";

    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_SYSTEM = "system";

    private static ThemeManager instance;
    private SharedPreferences prefs;

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the current theme color family
     */
    public String getThemeFamily() {
        return prefs != null ? prefs.getString(KEY_THEME_FAMILY, DEFAULT_FAMILY) : DEFAULT_FAMILY;
    }

    /**
     * Set the theme color family
     */
    public void setThemeFamily(Context context, String family) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        prefs.edit().putString(KEY_THEME_FAMILY, family).apply();
    }

    /**
     * Get the current theme mode
     */
    public String getThemeMode() {
        return prefs != null ? prefs.getString(KEY_THEME_MODE, DEFAULT_MODE) : DEFAULT_MODE;
    }

    /**
     * Set the theme mode
     */
    public void setThemeMode(Context context, String mode) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        prefs.edit().putString(KEY_THEME_MODE, mode).apply();
    }

    /**
     * Apply theme mode (light/dark/system) using AppCompatDelegate.
     */
    public void applyThemeMode(String mode) {
        switch (mode) {
            case MODE_LIGHT:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Apply theme to the Activity
     */
    public void applyTheme(Activity activity) {
        String family = getThemeFamily();
        int themeResId = getThemeResId(family);
        activity.setTheme(themeResId);
    }

    /**
     * Get theme resource ID based on color family
     */
    public int getThemeResId(String family) {
        switch (family) {
            case FAMILY_BLUE:
                return R.style.Theme_Usbvideo_Blue;
            case FAMILY_GREEN:
                return R.style.Theme_Usbvideo_Green;
            case FAMILY_PINK:
                return R.style.Theme_Usbvideo_Pink;
            case FAMILY_PURPLE:
                return R.style.Theme_Usbvideo_Purple;
            case FAMILY_RED:
                return R.style.Theme_Usbvideo_Red;
            case FAMILY_TEAL:
                return R.style.Theme_Usbvideo_Teal;
            case FAMILY_INDIGO:
                return R.style.Theme_Usbvideo_Indigo;
            case FAMILY_ORANGE:
            default:
                return R.style.Theme_Usbvideo;
        }
    }

    /**
     * Get theme color
     */
    public int getThemeColor(Context context, int attrId) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            }
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(context, typedValue.resourceId);
            }
        }
        return 0;
    }

    /**
     * Get all available theme color families
     */
    public static String[] getAllFamilies() {
        return new String[]{
                FAMILY_ORANGE,
                FAMILY_BLUE,
                FAMILY_GREEN,
                FAMILY_PINK,
                FAMILY_PURPLE,
                FAMILY_RED,
                FAMILY_TEAL,
                FAMILY_INDIGO
        };
    }

    /**
     * Get the display name of a theme color family
     */
    public static String getFamilyDisplayName(String family) {
        switch (family) {
            case FAMILY_ORANGE:
                return "Orange";
            case FAMILY_BLUE:
                return "Blue";
            case FAMILY_GREEN:
                return "Green";
            case FAMILY_PINK:
                return "Pink";
            case FAMILY_PURPLE:
                return "Purple";
            case FAMILY_RED:
                return "Red";
            case FAMILY_TEAL:
                return "Cyan";
            case FAMILY_INDIGO:
                return "Indigo";
            default:
                return family;
        }
    }

    /**
     * Get the preview color of a theme color family
     */
    public static int getFamilyPreviewColor(Context context, String family) {
        switch (family) {
            case FAMILY_BLUE:
                return ContextCompat.getColor(context, R.color.theme_accent_blue);
            case FAMILY_GREEN:
                return ContextCompat.getColor(context, R.color.theme_accent_green);
            case FAMILY_PINK:
                return ContextCompat.getColor(context, R.color.theme_accent_pink);
            case FAMILY_PURPLE:
                return ContextCompat.getColor(context, R.color.theme_accent_purple);
            case FAMILY_RED:
                return ContextCompat.getColor(context, R.color.theme_accent_red);
            case FAMILY_TEAL:
                return ContextCompat.getColor(context, R.color.theme_accent_teal);
            case FAMILY_INDIGO:
                return ContextCompat.getColor(context, R.color.theme_accent_indigo);
            case FAMILY_ORANGE:
            default:
                return ContextCompat.getColor(context, R.color.theme_accent_orange);
        }
    }
}