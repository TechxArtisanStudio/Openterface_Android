package com.openterface.AOS.serial;

import android.util.Log;
import android.view.View;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.MouseManager;

/**
 * Coordinate conversion utilities.
 * Handles all coordinate transformations between:
 * - Screen coordinates (Android system)
 * - View coordinates (TextureView)
 * - Content coordinates (viewport-aware)
 * - HID coordinates (0-4095 for CH9329)
 *
 * This is a pure utility class - no Android dependencies beyond basic types.
 */
public class CoordinateConverter {
    private static final String TAG = "OP-COORD";

    // ===== Activity reference for view lookups =====
    private static MainActivity activity;
    private static boolean isPortraitMode = false;

    // ===== Portrait mode zoom/pan state =====
    // Single source of truth is ZoomPanManager: the accessors below delegate to it so
    // coordinate mapping can never drift away from the transform actually applied.

    // ===== BT Mouse learned range =====
    private static float learnedMinX = Float.MAX_VALUE;
    private static float learnedMaxX = Float.MIN_VALUE;
    private static float learnedMinY = Float.MAX_VALUE;
    private static float learnedMaxY = Float.MIN_VALUE;
    private static boolean rangeLearned = false;

    // Track view size to detect when it changes (triggers learned range reset)
    private static float lastViewWidth = 0;
    private static float lastViewHeight = 0;

    // CH9329 protocol uses 12-bit coordinates (0..4095) for absolute mode
    public static final int HID_LOGICAL_MAX = 4095;

    private CoordinateConverter() {} // Utility class

    // ===== Initialization =====

    /**
     * Initialize with activity reference for view lookups.
     */
    public static void init(MainActivity mainActivity, boolean portrait) {
        activity = mainActivity;
        isPortraitMode = portrait;
    }

    /**
     * Set portrait mode state.
     */
    public static void setPortraitMode(boolean portrait) {
        isPortraitMode = portrait;
    }

    /**
     * Check if device is in portrait mode.
     */
    public static boolean isPortraitMode() {
        return isPortraitMode;
    }

    /**
     * Get the touch target view width (main preview TextureView).
     */
    public static float getTouchViewWidth() {
        if (activity != null && activity.mBinding != null
                && activity.mBinding.viewMainPreview != null) {
            float w = activity.mBinding.viewMainPreview.getWidth();
            if (w > 0) return w;
        }
        return MouseManager.screenWidth;
    }

    /**
     * Get the touch target view height (main preview TextureView).
     */
    public static float getTouchViewHeight() {
        if (activity != null && activity.mBinding != null
                && activity.mBinding.viewMainPreview != null) {
            float h = activity.mBinding.viewMainPreview.getHeight();
            if (h > 0) return h;
        }
        return MouseManager.screenHeight;
    }

    // ===== Portrait zoom/pan getters/setters =====

    public static float getPortraitZoomScale() {
        return ZoomPanManager.getPortraitZoomScale();
    }

    public static void setPortraitPan(float translateX, float translateY) {
        ZoomPanManager.setPortraitPan(translateX, translateY);
    }

    public static void setPortraitZoomScale(float scale) {
        ZoomPanManager.setPortraitZoomScale(scale);
    }

    public static float getPortraitTranslateX() {
        return ZoomPanManager.getPortraitTranslateX();
    }

    public static float getPortraitTranslateY() {
        return ZoomPanManager.getPortraitTranslateY();
    }

    public static void resetPortraitZoom() {
        ZoomPanManager.reset();
    }

    // ===== Video rect calculation =====

    /**
     * Get the video rectangle within the view.
     * Returns [offsetX, offsetY, videoWidth, videoHeight] in view-local coordinates.
     */
    public static float[] getVideoRectInView(float viewWidth, float viewHeight,
                                              float bufferWidth, float bufferHeight) {
        float offsetX = 0, offsetY = 0;
        float videoW = viewWidth, videoH = viewHeight;

        if (bufferWidth > 0 && bufferHeight > 0) {
            // Universal formula: fit video within view using min scale
            float minScale = Math.min(viewWidth / bufferWidth, viewHeight / bufferHeight);
            videoW = bufferWidth * minScale;
            videoH = bufferHeight * minScale;
            offsetX = (viewWidth - videoW) / 2f;
            offsetY = (viewHeight - videoH) / 2f;
        }

        return new float[]{offsetX, offsetY, videoW, videoH};
    }

