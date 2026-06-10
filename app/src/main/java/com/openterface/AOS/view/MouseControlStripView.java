/**
 * @Title: MouseControlStripView
 * @Package com.openterface.AOS.view
 * @Description: Mouse button strip with left, middle (scroll), right buttons
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
package com.openterface.AOS.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.openterface.AOS.R;
import com.openterface.AOS.target.HidManager;

/**
 * Enhanced mouse control strip with 5 zones:
 * [ScrollUp] [L] [M] [R] [ScrollDown]
 * - L/M/R: mouse button press/release
 * - ScrollUp/Down: scroll wheel with long-press acceleration
 */
public class MouseControlStripView extends LinearLayout {

    private static final String TAG = "MouseControlStripView";

    // Button mask constants
    public static final int BTN_LEFT = 0x01;
    public static final int BTN_RIGHT = 0x02;
    public static final int BTN_MIDDLE = 0x04;

    // Scroll acceleration constants
    private static final long INITIAL_SCROLL_DELAY = 120;  // ms
    private static final long MIN_SCROLL_DELAY = 30;       // ms
    private static final long ACCELERATION_STEP = 10;      // ms reduction per step

    private TextView btnLeft;
    private TextView btnMiddle;
    private TextView btnRight;
    private ImageView btnScrollUp;
    private ImageView btnScrollDown;

    private int currentOpacity = 100;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable releaseRunnable;
    private Runnable scrollRunnable;
    private boolean isScrolling = false;
    private long scrollDelay = INITIAL_SCROLL_DELAY;
    private int scrollDirection = 0;  // 1 = up, -1 = down

    // Track which buttons are currently pressed
    private boolean isLeftPressed = false;
    private boolean isRightPressed = false;
    private boolean isMiddlePressed = false;

    public interface OnMouseClickListener {
        void onMouseClick(int buttonMask);
        void onMouseRelease();
        void onScrollClick();
        void onScrollUp();
        void onScrollDown();
    }

    private OnMouseClickListener mouseClickListener;

    public MouseControlStripView(Context context) {
        super(context);
        init(context);
    }

