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
    private float startY1, startY2;
    private boolean isPanning = false;
    private boolean hasHandledMove = false;
    private Handler handler = new Handler();
    private Runnable twoFingerPressRunnable;
    private boolean isLongPress = false;
    private float StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY,
            LastClickX, LastClickY;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private static final long TWO_FINGER_PRESS_DELAY = 500; // 1 second
    private static UsbDeviceManager usbDeviceManager;
    private static boolean KeyMouse_state;
    private static final float CLICK_POSITION_THRESHOLD = 50.0f;

    public static void KeyMouse_state(boolean keyMouseState) {
        KeyMouse_state = keyMouseState;
    }

    public CustomTouchListener(UsbDeviceManager usbDeviceManager) {
        this.usbDeviceManager = usbDeviceManager;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isLongPress = false;
                StartMoveMSX = event.getX();
                StartMoveMSY = event.getY();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    startY1 = event.getY(0);
                    startY2 = event.getY(1);
                    isPanning = true;
                    hasHandledMove = false;

                    handler.postDelayed(twoFingerPressRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!hasHandledMove) {
                                Log.d(TAG, "two click right");
                                MouseManager.handleTwoPress();
                            }
                        }
                    }, TWO_FINGER_PRESS_DELAY);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isPanning && event.getPointerCount() == 2) {
                    float y1 = event.getY(0) - startY1;
                    float y2 = event.getY(1) - startY2;

//                    String rollingGearY;
//                    if (y1 > 0 && y2 > 0) {
//                        rollingGearY = "SlideUp";
//                    } else {
//                        rollingGearY = "Downward";
//                    }
                    StartMoveMSY = event.getY();

                    if (!hasHandledMove && ((y1 > 0 && y2 > 0) || (y1 < 0 && y2 < 0))) {
                        handler.removeCallbacks(twoFingerPressRunnable);
                        Log.d(TAG, "ACTION_MOVE");
                        MouseManager.handleDoubleFingerPan(StartMoveMSY, LastMoveMSY);
                        hasHandledMove = true;
                        LastMoveMSY = StartMoveMSY;
                    }
                } else if (!isLongPress) {
                    StartMoveMSX = event.getX();
                    StartMoveMSY = event.getY();
                    if (KeyMouse_state) {
                        MouseManager.sendHexAbsData(StartMoveMSX, StartMoveMSY);
                    } else {
                        MouseManager.sendHexRelData(StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
                        LastMoveMSX = StartMoveMSX;
                        LastMoveMSY = StartMoveMSY;
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2) {
                    isPanning = false;
                    hasHandledMove = false;
                    handler.removeCallbacks(twoFingerPressRunnable);
                    Log.d(TAG, "ACTION_POINTER_UP");
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(twoFingerPressRunnable);
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
                    if (Math.abs(StartMoveMSX - LastClickX) <= CLICK_POSITION_THRESHOLD && Math.abs(StartMoveMSY - LastClickY) <= CLICK_POSITION_THRESHOLD) {
                        Log.d(TAG, "Double click at the same position");
                        if (KeyMouse_state) {
                            MouseManager.handleDoubleClickAbs(StartMoveMSX, StartMoveMSY);
                        } else {
                            MouseManager.handleDoubleClickRel();
                        }
                    } else {
                        Log.d(TAG, "Double click at different positions");
                    }
                }
                LastMoveMSX = 0;
                LastMoveMSY = 0;
                LastClickX = StartMoveMSX;
                LastClickY = StartMoveMSY;
                lastClickTime = clickTime;
                isPanning = false;
                break;
        }
        return true;
    }
}
