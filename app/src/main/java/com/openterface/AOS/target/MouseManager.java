/**
* @Title: MouseManager
* @Package com.openterface.AOS.target
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
package com.openterface.AOS.target;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;

public class MouseManager {
    private static final String TAG = "OP-MOUSE";
    private static UsbDeviceManager usbDeviceManager;
    public static int screenWidth, screenHeight;

    // Mouse speed multiplier (default 1.0 = normal speed).
    // Applied to relative movement deltas only — does NOT increase packet rate.
    // Range: 0.25 (very slow) to 3.0 (very fast).
    private static volatile float mouseSpeedMultiplier = 1.0f;

    /**
     * Set mouse movement speed multiplier for relative mode.
     * Does NOT change the number of packets sent — only scales the delta values.
     * @param multiplier 0.25f to 3.0f (1.0f = normal speed)
     */
    public static void setMouseSpeedMultiplier(float multiplier) {
        mouseSpeedMultiplier = Math.max(0.25f, Math.min(3.0f, multiplier));
    }

    public static float getMouseSpeedMultiplier() {
        return mouseSpeedMultiplier;
    }

    // Single worker thread for all mouse events — avoids thread-per-event overhead
    private static HandlerThread sWorkerThread;
    private static MouseHandler sHandler;

    // Message types
    private static final int MSG_ABS_MOVE       = 1;
    private static final int MSG_ABS_CLICK      = 2;
    private static final int MSG_ABS_DRAG       = 3;
    private static final int MSG_ABS_DBL_CLICK  = 4;
    private static final int MSG_REL_MOVE       = 5;
    private static final int MSG_TWO_PRESS      = 6;
    private static final int MSG_TWO_PAN        = 7;
    private static final int MSG_TWO_PAN_SLIDE  = 8;
    private static final int MSG_REL_DBL_CLICK  = 9;
    private static final int MSG_ABS_RELEASE    = 10;
    // Bluetooth mouse message types - receive pre-converted HID coords, no secondary scaling
    private static final int MSG_BT_MOUSE_HID_MOVE    = 11;
    private static final int MSG_BT_MOUSE_HID_CLICK   = 12;
    private static final int MSG_BT_MOUSE_HID_DRAG    = 13;

    public static void setUsbDeviceManager(UsbDeviceManager deviceManager) {
        usbDeviceManager = deviceManager;
        Log.d(TAG, "UsbDeviceManager instance set for enhanced FE0C support");
    }

    /**
     * Initialize the worker thread. Call once at app start.
     */
    public static void init() {
        if (sWorkerThread == null) {
            sWorkerThread = new HandlerThread("MouseWorker");
            sWorkerThread.start();
            sHandler = new MouseHandler(sWorkerThread.getLooper());
            Log.d(TAG, "MouseManager worker thread started");
        }
    }

    /**
     * Release the worker thread. Call at app exit.
     */
    public static void release() {
        if (sWorkerThread != null) {
            sWorkerThread.quitSafely();
            try {
                sWorkerThread.join(1000);
            } catch (InterruptedException ignored) {}
            sWorkerThread = null;
            sHandler = null;
            Log.d(TAG, "MouseManager worker thread released");
        }
    }

    public static void width_height(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    // ---------- Public API (unchanged signatures, now posts to handler) ----------

    public static void sendHexAbsData(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_MOVE);
        msg.arg1 = (int) (x * 1000);  // pack float as int
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    public static void sendHexAbsButtonClickData(String MouseClick, float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_CLICK);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        msg.obj = MouseClick;
        sHandler.sendMessage(msg);
    }

    public static void sendHexAbsDragData(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_DRAG);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    /**
     * Send BT mouse absolute move with pre-converted HID coordinates [0, 4095].
     * Bypasses Handler's view→HID scaling - sends HID coords directly.
     * This avoids double-scaling when BT mouse path already converted to HID range.
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
     * Send BT mouse drag with pre-converted HID coordinates [0, 4095].
     * Bypasses Handler's view→HID scaling - sends HID coords directly.
     */
    public static void sendBtMouseHidAbsDragData(float hidX, float hidY) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_BT_MOUSE_HID_DRAG);
        msg.arg1 = (int) hidX;
        msg.arg2 = (int) hidY;
        sHandler.sendMessage(msg);
    }

    public static void handleDoubleClickAbs(float x, float y) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_ABS_DBL_CLICK);
        msg.arg1 = (int) (x * 1000);
        msg.arg2 = (int) (y * 1000);
        sHandler.sendMessage(msg);
    }

    public static void handleTwoPress() {
        if (sHandler == null) init();
        sHandler.sendEmptyMessage(MSG_TWO_PRESS);
    }

    public static void handleDoubleFingerPan(float StartMoveMSY, float LastMoveMSY) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_TWO_PAN);
        msg.arg1 = (int) (StartMoveMSY * 1000);
        msg.arg2 = (int) (LastMoveMSY * 1000);
        sHandler.sendMessage(msg);
    }

    public static void handleTwoFingerPanSlideUpDown(float SlideData) {
        if (sHandler == null) init();
        Message msg = sHandler.obtainMessage(MSG_TWO_PAN_SLIDE);
        msg.arg1 = (int) (SlideData * 1000);
        sHandler.sendMessage(msg);
    }

    public static void sendHexRelData(String MouseClick, float StartMoveMSX, float StartMoveMSY,
                                       float LastMoveMSX, float LastMoveMSY) {
        if (sHandler == null) init();
        RelData data = new RelData(MouseClick, StartMoveMSX, StartMoveMSY, LastMoveMSX, LastMoveMSY);
        Message msg = sHandler.obtainMessage(MSG_REL_MOVE, data);
        sHandler.sendMessage(msg);
    }

    public static void releaseMSAbsData(String xBytes0, String xBytes1, String yBytes0, String yBytes1) {
        AbsReleaseData data = new AbsReleaseData(xBytes0, xBytes1, yBytes0, yBytes1);
        if (sHandler != null) {
            Message msg = sHandler.obtainMessage(MSG_ABS_RELEASE, data);
            sHandler.sendMessage(msg);
        } else {
            doReleaseMSAbsData(xBytes0, xBytes1, yBytes0, yBytes1);
        }
    }

    public static void handleDoubleClickRel() {
        if (sHandler == null) init();
        sHandler.sendEmptyMessage(MSG_REL_DBL_CLICK);
    }

    public static void releaseMSRelData() {
        if (sHandler != null) {
            sHandler.sendEmptyMessage(MSG_REL_DBL_CLICK);  // reuse — handler does release
        } else {
            doReleaseMSRelData();
        }
    }

    // ---------- Handler ----------

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
                case MSG_ABS_DRAG:
                    doSendHexAbsData(
                            viewToHid(msg.arg1 / 1000f, screenWidth),
                            viewToHid(msg.arg2 / 1000f, screenHeight));
                    break;
                case MSG_ABS_DBL_CLICK:
                    doHandleDoubleClickAbs(
                            viewToHid(msg.arg1 / 1000f, screenWidth),
                            viewToHid(msg.arg2 / 1000f, screenHeight));
                    break;
                case MSG_TWO_PRESS:
                    doHandleTwoPress();
                    break;
                case MSG_TWO_PAN:
                    doHandleDoubleFingerPan(msg.arg1 / 1000f, msg.arg2 / 1000f);
                    break;
                case MSG_TWO_PAN_SLIDE:
                    doHandleTwoFingerPanSlideUpDown(msg.arg1 / 1000f);
                    break;
                case MSG_REL_MOVE:
                    RelData d = (RelData) msg.obj;
                    doSendHexRelData(d.click, d.sx, d.sy, d.lx, d.ly);
                    break;
                case MSG_REL_DBL_CLICK:
                    doHandleDoubleClickRel();
                    break;
                case MSG_ABS_RELEASE:
                    AbsReleaseData rd = (AbsReleaseData) msg.obj;
                    doReleaseMSAbsData(rd.x0, rd.x1, rd.y0, rd.y1);
                    break;
                // BT mouse HID events - receive pre-converted HID coords [0,4095], send directly
                // No scaling - normalizeBtMouseToHid in CustomTouchListener already did the conversion
                case MSG_BT_MOUSE_HID_MOVE:
                    if (CH9329Function.DEBUG_SEND_LOG) {
                        Log.d(TAG, "BT_HID_MOVE: received arg1=" + msg.arg1 + " arg2=" + msg.arg2);
                    }
                    doSendHexAbsData(msg.arg1, msg.arg2);
                    break;
                case MSG_BT_MOUSE_HID_CLICK:
                    if (CH9329Function.DEBUG_SEND_LOG) {
                        Log.d(TAG, "BT_HID_CLICK: received arg1=" + msg.arg1 + " arg2=" + msg.arg2 + " click=" + msg.obj);
                    }
                    doSendHexAbsButtonClickData((String) msg.obj, msg.arg1, msg.arg2);
                    break;
                case MSG_BT_MOUSE_HID_DRAG:
                    if (CH9329Function.DEBUG_SEND_LOG) {
                        Log.d(TAG, "BT_HID_DRAG: received arg1=" + msg.arg1 + " arg2=" + msg.arg2);
                    }
                    doSendHexAbsData(msg.arg1, msg.arg2);
                    break;
            }
        }
    }

    // Simple data holder to avoid String allocation in Message.obj for REL
    private static class RelData {
        String click;
        float sx, sy, lx, ly;
        RelData(String c, float sx_, float sy_, float lx_, float ly_) {
            click = c; sx = sx_; sy = sy_; lx = lx_; ly = ly_;
        }
    }

    private static class AbsReleaseData {
        String x0, x1, y0, y1;
        AbsReleaseData(String x0_, String x1_, String y0_, String y1_) {
            x0 = x0_; x1 = x1_; y0 = y0_; y1 = y1_;
        }
    }

    // ---------- Core send logic (moved from old per-thread runnables) ----------

    private static void doSendHexAbsData(int x1, int y1) {
        byte[] xBytes = CH9329Function.intToByteArray(x1);
        byte[] yBytes = CH9329Function.intToByteArray(y1);

        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                CH9329MSKBMap.MSAbsData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get("SecNullData") +
                String.format("%02X", xBytes[0]) +
                String.format("%02X", xBytes[1]) +
                String.format("%02X", yBytes[0]) +
                String.format("%02X", yBytes[1]) +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        sendToDevice(sendMSData, "sendHexAbsData");
    }

    private static void doSendHexAbsButtonClickData(String mouseClick, int x1, int y1) {
        byte[] xBytes = CH9329Function.intToByteArray(x1);
        byte[] yBytes = CH9329Function.intToByteArray(y1);

        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                CH9329MSKBMap.MSAbsData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get(mouseClick) +
                String.format("%02X", xBytes[0]) +
                String.format("%02X", xBytes[1]) +
                String.format("%02X", yBytes[0]) +
                String.format("%02X", yBytes[1]) +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        sendToDevice(sendMSData, "sendHexAbsButtonClickData");
    }

    private static void doHandleDoubleClickAbs(int x1, int y1) {
        byte[] xBytes = CH9329Function.intToByteArray(x1);
        byte[] yBytes = CH9329Function.intToByteArray(y1);

        String xBytes0 = String.format("%02X", xBytes[0]);
        String xBytes1 = String.format("%02X", xBytes[1]);
        String yBytes0 = String.format("%02X", yBytes[0]);
        String yBytes1 = String.format("%02X", yBytes[1]);

        String sendMSDoubleClickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                CH9329MSKBMap.MSAbsData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get("SecLeftData") +
                String.format("%02X", xBytes[0]) +
                String.format("%02X", xBytes[1]) +
                String.format("%02X", yBytes[0]) +
                String.format("%02X", yBytes[1]) +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);
        CH9329Function.checkSendLogData(sendMSDoubleClickData);
        byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

        if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
            boolean result = usbDeviceManager.writeData(sendKBDataBytes);
            if (result) {
                doReleaseMSAbsData(xBytes0, xBytes1, yBytes0, yBytes1);
            }
        }
    }

    private static void doHandleTwoPress() {
        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSRelData().get("SecRightData") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        sendToDevice(sendMSData, "handleTwoPress");
    }

    private static void doHandleDoubleFingerPan(float startMoveMSY, float lastMoveMSY) {
        int yMovement = (int) (startMoveMSY - lastMoveMSY);
        String yByte;
        if (yMovement == 0 || lastMoveMSY == 0) {
            yByte = "00";
        } else if (yMovement > 0) {
            yByte = "01";
        } else {
            yByte = "FF";
        }

        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSRelData().get("SecMiddleData") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                yByte;

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        if (sendMSData.length() % 2 != 0) sendMSData += "0";
        sendToDevice(sendMSData, "handleDoubleFingerPan");
    }

    private static void doHandleTwoFingerPanSlideUpDown(float slideData) {
        String yByte = slideData > 0 ? "01" : "FF";

        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSRelData().get("SecNullData") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                yByte;

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        if (sendMSData.length() % 2 != 0) sendMSData += "0";
        sendToDevice(sendMSData, "handleTwoFingerPanSlideUpDown");
    }

    private static void doSendHexRelData(String mouseClick, float startMoveMSX, float startMoveMSY,
                                          float lastMoveMSX, float lastMoveMSY) {
        // Apply speed multiplier to deltas (not to positions) — same packet count, scaled movement
        int xMovement = (int) ((startMoveMSX - lastMoveMSX) * mouseSpeedMultiplier);
        int yMovement = (int) ((startMoveMSY - lastMoveMSY) * mouseSpeedMultiplier);

        String xByte;
        if (xMovement == 0) {
            xByte = "00";
        } else if (xMovement > 0) {
            xByte = String.format("%02X", Math.min(xMovement, 0x7F));
        } else {
            xByte = String.format("%02X", 0x100 + xMovement);
        }

        String yByte;
        if (yMovement == 0) {
            yByte = "00";
        } else if (yMovement > 0) {
            yByte = String.format("%02X", Math.min(yMovement, 0x7F));
        } else {
            yByte = String.format("%02X", 0x100 + yMovement);
        }

        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get(mouseClick) +
                xByte + yByte +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        if (sendMSData.length() % 2 != 0) sendMSData += "0";
        sendToDevice(sendMSData, "sendHexRelData");
    }

    private static void doHandleDoubleClickRel() {
        // This is the releaseMSRelData function — renamed to avoid confusion
        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSRelData().get("SecNullData") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        sendToDevice(sendMSData, "releaseMSRelData");
    }

    static void doReleaseMSAbsData(String xBytes0, String xBytes1, String yBytes0, String yBytes1) {
        String sendReleaseMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                CH9329MSKBMap.MSAbsData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get("SecNullData") +
                xBytes0 + xBytes1 + yBytes0 + yBytes1 +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendReleaseMSData = sendReleaseMSData + CH9329Function.makeChecksum(sendReleaseMSData);
        sendToDevice(sendReleaseMSData, "releaseMSAbsData");
    }

    static void doReleaseMSRelData() {
        String sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                CH9329MSKBMap.MSRelData().get("FirstData") +
                CH9329MSKBMap.MSRelData().get("SecNullData") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull") +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);
        sendToDevice(sendMSData, "releaseMSRelData");
    }

    // ---------- Common USB write helper ----------

    private static void sendToDevice(String hexData, String tag) {
        CH9329Function.checkSendLogData(hexData);
        byte[] bytes = CH9329Function.hexStringToByteArray(hexData);
        try {
            // Diagnostic logging
            if (usbDeviceManager == null) {
                Log.e(TAG, "USB device manager is NULL for " + tag);
                return;
            }
            if (!usbDeviceManager.isConnected()) {
                Log.e(TAG, "USB device not connected for " + tag + " - data: " + hexData);
                return;
            }
            // 逐包 hex 日志（默认关闭，见 CH9329Function.DEBUG_SEND_LOG）
            if (CH9329Function.DEBUG_SEND_LOG) {
                Log.d(TAG, "Sending " + tag + ": " + hexData);
            }

            boolean result = usbDeviceManager.writeData(bytes);
            if (!result) {
                Log.e(TAG, "Failed to write " + tag);
            } else if (CH9329Function.DEBUG_SEND_LOG) {
                Log.d(TAG, "Successfully wrote " + tag);
            }
        } catch (Exception e) {
            // Catch all exceptions to prevent app crash on USB errors
            Log.e(TAG, "Error writing to port: " + e.getMessage(), e);
        }
    }
}
