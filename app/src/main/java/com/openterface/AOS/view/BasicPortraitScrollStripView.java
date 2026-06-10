/**
 * @Title: BasicPortraitScrollStripView
 * @Package com.openterface.AOS.view
 * @Description: Vertical scroll strip for touchpad with up/down buttons
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

import android.annotation.SuppressLint;
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

import com.openterface.AOS.R;
import com.openterface.AOS.target.MouseManager;

/**
 * A vertical scroll strip with up/down buttons for mouse wheel control.
 * Supports long-press for continuous scrolling with acceleration.
 */
public class BasicPortraitScrollStripView extends LinearLayout {

    private static final String TAG = "BasicPortraitScrollStripView";

    // Scroll acceleration constants
    private static final long INITIAL_SCROLL_DELAY = 100;  // ms
    private static final long MIN_SCROLL_DELAY = 30;       // ms
    private static final long ACCELERATION_STEP = 10;      // ms reduction per step

    private ImageView btnScrollUp;
    private ImageView btnScrollDown;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable scrollRunnable;
    private boolean isScrolling = false;
    private long scrollDelay = INITIAL_SCROLL_DELAY;
    private int scrollDirection = 0;  // 1 = up, -1 = down

    public interface OnScrollListener {
        void onScrollUp();
        void onScrollDown();
    }

    private OnScrollListener scrollListener;

    public BasicPortraitScrollStripView(Context context) {
        super(context);
        init(context);
    }

    public BasicPortraitScrollStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BasicPortraitScrollStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Create scroll up button
        btnScrollUp = createScrollButton(context, R.drawable.baseline_keyboard_arrow_up_24, 1);
        addView(btnScrollUp, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        // Create scroll down button
        btnScrollDown = createScrollButton(context, R.drawable.baseline_keyboard_arrow_down_24, -1);
        addView(btnScrollDown, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        applyBackgroundColor();
    }

    @SuppressLint("ClickableViewAccessibility")
    private ImageView createScrollButton(Context context, int iconRes, int direction) {
        ImageView button = new ImageView(context);
        button.setImageResource(iconRes);
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dpToPx(context, 8), dpToPx(context, 8), dpToPx(context, 8), dpToPx(context, 8));

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
        if (scrollListener != null) {
            if (direction > 0) {
                scrollListener.onScrollUp();
            } else {
                scrollListener.onScrollDown();
            }
        } else {
            // Default: use MouseManager to send scroll
            float scrollValue = direction > 0 ? 1.0f : -1.0f;
            MouseManager.handleTwoFingerPanSlideUpDown(scrollValue);
        }
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.scrollListener = listener;
    }

    private void applyBackgroundColor() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#2A2A2A"));
        drawable.setCornerRadius(dpToPx(getContext(), 4));
        setBackground(drawable);
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopContinuousScroll();
    }
}
