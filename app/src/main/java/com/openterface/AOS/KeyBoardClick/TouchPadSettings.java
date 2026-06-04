/**
 * @Title: TouchPadSettings
 * @Package com.openterface.AOS.KeyBoardClick
 * @Description: Settings dialog for TouchPad mouse sensitivity
 * ========================================================================== *
 *                                                                            *
 *    This file is part of the Openterface Mini KVM App Android version       *
 *                                                                            *
 *    Copyright (C) 2024   <info@openterface.com>                             *
 *                                                                            *
 *    This program is free software: you can redistribute it and/or modify    *
 *    it under the terms of the GNU General Public License as published by    *
 *    the Free Software Foundation version 3.                                 *
 *                                                                            *
 *    This program is distributed in the hope that it will be useful, but     *
 *    WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU        *
 *    General Public License for more details.                                *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.    *
 *                                                                            *
 * ========================================================================== *
 */
package com.openterface.AOS.KeyBoardClick;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.openterface.AOS.R;
import com.openterface.AOS.view.TouchPadView;

/**
 * Settings dialog for TouchPad mouse sensitivity.
 */
public class TouchPadSettings {

    private final Context context;
    private TouchPadView touchPadView;

    private static final long DEFAULT_TAP_DELAY_MS = 80;
    private static final float DEFAULT_SCROLL_SENSITIVITY = 1.0f;

    public TouchPadSettings(Context context) {
        this.context = context;
    }

    public void setTouchPadView(TouchPadView touchPadView) {
        this.touchPadView = touchPadView;
    }

    public void showSettingsDialog() {
        View dialogView = View.inflate(context, R.layout.dialog_touchpad_settings, null);

        // Tap Response Speed (0-200ms, inverted display: lower = faster)
        SeekBar tapDelaySeekBar = dialogView.findViewById(R.id.tap_delay_seekbar);
        TextView tapDelayValue = dialogView.findViewById(R.id.tap_delay_value);
        if (tapDelaySeekBar != null) {
            tapDelaySeekBar.setMax(200);
            tapDelaySeekBar.setProgress((int) (200 - getTapDelayMs()));
            updateTapDelayText(tapDelayValue, getTapDelayMs());
            tapDelaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    long delay = 200 - progress;
                    updateTapDelayText(tapDelayValue, delay);
                    if (touchPadView != null && fromUser) {
                        touchPadView.setTapDelayMs(delay);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Scroll Sensitivity (50-200%, 0.5-2.0)
        SeekBar scrollSensitivitySeekBar = dialogView.findViewById(R.id.scroll_sensitivity_seekbar);
        TextView scrollSensitivityValue = dialogView.findViewById(R.id.scroll_sensitivity_value);
        if (scrollSensitivitySeekBar != null) {
            scrollSensitivitySeekBar.setMax(150);
            scrollSensitivitySeekBar.setProgress((int) (getScrollSensitivity() * 100 - 50));
            updateScrollSensitivityText(scrollSensitivityValue, getScrollSensitivity());
            scrollSensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float sensitivity = (50 + progress) / 100f;
                    updateScrollSensitivityText(scrollSensitivityValue, sensitivity);
                    if (touchPadView != null && fromUser) {
                        touchPadView.setScrollSensitivity(sensitivity);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Reset button
        android.widget.Button resetButton = dialogView.findViewById(R.id.reset_defaults_button);
        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                if (tapDelaySeekBar != null) tapDelaySeekBar.setProgress(200 - (int) DEFAULT_TAP_DELAY_MS);
                if (scrollSensitivitySeekBar != null) scrollSensitivitySeekBar.setProgress((int) (DEFAULT_SCROLL_SENSITIVITY * 100 - 50));
                if (touchPadView != null) {
                    touchPadView.setTapDelayMs(DEFAULT_TAP_DELAY_MS);
                    touchPadView.setScrollSensitivity(DEFAULT_SCROLL_SENSITIVITY);
                }
            });
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.touchpad_settings_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok_button, null)
                .show();
    }

    private long getTapDelayMs() {
        return touchPadView != null ? touchPadView.getTapDelayMs() : DEFAULT_TAP_DELAY_MS;
    }

    private float getScrollSensitivity() {
        return touchPadView != null ? touchPadView.getScrollSensitivity() : DEFAULT_SCROLL_SENSITIVITY;
    }

    private void updateTapDelayText(TextView textView, long delayMs) {
        if (textView == null) return;
        String speed;
        if (delayMs <= 40) speed = "极快";
        else if (delayMs <= 80) speed = "快";
        else if (delayMs <= 120) speed = "中等";
        else if (delayMs <= 160) speed = "慢";
        else speed = "极慢";
        textView.setText(speed + " (" + delayMs + "ms)");
    }

    private void updateScrollSensitivityText(TextView textView, float sensitivity) {
        if (textView == null) return;
        int percent = (int) (sensitivity * 100);
        textView.setText(percent + "%");
    }
}