    /**
     * Get full video rect (same as getVideoRectInView, kept for compatibility).
     */
    public static float[] getFullVideoRect(float viewW, float viewH,
                                            float bufferWidth, float bufferHeight) {
        return getVideoRectInView(viewW, viewH, bufferWidth, bufferHeight);
    }

    /**
     * Get visible rect (same as getVideoRectInView, kept for compatibility).
     */
    public static float[] getVisibleRect(float viewW, float viewH,
                                          float bufferWidth, float bufferHeight) {
        return getVideoRectInView(viewW, viewH, bufferWidth, bufferHeight);
    }

    // ===== Coordinate conversion =====

    /**
     * Convert screen coordinates to view-local coordinates.
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @param textureView The TextureView to map coordinates to
     * @return View-local coordinates [x, y]
     */
    public static float[] toViewCoords(float screenX, float screenY,
                                        com.serenegiant.widget.AspectRatioTextureView textureView) {
        if (textureView != null) {
            int[] loc = new int[2];
            textureView.getLocationOnScreen(loc);
            return new float[]{screenX - loc[0], screenY - loc[1]};
        }
        return new float[]{screenX, screenY};
    }

    /**
     * Convert touch coordinates to content coordinates (viewport-aware).
     * Used by finger touch in absolute mode.
     *
     * @param touchX Touch X in view coordinates
     * @param touchY Touch Y in view coordinates
     * @param viewWidth View width
     * @param viewHeight View height
     * @param isPortrait Whether device is in portrait mode
     * @param landscapeDrawerScaleX DrawerLayout scaleX in landscape mode (1.0 if not zoomed)
     * @param landscapeScrollX DrawerLayout scrollX in landscape mode
     * @param landscapeScrollY DrawerLayout scrollY in landscape mode
     * @return Content coordinates in [0, viewWidth] x [0, viewHeight]
     */
    public static float[] mapToContentCoords(float touchX, float touchY,
                                              float viewWidth, float viewHeight,
                                              boolean isPortrait,
                                              float landscapeDrawerScaleX,
                                              float landscapeScrollX,
                                              float landscapeScrollY) {
        float contentX = touchX;
        float contentY = touchY;

        // Apply inverse viewport transform (undo zoom/pan)
        if (isPortrait) {
            // View matrix: viewX = (contentX - pivotX) * zoomScale + pivotX + translateX
            // Inverse: contentX = (viewX - translateX - pivotX) / zoomScale + pivotX
            float zoomScale = Math.max(1.0f, ZoomPanManager.getPortraitZoomScale());
            if (zoomScale > 1.0f) {
                float translateX = ZoomPanManager.getPortraitTranslateX();
                float translateY = ZoomPanManager.getPortraitTranslateY();
                float pivotX = viewWidth / 2f;
                float pivotY = viewHeight / 2f;
                contentX = (touchX - translateX - pivotX) / zoomScale + pivotX;
                contentY = (touchY - translateY - pivotY) / zoomScale + pivotY;
            }
        } else {
            // Landscape mode: drawerLayout uses scaleX (pivot at 0,0) + scrollTo
            float scaleX = Math.max(1.0f, landscapeDrawerScaleX);
            if (scaleX > 1.0f) {
                contentX = (touchX + landscapeScrollX) / scaleX;
                contentY = (touchY + landscapeScrollY) / scaleX;
            }
        }

        // Clamp to valid content range
        contentX = Math.max(0, Math.min(contentX, viewWidth));
        contentY = Math.max(0, Math.min(contentY, viewHeight));

        return new float[]{contentX, contentY};
    }

