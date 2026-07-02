/**
 * @Title: ZoomableVideoContainer
 * @Package com.openterface.AOS.view
 * @Description: A custom container that supports pinch-to-zoom for video preview in portrait mode.
 *              Mouse events are passed through to child views for normal operation.
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
 *    General Public License for more details.                              *
 *                                                                            *
 *    You should have received a copy of the GNU General Public License       *
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                            *
 * ========================================================================== *
 */
package com.openterface.AOS.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A container that supports pinch-to-zoom gestures for video preview.
 * Single-finger events are always passed to child views for mouse control.
 * Two-finger pinch events are handled by this container for zooming.
 */
public class ZoomableVideoContainer extends FrameLayout {
    private static final String TAG = "OP-UI";
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;

    private ScaleGestureDetector mScaleGestureDetector;
    private View mZoomableView;

    private float mScaleFactor = 1.0f;
    private float mTranslateX = 0f;
    private float mTranslateY = 0f;

    // State flags
    private boolean mShouldIntercept = false;  // Set true when we detect a pinch

    public ZoomableVideoContainer(Context context) {
        super(context);
        init(context);
    }

    public ZoomableVideoContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableVideoContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * Set the view that should be zoomed
     */
    public void setZoomableView(View view) {
        mZoomableView = view;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Always feed events to scale detector so it can detect pinch
        mScaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        // Determine if we should intercept based on gesture type
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Single finger - don't intercept (let children handle for mouse)
                mShouldIntercept = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger down - check if this is a pinch gesture
                if (mScaleGestureDetector.isInProgress() || pointerCount >= 2) {
                    mShouldIntercept = true;
                    Log.d(TAG, "Intercepting: second finger down, pinch detected");
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // If scale detector says we're pinching, intercept
                if (mScaleGestureDetector.isInProgress()) {
                    if (!mShouldIntercept) {
                        mShouldIntercept = true;
                        Log.d(TAG, "Intercepting: scale in progress during MOVE");
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset intercept state when all fingers lifted
                if (pointerCount <= 1) {
                    mShouldIntercept = false;
                }
                break;
        }

        // Default: don't intercept, pass to children
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Feed to scale detector
        mScaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();

        // If we intercepted for zoom, handle it
        if (mShouldIntercept) {
            // Handle pan during zoom if scale > 1
            if (mScaleFactor > 1.0f && event.getPointerCount() >= 2) {
                // Pan handling will be done in scale listener via focus points
            }

            // Reset when all fingers lifted
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (event.getPointerCount() <= 1) {
                    mShouldIntercept = false;
                }
            }

            return true;
        }

        // If we didn't intercept, don't consume the event
        return false;
    }

    /**
     * Apply the current scale and translation to the zoomable view
     */
    private void applyTransform() {
        if (mZoomableView != null) {
            mZoomableView.setScaleX(mScaleFactor);
            mZoomableView.setScaleY(mScaleFactor);
            mZoomableView.setTranslationX(mTranslateX);
            mZoomableView.setTranslationY(mTranslateY);
        }
    }

    /**
     * Constrain translation so the view doesn't move outside the container
     */
    private void constrainTranslation() {
        if (mZoomableView == null) return;

        int viewWidth = mZoomableView.getWidth();
        int viewHeight = mZoomableView.getHeight();
        int containerWidth = getWidth();
        int containerHeight = getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        // Calculate the maximum allowed translation
        float maxTranslateX = (viewWidth * mScaleFactor - containerWidth) / 2f;
        float maxTranslateY = (viewHeight * mScaleFactor - containerHeight) / 2f;

        // Constrain X translation
        if (maxTranslateX > 0) {
            mTranslateX = Math.max(-maxTranslateX, Math.min(maxTranslateX, mTranslateX));
        } else {
            mTranslateX = 0f;
        }

        // Constrain Y translation
        if (maxTranslateY > 0) {
            mTranslateY = Math.max(-maxTranslateY, Math.min(maxTranslateY, mTranslateY));
        } else {
            mTranslateY = 0f;
        }
    }

    /**
     * Reset zoom to default state
     */
    public void resetZoom() {
        mScaleFactor = 1.0f;
        mTranslateX = 0f;
        mTranslateY = 0f;
        mShouldIntercept = false;
        applyTransform();
    }

    /**
     * Get current scale factor
     */
    public float getScaleFactor() {
        return mScaleFactor;
    }

    private float mLastFocusX;
    private float mLastFocusY;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mLastFocusX = detector.getFocusX();
            mLastFocusY = detector.getFocusY();
            mShouldIntercept = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = mScaleFactor * scaleFactor;

            // Constrain scale
            if (newScale < MIN_SCALE) {
                newScale = MIN_SCALE;
            } else if (newScale > MAX_SCALE) {
                newScale = MAX_SCALE;
            }

            mScaleFactor = newScale;

            // Constrain translation
            constrainTranslation();
            applyTransform();

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Keep mShouldIntercept true until ACTION_UP/CANCEL
        }
    }
}
