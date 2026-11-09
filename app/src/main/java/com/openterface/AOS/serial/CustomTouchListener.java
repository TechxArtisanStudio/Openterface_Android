/**
 * @Title: CustomTouchListener
 * @Package com.openterface.AOS.serial
 * @Description:
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
package com.openterface.AOS.serial;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.HidManager;

/**
 * CustomTouchListener — thin facade that delegates to specialized handlers.
 *
 * Architecture:
 * - BtMouseInputHandler: handles Bluetooth mouse input (move, click, drag, connection)
 * - TouchInputHandler: handles finger touch gestures (tap, double-tap, drag, two-finger)
 * - CoordinateConverter: coordinate conversion utilities (view → content → HID)
 * - ZoomPanManager: zoom/pan state management for portrait and landscape modes
 *
 * This class retains:
 * - Public API (onTouch, handleGenericMotionEvent, static methods for MainActivity)
 * - Black border overlay management (show/hide, video rect update)
 * - System cursor management (transparent pointer icon for boundary removal)
 *
 * Gesture mapping (aligned with KeyCmd TouchPadView):
 *   1-finger tap        → left click
 *   1-finger double tap → double left click
 *   1-finger drag       → mouse move
 *   2-finger tap        → right click
 *   2-finger drag       → scroll
 */
public class CustomTouchListener implements View.OnTouchListener {

    private static final String TAG = "OP-MOUSE";

    // ===== Static references =====
    private static UsbDeviceManager usbDeviceManager;
    private static MainActivity activity;

    // ===== Instance handlers =====
    private final TouchInputHandler touchHandler;

    // ===== UI elements =====
    private final TextView floatingLabel;

    // ===== Portrait mode detection =====
    private static boolean isPortraitMode;

    // ===== Transparent PointerIcon =====
    // Cached 1x1 transparent PointerIcon - eliminates system cursor boundary constraint
    // Default arrow is ~136px wide, causing left/right edge offset of 63-73px
    private static android.view.PointerIcon sTransparentPointerIcon;

    // Track if cursor is in black border (for dynamic visibility)
    private static boolean wasCursorInBlackBorder = true;

    // ===== Constructor =====

    public CustomTouchListener(MainActivity activity, UsbDeviceManager usbDeviceManager) {
        CustomTouchListener.usbDeviceManager = usbDeviceManager;
        CustomTouchListener.activity = activity;
        floatingLabel = activity.findViewById(R.id.floating_label);

        // Detect portrait mode using screen orientation
        isPortraitMode = isCurrentlyPortrait();
        Log.d(TAG, "CustomTouchListener: isPortraitMode=" + isPortraitMode);

        // Initialize cached 1x1 transparent PointerIcon
        android.graphics.Bitmap transparentBitmap = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888);
        transparentBitmap.eraseColor(0x00000000);
        sTransparentPointerIcon = android.view.PointerIcon.create(transparentBitmap, 0, 0);

        // Create and configure TouchInputHandler
        touchHandler = new TouchInputHandler();
        touchHandler.setZoomCallback(zoomFactor -> {
            ZoomPanManager.adjustZoom(zoomFactor);
        });

        // Initialize CoordinateConverter
        CoordinateConverter.init(activity, isPortraitMode);

        // Initialize BtMouseInputHandler with callbacks
        BtMouseInputHandler.init(activity, new BtMouseInputHandler.Callbacks() {
            @Override
            public void onBlackBorderVisibilityChanged(boolean show) {
                updateBlackBorderOverlayVisibility();
            }

            @Override
            public void onSystemGestureExclusionChanged(boolean enabled) {
                updateSystemGestureExclusion(enabled);
            }

            @Override
            public void onBlackBorderHintUpdate(float cursorX, float cursorY) {
                updateBlackBorderCursorState(cursorX, cursorY);
            }
        });

