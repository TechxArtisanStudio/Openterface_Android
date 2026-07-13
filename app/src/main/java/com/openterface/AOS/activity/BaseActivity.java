package com.openterface.AOS.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.openterface.AOS.manager.LocaleManager;

import java.util.Locale;

/**
 * Base activity for all activities in the app.
 * Applies locale via TWO mechanisms for maximum reliability:
 * 1. attachBaseContext() + createConfigurationContext() — sets locale before Activity creation
 * 2. AppCompatDelegate.setApplicationLocales() — AndroidX standard for API 21+
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        LocaleManager localeManager = LocaleManager.getInstance();
        localeManager.initialize(newBase);

        String localeCode = localeManager.getLocale();
        Log.d(TAG, "attachBaseContext() - localeCode: " + localeCode);

        if (!"system".equals(localeCode)) {
            Locale locale = parseLocaleCode(localeCode);
            Configuration config = new Configuration(newBase.getResources().getConfiguration());
            config.setLocale(locale);

            // Create a new context with the locale applied
            // This MUST be done BEFORE super.attachBaseContext() so the Activity
            // is initialized with the correct locale from the start
            Context localeContext = newBase.createConfigurationContext(config);
            Log.d(TAG, "attachBaseContext() - wrapping context with locale: " + locale);
            super.attachBaseContext(localeContext);
        } else {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply locale via AppCompatDelegate as a second safety net
        // This ensures API 33+ per-app locale support and handles edge cases
        LocaleManager localeManager = LocaleManager.getInstance();
        String localeCode = localeManager.getLocale();
        if (!"system".equals(localeCode)) {
            String bcp47Tag = localeCode.replace("-r", "-");
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(bcp47Tag));
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }

        super.onCreate(savedInstanceState);

        // Diagnostic logging
        Configuration currentConfig = getResources().getConfiguration();
        Log.d(TAG, "onCreate() - Activity locale: " + currentConfig.locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "onCreate() - Activity locales: " + currentConfig.getLocales());
        }
    }

    /**
     * Parse locale code string to Locale object.
     * E.g., "ja-rJP" -> Locale("ja", "JP"), "en" -> Locale("en")
     */
    private Locale parseLocaleCode(String localeCode) {
        if ("system".equals(localeCode)) {
            return Locale.getDefault();
        }

        String[] parts = localeCode.split("-r");
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0]);
        }
    }
}
