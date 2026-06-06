/**
 * ==========================================================================
 *
 *    This file is part of the Openterface Mini KVM App Android version
 *
 *    Copyright (C) 2024   <info@openterface.com>
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation version 3.
 *
 *    This program is distributed in the hope that it will be useful, but
 *    WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * ==========================================================================
 */
package com.openterface.AOS.target;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.openterface.AOS.jni.KeymodJNI;
import com.openterface.AOS.serial.UsbDeviceManager;

/**
 * Core-based mouse manager using Openterface_Core JNI.
 * Replaces the original Java implementation with the C library.
 */
public class MouseManagerCore {
    private static final String TAG = MouseManagerCore.class.getSimpleName();
    private static UsbDeviceManager usbDeviceManager;
    public static int screenWidth, screenHeight;

    // Single worker thread for all mouse events
    private static HandlerThread sWorkerThread;
    private static MouseHandler sHandler;

    // Message types
    private static final int MSG_ABS_MOVE = 1;
    private static final int MSG_ABS_CLICK = 2;
    private static final int MSG_ABS_DRAG = 3;
    private static final int MSG_REL_MOVE = 5;

    static {
        // Load native library
        try {
            System.loadLibrary("keymod");
            Log.i(TAG, "Keymod JNI library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load keymod: " + e.getMessage());
        }
    }

    public static void setUsbDeviceManager(UsbDeviceManager deviceManager) {
        usbDeviceManager = deviceManager;
    }

    public static void init() {
        if (sWorkerThread == null) {
            sWorkerThread = new HandlerThread("MouseWorker");
            sWorkerThread.start();
            sHandler = new MouseHandler(sWorkerThread.getLooper());
            Log.d(TAG, "MouseManagerCore initialized");
        }
    }

    public static void release() {
        if (sWorkerThread != null) {
            sWorkerThread.quitSafely();
            try {
                sWorkerThread.join(1000);
            } catch (InterruptedException ignored) {}
            sWorkerThread = null;
            sHandler = null;
        }
    }

    public static void width_height(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    // ====== Public API ======

    public static void sendHexAbsData(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_MOVE);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    public static void sendHexAbsButtonClickData(String mouseClick, float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_CLICK);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        msg.obj = mouseClick;
        sHandler.sendMessage(msg);
    }

    public static void sendHexAbsDragData(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_DRAG);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    public static void sendHexRelData(String mouseClick, float sx, float sy, float lx, float ly) {
        if (sHandler == null) init();
        RelData data = new RelData(mouseClick, sx, sy, lx, ly);
        Message msg = sHandler.obtainMessage(MSG_REL_MOVE, data);
        sHandler.sendMessage(msg);
    }

    // ====== Handler ======

    private static class MouseHandler extends Handler {
        MouseHandler(android.os.Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ABS_MOVE:
                    doSendHexAbsData(
                            (int) ((msg.arg1 / 1000f) * 4096 / screenWidth),
                            (int) ((msg.arg2 / 1000f) * 4096 / screenHeight));
                    break;
                case MSG_ABS_CLICK:
                    doSendHexAbsButtonClickData((String) msg.obj,
                            (int) ((msg.arg1 / 1000f) * 4096 / screenWidth),
                            (int) ((msg.arg2 / 1000f) * 4096 / screenHeight));
                    break;
                case MSG_ABS_DRAG:
                    doSendHexAbsData(
                            (int) ((msg.arg1 / 1000f) * 4096 / screenWidth),
                            (int) ((msg.arg2 / 1000f) * 4096 / screenHeight));
                    break;
                case MSG_REL_MOVE:
                    RelData d = (RelData) msg.obj;
                    doSendHexRelData(d.click, d.sx, d.sy, d.lx, d.ly);
                    break;
            }
        }
    }

    // ====== Core HID Implementation ======

    private static void doSendHexAbsData(int x1, int y1) {
        byte[] packet = KeymodJNI.buildMouseAbsPacket(
                KeymodJNI.MS_BTN_NONE, x1, y1, 0);
        sendToDevice(packet, "absMove");
    }

    private static void doSendHexAbsButtonClickData(String mouseClick, int x1, int y1) {
        int buttons = parseButtonMask(mouseClick);
        byte[] packet = KeymodJNI.buildMouseAbsPacket(buttons, x1, y1, 0);
        sendToDevice(packet, "absClick");
    }

    private static void doSendHexRelData(String mouseClick, float sx, float sy, float lx, float ly) {
        int xMovement = (int) (sx - lx);
        int yMovement = (int) (sy - ly);

        // Clamp to signed byte range
        int dx = Math.max(-128, Math.min(127, xMovement));
        int dy = Math.max(-128, Math.min(127, yMovement));

        int buttons = parseButtonMask(mouseClick);
        byte[] packet = KeymodJNI.buildMouseRelPacket(buttons, dx, dy, 0);
        sendToDevice(packet, "relMove");
    }

    private static int parseButtonMask(String mouseClick) {
        if (mouseClick == null) return KeymodJNI.MS_BTN_NONE;
        switch (mouseClick) {
            case "SecLeftData": return KeymodJNI.MS_BTN_LEFT;
            case "SecRightData": return KeymodJNI.MS_BTN_RIGHT;
            case "SecMiddleData": return KeymodJNI.MS_BTN_MIDDLE;
            default: return KeymodJNI.MS_BTN_NONE;
        }
    }

    private static void sendToDevice(byte[] data, String tag) {
        if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
            Log.w(TAG, "USB not connected for " + tag);
            return;
        }

        boolean result = usbDeviceManager.writeData(data);
        if (!result) {
            Log.e(TAG, "Failed to send " + tag);
        }
    }

    // ====== Data Holder ======

    private static class RelData {
        String click;
        float sx, sy, lx, ly;
        RelData(String c, float sx_, float sy_, float lx_, float ly_) {
            click = c; sx = sx_; sy = sy_; lx = lx_; ly = ly_;
        }
    }

    // ====== Legacy Compatibility Methods ======

    public static void handleDoubleClickAbs(float x, float y) {
        sendHexAbsButtonClickData("SecLeftData", x, y);
    }

    public static void handleTwoPress() {
        // Right button press
        byte[] packet = KeymodJNI.buildMouseRelPacket(KeymodJNI.MS_BTN_RIGHT, 0, 0, 0);
        sendToDevice(packet, "twoPress");
    }

    public static void handleDoubleFingerPan(float startY, float lastY) {
        int dy = (int) (startY - lastY);
        dy = Math.max(-1, Math.min(1, dy)); // Clamp to -1, 0, 1
        byte[] packet = KeymodJNI.buildMouseRelPacket(KeymodJNI.MS_BTN_MIDDLE, 0, dy, 0);
        sendToDevice(packet, "pan");
    }

    public static void releaseMSAbsData() {
        // Release by sending null button at current position
        byte[] packet = KeymodJNI.buildMouseAbsPacket(KeymodJNI.MS_BTN_NONE, 0, 0, 0);
        sendToDevice(packet, "releaseAbs");
    }

    public static void releaseMSRelData() {
        // Release by sending null button
        byte[] packet = KeymodJNI.buildMouseRelPacket(KeymodJNI.MS_BTN_NONE, 0, 0, 0);
        sendToDevice(packet, "releaseRel");
    }
}