    /**
     * Normalize BT mouse view coordinates to HID absolute coordinates [0, 4095].
     * Uses the VIDEO RECT for mapping so that:
     * - Video area maps exactly to [0, 4095] on target computer
     * - Black bars are clamped to edges (cursor stops at boundary)
     *
     * @param viewX View X coordinate
     * @param viewY View Y coordinate
     * @param viewWidth View width
     * @param viewHeight View height
     * @param videoRect Video rect from getVideoRectInView() [offsetX, offsetY, videoW, videoH]
     * @return [hidX, hidY] in range [0, 4095]
     */
    public static float[] normalizeBtMouseToHid(float viewX, float viewY,
                                                  float viewWidth, float viewHeight,
                                                  float[] videoRect) {
        // Use video rect for mapping (not full view)
        // videoRect = [offsetX, offsetY, videoWidth, videoHeight]
        float videoOffsetX = videoRect[0];
        float videoOffsetY = videoRect[1];
        float videoWidth = videoRect[2];
        float videoHeight = videoRect[3];

        // Safety check: prevent division by zero
        if (videoWidth <= 0 || videoHeight <= 0) {
            // Fallback to view dimensions
            videoWidth = viewWidth;
            videoHeight = viewHeight;
            videoOffsetX = 0;
            videoOffsetY = 0;
        }

        // Normalize relative to video area
        // Coordinates within video: [0, 1] → [0, 4095]
        // Coordinates in black bars: <0 or >1 → clamped to 0 or 4095
        float normX = (viewX - videoOffsetX) / videoWidth;
        float normY = (viewY - videoOffsetY) / videoHeight;

        // Clamp to [0, 1] - this clamps black bars to edges
        normX = Math.max(0f, Math.min(1f, normX));
        normY = Math.max(0f, Math.min(1f, normY));

        float hidX = normX * HID_LOGICAL_MAX;
        float hidY = normY * HID_LOGICAL_MAX;

        // Debug log
        android.util.Log.d("OP-COORD", "normalizeBtMouseToHid: view=(" + viewX + "," + viewY
                + ") videoRect=[" + videoOffsetX + "," + videoOffsetY + "," + videoWidth + "," + videoHeight + "]"
                + " norm=(" + normX + "," + normY + ")"
                + " hid=(" + hidX + "," + hidY + ")");

        return new float[]{hidX, hidY};
    }

    /**
     * Update learned cursor reachable range.
     * Called on each BT mouse move to track min/max positions.
     * Auto-resets when view size changes (e.g., aspect ratio adjustment).
     */
    public static void updateLearnedRange(float cursorX, float cursorY, float viewW, float viewH) {
        // Detect view size change → reset learned range
        if (viewW != lastViewWidth || viewH != lastViewHeight) {
            if (lastViewWidth > 0 && lastViewHeight > 0) {
                Log.d(TAG, "View size changed: " + lastViewWidth + "x" + lastViewHeight
                        + " -> " + viewW + "x" + viewH + ", resetting learned range");
            }
            resetLearnedRange();
            lastViewWidth = viewW;
            lastViewHeight = viewH;
        }

        // Sanity check: only learn positions within view bounds
        if (cursorX >= 0 && cursorX <= viewW) {
            if (cursorX < learnedMinX) {
                learnedMinX = cursorX;
                rangeLearned = true;
                Log.d(TAG, "Range learned: minX=" + learnedMinX);
            }
            if (cursorX > learnedMaxX) {
                learnedMaxX = cursorX;
                rangeLearned = true;
                Log.d(TAG, "Range learned: maxX=" + learnedMaxX);
            }
        }
        if (cursorY >= 0 && cursorY <= viewH) {
            if (cursorY < learnedMinY) {
                learnedMinY = cursorY;
            }
            if (cursorY > learnedMaxY) {
                learnedMaxY = cursorY;
            }
        }
    }

    /**
     * Reset learned range (e.g., on orientation change).
     */
    public static void resetLearnedRange() {
        learnedMinX = Float.MAX_VALUE;
        learnedMaxX = Float.MIN_VALUE;
        learnedMinY = Float.MAX_VALUE;
        learnedMaxY = Float.MIN_VALUE;
        rangeLearned = false;
        lastViewWidth = 0;
        lastViewHeight = 0;
    }

    /**
     * Check if range has been learned.
     */
    public static boolean isRangeLearned() {
        return rangeLearned;
    }
}
