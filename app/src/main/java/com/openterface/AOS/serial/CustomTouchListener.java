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

import android.graphics.Matrix;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.drawerLayout.ZoomLayoutDeal;
import com.openterface.AOS.target.HidManager;
import com.openterface.AOS.target.MouseManager;
import com.serenegiant.widget.AspectRatioTextureView;

/**
 * CustomTouchListener — handles touch events on the full screen (pointer mode).
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
    private static boolean KeyMouse_state, keyMouseAbsCtrl;
    private static boolean virtualTrackpadMode = false;

    /**
     * Set mouse control mode.
     * @param keyMouseState true = absolute mode, false = relative/trackpad
     * @param keyMouseAbsCtrlState true = absolute drag mode
     */
    public static void KeyMouse_state(boolean keyMouseState, boolean keyMouseAbsCtrlState) {
        KeyMouse_state = keyMouseState;
        keyMouseAbsCtrl = keyMouseAbsCtrlState;
    }

    /**
     * Enable/disable virtual trackpad mode.
     * When enabled, all drags send relative movements (like a laptop trackpad),
     * and sensitivity is scaled by 1/zoomScale for precise control when zoomed in.
     */
    public static void setVirtualTrackpadMode(boolean enabled) {
        virtualTrackpadMode = enabled;
    }

    public static boolean isVirtualTrackpadMode() {
        return virtualTrackpadMode;
    }

    //system module
    private static UsbDeviceManager usbDeviceManager;
    private Handler handler = new Handler();
    private Runnable twoFingerPressRunnable;
    private TextView floatingLabel;
    private static MainActivity activity;

    //Event processing time
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;
    private static final long MOVE_THROTTLE_MS = 16;

    //coordinate record
    private float firstClickX, firstClickY;
    private float startX1, startX2, startY1, startY2;
    private float currentX, currentY;
    private float startMoveMSX, startMoveMSY;
    private static float lastMoveMSX, lastMoveMSY;
    private float lastMoveX, lastMoveY;  // Track last move position for delta calculation
    private boolean suppressSingleTapFromDoubleTap = false;

    //state sign
    private boolean DrawMode = false;
    private boolean isPanning = false;
    private boolean isLongPress = false;
    private boolean hasHandledMove = false;
    private boolean isDoubleClickPhase = false;
    private boolean mouseLeftClick = false;
    private boolean mouseRightClick = false;
    private boolean mouseScrollClick = false;

    //time sign
    private long longPressStartTime;
    private long lastClickTime = 0;
    private long lastMoveTime = 0;
    private long ignoreMoveUntil = 0;
    private long firstClickTime = 0;

    //click scope
    private static final float CLICK_RADIUS = 100f;
    private static final float DRAG_THRESHOLD = 10f;

    //zoom set
    private final DrawerLayout drawerLayout;
    private static boolean isPortraitMode;

    private long rightClickCurrentTime;
    private long rightReleaseCurrentTime;
    private long dragModeStartTime;
    private long dragModeEndTime;
    private boolean hasEnteredDragMode = false;

    // ===== TouchPad-style gesture detection =====
    // 1-finger tap tracking
    private float tapDownX, tapDownY;
    private long tapDownTime;
    private boolean tapCancelled = false;
    private Runnable pendingSingleTap = null;
    private long lastSingleTapTime = 0;
    private float lastSingleTapX, lastSingleTapY;

    // 2-finger scroll tracking (KeyCmd-style with accumulator)
    private float twoFingerDownCenterX, twoFingerDownCenterY;
    private float twoFingerPrevX, twoFingerPrevY;
    private float twoFingerScrollAccumX = 0f;
    private float twoFingerScrollAccumY = 0f;
    private boolean twoFingerMoved = false;
    private boolean isTwoFingerScrolling = false;

    // Gesture phase tracking — prevents single-click from firing after two-finger gesture
    private boolean inTwoFingerSequence = false;

    // Pending initial absolute position send — delayed to allow 2-finger gesture detection
    // so we don't send mouse data when the user intends to pan/scroll the view
    private Runnable pendingInitialAbsDown = null;
    private boolean initialAbsDownSent = false;

    // Two-finger right-click button state
    private boolean rightButtonPressed = false;      // 右键当前是否按下
    private boolean isTwoFingerClick = false;      // 是否为点击模式(未拖动)
    private boolean twoFingerDragConfirmed = false;  // 已确认为拖动(不再发送右键)

    // Two-finger pinch-to-zoom state
    private float twoFingerStartDist = 0f;       // Initial distance between two fingers
    private boolean isPinching = false;          // Currently in pinch-to-zoom mode
    private float finger0StartX, finger0StartY;  // Finger 0 starting position
    private float finger1StartX, finger1StartY;  // Finger 1 starting position
    private static final float PINCH_THRESHOLD = 15f; // px distance change to start zoom

    // Timing thresholds (aligned with KeyCmd TouchPadView)
    private static final long TAP_DELAY_MS = 150;           // wait before confirming single tap
    private static final long TAP_TIMEOUT_MS = 400;         // finger must lift within this time
    private static final long DOUBLE_TAP_TIMEOUT_MS = 300;
    private static final long LONG_PRESS_TIMEOUT_MS = 500; // Enter drag mode after 500ms
    private static final float TAP_MOVE_THRESHOLD_PX = 10f; // px — movement cancels tap and starts drag
    private static final float TWO_FINGER_TAP_MOVE_THRESHOLD = 12f;

    // Drag mode state
    private boolean isDragMode = false;      // Long-press drag mode (left button held until tap)
    private Runnable longPressRunnable = null;

    public CustomTouchListener(MainActivity activity, UsbDeviceManager usbDeviceManager) {
        CustomTouchListener.usbDeviceManager = usbDeviceManager;
        CustomTouchListener.activity = activity;
        floatingLabel = activity.findViewById(R.id.floating_label);
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        // Detect portrait mode: portrait layout has module_selector_bar, landscape doesn't
        isPortraitMode = (activity.findViewById(R.id.module_selector_bar) != null);
        Log.d(TAG, "CustomTouchListener: isPortraitMode=" + isPortraitMode);
    }

    public static boolean handleGenericMotionEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float scrollAmount = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (scrollAmount != 0) {
                Log.d(TAG, "Scroll amount: " + scrollAmount);
                HidManager.handleTwoFingerPanSlideUpDown(scrollAmount);
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_HOVER_MOVE ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            float cursorX = event.getX();
            float cursorY = event.getY();
            for (int i = 0; i < event.getPointerCount(); i++) {
                int toolType = event.getToolType(i);
                switch (toolType) {
                    case MotionEvent.TOOL_TYPE_MOUSE:
                        if(KeyMouse_state && !virtualTrackpadMode){
                            // Map touch coords to content coords (viewport-aware)
                            float[] content = mapToContentCoords(cursorX, cursorY,
                                    getTouchViewWidth(), getTouchViewHeight());
                            if(keyMouseAbsCtrl){
                                HidManager.sendHexAbsDragData(content[0], content[1]);
                            }else {
                                HidManager.sendHexAbsData(content[0], content[1]);
                            }
                        }else {
                            // Feature 2: Apply zoom-aware sensitivity scaling to deltas
                            float zoomScale = getEffectiveZoomScale();
                            float effectiveDeltaScale = 1.0f / zoomScale;
                            float adjX = lastMoveMSX + (cursorX - lastMoveMSX) * effectiveDeltaScale;
                            float adjY = lastMoveMSY + (cursorY - lastMoveMSY) * effectiveDeltaScale;
                            HidManager.sendHexRelData("SecNullData", adjX, adjY, lastMoveMSX, lastMoveMSY);
                            lastMoveMSX = cursorX;
                            lastMoveMSY = cursorY;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        // ---- Track gesture phase ----
        // Once two fingers are detected, stay in two-finger event routing until
        // ALL fingers are lifted. This handles non-simultaneous finger lifts.
        // Key insight: ACTION_POINTER_DOWN starts the sequence; the subsequent
        // ACTION_POINTER_UP is "first finger lifts" (still 1 finger down);
        // final ACTION_UP (or ACTION_CANCEL) is "last finger lifts".
        if (action == MotionEvent.ACTION_POINTER_DOWN || pointerCount == 2) {
            inTwoFingerSequence = true;
            // User is starting a 2-finger gesture (scroll/pinch) — cancel any pending
            // initial absolute position from the first finger so we don't send mouse data
            // when the user's intent is to move the viewpoint
            cancelPendingInitialAbsDown();
        }

        if (inTwoFingerSequence) {
            // Only exit when we see the final lift event AND no fingers remain
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && pointerCount <= 1) {
                // Still route this last event to two-finger handler, then clear flag
                boolean result = handleTwoFingerGesture(event);
                inTwoFingerSequence = false;
                return result;
            }
            return handleTwoFingerGesture(event);
        }

        // Handle 1-finger touchpad gestures
        return handleOneFingerGesture(event);
    }

    /**
     * 1-finger gesture handler (tap/click/double-tap/move/drag).
     * KeyCmd logic:
     *   - Move: just move mouse (no button pressed)
     *   - Long press (>500ms) then move: drag mode (left button held)
     *   - Tap: left click
     *   - Double tap: double click
     */
    private boolean handleOneFingerGesture(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                tapDownX = x;
                tapDownY = y;
                tapDownTime = event.getEventTime();
                tapCancelled = false;
                suppressSingleTapFromDoubleTap = false;
                initialAbsDownSent = false;

                // Cancel any pending long press
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }

                detectDoubleClick(event);
                initHandActionDownMouse(event);
                // Track button state but DON'T send absolute position yet — delay it
                // so we can cancel if a second finger comes down (2-finger scroll/pan)
                trackButtonState(event);

                // Schedule delayed initial abs down — fires only if no 2nd finger arrives
                pendingInitialAbsDown = () -> {
                    if (!initialAbsDownSent && KeyMouse_state && !virtualTrackpadMode) {
                        try {
                            float[] content = mapToContentCoords(startMoveMSX, startMoveMSY,
                                    getTouchViewWidth(), getTouchViewHeight());
                            HidManager.sendHexAbsData(content[0], content[1]);
                        } catch (Exception e) {
                            Log.e(TAG, "pendingInitialAbsDown error: " + e.getMessage(), e);
                        }
                        initialAbsDownSent = true;
                    }
                };
                handler.postDelayed(pendingInitialAbsDown, 80);

                // Schedule long press detection for drag mode (KeyCmd-style: toggle drag on long press)
                longPressRunnable = () -> {
                    if (!tapCancelled) {
                        isDragMode = true;
                        // In KeyCmd, drag mode is toggled and stays on until tap exits
                        Log.d(TAG, "Long press detected -> enter drag mode");
                    }
                };
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS);
                break;

            case MotionEvent.ACTION_MOVE:
                float distMoved = dist(x, y, tapDownX, tapDownY);
                if (distMoved > TAP_MOVE_THRESHOLD_PX && !tapCancelled && !isDragMode) {
                    // Movement cancels tap detection, but does NOT auto-enter drag mode
                    // Drag mode should only be entered via long press (500ms)
                    cancelPendingSingleTap();
                    tapCancelled = true;
                    // Cancel long press runnable since tap is cancelled
                    if (longPressRunnable != null) {
                        handler.removeCallbacks(longPressRunnable);
                        longPressRunnable = null;
                    }
                }

                // Only drag if explicitly in drag mode (long press triggered)
                if (isDragMode) {
                    // In drag mode: move with left button held
                    handleDragMove(x, y);
                } else {
                    // Normal move: just move mouse (no button pressed)
                    handleNormalMove(x, y);
                }
                lastMoveX = x;
                lastMoveY = y;
                break;

            case MotionEvent.ACTION_UP:
                // Safety: if we're somehow in a two-finger sequence, don't process single-finger
                if (inTwoFingerSequence) {
                    return true;
                }

                // Cancel long press detection
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }

                // Cancel pending initial abs down
                cancelPendingInitialAbsDown();

                long duration = event.getEventTime() - tapDownTime;
                float distLifted = dist(x, y, tapDownX, tapDownY);

                // KeyCmd-style: if double-tap just fired, suppress single-tap and release only
                if (suppressSingleTapFromDoubleTap) {
                    suppressSingleTapFromDoubleTap = false;
                    if (isDragMode) {
                        releaseMouseAndReset();
                    }
                    isDragMode = false;
                    lastMoveX = 0;
                    lastMoveY = 0;
                    break;
                }

                if (isDragMode) {
                    Log.d(TAG, "Drag ended, releasing left button");
                    releaseMouseAndReset();
                    isDragMode = false;
                    lastMoveMSX = 0;
                    lastMoveMSY = 0;
                    break;
                }

                // Check for tap (KeyCmd-style)
                boolean validTap = !tapCancelled
                        && duration < TAP_TIMEOUT_MS
                        && distLifted <= TAP_MOVE_THRESHOLD_PX;

                if (validTap) {
                    long now = event.getEventTime();
                    long timeSinceLastTap = now - lastSingleTapTime;
                    float distSinceLastTap = dist(x, y, lastSingleTapX, lastSingleTapY);

                    if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && distSinceLastTap < TAP_MOVE_THRESHOLD_PX * 2) {
                        cancelPendingSingleTap();
                        handleDoubleClick();
                        lastSingleTapTime = 0;
                    } else {
                        cancelPendingSingleTap();
                        pendingSingleTap = () -> {
                            handleSingleClick(x, y);
                            pendingSingleTap = null;
                        };
                        lastSingleTapTime = now;
                        lastSingleTapX = x;
                        lastSingleTapY = y;
                        handler.postDelayed(pendingSingleTap, TAP_DELAY_MS);
                    }
                }

                // Only release mouse if not in a drag/tap-canceled sequence
                if (!tapCancelled) {
                    handActionUpMouse(event);
                } else {
                    HidManager.releaseMSRelData();
                    lastMoveMSX = 0;
                    lastMoveMSY = 0;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                // Safety: if we're in a two-finger sequence, don't process
                if (inTwoFingerSequence) {
                    return true;
                }

                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }
                cancelPendingInitialAbsDown();
                cancelPendingSingleTap();
                if (isDragMode) {
                    releaseMouseAndReset();
                    isDragMode = false;
                } else {
                    HidManager.releaseMSRelData();
                }
                lastMoveMSX = 0;
                lastMoveMSY = 0;
                lastMoveX = 0;
                lastMoveY = 0;
                break;
        }
        return true;
    }

    // Portrait zoom tracking
    private static float mPortraitZoomScale = 1.0f;
    private static float mPortraitTranslateX = 0f;
    private static float mPortraitTranslateY = 0f;

    /**
     * Get current portrait zoom scale (called by ZoomLayoutDeal)
     */
    public static float getPortraitZoomScale() {
        return mPortraitZoomScale;
    }

    /**
     * Set portrait pan translation from external source (e.g., PiP indicator drag)
     * Called by ZoomLayoutDeal.syncMainViewPosition
     */
    public static void setPortraitPan(float translateX, float translateY) {
        mPortraitTranslateX = translateX;
        mPortraitTranslateY = translateY;
    }

    /**
     * Apply the current portrait zoom transform to the view
     * Called by ZoomLayoutDeal after updating pan values
     */
    public static void applyCurrentPortraitTransform() {
        if (mPortraitZoomScale > 1.0f && activity != null && activity.mBinding != null
            && activity.mBinding.viewMainPreview != null) {
            AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;
            float viewWidth = textureView.getWidth();
            float viewHeight = textureView.getHeight();

            if (viewWidth <= 0 || viewHeight <= 0) return;

            Matrix matrix = new Matrix();
            float pivotX = viewWidth / 2f;
            float pivotY = viewHeight / 2f;
            float scale = mPortraitZoomScale;
            matrix.setScale(scale, scale, pivotX, pivotY);
            matrix.postTranslate(mPortraitTranslateX, mPortraitTranslateY);
            textureView.setTransform(matrix);
            textureView.invalidate();
        }
    }

    /**
     * Move the mouse cursor to the center of the currently visible viewport.
     * Used after two-finger pan ends or PiP indicator drag ends, so the mouse follows the view.
     *
     * How it works: Uses mapToContentCoords to inverse-transform the viewport center
     * (viewWidth/2, viewHeight/2) back to content coordinates, then scales to HID coordinates.
     */
    public static void moveMouseToViewportCenter() {
        if (activity == null || activity.mBinding == null
            || activity.mBinding.viewMainPreview == null) {
            return;
        }

        float viewWidth = activity.mBinding.viewMainPreview.getWidth();
        float viewHeight = activity.mBinding.viewMainPreview.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        // Use mapToContentCoords to inverse-transform the viewport center to content coordinates
        float[] content = mapToContentCoords(viewWidth / 2f, viewHeight / 2f, viewWidth, viewHeight);

        // Content coords are in range [0, viewWidth] x [0, viewHeight]
        // HidManager.sendHexAbsData expects HID reference coordinates
        // Scale proportionally to convert
        int hidWidth = HidManager.getScreenWidth();
        int hidHeight = HidManager.getScreenHeight();
        float hidX = content[0] / viewWidth * hidWidth;
        float hidY = content[1] / viewHeight * hidHeight;

        HidManager.sendHexAbsData(hidX, hidY);
        Log.d(TAG, "moveMouseToViewportCenter: content=(" + content[0] + "," + content[1]
            + ") hid=(" + hidX + "," + hidY + ")");
    }

    /**
     * Normal mouse move (no button pressed)
     * Feature 2: Applies zoom-aware sensitivity scaling to relative mode deltas.
     * Feature 3: In virtual trackpad mode, uses relative movement regardless of mode.
     */
    private void handleNormalMove(float x, float y) {
        if (KeyMouse_state && !virtualTrackpadMode) {
            // Absolute mode: map touch coords to content coords (viewport-aware)
            firePendingInitialAbsDown();
            float[] content = mapToContentCoords(x, y, getTouchViewWidth(), getTouchViewHeight());
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            // Relative mode or virtual trackpad mode: apply zoom-aware scaling
            float zoomScale = getEffectiveZoomScale();
            float effectiveDeltaScale = 1.0f / zoomScale;
            float adjX = lastMoveMSX + (x - lastMoveMSX) * effectiveDeltaScale;
            float adjY = lastMoveMSY + (y - lastMoveMSY) * effectiveDeltaScale;
            HidManager.sendHexRelData("SecNullData", adjX, adjY, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    /**
     * Drag mode mouse move (left button held)
     * Feature 2: Applies zoom-aware sensitivity scaling to relative mode deltas.
     * Feature 3: In virtual trackpad mode, uses relative movement regardless of mode.
     */
    private void handleDragMove(float x, float y) {
        if (KeyMouse_state && !virtualTrackpadMode) {
            // In drag mode with absolute: map touch coords to content coords (viewport-aware)
            firePendingInitialAbsDown();
            float[] content = mapToContentCoords(x, y, getTouchViewWidth(), getTouchViewHeight());
            HidManager.sendHexAbsButtonClickData("SecLeftData", content[0], content[1]);
        } else {
            // In drag mode with relative or virtual trackpad: apply zoom-aware scaling
            float zoomScale = getEffectiveZoomScale();
            float effectiveDeltaScale = 1.0f / zoomScale;
            float adjX = lastMoveMSX + (x - lastMoveMSX) * effectiveDeltaScale;
            float adjY = lastMoveMSY + (y - lastMoveMSY) * effectiveDeltaScale;
            HidManager.sendHexRelData("SecLeftData", adjX, adjY, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    /**
     * Get the effective zoom scale (portrait or landscape, whichever is active).
     * Returns >= 1.0f. Used for dynamic sensitivity scaling (Feature 2).
     */
    private static float getEffectiveZoomScale() {
        if (isPortraitMode) {
            return Math.max(1.0f, mPortraitZoomScale);
        } else if (activity != null) {
            View drawer = activity.findViewById(R.id.drawer_layout);
            if (drawer != null) {
                return Math.max(1.0f, drawer.getScaleX());
            }
        }
        return 1.0f;
    }

    /**
     * Map touch coordinates (view space) to camera content coordinates.
     *
     * Key insight: The camera content fills the TextureView's measured layout bounds
     * (aspect ratio is maintained by AspectRatioTextureView's onMeasure). The zoom
     * Matrix transform (scale around center pivot + translate) visually scales the
     * content — to map a touch position back to the original content space, we apply
     * the INVERSE transform.
     *
     * The viewport center on screen and the HID cursor position represent the same
     * point on the target screen.
     *
     * Returns coordinates in [0, viewWidth] × [0, viewHeight] range (matches the
     * camera content space), which MouseManager then converts to HID [0, 4095].
     */
    private static float[] mapToContentCoords(float touchX, float touchY,
                                                float viewWidth, float viewHeight) {
        float contentX = touchX;
        float contentY = touchY;

        if (isPortraitMode) {
            // Apply inverse viewport transform:
            //   viewX = (contentX - pivotX) * zoomScale + pivotX + translateX
            //   → contentX = (viewX - translateX - pivotX) / zoomScale + pivotX
            float zoomScale = Math.max(1.0f, mPortraitZoomScale);
            if (zoomScale > 1.0f) {
                float pivotX = viewWidth / 2f;
                float pivotY = viewHeight / 2f;
                contentX = (touchX - mPortraitTranslateX - pivotX) / zoomScale + pivotX;
                contentY = (touchY - mPortraitTranslateY - pivotY) / zoomScale + pivotY;
            }
        } else if (activity != null) {
            // Landscape mode: drawerLayout uses scaleX (pivot at 0,0) + scrollTo
            View drawer = activity.findViewById(R.id.drawer_layout);
            if (drawer != null) {
                float scaleX = Math.max(1.0f, drawer.getScaleX());
                if (scaleX > 1.0f) {
                    float scrollX = drawer.getScrollX();
                    float scrollY = drawer.getScrollY();
                    contentX = (touchX + scrollX) / scaleX;
                    contentY = (touchY + scrollY) / scaleX;
                }
            }
        }

        // Clamp to valid content range
        contentX = Math.max(0, Math.min(contentX, viewWidth));
        contentY = Math.max(0, Math.min(contentY, viewHeight));

        return new float[]{contentX, contentY};
    }

    /**
     * Get the touch target view width (main preview TextureView).
     */
    private static float getTouchViewWidth() {
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
    private static float getTouchViewHeight() {
        if (activity != null && activity.mBinding != null
            && activity.mBinding.viewMainPreview != null) {
            float h = activity.mBinding.viewMainPreview.getHeight();
            if (h > 0) return h;
        }
        return MouseManager.screenHeight;
    }

    /**
     * Get the floating label view, re-fetching if null.
     * The view reference can become stale after layout reload (e.g., orientation change).
     */
    private TextView getFloatingLabel() {
        if (floatingLabel == null && activity != null) {
            floatingLabel = activity.findViewById(R.id.floating_label);
        }
        return floatingLabel;
    }

    /**
     * Fire the pending initial absolute position immediately (if not already sent).
     * Called before sending move/click data in absolute mode to ensure the cursor
     * is at the correct position.
     */
    private void firePendingInitialAbsDown() {
        if (!initialAbsDownSent && pendingInitialAbsDown != null) {
            handler.removeCallbacks(pendingInitialAbsDown);
            pendingInitialAbsDown.run();
        }
    }

    /**
     * Cancel the pending initial absolute position send.
     * Called when a 2-finger gesture is detected so we don't send mouse data
     * when the user is trying to move the viewpoint.
     */
    private void cancelPendingInitialAbsDown() {
        if (pendingInitialAbsDown != null) {
            handler.removeCallbacks(pendingInitialAbsDown);
            pendingInitialAbsDown = null;
        }
    }

    /**
     * Track external mouse button state without sending absolute position.
     * The position send is delayed to avoid sending data during 2-finger gestures.
     */
    private void trackButtonState(MotionEvent event) {
        int buttonState = event.getButtonState();
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            mouseLeftClick = true;
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            mouseRightClick = true;
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            mouseScrollClick = true;
        }
    }

    /**
     * Reset portrait zoom and pan state
     */
    private void resetPortraitZoom() {
        mPortraitZoomScale = 1.0f;
        mPortraitTranslateX = 0f;
        mPortraitTranslateY = 0f;
        applyPortraitZoomTransform();
    }

    /**
     * 2-finger gesture handler — clean right-click press/release + scroll.
     *
     * Behavior:
     *   - Two fingers press   → right button DOWN
     *   - Two fingers drag    → scroll only (right button released if pressed)
     *   - Two fingers release → right button UP (only if still pressed, meaning no drag)
     */
    private boolean handleTwoFingerGesture(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger came down — cancel any pending single-tap AND long press
                cancelPendingSingleTap();
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }

                twoFingerDownCenterX = (event.getX(0) + event.getX(1)) / 2f;
                twoFingerDownCenterY = (event.getY(0) + event.getY(1)) / 2f;
                twoFingerPrevX = twoFingerDownCenterX;
                twoFingerPrevY = twoFingerDownCenterY;
                twoFingerScrollAccumX = 0f;
                twoFingerScrollAccumY = 0f;
                twoFingerMoved = false;
                isTwoFingerScrolling = false;
                isTwoFingerClick = true;
                rightButtonPressed = false;
                rightClickCurrentTime = System.currentTimeMillis();

                // Record initial distance between two fingers for pinch-to-zoom
                twoFingerStartDist = calculateDistance(
                        event.getX(0), event.getY(0),
                        event.getX(1), event.getY(1));
                isPinching = false;

                // Record individual finger start positions for direction analysis
                finger0StartX = event.getX(0);
                finger0StartY = event.getY(0);
                finger1StartX = event.getX(1);
                finger1StartY = event.getY(1);

                Log.d(TAG, "Two fingers pressed — waiting to confirm tap or drag");
                break;

            case MotionEvent.ACTION_MOVE:
                int activePointers = event.getPointerCount();

                if (activePointers == 2) {
                    float cx = (event.getX(0) + event.getX(1)) / 2f;
                    float cy = (event.getY(0) + event.getY(1)) / 2f;

                    // --- Gesture classification: compare distance change vs center movement ---
                    float currentDist = calculateDistance(
                            event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1));
                    float distChange = Math.abs(currentDist - twoFingerStartDist);
                    float centerDx = cx - twoFingerDownCenterX;
                    float centerDy = cy - twoFingerDownCenterY;
                    float centerMove = (float) Math.sqrt(centerDx * centerDx + centerDy * centerDy);

                    // Only classify after enough total movement
                    float totalActivity = distChange + centerMove;
                    if (totalActivity > PINCH_THRESHOLD && !isPinching && !twoFingerDragConfirmed) {
                        if (distChange > centerMove) {
                            // Distance changed more than center moved → pinch/zoom
                            isPinching = true;
                            twoFingerDragConfirmed = true;
                            isTwoFingerClick = false;
                            Log.d(TAG, "Pinch: distChange=" + distChange + " centerMove=" + centerMove);
                        } else {
                            // Center moved more than distance → scroll
                            twoFingerMoved = true;
                            twoFingerDragConfirmed = true;
                            isTwoFingerClick = false;
                            isTwoFingerScrolling = true;
                            Log.d(TAG, "Scroll: distChange=" + distChange + " centerMove=" + centerMove);
                        }
                    }

                    if (isPinching) {
                        float zoomFactor = currentDist / twoFingerStartDist;
                        adjustZoom(zoomFactor);
                        twoFingerStartDist = currentDist;
                    } else {
                        float dx = cx - twoFingerPrevX;
                        float dy = cy - twoFingerPrevY;

                        twoFingerScrollAccumX += dx / 3f;
                        twoFingerScrollAccumY += -dy / 3f;

                        int scrollX = (int) twoFingerScrollAccumX;
                        int scrollY = (int) twoFingerScrollAccumY;

                        if (scrollX != 0) twoFingerScrollAccumX -= scrollX;
                        if (scrollY != 0) twoFingerScrollAccumY -= scrollY;

                        if (scrollX != 0 || scrollY != 0) {
                            HidManager.handleTwoFingerPanSlideUpDown(scrollY);
                        }

                        twoFingerPrevX = cx;
                        twoFingerPrevY = cy;
                    }
                } else if (activePointers == 1) {
                    // First finger already lifted, second finger still dragging
                    isTwoFingerClick = false;
                    twoFingerDragConfirmed = true;

                    float cx = event.getX(0);
                    float cy = event.getY(0);
                    float dx = cx - twoFingerPrevX;
                    float dy = cy - twoFingerPrevY;

                    if (Math.abs(dx) > 0f || Math.abs(dy) > 0f) {
                        isTwoFingerScrolling = true;
                    }

                    twoFingerScrollAccumX += dx / 3f;
                    twoFingerScrollAccumY += -dy / 3f;

                    int scrollX = (int) twoFingerScrollAccumX;
                    int scrollY = (int) twoFingerScrollAccumY;

                    if (scrollX != 0) twoFingerScrollAccumX -= scrollX;
                    if (scrollY != 0) twoFingerScrollAccumY -= scrollY;

                    if (scrollX != 0 || scrollY != 0) {
                        HidManager.handleTwoFingerPanSlideUpDown(scrollY);
                    }

                    twoFingerPrevX = cx;
                    twoFingerPrevY = cy;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // First finger lifted — check if this was a tap using the permanent drag flag
                if (twoFingerDragConfirmed) {
                    // Already confirmed as drag/pinch — do NOT send right click
                    Log.d(TAG, "First finger lifted during drag/pinch — no right click");
                } else if (isTwoFingerClick) {
                    // Still in tap mode — send RIGHT CLICK
                    if (KeyMouse_state && !virtualTrackpadMode) {
                        float[] content = mapToContentCoords(twoFingerDownCenterX, twoFingerDownCenterY,
                                getTouchViewWidth(), getTouchViewHeight());
                        HidManager.sendHexAbsButtonClickData("SecRightData", content[0], content[1]);
                        HidManager.sendHexAbsData(content[0], content[1]);
                    } else {
                        HidManager.sendHexRelData("SecRightData",
                                twoFingerDownCenterX, twoFingerDownCenterY, 0, 0);
                        HidManager.releaseMSRelData();
                    }
                    Log.d(TAG, "Two-finger tap confirmed on POINTER_UP → RIGHT CLICK");
                }

                // Update twoFingerPrevX/Y to the remaining finger's position
                // to avoid a large scroll delta jump when the remaining finger moves
                if (event.getPointerCount() == 1) {
                    twoFingerPrevX = event.getX(0);
                    twoFingerPrevY = event.getY(0);
                }

                // Reset scroll accumulators and pinch state
                twoFingerScrollAccumX = 0f;
                twoFingerScrollAccumY = 0f;
                isPinching = false;
                rightReleaseCurrentTime = System.currentTimeMillis();
                Log.d(TAG, "ACTION_POINTER_UP");
                break;

            case MotionEvent.ACTION_UP:
                // Last finger lifted — final cleanup
                // Only send right click if drag/pinch was never confirmed
                if (!twoFingerDragConfirmed) {
                    // Confirmed tap — send RIGHT CLICK
                    if (KeyMouse_state && !virtualTrackpadMode) {
                        float[] content = mapToContentCoords(twoFingerDownCenterX, twoFingerDownCenterY,
                                getTouchViewWidth(), getTouchViewHeight());
                        HidManager.sendHexAbsButtonClickData("SecRightData", content[0], content[1]);
                        HidManager.sendHexAbsData(content[0], content[1]);
                    } else {
                        HidManager.sendHexRelData("SecRightData",
                                twoFingerDownCenterX, twoFingerDownCenterY, 0, 0);
                        HidManager.releaseMSRelData();
                    }
                    Log.d(TAG, "Two-finger final lift → RIGHT CLICK");
                } else {
                    // Two-finger drag ended — if zoomed in, move cursor to viewport center
                    if (isPortraitMode && mPortraitZoomScale > 1.0f
                        && KeyMouse_state && !virtualTrackpadMode) {
                        moveMouseToViewportCenter();
                    }
                    Log.d(TAG, "Two-finger drag/pinch ended — no right click");
                }

                // Full reset
                twoFingerScrollAccumX = 0f;
                twoFingerScrollAccumY = 0f;
                twoFingerMoved = false;
                isTwoFingerScrolling = false;
                isTwoFingerClick = false;
                twoFingerDragConfirmed = false;
                rightButtonPressed = false;
                isPinching = false;
                break;
        }
        return true;
    }

    /**
     * Release right button — sends release command for both abs and rel modes.
     */
    private void releaseRightButton() {
        if (KeyMouse_state && !virtualTrackpadMode) {
            float[] content = mapToContentCoords(lastMoveMSX, lastMoveMSY,
                    getTouchViewWidth(), getTouchViewHeight());
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            HidManager.releaseMSRelData();
        }
    }

    private void handleSingleClick(float x, float y) {
        Log.d(TAG, "Single click at (" + x + "," + y + ")");
        if (KeyMouse_state && !virtualTrackpadMode) {
            // Absolute mode: map coords to content space (viewport-aware), click at touched position
            firePendingInitialAbsDown();
            float[] content = mapToContentCoords(x, y, getTouchViewWidth(), getTouchViewHeight());
            Log.d(TAG, "handleSingleClick: content=(" + content[0] + "," + content[1] + ")");
            try {
                HidManager.sendHexAbsButtonClickData("SecLeftData", content[0], content[1]);
                final float cx = content[0], cy = content[1];
                handler.postDelayed(() -> {
                    try {
                        HidManager.sendHexAbsData(cx, cy);
                    } catch (Exception e) {
                        Log.e(TAG, "handleSingleClick release error: " + e.getMessage());
                    }
                }, 50);
            } catch (Exception e) {
                Log.e(TAG, "handleSingleClick error: " + e.getMessage(), e);
            }
        } else {
            // Relative mode or virtual trackpad: send 0-delta click at cursor's current position
            HidManager.sendHexRelData("SecLeftData", lastMoveMSX, lastMoveMSY, lastMoveMSX, lastMoveMSY);
            handler.postDelayed(() -> HidManager.releaseMSRelData(), 50);
        }
    }

    private void handleDoubleClick() {
        Log.d(TAG, "Double click!");
        // KeyCmd-style: suppress single-tap after double-tap
        suppressSingleTapFromDoubleTap = true;
        if (KeyMouse_state && !virtualTrackpadMode) {
            try {
                float[] content = mapToContentCoords(startMoveMSX, startMoveMSY,
                        getTouchViewWidth(), getTouchViewHeight());
                HidManager.handleDoubleClickAbs(content[0], content[1]);
            } catch (Exception e) {
                Log.e(TAG, "handleDoubleClick error: " + e.getMessage(), e);
            }
        } else {
            HidManager.handleDoubleClickRel();
        }
    }

    private void cancelPendingSingleTap() {
        if (pendingSingleTap != null) {
            handler.removeCallbacks(pendingSingleTap);
            pendingSingleTap = null;
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Release mouse state — sends appropriate release command for both abs and rel modes.
     */
    private void releaseMouseAndReset() {
        if (KeyMouse_state && !virtualTrackpadMode) {
            // In absolute mode: map coords to content space, send move to current position
            float[] content = mapToContentCoords(lastMoveMSX, lastMoveMSY,
                    getTouchViewWidth(), getTouchViewHeight());
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            // In relative mode or virtual trackpad: just release buttons, no movement
            HidManager.releaseMSRelData();
        }
    }

    // --- Original methods below (unchanged) ---

    private void handActionDownMouse(MotionEvent event){
        detectDoubleClick(event);

        initHandActionDownMouse(event);

        int buttonState = event.getButtonState();
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            mouseLeftClick = true;
            Log.d(TAG, "Left button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            mouseRightClick = true;
            Log.d(TAG, "Right button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            mouseScrollClick = true;
            Log.d(TAG, "Middle button pressed");
        }

        if (KeyMouse_state && !virtualTrackpadMode){
            try {
                float[] content = mapToContentCoords(startMoveMSX, startMoveMSY,
                        getTouchViewWidth(), getTouchViewHeight());
                HidManager.sendHexAbsData(content[0], content[1]);
            } catch (Exception e) {
                Log.e(TAG, "handActionDownMouse error: " + e.getMessage(), e);
            }
        }
    }

    //deal double click move
    private void detectDoubleClick(MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - firstClickTime < DOUBLE_CLICK_TIME_DELTA
                && Math.abs(event.getX() - firstClickX) < CLICK_RADIUS
                && Math.abs(event.getY() - firstClickY) < CLICK_RADIUS) {
            isDoubleClickPhase = true;
            Log.d(TAG, "this is isDoubleClickPhase");
        } else {
            firstClickTime = currentTime;
            firstClickX = event.getX();
            firstClickY = event.getY();
            isDoubleClickPhase = false;
        }
    }

    //init handActionDownMouse state
    private void  initHandActionDownMouse(MotionEvent event){
        isLongPress = false;
        currentX = event.getX();
        currentY = event.getY();
        startMoveMSX = event.getX();
        startMoveMSY = event.getY();
        longPressStartTime = System.currentTimeMillis();
    }

    private void handActionPointerDownMouse(MotionEvent event){
        if (event.getPointerCount() == 2) {
            rightClickCurrentTime = System.currentTimeMillis();
            startX1 = event.getX(0);
            startX2 = event.getX(1);
            startY1 = event.getY(0);
            startY2 = event.getY(1);
            isPanning = true;
            hasHandledMove = false;
        }
    }

    private void handActionMoveMouse(MotionEvent event){
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < MOVE_THROTTLE_MS) {
            return;
        }
        lastMoveTime = currentTime;

        // Added double-click drag and drop processing
        if (isDoubleClickPhase && event.getPointerCount() == 1) {
            if (!hasEnteredDragMode){
                dragModeStartTime = System.currentTimeMillis();
                hasEnteredDragMode = true;
                Log.d(TAG, "dragModeStartTime: " + dragModeStartTime);
            }
            startMoveMSX = event.getX();
            startMoveMSY = event.getY();

            if (KeyMouse_state && !virtualTrackpadMode) {
                // Map to content coords (viewport-aware) for all absolute sends in this block
                float[] content = mapToContentCoords(startMoveMSX, startMoveMSY,
                        getTouchViewWidth(), getTouchViewHeight());
                HidManager.sendHexAbsDragData(content[0], content[1]);
            } else {
                // Relative mode or virtual trackpad: apply zoom-aware scaling
                float zoomScale = getEffectiveZoomScale();
                float effectiveDeltaScale = 1.0f / zoomScale;
                float adjX = lastMoveMSX + (startMoveMSX - lastMoveMSX) * effectiveDeltaScale;
                float adjY = lastMoveMSY + (startMoveMSY - lastMoveMSY) * effectiveDeltaScale;
                HidManager.sendHexRelData("SecLeftData", adjX, adjY, lastMoveMSX, lastMoveMSY);
                lastMoveMSX = startMoveMSX;
                lastMoveMSY = startMoveMSY;
            }

            TextView label = getFloatingLabel();
            if (label != null) {
                label.setVisibility(View.VISIBLE);
            }

            return;
        }

        if (isPanning && event.getPointerCount() == 2) {  //deal Middle click

            float zoomX1 = event.getX(0);
            float zoomY1 = event.getY(0);
            float zoomX2 = event.getX(1);
            float zoomY2 = event.getY(1);

            // Calculate initial and current distances between fingers
            float initialDistance = calculateDistance(startX1, startY1, startX2, startY2);
            float currentDistance = calculateDistance(zoomX1, zoomY1, zoomX2, zoomY2);

            float ZOOM_SENSITIVITY = 5f; // Minimum distance change to trigger zoom

            // Check if the distance change is significant enough
            if (Math.abs(currentDistance - initialDistance) > ZOOM_SENSITIVITY) {
                // Check if enough time has elapsed since the last move
                if (currentTime - lastMoveTime >= 16) {
                    handler.removeCallbacks(twoFingerPressRunnable);
                    Log.d(TAG, "ACTION_MOVE: Pinch to Zoom");

                    // Calculate the zoom factor (ratio of current to initial distance)
                    float zoomFactor = currentDistance / initialDistance;

                    // Apply zoom to your view (e.g., drawerLayout or viewMainPreview)
                    adjustZoom(zoomFactor);

                    // Update the starting points to the current ones for the next move
                    startX1 = zoomX1;
                    startY1 = zoomY1;
                    startX2 = zoomX2;
                    startY2 = zoomY2;

                    hasHandledMove = true;
                    lastMoveTime = currentTime;
                }
            }
        } else if (!isLongPress) {
            startMoveMSX = event.getX();
            startMoveMSY = event.getY();

            float distance = (float) Math.sqrt(Math.pow(currentX - startMoveMSX, 2) + Math.pow(currentY - startMoveMSY, 2));

            if (currentTime < ignoreMoveUntil) {
                return;
            }

            if (KeyMouse_state && !virtualTrackpadMode) {
                // Map to content coords (viewport-aware) for all absolute sends below
                float[] absContent = mapToContentCoords(startMoveMSX, startMoveMSY,
                        getTouchViewWidth(), getTouchViewHeight());
                float absX = absContent[0], absY = absContent[1];

                if (keyMouseAbsCtrl){
                    HidManager.sendHexAbsDragData(absX, absY);
                    TextView label = getFloatingLabel();
                    if (label != null) {
                        label.setVisibility(View.VISIBLE);
                    }
                }else {
                    if (DrawMode && currentTime - longPressStartTime >= 2000 && distance < 30){
                        HidManager.handleTwoPress();
                        TextView label = getFloatingLabel();
                        if (label != null) {
                            label.setVisibility(View.GONE);
                        }
                        return;
                    } else if (DrawMode) {
                        HidManager.sendHexAbsDragData(absX, absY);
                        return;
                    }

                    if (distance < 30) {
                        if (currentTime - longPressStartTime >= 1000){
                            HidManager.sendHexAbsDragData(absX, absY);
                            DrawMode = true;
                        }
                    } else {
                        if (mouseLeftClick){
                            Log.d(TAG, "mouse left click");
                            HidManager.sendHexAbsButtonClickData("SecLeftData", absX, absY);
                        }else if (mouseRightClick){
                            Log.d(TAG, "mouse right click");
                            HidManager.sendHexAbsButtonClickData("SecRightData", absX, absY);
                        } else if (mouseScrollClick) {
                            Log.d(TAG, "mouse scroll click");
                            HidManager.sendHexAbsButtonClickData("SecMiddleData", absX, absY);
                        }else {
                            HidManager.sendHexAbsData(absX, absY);
                        }
                        longPressStartTime = currentTime;
                    }
                }
            } else {
                // Relative mode or virtual trackpad: apply zoom-aware scaling
                float zoomScale = getEffectiveZoomScale();
                float effectiveDeltaScale = 1.0f / zoomScale;
                float adjX = lastMoveMSX + (startMoveMSX - lastMoveMSX) * effectiveDeltaScale;
                float adjY = lastMoveMSY + (startMoveMSY - lastMoveMSY) * effectiveDeltaScale;
                HidManager.sendHexRelData("SecNullData", adjX, adjY, lastMoveMSX, lastMoveMSY);
                lastMoveMSX = startMoveMSX;
                lastMoveMSY = startMoveMSY;
            }
        }
    }

    // Helper method to calculate distance between two points
    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Apply zoom using TextureView.setTransform(Matrix) for GPU-quality scaling.
     * When zoomed in, the TextureView's physical size is expanded to show more vertical content.
     */
    private void applyPortraitZoomTransform() {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;

        // 先计算目标尺寸，返回 [targetWidth, targetHeight]；scale<=1 时返回 null（用 MATCH_PARENT）
        int[] targetSize = expandTextureViewToTarget();

        // 用目标尺寸作为 Matrix 的 pivot 中心（layout 是异步的，getWidth/getHeight 返回的还是旧值）
        float pivotX, pivotY;
        if (targetSize != null) {
            pivotX = targetSize[0] / 2f;
            pivotY = targetSize[1] / 2f;
        } else {
            // scale <= 1，回落到当前 view 尺寸（MATCH_PARENT 状态）
            pivotX = textureView.getWidth() / 2f;
            pivotY = textureView.getHeight() / 2f;
        }

        if (pivotX <= 0 || pivotY <= 0) {
            return;
        }

        // Create transform matrix
        Matrix matrix = new Matrix();

        // Uniform scaling (scaleX == scaleY)
        float scale = mPortraitZoomScale;

        matrix.setScale(scale, scale, pivotX, pivotY);
        matrix.postTranslate(mPortraitTranslateX, mPortraitTranslateY);

        // Apply the matrix to TextureView (GPU-accelerated, bilinear filtering)
        textureView.setTransform(matrix);
        textureView.invalidate();

        // Sync the PiP indicator with the main view's zoom/pan state
        ZoomLayoutDeal.updateIndicatorFromMainView(
            scale,
            mPortraitTranslateX,
            mPortraitTranslateY
        );

        Log.d(TAG, "Portrait zoom applied: scale=" + scale +
              " pivot=(" + pivotX + "," + pivotY + ")" +
              " translateX=" + mPortraitTranslateX +
              " translateY=" + mPortraitTranslateY);
    }

    /**
     * Expand TextureView's physical size when zoomed in, so more vertical content is visible.
     * Both width and height are expanded proportionally to maintain the camera's aspect ratio,
     * avoiding distortion. The parent's clipChildren clips any excess width beyond screen bounds.
     *
     * @return int[]{targetWidth, targetHeight} if expanded, or null if scale <= 1 (reset to MATCH_PARENT).
     */
    private int[] expandTextureViewToTarget() {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return null;
        }

        AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;
        View container = activity.mBinding.videoAreaContainer;

        if (container == null) return null;

        // Get screen dimensions
        WindowManager wm = (WindowManager) activity.getSystemService("window");
        if (wm == null) return null;
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // 用视频自然尺寸作为最小值（不缩放时的状态）
        // 自然高度 = screenWidth / aspectRatio
        int naturalHeight;
        int naturalWidth = screenWidth;
        if (activity.getPreviewHeight() > 0 && activity.getPreviewWidth() > 0) {
            float aspectRatio = (float) activity.getPreviewWidth() / activity.getPreviewHeight();
            naturalHeight = (int) (naturalWidth / aspectRatio);
        } else {
            // fallback：用当前 TextureView 的实际尺寸
            naturalWidth = textureView.getWidth();
            naturalHeight = textureView.getHeight();
        }
        if (naturalHeight <= 0 || naturalWidth <= 0) return null;

        // Calculate expanded size based on zoom scale
        // scale = 1.0 → size = naturalSize (original, matches camera aspect ratio)
        // scale = MAX_SCALE → height = MAX_HEIGHT_RATIO * screenHeight, width scales proportionally
        // Linear interpolation between these two points
        final float MAX_SCALE = 3.0f;
        final float MAX_HEIGHT_RATIO = 0.5f;  // Cap at 50% of screen height

        int maxHeight = (int) (screenHeight * MAX_HEIGHT_RATIO);
        int minHeight = naturalHeight;

        float scale = mPortraitZoomScale;
        float t = (scale - 1.0f) / (MAX_SCALE - 1.0f);  // 0 to 1 as scale goes from 1.0 to MAX_SCALE
        t = Math.max(0f, Math.min(1f, t));

        int targetHeight = minHeight + (int) ((maxHeight - minHeight) * t);
        // 宽高同比扩展，保持摄像头画面的宽高比，避免拉伸变形
        float heightScale = (float) targetHeight / naturalHeight;
        int targetWidth = (int) (naturalWidth * heightScale);

        // Update LayoutParams if size changed
        ViewGroup.LayoutParams params = textureView.getLayoutParams();
        // 缩放接近 1.0 时恢复宽高比约束，让 onMeasure 按摄像头比例计算尺寸
        if (scale <= 1.01f) {
            textureView.setAspectRatioEnabled(true);
            if (params.height != ViewGroup.LayoutParams.MATCH_PARENT
                    || params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                textureView.setLayoutParams(params);
                Log.d(TAG, "Reset TextureView to MATCH_PARENT (scale=1.0)");
            }
            return null;
        } else if (params.height != targetHeight || params.width != targetWidth) {
            // 放大时禁用宽高比约束，让扩展的尺寸生效
            textureView.setAspectRatioEnabled(false);
            params.height = targetHeight;
            params.width = targetWidth;
            textureView.setLayoutParams(params);
            Log.d(TAG, "Expanded TextureView: " + targetWidth + "x" + targetHeight + " (scale=" + scale + ")");
        }
        return new int[]{targetWidth, targetHeight};
    }

    /**
     * Reset TextureView's size back to match_parent when zoomed out.
     */
    private void resetTextureViewHeight() {
        if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
            return;
        }

        AspectRatioTextureView textureView = activity.mBinding.viewMainPreview;
        // 恢复宽高比约束，让 onMeasure 重新按摄像头比例计算尺寸
        textureView.setAspectRatioEnabled(true);
        ViewGroup.LayoutParams params = textureView.getLayoutParams();
        if (params.height != ViewGroup.LayoutParams.MATCH_PARENT
                || params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            textureView.setLayoutParams(params);
            Log.d(TAG, "Reset TextureView to MATCH_PARENT and re-enabled aspect ratio");
        }
    }

    // Method to apply zoom to your view
    private void adjustZoom(float zoomFactor) {
        // In portrait mode, use TextureView.setTransform(Matrix) for quality scaling
        if (isPortraitMode) {
            if (activity == null || activity.mBinding == null || activity.mBinding.viewMainPreview == null) {
                return;
            }

            // Define scale limits
            float MIN_SCALE = 1f;
            float MAX_SCALE = 3.0f;

            // Calculate new scale
            float currentScale = mPortraitZoomScale;
            float newScale = currentScale * zoomFactor;

            // Clamp within bounds
            if (newScale < MIN_SCALE) newScale = MIN_SCALE;
            if (newScale > MAX_SCALE) newScale = MAX_SCALE;

            // Store scale for auto-pan calculation
            mPortraitZoomScale = newScale;

            // If zooming out to minimum, hide PiP and reset translation
            if (newScale <= MIN_SCALE) {
                mPortraitTranslateX = 0f;
                mPortraitTranslateY = 0f;
                ZoomLayoutDeal.zoomOut();
                resetTextureViewHeight();  // Reset TextureView height when zoomed out
            } else {
                // Show PiP when zoomed in
                ZoomLayoutDeal.enlargeView();
            }

            // Apply transform using Matrix (GPU-quality scaling)
            applyPortraitZoomTransform();

            Log.d(TAG, "Portrait zoom adjusted: newScale=" + newScale +
                  " translateX=" + mPortraitTranslateX +
                  " translateY=" + mPortraitTranslateY);
            return;
        }

        // Landscape mode - use existing drawerLayout zoom logic
        if (drawerLayout == null) {
            Log.d(TAG, "adjustZoom: drawerLayout not available, skipping zoom");
            return;
        }

        // Define scale limits
        float MIN_SCALE = 1f; // Minimum zoom level
        float MAX_SCALE = 2.0f; // Maximum zoom level

        // Get current scale from the view (assuming drawerLayout is being scaled)
        float currentScale = drawerLayout.getScaleX(); // Assume uniform scaling (X = Y)
        float newScale = currentScale * zoomFactor;

        // Clamp the new scale within bounds
        if (newScale < MIN_SCALE){
            newScale = MIN_SCALE;
            ZoomLayoutDeal.zoomOut();
        }

        if (newScale > MAX_SCALE){
            newScale = MAX_SCALE;
            ZoomLayoutDeal.enlargeView();
        }

        // If zoomed in at all, show the thumbnail window
        if (newScale > MIN_SCALE) {
            ZoomLayoutDeal.enlargeView();
        }

        // Apply the new scale
        drawerLayout.setScaleX(newScale);
        drawerLayout.setScaleY(newScale);

        // Ensure pivot is centered for proper scaling
        drawerLayout.setPivotX(drawerLayout.getWidth() / 2f);
        drawerLayout.setPivotY(drawerLayout.getHeight() / 2f);

        // Enforce clipping to prevent white edges from showing
        drawerLayout.setClipChildren(true);
        drawerLayout.setClipToPadding(true);

        Log.d(TAG, "Zoom adjusted: newScale=" + newScale);
    }

    private void handActionPointerUpMouse(MotionEvent event){
        if (event.getPointerCount() == 2) {
            if ((rightReleaseCurrentTime - rightClickCurrentTime < 500) && !hasHandledMove) {
                HidManager.handleTwoPress();//deal right click
                Log.d(TAG, "this release the right click");
            }
            isPanning = false;
            hasHandledMove = false;
            Log.d(TAG, "ACTION_POINTER_UP");
            ignoreMoveUntil = System.currentTimeMillis() + 200;
            rightReleaseCurrentTime = System.currentTimeMillis();

        }
    }

    private void handActionUpMouse(MotionEvent event){
        // Map to content coords once for all absolute sends below (viewport-aware)
        float[] upContent = mapToContentCoords(startMoveMSX, startMoveMSY,
                getTouchViewWidth(), getTouchViewHeight());
        float upX = upContent[0], upY = upContent[1];

        // double-click end processing
        dragModeEndTime = System.currentTimeMillis();
        if (isDoubleClickPhase && ( dragModeEndTime - dragModeStartTime) > 500) {
            if (KeyMouse_state && !virtualTrackpadMode){
                HidManager.sendHexAbsData(upX, upY);//release abs state
            }else {
                HidManager.sendHexRelData("SecNullData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
            }
            isDoubleClickPhase = false;
            TextView label = getFloatingLabel();
            if (label != null) {
                label.setVisibility(View.GONE);
            }
            hasEnteredDragMode = false;
            lastMoveMSX = 0;
            lastMoveMSY = 0;
            return;
        }

        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
            Log.d(TAG, "Double click at the same position");

            if (KeyMouse_state && !virtualTrackpadMode) {
                HidManager.handleDoubleClickAbs(upX, upY);
            } else {
                HidManager.handleDoubleClickRel();
            }

        }

        if (KeyMouse_state && !virtualTrackpadMode) {
            HidManager.sendHexAbsData(upX, upY);
        }else{
            HidManager.sendHexRelData("SecNullData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
        }
        DrawMode = false;
        TextView label = getFloatingLabel();
        if (label != null) {
            label.setVisibility(View.GONE);
        }
        longPressStartTime = 0;
        lastMoveMSX = 0;
        lastMoveMSY = 0;
        lastClickTime = clickTime;
        isPanning = false;
        hasEnteredDragMode = false;
        resetMouseClickState();
    }

    //reset mouse click state
    private void resetMouseClickState(){
        mouseLeftClick = false;
        mouseRightClick = false;
        mouseScrollClick = false;
    }
}
