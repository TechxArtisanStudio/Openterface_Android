package com.openterface.AOS.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.openterface.AOS.R;

/**
 * Floating settings interface - pops up from bottom right, supports swipe left to close
 * Replaces the original bottom drawer style settings module
 */
public class SettingsFloatingFragment extends DialogFragment {

    private GestureDetector gestureDetector;
    private OnDismissListener onDismissListener;

    public interface OnDismissListener {
        void onSettingsDismissed();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings_floating, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup gesture detector (swipe left to close)
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Swipe left: X movement > 50px, and speed > 100px/s
                if (e1 != null && e1.getX() - e2.getX() > 50 && Math.abs(velocityX) > 100) {
                    dismissWithCallback();
                    return true;
                }
                return false;
            }
        });

        // Set touch listener for floating card
        View card = view.findViewById(R.id.settings_floating_card);
        if (card != null) {
            card.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }

        // Dismiss on overlay click
        View overlay = view.findViewById(R.id.settings_dim_overlay);
        if (overlay != null) {
            overlay.setOnClickListener(v -> dismissWithCallback());
        }

        // Close button
        View closeBtn = view.findViewById(R.id.btn_close_settings);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> dismissWithCallback());
        }

        // Load settings content
        loadSettingsContent(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    private void dismissWithCallback() {
        if (onDismissListener != null) {
            onDismissListener.onSettingsDismissed();
        }
        dismiss();
    }

    private void loadSettingsContent(View rootView) {
        LinearLayout container = rootView.findViewById(R.id.settings_content_container);
        if (container == null) return;

        // Clear container
        container.removeAllViews();

        // Load settings content from module_portrait_settings.xml
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View settingsContent = inflater.inflate(R.layout.module_portrait_settings_inner, container, false);
        container.addView(settingsContent);

        // Setup each settings item
        setupSettingsItems(settingsContent);
    }

    private void setupSettingsItems(View view) {
        // Let MainActivity set up specific items (via callback or direct call)
        if (getActivity() instanceof SettingsCallback) {
            ((SettingsCallback) getActivity()).setupSettingsContent(view);
        }
    }

    /**
     * Settings callback interface for MainActivity
     */
    public interface SettingsCallback {
        void setupSettingsContent(View view);
        boolean isSoundEnabled();
        int getSoundVolume();
        boolean isVibrateEnabled();
        int getVibrateIntensity();
        int getScrollSpeed();
        int getMouseSpeed();
        void setSoundEnabled(boolean enabled);
        void setSoundVolume(int volume);
        void setVibrateEnabled(boolean enabled);
        void setVibrateIntensity(int intensity);
        void setScrollSpeed(int speed);
        void setMouseSpeed(int speed);
    }
}