package com.openterface.AOS.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Manages app locale/language settings.
 * Allows users to override the system language within the app.
 * Uses AppCompatDelegate.setApplicationLocales() (AndroidX standard) to apply locale,
 * which works reliably across all AppCompat-based activities.
 */
public class LocaleManager {
    private static final String TAG = "LocaleManager";
    private static final String PREF_NAME = "locale_prefs";
    private static final String KEY_LOCALE = "app_locale";
    private static final String VALUE_SYSTEM = "system";

    private static LocaleManager instance;
    private String currentLocale = VALUE_SYSTEM;

    private LocaleManager() {}

    public static synchronized LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    /**
     * Initialize the locale manager, load saved preference, and apply it.
     * Should be called in Application.onCreate().
     */
    public void initialize(Context context) {
        Context appCtx = context.getApplicationContext();
        SharedPreferences prefs = appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentLocale = prefs.getString(KEY_LOCALE, VALUE_SYSTEM);
        Log.d(TAG, "initialize() - loaded from prefs: " + currentLocale);
        // Migrate old locale codes (without country) to new resource-matching codes
        if (!VALUE_SYSTEM.equals(currentLocale)) {
            String migrated = migrateLocaleCode(currentLocale);
            if (!migrated.equals(currentLocale)) {
                Log.d(TAG, "initialize() - migrating " + currentLocale + " to " + migrated);
                currentLocale = migrated;
                prefs.edit().putString(KEY_LOCALE, migrated).apply();
            }
        }
        Log.d(TAG, "initialize() - final currentLocale: " + currentLocale);
    }

    /**
     * Apply the current persisted locale via AndroidX AppCompatDelegate.
     * This is the standard AndroidX API — it correctly handles locale resolution
     * for all AppCompat-based activities (API 21+), including resource fallback.
     * Should be called in each Activity's attachBaseContext() after super.attachBaseContext().
     */
    public void applyPersistedLocale() {
        applyLocaleCode(currentLocale);
    }    /**
     * Migrate old locale codes to new resource-matching codes.
     * Old codes used bare language ("ja", "ko", "de", etc.) but resource
     * directories use full codes ("ja-rJP", "ko-rKR", "de-rDE", etc.).
     */
    private static String migrateLocaleCode(String code) {
        switch (code) {
            case "de":  return "de-rDE";
            case "ja":  return "ja-rJP";
            case "ko":  return "ko-rKR";
            case "es":  return "es-rES";
            case "fr":  return "fr-rFR";
            case "ru":  return "ru-rRU";
            case "pt":  return "pt-rPT";
            case "ar":  return "ar-rSA";
            case "hi":  return "hi-rIN";
            case "it":  return "it-rIT";
            case "nl":  return "nl-rNL";
            case "pl":  return "pl-rPL";
            case "th":  return "th-rTH";
            case "tr":  return "tr-rTR";
            case "vi":  return "vi-rVN";
            default:    return code; // "en", "zh-rCN", "zh-rTW" stay as-is
        }
    }

    /**
     * Apply a locale code via AppCompatDelegate.
     * Pass "system" to follow the device locale.
     */
    private void applyLocaleCode(String code) {
        if (VALUE_SYSTEM.equals(code)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            // Convert resource-style code (e.g., "ja-rJP") to BCP 47 tag ("ja-JP")
            String bcp47Tag = code.replace("-r", "-");
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(bcp47Tag));
        }
    }

    /**
     * Get the currently saved locale code (e.g., "en", "zh-rCN", "system").
     */
    public String getLocale() {
        return currentLocale;
    }

    /**
     * Get the fully resolved locale code for the current setting.
     * For "system", resolves to the device's full locale code (e.g., "ja-rJP").
     * For explicit codes, returns the saved code as-is (e.g., "zh-rCN", "de-rDE").
     */
    public String getResolvedLocaleCode() {
        if (VALUE_SYSTEM.equals(currentLocale)) {
            return buildLocaleCode(Locale.getDefault());
        }
        return currentLocale;
    }

    /**
     * Build a locale code string (matching resource directory qualifiers) from a Locale object.
     * E.g., Locale("ja", "JP") -> "ja-rJP", Locale("en") -> "en"
     */
    private static String buildLocaleCode(Locale locale) {
        String lang = locale.getLanguage();
        String country = locale.getCountry();
        if (country != null && !country.isEmpty()) {
            return lang + "-r" + country;
        }
        return lang;
    }

    /**
     * Set the app locale, persist it, and apply it immediately.
     * Pass "system" to follow the system language.
     * Uses commit() (not apply()) so the value is persisted before setApplicationLocales
     * triggers activity recreation (avoids races).
     */
    public void setLocale(Context context, String localeCode) {
        Log.d(TAG, "setLocale() - called with localeCode: " + localeCode);
        Log.d(TAG, "setLocale() - previous currentLocale: " + currentLocale);
        currentLocale = localeCode;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean saved = prefs.edit().putString(KEY_LOCALE, localeCode).commit();
        Log.d(TAG, "setLocale() - saved to prefs: " + localeCode + " (success=" + saved + ")");
        // Apply immediately via AppCompatDelegate
        applyLocaleCode(localeCode);
    }

    /**
     * Get available locales for the language picker.
     * Returns an array of {code, displayName} pairs.
     * The codes must match the resource directory qualifiers (e.g., "ja-rJP" for values-ja-rJP).
     */
    public static String[][] getAvailableLocales() {
        return new String[][]{
            {"system", "System Default"},
            {"en", "English"},
            {"de-rDE", "Deutsch (German)"},
            {"zh-rCN", "简体中文 (Simplified Chinese)"},
            {"zh-rTW", "繁體中文 (Traditional Chinese)"},
            {"ja-rJP", "日本語 (Japanese)"},
            {"ko-rKR", "한국어 (Korean)"},
            {"es-rES", "Español (Spanish)"},
            {"fr-rFR", "Français (French)"},
            {"ru-rRU", "Русский (Russian)"},
            {"pt-rPT", "Português (Portuguese)"},
            {"ar-rSA", "العربية (Arabic)"},
            {"hi-rIN", "हिन्दी (Hindi)"},
            {"it-rIT", "Italiano (Italian)"},
            {"nl-rNL", "Nederlands (Dutch)"},
            {"pl-rPL", "Polski (Polish)"},
            {"th-rTH", "ไทย (Thai)"},
            {"tr-rTR", "Türkçe (Turkish)"},
            {"vi-rVN", "Tiếng Việt (Vietnamese)"}
        };
    }

    /**
     * Get display name for a locale code.
     */
    public static String getLocaleDisplayName(String localeCode) {
        String[][] locales = getAvailableLocales();
        for (String[] entry : locales) {
            if (entry[0].equals(localeCode)) {
                return entry[1];
            }
        }
        return localeCode;
    }

    /**
     * Find the index of the current locale in the available locales list.
     */
    public int getCurrentLocaleIndex() {
        String[][] locales = getAvailableLocales();
        for (int i = 0; i < locales.length; i++) {
            if (locales[i][0].equals(currentLocale)) {
                return i;
            }
        }
        return 0; // Default to "System Default"
    }
}
