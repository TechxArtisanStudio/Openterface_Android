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

import androidx.drawerlayout.widget.DrawerLayout;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.drawerLayout.ZoomLayoutDeal;
import com.openterface.AOS.target.MouseManager;

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

    private static final String TAG = CustomTouchListener.class.getSimpleName();
    private static boolean KeyMouse_state, keyMouseAbsCtrl;

    //system module
    private static UsbDeviceManager usbDeviceManager;
    private Handler handler = new Handler();
    private Runnable twoFingerPressRunnable;
    private final TextView floatingLabel;
    private final MainActivity activity;

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

    public static void KeyMouse_state(boolean keyMouseState, boolean keyMouseAbsCtrlState) {
        KeyMouse_state = keyMouseState;
        keyMouseAbsCtrl = keyMouseAbsCtrlState;
    }

    public CustomTouchListener(MainActivity activity, UsbDeviceManager usbDeviceManager) {
        CustomTouchListener.usbDeviceManager = usbDeviceManager;
        floatingLabel = activity.findViewById(R.id.floating_label);
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        this.activity = activity;
    }

    public static boolean handleGenericMotionEvent(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float scrollAmount = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (scrollAmount != 0) {
                Log.d("MouseEvent", "Scroll amount: " + scrollAmount);
                MouseManager.handleTwoFingerPanSlideUpDown(scrollAmount);
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
                        if(KeyMouse_state){
                            if(keyMouseAbsCtrl){
                                MouseManager.sendHexAbsDragData(cursorX, cursorY);
                            }else {
                                MouseManager.sendHexAbsData(cursorX, cursorY);
                            }
                        }else {
                            MouseManager.sendHexRelData("SecNullData", cursorX, cursorY, lastMoveMSX, lastMoveMSY);
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
        // Once a two-finger sequence starts, ALL events in that sequence go to two-finger handler
        // even if the pointer count drops to 1 when one finger lifts
        if (pointerCount == 2 || action == MotionEvent.ACTION_POINTER_DOWN) {
            inTwoFingerSequence = true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            inTwoFingerSequence = false;
        }

        if (inTwoFingerSequence) {
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

                // Cancel any pending long press
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }

                detectDoubleClick(event);
                initHandActionDownMouse(event);
                handActionDownMouse(event);

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
                // Cancel long press detection
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }

                long duration = event.getEventTime() - tapDownTime;
                float distLifted = dist(x, y, tapDownX, tapDownY);

                // KeyCmd-style: if double-tap just fired, suppress single-tap and release only
                if (suppressSingleTapFromDoubleTap) {
                    suppressSingleTapFromDoubleTap = false;
                    if (isDragMode) {
                        // Release any held button
                        releaseMouseAndReset();
                    }
                    isDragMode = false;
                    lastMoveX = 0;
                    lastMoveY = 0;
                    // Don't call handActionUpMouse — it would send an unwanted move delta
                    break;
                }

                if (isDragMode) {
                    // KeyCmd-style: long-press drag mode stays on until tap exits
                    Log.d(TAG, "Drag ended, releasing left button");
                    releaseMouseAndReset();
                    isDragMode = false;
                    // Don't call handActionUpMouse — it would send an unwanted move delta
                    // from startMoveMSX (down position) to lastMoveMSX (drag position), causing a jump
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
                    // Movement happened — just release any held mouse state
                    MouseManager.releaseMSRelData();
                    lastMoveMSX = 0;
                    lastMoveMSY = 0;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                    longPressRunnable = null;
                }
                cancelPendingSingleTap();
                // KeyCmd-style: release mouse state on cancel
                if (isDragMode) {
                    releaseMouseAndReset();
                    isDragMode = false;
                } else {
                    // Just release — don't call handActionUpMouse which sends unwanted move
                    MouseManager.releaseMSRelData();
                }
                lastMoveMSX = 0;
                lastMoveMSY = 0;
                lastMoveX = 0;
                lastMoveY = 0;
                break;
        }
        return true;
    }

    /**
     * Normal mouse move (no button pressed)
     */
    private void handleNormalMove(float x, float y) {
        if (KeyMouse_state) {
            MouseManager.sendHexAbsData(x, y);
        } else {
            MouseManager.sendHexRelData("SecNullData", x, y, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    /**
     * Drag mode mouse move (left button held)
     */
    private void handleDragMove(float x, float y) {
        if (KeyMouse_state) {
            // In drag mode with absolute: send with left button pressed
            MouseManager.sendHexAbsButtonClickData("SecLeftData", x, y);
        } else {
            // In drag mode with relative: send with left button pressed
            MouseManager.sendHexRelData("SecLeftData", x, y, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    /**
     * 2-finger gesture handler (right-click / scroll).
     * KeyCmd-style: uses accumulator to distinguish tap from scroll.
     */
    private boolean handleTwoFingerGesture(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger came down — cancel any pending single-tap
                cancelPendingSingleTap();

                twoFingerDownCenterX = (event.getX(0) + event.getX(1)) / 2f;
                twoFingerDownCenterY = (event.getY(0) + event.getY(1)) / 2f;
                twoFingerPrevX = twoFingerDownCenterX;
                twoFingerPrevY = twoFingerDownCenterY;
                twoFingerScrollAccumX = 0f;
                twoFingerScrollAccumY = 0f;
                twoFingerMoved = false;
                isTwoFingerScrolling = false;
                rightClickCurrentTime = System.currentTimeMillis();

                handActionPointerDownMouse(event);
                break;

            case MotionEvent.ACTION_MOVE:
                float cx = (event.getX(0) + event.getX(1)) / 2f;
                float cy = (event.getY(0) + event.getY(1)) / 2f;

                // Check if moved significantly from start position
                float totalDist = dist(cx, cy, twoFingerDownCenterX, twoFingerDownCenterY);
                if (totalDist > TWO_FINGER_TAP_MOVE_THRESHOLD) {
                    twoFingerMoved = true;
                }

                float dx = cx - twoFingerPrevX;
                float dy = cy - twoFingerPrevY;

                // Accumulate scroll values (KeyCmd approach)
                twoFingerScrollAccumX += dx / 3f;
                twoFingerScrollAccumY += -dy / 3f;

                int scrollX = (int) twoFingerScrollAccumX;
                int scrollY = (int) twoFingerScrollAccumY;

                if (scrollX != 0) twoFingerScrollAccumX -= scrollX;
                if (scrollY != 0) twoFingerScrollAccumY -= scrollY;

                if (scrollX != 0 || scrollY != 0) {
                    isTwoFingerScrolling = true;
                    MouseManager.handleTwoFingerPanSlideUpDown(scrollY);
                }

                twoFingerPrevX = cx;
                twoFingerPrevY = cy;

                // Also handle original move logic (zoom, drag)
                handActionMoveMouse(event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                handleTwoFingerRelease();
                handActionPointerUpMouse(event);
                break;

            case MotionEvent.ACTION_UP:
                // Last finger lifted — also a two-finger gesture end
                handleTwoFingerRelease();
                handActionUpMouse(event);
                break;
        }
        return true;
    }

    /**
     * Decide whether two-finger gesture should trigger right-click.
     * Only right-click if there was NO significant movement AND NO scrolling.
     */
    private void handleTwoFingerRelease() {
        long pressDuration = System.currentTimeMillis() - rightClickCurrentTime;
        if (pressDuration < 500 && !twoFingerMoved && !isTwoFingerScrolling) {
            MouseManager.handleTwoPress();
            Log.d(TAG, "Two-finger tap -> right click");
        } else {
            Log.d(TAG, "Two-finger ended: moved=" + twoFingerMoved
                    + " scrolled=" + isTwoFingerScrolling + " duration=" + pressDuration);
        }
    }

    private void handleSingleClick(float x, float y) {
        Log.d(TAG, "Single click at (" + x + "," + y + ")");
        if (KeyMouse_state) {
            MouseManager.sendHexAbsButtonClickData("SecLeftData", x, y);
            handler.postDelayed(() -> MouseManager.sendHexAbsData(x, y), 50);
        } else {
            MouseManager.sendHexRelData("SecLeftData", x, y, lastMoveMSX, lastMoveMSY);
            handler.postDelayed(() -> MouseManager.releaseMSRelData(), 50);
        }
    }

    private void handleDoubleClick() {
        Log.d(TAG, "Double click!");
        // KeyCmd-style: suppress single-tap after double-tap
        suppressSingleTapFromDoubleTap = true;
        if (KeyMouse_state) {
            MouseManager.handleDoubleClickAbs(startMoveMSX, startMoveMSY);
        } else {
            MouseManager.handleDoubleClickRel();
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
        if (KeyMouse_state) {
            // In absolute mode: send a move to current position with no buttons pressed
            // We use the last known position to avoid cursor jump
            MouseManager.sendHexAbsData(lastMoveMSX, lastMoveMSY);
        } else {
            // In relative mode: just release buttons, no movement
            MouseManager.releaseMSRelData();
        }
    }

    // --- Original methods below (unchanged) ---

    private void handActionDownMouse(MotionEvent event){
        detectDoubleClick(event);

        initHandActionDownMouse(event);

        int buttonState = event.getButtonState();
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            mouseLeftClick = true;
            Log.d("MouseEvent", "Left button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            mouseRightClick = true;
            Log.d("MouseEvent", "Right button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            mouseScrollClick = true;
            Log.d("MouseEvent", "Middle button pressed");
        }

        if (KeyMouse_state){
            Log.d("handActionDownMouse", "KeyMouse_state");
            MouseManager.sendHexAbsData(startMoveMSX, startMoveMSY);
        }
    }

    //deal double click move
    private void detectDoubleClick(MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - firstClickTime < DOUBLE_CLICK_TIME_DELTA
                && Math.abs(event.getX() - firstClickX) < CLICK_RADIUS
                && Math.abs(event.getY() - firstClickY) < CLICK_RADIUS) {
            isDoubleClickPhase = true;
            Log.d("detectDoubleClick", "this is isDoubleClickPhase");
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
                Log.d("handActionMoveMouse", "dragModeStartTime: " + dragModeStartTime);
            }
            startMoveMSX = event.getX();
            startMoveMSY = event.getY();

            if (KeyMouse_state) {
                MouseManager.sendHexAbsDragData(startMoveMSX, startMoveMSY);
            } else {
                MouseManager.sendHexRelData("SecLeftData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
                lastMoveMSX = startMoveMSX;
                lastMoveMSY = startMoveMSY;
            }

            if (floatingLabel != null) {
                floatingLabel.setVisibility(View.VISIBLE);
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

            if (KeyMouse_state) {
                if (keyMouseAbsCtrl){
                    MouseManager.sendHexAbsDragData(startMoveMSX, startMoveMSY);
                    floatingLabel.setVisibility(View.VISIBLE);
                }else {
                    if (DrawMode && currentTime - longPressStartTime >= 2000 && distance < 30){
                        MouseManager.handleTwoPress();
                        floatingLabel.setVisibility(View.GONE);
                        return;
                    } else if (DrawMode) {
                        MouseManager.sendHexAbsDragData(startMoveMSX, startMoveMSY);
                        return;
                    }

                    if (distance < 30) {
                        if (currentTime - longPressStartTime >= 1000){
                            MouseManager.sendHexAbsDragData(startMoveMSX, startMoveMSY);
                            DrawMode = true;
                        }
                    } else {
                        if (mouseLeftClick){
                            Log.d("mouse", "mouse left click");
                            MouseManager.sendHexAbsButtonClickData("SecLeftData", startMoveMSX, startMoveMSY);
                        }else if (mouseRightClick){
                            Log.d("mouse", "mouse right click");
                            MouseManager.sendHexAbsButtonClickData("SecRightData", startMoveMSX, startMoveMSY);
                        } else if (mouseScrollClick) {
                            Log.d("mouse", "mouse scroll click");
                            MouseManager.sendHexAbsButtonClickData("SecMiddleData", startMoveMSX, startMoveMSY);
                        }else {
                            MouseManager.sendHexAbsData(startMoveMSX, startMoveMSY);
                        }
                        longPressStartTime = currentTime;
                    }
                }
            } else {
                MouseManager.sendHexRelData("SecNullData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
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

    // Method to apply zoom to your view
    private void adjustZoom(float zoomFactor) {
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

        // Apply the new scale
        drawerLayout.setScaleX(newScale);
        drawerLayout.setScaleY(newScale);

        // Ensure pivot is centered for proper scaling
        drawerLayout.setPivotX(drawerLayout.getWidth() / 2f);
        drawerLayout.setPivotY(drawerLayout.getHeight() / 2f);

        Log.d(TAG, "Zoom adjusted: newScale=" + newScale);
    }

    private void handActionPointerUpMouse(MotionEvent event){
        if (event.getPointerCount() == 2) {
            if ((rightReleaseCurrentTime - rightClickCurrentTime < 500) && !hasHandledMove) {
                MouseManager.handleTwoPress();//deal right click
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
        // double-click end processing
        dragModeEndTime = System.currentTimeMillis();
        if (isDoubleClickPhase && ( dragModeEndTime - dragModeStartTime) > 500) {
            if (KeyMouse_state){
                MouseManager.sendHexAbsData(startMoveMSX, startMoveMSY);//release abs state
            }else {
                MouseManager.sendHexRelData("SecNullData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
            }
            isDoubleClickPhase = false;
            if (floatingLabel != null) {
                floatingLabel.setVisibility(View.GONE);
            }
            hasEnteredDragMode = false;
            lastMoveMSX = 0;
            lastMoveMSY = 0;
            return;
        }

        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
            Log.d(TAG, "Double click at the same position");

            if (KeyMouse_state) {
                MouseManager.handleDoubleClickAbs(startMoveMSX, startMoveMSY);
            } else {
                MouseManager.handleDoubleClickRel();
            }

        }

        if (KeyMouse_state) {
            MouseManager.sendHexAbsData(startMoveMSX, startMoveMSY);
        }else{
            MouseManager.sendHexRelData("SecNullData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
        }
        DrawMode = false;
        floatingLabel.setVisibility(View.GONE);
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
