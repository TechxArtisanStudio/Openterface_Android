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
    private Runnable longPressRunnable;
    private boolean isLongPress = false;
    private float x, y;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private static UsbDeviceManager usbDeviceManager;
    private Runnable runnable;
    private static boolean KeyMouse_state;
    private float lastX, lastY;

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
                x = event.getX(); // save start x, y
                y = event.getY();

//                handler.postDelayed(longPressRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        isLongPress = true;
//                        Log.d(TAG, "Long pressed at: (" + x + ", " + y + ")");
//                        // deal long press event
//                        MouseManager.handleLongPress(x, y);
//                    }
//                }, 1000); // 1000 millisecond trigger long press
//                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    startY1 = event.getY(0);
                    startY2 = event.getY(1);
                    isPanning = true;
                    hasHandledMove = false; // Reset the flag
                    Log.d(TAG, "ACTION_POINTER_DOWN");
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isPanning && event.getPointerCount() == 2) {
                    float y1 = event.getY(0) - startY1;
                    float y2 = event.getY(1) - startY2;

                    String rollingGearY;

                    Log.d(TAG, "event.getY(0) " + event.getY(0));
                    Log.d(TAG, "event.getY(1) " + event.getY(1));
                    if (y1 > 0 && y2 > 0) {
                        rollingGearY = "SlideUp";
                    } else {
                        rollingGearY = "Downward";
                    }

                    // Check if both fingers are moving in the same vertical direction
                    if (!hasHandledMove && ((y1 > 0 && y2 > 0) || (y1 < 0 && y2 < 0))) {
                        // Handle the double finger pan up or down
                        Log.d(TAG, "ACTION_MOVE");
                        MouseManager.handleDoubleFingerPan(event.getX(1), event.getY(1), rollingGearY);

                        hasHandledMove = true; // Set the flag to true after handling the move
                    }else {
                        MouseManager.handleLongPress(x, y);
                    }
                } else if (!isLongPress) {
                    // For single-finger tracking
                    x = event.getX();
                    y = event.getY();
//                    Log.d(TAG, "Touched at: (" + x + ", " + y + ")");
//                    System.out.println("this is KeyMouse Boolean state22: " + KeyMouse_state);
                    if (KeyMouse_state){
//                        System.out.println("startY1: " + startY1+ "startY2: " + startY2);
                        MouseManager.sendHexAbsData(x, y);

                    }else {
                        MouseManager.sendHexRelData(x, y, lastX, lastY);
                        lastX = x;
                        lastY = y;
//                        System.out.println("send REL MS Data");
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() == 2) {
                    isPanning = false;
                    hasHandledMove = false; // Reset the flag
                    Log.d(TAG, "ACTION_POINTER_UP");
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                handler.removeCallbacks(runnable); // Stop the runnable for single click
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime <= DOUBLE_CLICK_TIME_DELTA) {
                    // deal double click event
                    Log.d(TAG, "click double button ");
                    MouseManager.handleDoubleClick(x, y);
                }
//                else {
//                    if (!isLongPress) {
//                        // Stop the single click runnable
//                        Log.d(TAG, "one click");
//                        Log.d(TAG, "Touched at: (" + x + ", " + y + ")");
//                    }
//                }
                lastClickTime = clickTime;
                isPanning = false;
                break;
        }
        return true;
    }

}