        // Initialize ZoomPanManager
        // IMPORTANT: the portrait layout has no drawer_layout at all, so the manager must
        // still be initialized (with a null drawer) — otherwise ZoomPanManager keeps
        // isPortraitMode=false and silently sends every pinch down the landscape path,
        // where drawerLayout==null makes it a no-op (portrait pinch did nothing).
        androidx.drawerlayout.widget.DrawerLayout drawerLayoutRef = null;
        try {
            View drawerView = activity.findViewById(R.id.drawer_layout);
            if (drawerView instanceof androidx.drawerlayout.widget.DrawerLayout) {
                drawerLayoutRef = (androidx.drawerlayout.widget.DrawerLayout) drawerView;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to resolve drawerLayout: " + e.getMessage());
        }

        // No overlay callback: the black border overlay is fully transparent now (it no
        // longer draws a mask or a hint), so a zoom step has nothing to refresh there.
        ZoomPanManager.init(activity, drawerLayoutRef, isPortraitMode, null);

        // In portrait mode, overlay should be VISIBLE but transparent (alpha=0)
        // This allows it to receive cursor events for transparent PointerIcon
        if (isPortraitMode && activity.mBinding != null && activity.mBinding.blackBorderOverlay != null) {
            activity.mBinding.blackBorderOverlay.setVisibility(View.VISIBLE);
            activity.mBinding.blackBorderOverlay.setAlpha(0f);
        }
    }

    /**
     * Check if device is currently in portrait orientation.
     *
     * Layout based detection is authoritative (layout-port defines module_selector_bar);
     * the display metrics are only used as a fallback, because they may still report the
     * previous orientation while onConfigurationChanged() is running.
     */
    private static boolean isCurrentlyPortrait() {
        if (activity == null) return false;
        try {
            if (activity.findViewById(R.id.module_selector_bar) != null) {
                return true;
            }
            if (activity.findViewById(R.id.drawer_layout) != null) {
                return false;
            }
        } catch (Exception ignored) {
            // fall through to metrics
        }
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        return metrics.heightPixels > metrics.widthPixels;
    }

