/**
 * @Title: TouchPadView
 * @Package com.openterface.AOS.view
 * @Description: TouchPad view for mouse control with gesture support
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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * TouchPadView — gesture mapping aligned with KeyCmd/iOS Touchpad:
 *
 *  1-finger pan      → mouse move
 *  single tap        → left click (delayed to distinguish double-tap)
 *  double tap        → double click
 *  long press        → drag start
 *  2-finger tap      → right click (only if no significant movement)
 *  2-finger pan      → scroll (natural direction, reduced sensitivity)
 */
public class TouchPadView extends View {

    private static final String TAG = "OP-MOUSE";

    // Thresholds — mirror KeyCmd defaults
    private static final long   TAP_DELAY_MS       = 150;   // wait before confirming single tap
    private static final long   TAP_DURATION_MAX_MS = 400;  // finger must lift within this time
    private static final float  TAP_MOVE_THRESHOLD  = 10f;  // px — movement cancels tap
    private static final float  TWO_FINGER_TAP_MOVE_THRESHOLD = 12f;

    public interface OnTouchPadListener {
        void onTouchMove(float startX, float startY, float lastX, float lastY);
        void onTouchClick();
        void onTouchDoubleClick();
        void onTouchRightClick();
        default void onTouchRelease() {}
        default void onTouchLongPress() {}
    }

    private OnTouchPadListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * When false, the pad still sends pointer moves and two-finger scroll, but does not emit
     * single-tap, double-tap, long-press (drag), or two-finger right-click.
     */
    private boolean padClickDragGesturesEnabled = true;

    // 1-finger move state
    private float lastMoveX, lastMoveY;
    private boolean isDragging = false;

    // Tap detection state
    private float tapDownX, tapDownY;
    private long  tapDownTime;
    private boolean tapCancelled = false;
    private boolean suppressSingleTapFromDoubleTap = false;
    private Runnable pendingSingleTap = null;

    // 2-finger scroll state
    private float twoFingerPrevX, twoFingerPrevY;
    private float twoFingerScrollAccumX = 0f;
    private float twoFingerScrollAccumY = 0f;
    private float twoFingerDownCenterX, twoFingerDownCenterY;
    private boolean twoFingerMoved = false;
    private boolean isTwoFingerScrolling = false;

    private final GestureDetector gestureDetector;

    public TouchPadView(Context context) {
        super(context);
        gestureDetector = buildGestureDetector(context);
        setClickable(true);
    }

