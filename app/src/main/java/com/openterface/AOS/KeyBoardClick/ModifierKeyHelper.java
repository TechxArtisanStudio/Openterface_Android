/**
 * Helper class for modifier keys with long-press swipe-to-lock gesture.
 *
 * Behavior:
 *   - Press (ACTION_DOWN) = activate modifier, show blue
 *   - Release (ACTION_UP) = deactivate modifier, show white
 *   - Long press (500ms) + swipe up = lock modifier, show lock icon, stay blue
 *   - Tap again when locked = unlock, hide lock icon
 */
package com.openterface.AOS.KeyBoardClick;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.openterface.AOS.R;

public class ModifierKeyHelper {
    private static final long LONG_PRESS_MS = 500;
    private static final int SWIPE_DISTANCE = 40;

    public interface ModifierCallback {
        void onPress();
        void onRelease();
        void onLock();
        void onUnlock();
        boolean isLocked();
        boolean isPressed();
    }

    private final View view;
    private final ModifierCallback callback;
    private final Handler handler = new Handler();
    private boolean longPressFired = false;
    private float startRawY;

    public ModifierKeyHelper(View view, ModifierCallback callback) {
        this.view = view;
        this.callback = callback;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startRawY = event.getRawY();
                    longPressFired = false;
                    // If already locked, tap once to unlock
                    if (callback.isLocked()) {
                        callback.onUnlock();
                        hideLockIcon(v);
                        v.setBackgroundResource(R.drawable.nopress_button_background);
                        return true;
                    }
                    // Normal press
                    v.setBackgroundResource(R.drawable.press_button_background);
                    callback.onPress();
                    handler.postDelayed(() -> longPressFired = true, LONG_PRESS_MS);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Check if swiped up past threshold after long press
                    float dy = startRawY - event.getRawY();
                    if (longPressFired && dy > SWIPE_DISTANCE) {
                        callback.onLock();
                        showLockIcon(v);
                        longPressFired = false;
                        handler.removeCallbacksAndMessages(null);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacksAndMessages(null);
                    // Only release if not locked
                    if (!callback.isLocked()) {
                        v.setBackgroundResource(R.drawable.nopress_button_background);
                        callback.onRelease();
                    }
                    longPressFired = false;
                    return true;
            }
            return false;
        });
    }

    /**
     * Show lock icon above the button text.
     */
    private void showLockIcon(View v) {
        if (v instanceof Button) {
            Drawable lock = v.getContext().getDrawable(R.drawable.ic_lock);
            if (lock != null) {
                // left, top, right, bottom
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(null, lock, null, null);
            }
        }
    }

    /**
     * Hide lock icon above the button text.
     */
    private void hideLockIcon(View v) {
        if (v instanceof Button) {
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }
}
