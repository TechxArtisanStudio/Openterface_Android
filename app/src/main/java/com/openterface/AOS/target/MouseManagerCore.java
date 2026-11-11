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
    private static final String TAG = "OP-MOUSE";
    private static UsbDeviceManager usbDeviceManager;
    public static int screenWidth, screenHeight;

    // Mouse speed multiplier — mirrors MouseManager.mouseSpeedMultiplier.
    // Applied to relative deltas; does NOT increase packet rate.
    private static volatile float mouseSpeedMultiplier = 1.0f;

    public static void setMouseSpeedMultiplier(float multiplier) {
        mouseSpeedMultiplier = Math.max(0.25f, Math.min(3.0f, multiplier));
    }

    public static float getMouseSpeedMultiplier() {
        return mouseSpeedMultiplier;
    }

    // Buttons held by a gesture in flight and buttons held by a locked keycap. A
    // gesture release must not drop a lock, and a lock has to survive the finger being
    // lifted, so the two masks are kept apart. Move frames carry their union: the next
    // movement packet would otherwise release the button in the host and a drag would
    // stop after one pixel.
    private static volatile int gestureButtons = KeymodJNI.MS_BTN_NONE;
    private static volatile int lockedButtons = KeymodJNI.MS_BTN_NONE;

    /** Set the gesture buttons; 0 releases them, locked keycaps stay pressed. */
    public static void setHeldButtons(int buttons) {
        gestureButtons = buttons;
    }

    /** Add a button that stays pressed until it is unlocked (keycap long press). */
    public static void lockButtons(int buttons) {
        lockedButtons |= buttons;
    }

    /** Drop one locked button (the keycap was tapped again to unlock). */
    public static void unlockButtons(int buttons) {
        lockedButtons &= ~buttons;
        gestureButtons &= ~buttons;
    }

    /** Drop every gesture and locked button: disconnect, rotation, activity stop. */
    public static void unlockAll() {
        gestureButtons = KeymodJNI.MS_BTN_NONE;
        lockedButtons = KeymodJNI.MS_BTN_NONE;
    }

    /** Button mask every move frame has to carry. */
    public static int getHeldButtons() {
        return gestureButtons | lockedButtons;
    }

    public static int getLockedButtons() {
        return lockedButtons;
    }

    // Single worker thread for all mouse events
    private static HandlerThread sWorkerThread;
    private static MouseHandler sHandler;

    // Message types
    private static final int MSG_ABS_MOVE = 1;
    private static final int MSG_ABS_CLICK = 2;
    private static final int MSG_ABS_RELEASE_XY = 4;
    private static final int MSG_REL_MOVE = 5;
    // BT mouse HID events - receive pre-converted HID coords, no scaling
    private static final int MSG_BT_MOUSE_HID_MOVE = 11;
    private static final int MSG_BT_MOUSE_HID_CLICK = 12;
    private static final int MSG_BT_MOUSE_HID_RELEASE = 15;

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

    /**
     * Absolute move that releases the gesture buttons (end of click / drag). A locked
     * keycap keeps its button pressed.
     */
    public static void sendHexAbsReleaseData(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_RELEASE_XY);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    /**
     * Send BT mouse absolute move with pre-converted HID coordinates [0, 4095].
     * Bypasses Handler's view→HID scaling - sends HID coords directly.
     */
    public static void sendBtMouseHidAbsData(float hidX, float hidY) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_BT_MOUSE_HID_MOVE);
        msg.arg1 = (int) hidX;
        msg.arg2 = (int) hidY;
        sHandler.sendMessage(msg);
    }

    /**
     * Send BT mouse button click with pre-converted HID coordinates [0, 4095].
     * Bypasses Handler's view→HID scaling - sends HID coords directly.
     */
    public static void sendBtMouseHidAbsButtonClickData(String mouseClick, float hidX, float hidY) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_BT_MOUSE_HID_CLICK);
        msg.arg1 = (int) hidX;
        msg.arg2 = (int) hidY;
        msg.obj = mouseClick;
        sHandler.sendMessage(msg);
    }

    /**
     * BT mouse release with pre-converted HID coordinates [0, 4095].
     */
    public static void sendBtMouseHidAbsReleaseData(float hidX, float hidY) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_BT_MOUSE_HID_RELEASE);
        msg.arg1 = (int) hidX;
        msg.arg2 = (int) hidY;
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

        /**
         * Convert view coordinate to HID absolute coordinate.
         * Uses 4095 (12-bit max) instead of 4096 to prevent overflow/wrapping at right/bottom edges.
         * Clamps result to [0, 4095] range.
         */
        private int viewToHid(float viewCoord, float screenSize) {
            if (screenSize <= 0) return 0;
            int hid = (int) ((viewCoord / screenSize) * 4095f);
            return Math.max(0, Math.min(4095, hid));
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ABS_MOVE:
                    doSendHexAbsData(
                            viewToHid(msg.arg1 / 1000f, screenWidth),
                            viewToHid(msg.arg2 / 1000f, screenHeight));
                    break;
                case MSG_ABS_CLICK:
                    doSendHexAbsButtonClickData((String) msg.obj,
                            viewToHid(msg.arg1 / 1000f, screenWidth),
                            viewToHid(msg.arg2 / 1000f, screenHeight));
                    break;
                case MSG_ABS_RELEASE_XY:
                    doSendHexAbsReleaseData(
                            viewToHid(msg.arg1 / 1000f, screenWidth),
                            viewToHid(msg.arg2 / 1000f, screenHeight));
                    break;
                case MSG_REL_MOVE:
                    RelData d = (RelData) msg.obj;
                    doSendHexRelData(d.click, d.sx, d.sy, d.lx, d.ly);
                    break;
                // BT mouse HID events - receive pre-converted HID coords [0,4095], send directly
                // No scaling - normalizeBtMouseToHid in CustomTouchListener already did the conversion
                case MSG_BT_MOUSE_HID_MOVE:
                    doSendHexAbsData(msg.arg1, msg.arg2);
                    break;
                case MSG_BT_MOUSE_HID_CLICK:
                    doSendHexAbsButtonClickData((String) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_BT_MOUSE_HID_RELEASE:
                    doSendHexAbsReleaseData(msg.arg1, msg.arg2);
                    break;
            }
        }
    }

    // ====== Core HID Implementation ======

    private static void doSendHexAbsData(int x1, int y1) {
        // Keep the buttons that are currently held (locked keycap / ongoing drag).
        byte[] packet = KeymodJNI.buildMouseAbsPacket(
                gestureButtons | lockedButtons, x1, y1, 0);
        sendToDevice(packet, "absMove");
    }

    private static void doSendHexAbsReleaseData(int x1, int y1) {
        gestureButtons = KeymodJNI.MS_BTN_NONE;
        doSendHexAbsData(x1, y1);
    }

    private static void doSendHexAbsButtonClickData(String mouseClick, int x1, int y1) {
        int buttons = parseButtonMask(mouseClick);
        gestureButtons = buttons;
        byte[] packet = KeymodJNI.buildMouseAbsPacket(
                buttons | lockedButtons, x1, y1, 0);
        sendToDevice(packet, "absClick");
    }

    private static void doSendHexRelData(String mouseClick, float sx, float sy, float lx, float ly) {
        // Apply speed multiplier to deltas — same packet count, scaled movement
        int xMovement = (int) ((sx - lx) * mouseSpeedMultiplier);
        int yMovement = (int) ((sy - ly) * mouseSpeedMultiplier);

        // Clamp to signed byte range
        int dx = Math.max(-128, Math.min(127, xMovement));
        int dy = Math.max(-128, Math.min(127, yMovement));

        int buttons = parseButtonMask(mouseClick);
        if (buttons == KeymodJNI.MS_BTN_NONE) {
            // A move frame must not drop a held button (locked keycap / BT mouse drag).
            buttons = gestureButtons | lockedButtons;
        }
        byte[] packet = KeymodJNI.buildMouseRelPacket(buttons, dx, dy, 0);
        sendToDevice(packet, "relMove");
    }

    private static int parseButtonMask(String mouseClick) {
        if (mouseClick == null) return KeymodJNI.MS_BTN_NONE;
        switch (mouseClick) {
            case "SecLeftData": return KeymodJNI.MS_BTN_LEFT;
            case "SecRightData": return KeymodJNI.MS_BTN_RIGHT;
            case "SecMiddleData": return KeymodJNI.MS_BTN_MIDDLE;
            case "SecBackData": return KeymodJNI.MS_BTN_BACK;
            case "SecForwardData": return KeymodJNI.MS_BTN_FORWARD;
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
        // The press above latched the left button; send the matching release at the same
        // position so the click does not leave the host button down.
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_RELEASE_XY);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    public static void handleTwoPress() {
        // Right button press - a locked keycap keeps its button held
        byte[] packet = KeymodJNI.buildMouseRelPacket(
                KeymodJNI.MS_BTN_RIGHT | gestureButtons | lockedButtons, 0, 0, 0);
        sendToDevice(packet, "twoPress");
    }

    public static void handleDoubleFingerPan(float startY, float lastY) {
        int dy = (int) (startY - lastY);
        dy = Math.max(-1, Math.min(1, dy)); // Clamp to -1, 0, 1
        byte[] packet = KeymodJNI.buildMouseRelPacket(
                KeymodJNI.MS_BTN_MIDDLE | gestureButtons | lockedButtons, 0, dy, 0);
        sendToDevice(packet, "pan");
    }

    public static void releaseMSAbsData(float x, float y) {
        // Release by sending null button at the given position
        gestureButtons = KeymodJNI.MS_BTN_NONE;
        byte[] packet = KeymodJNI.buildMouseAbsPacket(lockedButtons, (int) x, (int) y, 0);
        sendToDevice(packet, "releaseAbs");
    }

    public static void releaseMSRelData() {
        // Release by sending null button; a locked keycap keeps its button pressed
        gestureButtons = KeymodJNI.MS_BTN_NONE;
        byte[] packet = KeymodJNI.buildMouseRelPacket(lockedButtons, 0, 0, 0);
        sendToDevice(packet, "releaseRel");
    }
}