    public TouchPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = buildGestureDetector(context);
        setClickable(true);
    }

    private GestureDetector buildGestureDetector(Context context) {
        return new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                cancelPendingSingleTap();
                suppressSingleTapFromDoubleTap = true;
                if (padClickDragGesturesEnabled && listener != null) {
                    listener.onTouchDoubleClick();
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (e.getPointerCount() == 1) {
                    cancelPendingSingleTap();
                    if (padClickDragGesturesEnabled && listener != null) {
                        listener.onTouchLongPress();
                    }
                }
            }
        });
    }

    public void setPadClickDragGesturesEnabled(boolean enabled) {
        if (padClickDragGesturesEnabled == enabled) return;
        padClickDragGesturesEnabled = enabled;
        cancelPendingSingleTap();
        suppressSingleTapFromDoubleTap = false;
    }

    public boolean isPadClickDragGesturesEnabled() {
        return padClickDragGesturesEnabled;
    }

    public void setOnTouchPadListener(OnTouchPadListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();

        // ---- 2-finger gestures ----
        if (pointerCount == 2) {
            cancelPendingSingleTap();
            isDragging = false;

            switch (action) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    twoFingerDownCenterX = (event.getX(0) + event.getX(1)) / 2f;
                    twoFingerDownCenterY = (event.getY(0) + event.getY(1)) / 2f;
                    twoFingerPrevX = twoFingerDownCenterX;
                    twoFingerPrevY = twoFingerDownCenterY;
                    twoFingerScrollAccumX = 0f;
                    twoFingerScrollAccumY = 0f;
                    twoFingerMoved = false;
                    isTwoFingerScrolling = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float cx = (event.getX(0) + event.getX(1)) / 2f;
                    float cy = (event.getY(0) + event.getY(1)) / 2f;
                    float totalDist = dist(cx, cy, twoFingerDownCenterX, twoFingerDownCenterY);
                    if (totalDist > TWO_FINGER_TAP_MOVE_THRESHOLD) {
                        twoFingerMoved = true;
                    }

                    float dx = cx - twoFingerPrevX;
                    float dy = cy - twoFingerPrevY;
                    if (Math.abs(dx) > 0f || Math.abs(dy) > 0f) {
                        isTwoFingerScrolling = true;
                    }

                    // Accumulate scroll values (KeyCmd approach)
                    twoFingerScrollAccumX += dx / 3f;
                    twoFingerScrollAccumY += -dy / 3f;

                    int scrollX = (int) twoFingerScrollAccumX;
                    int scrollY = (int) twoFingerScrollAccumY;

                    if (scrollX != 0) {
                        twoFingerScrollAccumX -= scrollX;
                    }
                    if (scrollY != 0) {
                        twoFingerScrollAccumY -= scrollY;
                    }

                    if (listener != null && (scrollX != 0 || scrollY != 0)) {
                        listener.onTouchMove(scrollX, scrollY, 0, 0);
                    }
                    twoFingerPrevX = cx;
                    twoFingerPrevY = cy;
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    // Only trigger right-click if there was no meaningful movement AND no scrolling
                    if (padClickDragGesturesEnabled && !twoFingerMoved && !isTwoFingerScrolling) {
                        Log.d(TAG, "Two-finger tap detected -> right click");
                        if (listener != null) {
                            listener.onTouchRightClick();
                        }
                    } else {
                        Log.d(TAG, "Two-finger gesture ended: moved=" + twoFingerMoved + " scrolled=" + isTwoFingerScrolling);
                    }
                    if (listener != null) listener.onTouchRelease();
                    twoFingerScrollAccumX = 0f;
                    twoFingerScrollAccumY = 0f;
                    twoFingerMoved = false;
                    isTwoFingerScrolling = false;
                    break;
            }
            return true;
        }

        twoFingerMoved = false;
        isTwoFingerScrolling = false;

        // ---- 1-finger gestures ----
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastMoveX = x;
                lastMoveY = y;
                tapDownX = x;
                tapDownY = y;
                tapDownTime = event.getEventTime();
                tapCancelled = false;
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float distMoved = dist(x, y, tapDownX, tapDownY);
                if (distMoved > TAP_MOVE_THRESHOLD) {
                    cancelPendingSingleTap();
                    tapCancelled = true;
                    isDragging = true;
                }
                if (listener != null) {
                    listener.onTouchMove(x, y, lastMoveX, lastMoveY);
                }
                lastMoveX = x;
                lastMoveY = y;
                break;

            case MotionEvent.ACTION_UP:
                long duration = event.getEventTime() - tapDownTime;
                float distLifted = dist(x, y, tapDownX, tapDownY);
                if (suppressSingleTapFromDoubleTap) {
                    suppressSingleTapFromDoubleTap = false;
                    if (listener != null) listener.onTouchRelease();
                    lastMoveX = 0;
                    lastMoveY = 0;
                    isDragging = false;
                    break;
                }
                boolean validTap = !tapCancelled
                        && duration < TAP_DURATION_MAX_MS
                        && distLifted <= TAP_MOVE_THRESHOLD;

                if (validTap && padClickDragGesturesEnabled) {
                    Log.d(TAG, "Single tap detected -> scheduling click");
                    pendingSingleTap = () -> {
                        if (listener != null) listener.onTouchClick();
                        pendingSingleTap = null;
                    };
                    mainHandler.postDelayed(pendingSingleTap, TAP_DELAY_MS);
                }

                if (listener != null) listener.onTouchRelease();

                lastMoveX = 0;
                lastMoveY = 0;
                isDragging = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                cancelPendingSingleTap();
                if (listener != null) listener.onTouchRelease();
                isDragging = false;
                break;
        }
        return true;
    }

    private void cancelPendingSingleTap() {
        if (pendingSingleTap != null) {
            mainHandler.removeCallbacks(pendingSingleTap);
            pendingSingleTap = null;
        }
    }

    // Settings accessors for TouchPadSettings dialog
    public void setTapDelayMs(long delayMs) {
        // Note: TAP_DELAY_MS is final, this is for API compatibility
    }

    public long getTapDelayMs() {
        return TAP_DELAY_MS;
    }

    public void setScrollSensitivity(float sensitivity) {
        // Note: Scroll sensitivity is fixed, this is for API compatibility
    }

    public float getScrollSensitivity() {
        return 1.0f;
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
