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
import com.openterface.AOS.target.MouseManager;

public class CustomTouchListener implements View.OnTouchListener {

    private static final String TAG = CustomTouchListener.class.getSimpleName();
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private static final long TWO_FINGER_PRESS_DELAY = 750; // 0.75 second
    private static UsbDeviceManager usbDeviceManager;
    private static boolean KeyMouse_state, keyMouseAbsCtrl;

    private float startY1, startY2;
    private boolean isPanning = false;
    private boolean hasHandledMove = false;
    private Handler handler = new Handler();
    private Runnable twoFingerPressRunnable;
    private boolean isLongPress = false;
    private float StartMoveMSX;
    private float StartMoveMSY;
    private static float LastMoveMSX;
    private static float LastMoveMSY;
    private long lastClickTime = 0;

    private long lastMoveTime = 0; // To store the last execution time
    private static final long MOVE_DELAY = 50; // 0.05 seconds in milliseconds

    private long ignoreMoveUntil = 0;
    private long longPressStartTime;
    private float currentX, currentY;

    private boolean DrawMode = false;

    private final TextView floating_label;

    private boolean MouseLeftClcik, MouseRightClick, MouseScrollClick = false;

    private boolean isDoubleClickPhase = false;
    private float firstClickX, firstClickY;
    private long firstClickTime = 0;

    public static void KeyMouse_state(boolean keyMouseState, boolean keyMouseAbsCtrlState) {
        KeyMouse_state = keyMouseState;
        keyMouseAbsCtrl = keyMouseAbsCtrlState;
    }

    public CustomTouchListener(MainActivity activity, UsbDeviceManager usbDeviceManager) {
        CustomTouchListener.usbDeviceManager = usbDeviceManager;
        floating_label = activity.findViewById(R.id.floating_label);
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
                            MouseManager.sendHexRelData("SecNullData", cursorX, cursorY, LastMoveMSX, LastMoveMSY);
                            LastMoveMSX = cursorX;
                            LastMoveMSY = cursorY;
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
        long currentTime = System.currentTimeMillis();
        if (currentTime - firstClickTime < DOUBLE_CLICK_TIME_DELTA
                && Math.abs(event.getX() - firstClickX) < 100
                && Math.abs(event.getY() - firstClickY) < 100) {
            isDoubleClickPhase = true;
        } else {
            firstClickTime = currentTime;
            firstClickX = event.getX();
            firstClickY = event.getY();
        }

        System.out.println("this is action down");
        isLongPress = false;
        StartMoveMSX = event.getX();
        StartMoveMSY = event.getY();
        longPressStartTime = System.currentTimeMillis();
        currentX = event.getX();
        currentY = event.getY();
        int buttonState = event.getButtonState();

        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            MouseLeftClcik = true;
            Log.d("MouseEvent", "Left button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            MouseRightClick = true;
            Log.d("MouseEvent", "Right button pressed");
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            MouseScrollClick = true;
            Log.d("MouseEvent", "Middle button pressed");
        }

        if (KeyMouse_state){
            MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
        }
    }

