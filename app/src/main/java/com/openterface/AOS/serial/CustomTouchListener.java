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
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;    // double click time threshold
    private static final long TWO_FINGER_PRESS_DELAY = 750;     // two finger press delay
    private static final long MOVE_DELAY = 50; //move delay

    //coordinate record
    private float firstClickX, firstClickY;
    private float startX1, startX2, startY1, startY2;
    private float currentX, currentY;
    private float startMoveMSX, startMoveMSY;
    private static float lastMoveMSX, lastMoveMSY;

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
    private long lastMoveTime = 0; // To store the last execution time
    private long ignoreMoveUntil = 0;
    private long firstClickTime = 0;

    //click scope
    private static final float CLICK_RADIUS = 100f;           // Click radius
    private static final float PAN_SENSITIVITY = 100f;        // Sliding sensitivity
    private static final float DRAG_THRESHOLD = 10f;

    //zoom set
    private final DrawerLayout drawerLayout;

    private long rightClickCurrentTime;
    private long rightReleaseCurrentTime;
    private long dragModeStartTime;
    private long dragModeEndTime;
    private boolean hasEnteredDragMode = false;

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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handActionDownMouse(event);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                handActionPointerDownMouse(event);
                break;

            case MotionEvent.ACTION_MOVE:
                handActionMoveMouse(event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                handActionPointerUpMouse(event);
                break;

            case MotionEvent.ACTION_UP:
                handActionUpMouse(event);
                break;

            case MotionEvent.ACTION_CANCEL:
                handActionUpMouse(event);
                break;
        }
        return true;
    }

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

//            handler.postDelayed(twoFingerPressRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    if (!hasHandledMove) {
//                        MouseManager.handleTwoPress();//deal right click
//                    }
//                }
//            }, TWO_FINGER_PRESS_DELAY);
        }
    }

    private void handActionMoveMouse(MotionEvent event){
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
                System.out.println("double click drag AbsDragData");
                MouseManager.sendHexAbsDragData(startMoveMSX, startMoveMSY);
            } else {
                System.out.println("double click drag RelDragData");
                MouseManager.sendHexRelData("SecLeftData", startMoveMSX, startMoveMSY, lastMoveMSX, lastMoveMSY);
                lastMoveMSX = startMoveMSX;
                lastMoveMSY = startMoveMSY;
            }

            if (floatingLabel != null) {
                floatingLabel.setVisibility(View.VISIBLE);
            }

            return;
        }

        long currentTime = System.currentTimeMillis();
        if (isPanning && event.getPointerCount() == 2) {  //deal Middle click

//            float y1 = event.getY(0) - startY1;
//            float y2 = event.getY(1) - startY2;
//            startMoveMSY = event.getY();
//            if (((y1 > PAN_SENSITIVITY && y2 > PAN_SENSITIVITY) || (y1 < -PAN_SENSITIVITY && y2 < -PAN_SENSITIVITY))) {
//                // Check if enough time has elapsed since the last move
//                if (currentTime - lastMoveTime >= MOVE_DELAY) {
//                    handler.removeCallbacks(twoFingerPressRunnable);
//                    Log.d(TAG, "ACTION_MOVE");
//
//                    MouseManager.handleDoubleFingerPan(startMoveMSY, lastMoveMSY);
//                    hasHandledMove = true;
//                    lastMoveMSY = startMoveMSY;
//
//                    // Update lastMoveTime to the current time
//                    lastMoveTime = currentTime;
//                }
//            }

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
                Log.d(TAG, "Rel data send now");
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