    // ===== Public touch API =====

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Delegate to TouchInputHandler
        return touchHandler.onTouch(event);
    }

    /**
     * Handle GenericMotionEvent for Bluetooth mouse.
     * Called from MainActivity's dispatchGenericMotionEvent.
     * @return true if event was consumed
     */
    public static boolean handleGenericMotionEvent(MotionEvent event) {
        // Delegate to BtMouseInputHandler
        return BtMouseInputHandler.handleGenericMotionEvent(event);
    }

    // ===== BT Mouse state accessors (delegate to BtMouseInputHandler) =====

    /**
     * Check if any Bluetooth mouse button is currently pressed.
     */
    public static boolean isBtMouseButtonPressed() {
        return BtMouseInputHandler.isBtMouseButtonPressed();
    }

    /**
     * Check if touch events should be ignored based on recent BT mouse activity.
     */
    public static boolean shouldIgnoreTouch() {
        return BtMouseInputHandler.shouldIgnoreTouch();
    }

    public static boolean isBtMouseConnected() {
        return BtMouseInputHandler.isBtMouseConnected();
    }

    /**
     * Callback interface for Bluetooth mouse connection state changes.
     * Delegated to BtMouseInputHandler.
     */
    public interface OnBtMouseConnectionListener {
        void onBtMouseConnected(boolean connected);
    }

    public static void setOnBtMouseConnectionListener(OnBtMouseConnectionListener listener) {
        BtMouseInputHandler.setOnBtMouseConnectionListener(listener != null
                ? connected -> listener.onBtMouseConnected(connected)
                : null);
    }

    public static void cancelBtMouseCursorRestore() {
        BtMouseInputHandler.cancelBtMouseCursorRestore();
    }

    public static float getBtMouseLastX() {
        return BtMouseInputHandler.getBtMouseLastX();
    }

    public static float getBtMouseLastY() {
        return BtMouseInputHandler.getBtMouseLastY();
    }

    public static void setBtMouseLastPosition(float x, float y) {
        BtMouseInputHandler.setBtMouseLastPosition(x, y);
    }

    public static float getVirtualHidX() {
        return BtMouseInputHandler.getVirtualHidX();
    }

    public static float getVirtualHidY() {
        return BtMouseInputHandler.getVirtualHidY();
    }

    // ===== Portrait zoom/pan (delegate to ZoomPanManager) =====

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

    public static void applyCurrentPortraitTransform() {
        ZoomPanManager.applyCurrentPortraitTransform();
    }

    /**
     * Apply portrait zoom transform to the TextureView.
     * Instance method for backwards compatibility with MainActivity.
     */
    public void applyPortraitZoomTransform() {
        ZoomPanManager.applyPortraitZoomTransform();
    }

    public static boolean isPortraitMode() {
        return isPortraitMode;
    }

    // ===== Touch view dimensions (delegate to CoordinateConverter) =====

    /**
     * Get the touch view width for coordinate conversion.
     */
    public static float getTouchViewWidth() {
        return CoordinateConverter.getTouchViewWidth();
    }

    /**
     * Get the touch view height for coordinate conversion.
     */
    public static float getTouchViewHeight() {
        return CoordinateConverter.getTouchViewHeight();
    }

    /**
     * Get effective zoom scale (minimum 1.0).
     */
    public static float getEffectiveZoomScale() {
        return ZoomPanManager.getEffectiveZoomScale();
    }

    /**
     * Attach a pinch-to-zoom fallback to the video area container.
     *
     * In portrait the preview keeps the camera aspect ratio, so it is a short band
     * centered inside the much taller video container. Touches landing on the black
     * area around that band never reach viewMainPreview, so the pinch was ignored
     * whenever the two fingers were not both on the band.
     *
     * A ViewGroup still offers a touch to its own listener only when no child consumed
     * it, so this listener runs exactly in the black area and never competes with the
     * preview's own listener. It only adjusts zoom (no coordinates are needed), and it
     * consumes the gesture so the trailing single finger cannot move the mouse.
     */
    public static void attachVideoAreaZoomFallback(View videoArea) {
        if (videoArea == null) return;

        videoArea.setOnTouchListener(new View.OnTouchListener() {
            private float prevDist = 0f;
            private boolean pinching = false;

            private float distance(MotionEvent event) {
                float dx = event.getX(0) - event.getX(1);
                float dy = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(dx * dx + dy * dy);
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // Must consume, otherwise the second finger of this gesture
                        // would not be delivered here.
                        pinching = false;
                        prevDist = 0f;
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (event.getPointerCount() == 2) {
                            prevDist = distance(event);
                            pinching = true;
                            Log.d(TAG, "Fallback pinch started: dist=" + prevDist);
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (pinching && event.getPointerCount() == 2 && prevDist > 0f) {
                            float dist = distance(event);
                            if (dist > 0f) {
                                float factor = Math.max(0.7f, Math.min(1.4f, dist / prevDist));
                                if (Math.abs(factor - 1f) > 0.001f) {
                                    ZoomPanManager.adjustZoom(factor);
                                }
                                prevDist = dist;
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_POINTER_UP:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        pinching = false;
                        prevDist = 0f;
                        return true;
                }
                return true;
            }
        });
    }

    // ===== Black border overlay management =====

    /**
     * Update the black border overlay visibility.
     * Shows overlay when BT mouse is connected, hides when disconnected.
     * In portrait mode, video fills the entire TextureView (via Matrix rotation),
     * so there are no black borders to show — but still need to hide system cursor.
     */
    public static void updateBlackBorderOverlayVisibility() {
        if (activity == null || activity.mBinding == null) {
            return;
        }

        boolean btConnected = BtMouseInputHandler.isBtMouseConnected();

        if (btConnected) {
            // Keep the (fully transparent) overlay visible in both orientations so its
            // pointer icon also applies outside the preview — the system cursor must not
            // reappear there. Setting the icon on a hidden view has no effect.
            if (activity.mBinding.blackBorderOverlay != null) {
                activity.mBinding.blackBorderOverlay.showOverlay();
                if (sTransparentPointerIcon != null) {
                    activity.mBinding.blackBorderOverlay.setPointerIcon(sTransparentPointerIcon);
                }
            }
            if (sTransparentPointerIcon != null && activity.mBinding.viewMainPreview != null) {
                activity.mBinding.viewMainPreview.setPointerIcon(sTransparentPointerIcon);
            }
        } else {
            // BT disconnected: hide overlay
            if (activity.mBinding.blackBorderOverlay != null) {
                activity.mBinding.blackBorderOverlay.hideOverlay();
            }
        }
    }

    /**
     * Called when screen configuration changes (e.g., rotation).
     */
    public static void onConfigurationChanged() {
        boolean newPortraitMode = isCurrentlyPortrait();
        if (newPortraitMode != isPortraitMode) {
            Log.d(TAG, "Configuration changed: isPortraitMode " + isPortraitMode + " -> " + newPortraitMode);
            isPortraitMode = newPortraitMode;
            ZoomPanManager.setPortraitMode(newPortraitMode);

            // Reset learned cursor range
            CoordinateConverter.resetLearnedRange();

            // The layout is re-inflated on rotation, so zoom/pan state cannot survive
            // in either direction — otherwise the manager reports a zoom level that the
            // freshly laid out preview does not have (next pinch starts from a bogus scale).
            ZoomPanManager.reset();
            Log.d(TAG, "Reset portrait zoom state due to rotation");
        }
    }

    /**
     * Track whether the mouse is over the black border and update the system cursor.
     * In portrait mode the video fills the entire view, so the cursor is always over the video.
     */
    private static void updateBlackBorderCursorState(float viewX, float viewY) {
        if (isPortraitMode) return;

        if (activity == null || activity.mBinding == null
                || activity.mBinding.blackBorderOverlay == null
                || activity.mBinding.viewMainPreview == null) {
            return;
        }

        float viewW = activity.mBinding.viewMainPreview.getWidth();
        float viewH = activity.mBinding.viewMainPreview.getHeight();

        if (viewW <= 0 || viewH <= 0) {
            return;
        }

        float bufferW = activity.getPreviewWidth();
        float bufferH = activity.getPreviewHeight();
        float[] videoRect = CoordinateConverter.getVideoRectInView(viewW, viewH, bufferW, bufferH);

        float visLeft = videoRect[0];
        float visTop = videoRect[1];
        float visW = videoRect[2];
        float visH = videoRect[3];

        boolean inBlackBorder = viewX < visLeft || viewX > visLeft + visW
                || viewY < visTop || viewY > visTop + visH;

        updateSystemCursorVisibility(inBlackBorder);
    }

    /**
     * Update system cursor visibility based on whether cursor is in black border.
     */
    private static void updateSystemCursorVisibility(boolean inBlackBorder) {
        if (activity == null || activity.mBinding == null
                || activity.mBinding.viewMainPreview == null) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            return;
        }

        if (inBlackBorder != wasCursorInBlackBorder) {
            wasCursorInBlackBorder = inBlackBorder;
            if (sTransparentPointerIcon != null) {
                activity.mBinding.viewMainPreview.setPointerIcon(sTransparentPointerIcon);
                Log.d(TAG, "System cursor hidden (cached transparent 1x1 icon)");
            }
        }
    }

    /**
     * Move the mouse cursor to the center of the currently visible viewport.
     */
    public static void moveMouseToViewportCenter() {
        Log.d(TAG, "moveMouseToViewportCenter CALLED FROM:", new Exception("Stack trace"));

        if (activity == null || activity.mBinding == null
                || activity.mBinding.viewMainPreview == null) {
            return;
        }

        float viewWidth = activity.mBinding.viewMainPreview.getWidth();
        float viewHeight = activity.mBinding.viewMainPreview.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        // viewWidth/2 is already a view-local coordinate (Android reverses the parent matrix
        // before dispatch), so the landscape drawer scale/scroll must NOT be applied here —
        // doing so moved the cursor off-centre whenever landscape was zoomed.
        float[] content = CoordinateConverter.mapToContentCoords(
                viewWidth / 2f, viewHeight / 2f, viewWidth, viewHeight,
                isPortraitMode, 1.0f, 0f, 0f);

        int hidWidth = HidManager.getScreenWidth();
        int hidHeight = HidManager.getScreenHeight();
        float hidX = content[0] / viewWidth * hidWidth;
        float hidY = content[1] / viewHeight * hidHeight;

        HidManager.sendHexAbsData(hidX, hidY);
        Log.d(TAG, "moveMouseToViewportCenter: content=(" + content[0] + "," + content[1]
                + ") hid=(" + hidX + "," + hidY + ")");
    }

    // ===== System gesture exclusion =====

    /**
     * Update system gesture exclusion rects to prevent Android system gestures
     * from intercepting BT mouse at screen edges.
     */
    private static void updateSystemGestureExclusion(boolean enable) {
        // This is handled by the activity or can be delegated
        // For now, this is a no-op placeholder
        Log.d(TAG, "updateSystemGestureExclusion: " + enable);
    }

    // ===== Floating label accessor =====

    public TextView getFloatingLabel() {
        return floatingLabel;
    }

    // ===== Reset =====

    public static void reset() {
        BtMouseInputHandler.reset();
        CoordinateConverter.resetLearnedRange();
        ZoomPanManager.reset();
    }
}