    private void handActionPointerDownMouse(MotionEvent event){
        if (event.getPointerCount() == 2) {
            startY1 = event.getY(0);
            startY2 = event.getY(1);
            isPanning = true;
            hasHandledMove = false;

            handler.postDelayed(twoFingerPressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!hasHandledMove) {
                        MouseManager.handleTwoPress();
                    }
                }
            }, TWO_FINGER_PRESS_DELAY);
        }
    }

    private void handActionMoveMouse(MotionEvent event){
        // Added double-click drag and drop processing
        if (isDoubleClickPhase && event.getPointerCount() == 1) {
            StartMoveMSX = event.getX();
            StartMoveMSY = event.getY();

            if (KeyMouse_state) {
                System.out.println("double click drag AbsDragData");
                MouseManager.sendHexAbsDragData(event.getX(), event.getY());
            } else {
                System.out.println("double click drag RelDragData");
                MouseManager.sendHexRelData("SecLeftData", StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
                LastMoveMSX = StartMoveMSX;
                LastMoveMSY = StartMoveMSY;
            }

            if (floating_label != null) {
                floating_label.setVisibility(View.VISIBLE);
            }

            return;
        }

        long currentTime = System.currentTimeMillis();
        if (isPanning && event.getPointerCount() == 2) {
            float y1 = event.getY(0) - startY1;
            float y2 = event.getY(1) - startY2;
            StartMoveMSY = event.getY();
            if (((y1 > 100 && y2 > 100) || (y1 < -100 && y2 < -100))) {
                // Check if enough time has elapsed since the last move
                if (currentTime - lastMoveTime >= MOVE_DELAY) {
                    handler.removeCallbacks(twoFingerPressRunnable);
                    Log.d(TAG, "ACTION_MOVE");

                    MouseManager.handleDoubleFingerPan(StartMoveMSY, LastMoveMSY);
                    hasHandledMove = true;
                    LastMoveMSY = StartMoveMSY;

                    // Update lastMoveTime to the current time
                    lastMoveTime = currentTime;
                }
            }
        } else if (!isLongPress) {
            StartMoveMSX = event.getX();
            StartMoveMSY = event.getY();

            float distance = (float) Math.sqrt(Math.pow(currentX - StartMoveMSX, 2) + Math.pow(currentY - StartMoveMSY, 2));

            if (currentTime < ignoreMoveUntil) {
                return;
            }

            if (KeyMouse_state) {
                if (keyMouseAbsCtrl){
                    MouseManager.sendHexAbsDragData(StartMoveMSX, StartMoveMSY);
                }else {
                    if (DrawMode && currentTime - longPressStartTime >= 2000 && distance < 30){
                        MouseManager.handleTwoPress();
                        floating_label.setVisibility(View.GONE);
                        return;
                    } else if (DrawMode) {
                        floating_label.setVisibility(View.VISIBLE);
                        MouseManager.sendHexAbsDragData(StartMoveMSX, StartMoveMSY);
                        return;
                    }

                    if (distance < 30) {
                        if (currentTime - longPressStartTime >= 1000){
                            MouseManager.sendHexAbsDragData(StartMoveMSX, StartMoveMSY);
                            DrawMode = true;
                        }
                    } else {
                        if (MouseLeftClcik){
                            Log.d("mouse", "mouse left click");
                            MouseManager.sendHexAbsButtonClickData("SecLeftData", StartMoveMSX, StartMoveMSY);
                        }else if (MouseRightClick){
                            Log.d("mouse", "mouse right click");
                            MouseManager.sendHexAbsButtonClickData("SecRightData", StartMoveMSX, StartMoveMSY);
                        } else if (MouseScrollClick) {
                            Log.d("mouse", "mouse scroll click");
                            MouseManager.sendHexAbsButtonClickData("SecMiddleData", StartMoveMSX, StartMoveMSY);
                        }else {
                            MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
                        }
                        longPressStartTime = currentTime;
                    }
                }
            } else {
                Log.d(TAG, "Rel data send now");
                MouseManager.sendHexRelData("SecNullData", StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
                LastMoveMSX = StartMoveMSX;
                LastMoveMSY = StartMoveMSY;
            }
        }
    }

    private void handActionPointerUpMouse(MotionEvent event){
        if (event.getPointerCount() == 2) {
            isPanning = false;
            hasHandledMove = false;
            Log.d(TAG, "ACTION_POINTER_UP");
            ignoreMoveUntil = System.currentTimeMillis() + 200;
        }
    }

    private void handActionUpMouse(MotionEvent event){
        // double-click end processing
        if (isDoubleClickPhase) {
            Log.d(TAG, "Double click drag end");
            if (KeyMouse_state){
                MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);//release abs state
            }else {
                MouseManager.sendHexRelData("SecNullData", StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
            }
            isDoubleClickPhase = false;
            if (floating_label != null) {
                floating_label.setVisibility(View.GONE);
            }
            LastMoveMSX = 0;
            LastMoveMSY = 0;
            return;
        }

        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
            Log.d(TAG, "Double click at the same position");

            if (KeyMouse_state) {
                MouseManager.handleDoubleClickAbs(StartMoveMSX, StartMoveMSY);
            } else {
                MouseManager.handleDoubleClickRel();
            }

        }
        if (KeyMouse_state) {
            MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
        }else{
            MouseManager.sendHexRelData("SecNullData", StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
        }
        DrawMode = false;
        floating_label.setVisibility(View.GONE);
        longPressStartTime = 0;
        LastMoveMSX = 0;
        LastMoveMSY = 0;
        lastClickTime = clickTime;
        isPanning = false;
        MouseLeftClcik = false;
        MouseRightClick = false;
        MouseScrollClick = false;
    }
}