    public MouseControlStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MouseControlStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // Create the five buttons programmatically
        createButtonLayout(context);
    }

    private void createButtonLayout(Context context) {
        removeAllViews();

        int buttonHeight = dpToPx(context, 52);
        int gap = dpToPx(context, 6);

        // Scroll Up button (weight 1)
        btnScrollUp = createScrollButton(context, R.drawable.baseline_keyboard_arrow_up_24, 1);
        LayoutParams scrollUpLp = new LayoutParams(0, buttonHeight, 0.8f);
        addView(btnScrollUp, scrollUpLp);

        // Left button (weight 2)
        btnLeft = createButton(context, "L", buttonHeight);
        btnLeft.setTag(BTN_LEFT);
        LayoutParams leftLp = new LayoutParams(0, buttonHeight, 2f);
        leftLp.setMarginStart(gap);
        addView(btnLeft, leftLp);

        // Middle button (weight 1)
        btnMiddle = createButton(context, "M", buttonHeight);
        btnMiddle.setTag(BTN_MIDDLE);
        LayoutParams middleLp = new LayoutParams(0, buttonHeight, 1f);
        middleLp.setMarginStart(gap);
        addView(btnMiddle, middleLp);

        // Right button (weight 2)
        btnRight = createButton(context, "R", buttonHeight);
        btnRight.setTag(BTN_RIGHT);
        LayoutParams rightLp = new LayoutParams(0, buttonHeight, 2f);
        rightLp.setMarginStart(gap);
        addView(btnRight, rightLp);

        // Scroll Down button (weight 1)
        btnScrollDown = createScrollButton(context, R.drawable.baseline_keyboard_arrow_down_24, -1);
        LayoutParams scrollDownLp = new LayoutParams(0, buttonHeight, 0.8f);
        scrollDownLp.setMarginStart(gap);
        addView(btnScrollDown, scrollDownLp);

        applyOpacity();
    }

    private TextView createButton(Context context, String label, int height) {
        TextView button = new TextView(context);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setGravity(android.view.Gravity.CENTER);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#333333"));
        drawable.setCornerRadius(dpToPx(context, 8));
        button.setBackground(drawable);
        button.setElevation(2f);

        button.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int btnMask = (int) v.getTag();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f);
                        setButtonPressed(btnMask, true);
                        // Send mouse button press
                        sendMouseButtonPress(btnMask);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1.0f);
                        setButtonPressed(btnMask, false);
                        // Send mouse button release
                        sendMouseRelease();
                        return true;
                }
                return false;
            }
        });

        return button;
    }

    private ImageView createScrollButton(Context context, int iconRes, int direction) {
        ImageView button = new ImageView(context);
        button.setImageResource(iconRes);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dpToPx(context, 4), dpToPx(context, 4), dpToPx(context, 4), dpToPx(context, 4));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#2A2A2A"));
        drawable.setCornerRadius(dpToPx(context, 8));
        button.setBackground(drawable);
        button.setElevation(2f);

        button.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f);
                        scrollDirection = direction;
                        scrollDelay = INITIAL_SCROLL_DELAY;
                        isScrolling = true;
                        // Send first scroll immediately
                        sendScrollEvent(direction);
                        // Start continuous scrolling
                        startContinuousScroll();
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1.0f);
                        stopContinuousScroll();
                        return true;
                }
                return false;
            }
        });

        return button;
    }

    private void startContinuousScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
        }

        scrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScrolling) {
                    sendScrollEvent(scrollDirection);
                    // Accelerate: reduce delay over time
                    scrollDelay = Math.max(MIN_SCROLL_DELAY, scrollDelay - ACCELERATION_STEP);
                    handler.postDelayed(this, scrollDelay);
                }
            }
        };

        handler.postDelayed(scrollRunnable, INITIAL_SCROLL_DELAY);
    }

    private void stopContinuousScroll() {
        isScrolling = false;
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
            scrollRunnable = null;
        }
    }

    private void sendScrollEvent(int direction) {
        if (mouseClickListener != null) {
            if (direction > 0) {
                mouseClickListener.onScrollUp();
            } else {
                mouseClickListener.onScrollDown();
            }
        } else {
            float scrollValue = direction > 0 ? 1.0f : -1.0f;
            HidManager.handleTwoFingerPanSlideUpDown(scrollValue);
        }
    }

    private void setButtonPressed(int mask, boolean pressed) {
        switch (mask) {
            case BTN_LEFT:
                isLeftPressed = pressed;
                break;
            case BTN_MIDDLE:
                isMiddlePressed = pressed;
                break;
            case BTN_RIGHT:
                isRightPressed = pressed;
                break;
        }
    }

    /**
     * Get the combined button mask for currently pressed buttons
     */
    public int getPressedButtonMask() {
        int mask = 0;
        if (isLeftPressed) mask |= BTN_LEFT;
        if (isRightPressed) mask |= BTN_RIGHT;
        if (isMiddlePressed) mask |= BTN_MIDDLE;
        return mask;
    }

    private void sendMouseButtonPress(int buttonMask) {
        if (mouseClickListener != null) {
            mouseClickListener.onMouseClick(buttonMask);
        } else {
            // Default: send absolute mouse click
            // Use the center of the screen as default position
            int x = HidManager.getScreenWidth() / 2;
            int y = HidManager.getScreenHeight() / 2;
            String clickType = getClickTypeString(buttonMask);
            HidManager.sendHexAbsButtonClickData(clickType, x, y);
        }
    }

    private void sendMouseRelease() {
        if (mouseClickListener != null) {
            mouseClickListener.onMouseRelease();
        } else {
            HidManager.releaseMSRelData();
        }
    }

    private String getClickTypeString(int mask) {
        switch (mask) {
            case BTN_LEFT:
                return "SecLeftData";
            case BTN_MIDDLE:
                return "SecMiddleData";
            case BTN_RIGHT:
                return "SecRightData";
            default:
                return "SecNullData";
        }
    }

    public void setOnMouseClickListener(OnMouseClickListener listener) {
        this.mouseClickListener = listener;
    }

    public void setOpacity(int opacity) {
        currentOpacity = Math.max(0, Math.min(100, opacity));
        applyOpacity();
    }

    public int getOpacity() {
        return currentOpacity;
    }

    private void applyOpacity() {
        float alpha = currentOpacity / 100f;
        setAlpha(alpha);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public TextView getBtnLeft() { return btnLeft; }
    public TextView getBtnMiddle() { return btnMiddle; }
    public TextView getBtnRight() { return btnRight; }
    public ImageView getBtnScrollUp() { return btnScrollUp; }
    public ImageView getBtnScrollDown() { return btnScrollDown; }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopContinuousScroll();
    }
}
