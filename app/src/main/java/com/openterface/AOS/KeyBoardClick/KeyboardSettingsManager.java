package com.openterface.AOS.KeyBoardClick;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

/**
 * Manages keyboard settings including sound and vibration feedback
 */
public class KeyboardSettingsManager {
    private static final String TAG = "OP-KB";
    private static final String PREF_NAME = "keyboard_settings";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_SOUND_VOLUME = "sound_volume";
    private static final String KEY_VIBRATE_ENABLED = "vibrate_enabled";
    private static final String KEY_VIBRATE_STRENGTH = "vibrate_strength";

    private final Context context;
    private final SharedPreferences prefs;
    private final Vibrator vibrator;
    private ToneGenerator toneGenerator;

    public KeyboardSettingsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        int volume = getSoundVolume();
        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, volume);
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, false);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public int getSoundVolume() {
        return prefs.getInt(KEY_SOUND_VOLUME, 50);
    }

    public void setSoundVolume(int volume) {
        prefs.edit().putInt(KEY_SOUND_VOLUME, volume).apply();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, volume);
        }
    }

    public boolean isVibrateEnabled() {
        return prefs.getBoolean(KEY_VIBRATE_ENABLED, true);
    }

    public void setVibrateEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, enabled).apply();
    }

    public int getVibrateStrength() {
        return prefs.getInt(KEY_VIBRATE_STRENGTH, 50);
    }

    public void setVibrateStrength(int strength) {
        prefs.edit().putInt(KEY_VIBRATE_STRENGTH, strength).apply();
    }

    /**
     * Play key click sound if enabled
     */
    public void playKeySound() {
        if (isSoundEnabled() && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 50);
            } catch (Exception e) {
                Log.e(TAG, "Error playing key sound", e);
            }
        }
    }

    /**
     * Trigger vibration feedback if enabled
     */
    public void vibrate(android.view.View view) {
        if (isVibrateEnabled() && vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    int strength = getVibrateStrength();
                    long milliseconds = Math.max(10, strength / 5); // 10-20ms
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds,
                        VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(30);
                }

                // Also trigger haptic feedback
                if (view != null) {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error triggering vibration", e);
            }
        }
    }

    /**
     * Release resources
     */
    public void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
