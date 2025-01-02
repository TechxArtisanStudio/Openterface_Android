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
import com.openterface.AOS.target.MouseManager;

public class CustomTouchListener implements View.OnTouchListener {

    private static final String TAG = CustomTouchListener.class.getSimpleName();
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private static final long TWO_FINGER_PRESS_DELAY = 750; // 0.5 second
    private static final float CLICK_POSITION_THRESHOLD = 50.0f;
    private static UsbDeviceManager usbDeviceManager;
    private static boolean KeyMouse_state, keyMouseAbsCtrl;

    private float startY1, startY2;
    private boolean isPanning = false;
    private boolean hasHandledMove = false;
    private Handler handler = new Handler();
    private Runnable twoFingerPressRunnable;
    private boolean isLongPress = false;
    private float StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY, LastClickX, LastClickY;
    private long lastClickTime = 0;
    private boolean isDoubleClickHandled = false;
    private long lastDoubleClickTime = 0;

    private long lastMoveTime = 0; // To store the last execution time
    private static final long MOVE_DELAY = 50; // 0.05 seconds in milliseconds

    private long ignoreMoveUntil = 0;

    long lastSendTime = System.currentTimeMillis();

    public static void KeyMouse_state(boolean keyMouseState, boolean keyMouseAbsCtrlState) {
        KeyMouse_state = keyMouseState;
        keyMouseAbsCtrl = keyMouseAbsCtrlState;
    }

    public CustomTouchListener(UsbDeviceManager usbDeviceManager) {
        CustomTouchListener.usbDeviceManager = usbDeviceManager;
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
                if (KeyMouse_state){
                    MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
                    System.out.println("this is action up");
                }else {
                    handActionUpMouse(event);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                handActionUpMouse(event);
                break;
        }
        return true;
    }

    private void handActionDownMouse(MotionEvent event){
        System.out.println("this is action down");
        isLongPress = false;
        StartMoveMSX = event.getX();
        StartMoveMSY = event.getY();
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
            if (currentTime < ignoreMoveUntil) {
                return;
            }

            if (KeyMouse_state) {
                if (keyMouseAbsCtrl){
                    MouseManager.sendHexAbsDragData(StartMoveMSX, StartMoveMSY);
                }else {
                    MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
                }
            } else {
                Log.d(TAG, "Rel data send now");
                MouseManager.sendHexRelData(StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
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
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
                Log.d(TAG, "Double click at the same position");

                if (KeyMouse_state) {
                    MouseManager.handleDoubleClickAbs(StartMoveMSX, StartMoveMSY);
                } else {
                    MouseManager.handleDoubleClickRel();
                }

                isDoubleClickHandled = true;
                lastDoubleClickTime = clickTime;

        }
        LastMoveMSX = 0;
        LastMoveMSY = 0;
        LastClickX = StartMoveMSX;
        LastClickY = StartMoveMSY;
        lastClickTime = clickTime;
        isPanning = false;
    }
}
