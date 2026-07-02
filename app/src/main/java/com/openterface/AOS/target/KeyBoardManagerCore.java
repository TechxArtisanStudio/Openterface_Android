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

import android.content.Context;
import android.util.Log;

import com.openterface.AOS.jni.KeymodJNI;
import com.openterface.AOS.serial.UsbDeviceManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Core-based keyboard manager using Openterface_Core JNI.
 * Replaces the original Java implementation with the C library.
 */
public class KeyBoardManagerCore {
    private static final String TAG = "OP-KB";
    private static Context context;
    private static UsbDeviceManager usbDeviceManager;

    // Single-threaded executor for sequential keyboard operations
    private static final ExecutorService keyboardExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Keyboard-Core-Executor");
            t.setDaemon(true);
            return t;
        });

    // Track currently pressed modifier keys
    private static int currentModifiers = KeymodJNI.MOD_NONE;

    static {
        // Load native library
        try {
            System.loadLibrary("keymod");
            Log.i(TAG, "Keymod JNI library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load keymod library: " + e.getMessage());
        }
    }

    public static void setUsbDeviceManager(UsbDeviceManager manager) {
        usbDeviceManager = manager;
    }

    public static void setKeyBoardLanguage() {
        // Language support can be added later via Core
        String currentLang = Locale.getDefault().getLanguage();
        Log.d(TAG, "Language: " + currentLang);
    }

    // ====== Native JNI Methods ======

    /**
     * Send keyboard data using Core HID implementation.
     * @param modifiers Modifier bitmask
     * @param keyName Key name from CH9329MSKBMap
     */
    public static void sendKeyBoardData(String functionKey, String keyName) {
        if (keyName == null || usbDeviceManager == null) return;

        int modifiers = parseModifiers(functionKey);
        int hidCode = getHidCode(keyName);
        if (hidCode < 0) {
            Log.w(TAG, "Unknown key: " + keyName);
            return;
        }

        byte[] packet = KeymodJNI.buildPressRelease(modifiers, hidCode);
        sendPacket(packet);
    }

    /**
     * Send keyboard press using Core HID.
     */
    public static void sendKeyBoardPress(String functionKey, String keyName) {
        if (keyName == null || usbDeviceManager == null) return;

        int modifiers = parseModifiers(functionKey);
        int hidCode = getHidCode(keyName);
        if (hidCode < 0) return;

        byte[] packet = KeymodJNI.buildKeyboardPacket(modifiers, new int[]{hidCode});
        sendPacket(packet);
    }

    /**
     * Send keyboard release using Core HID.
     */
    public static void sendKeyBoardRelease() {
        if (usbDeviceManager == null) return;

        byte[] packet = KeymodJNI.buildKeyboardRelease();
        sendPacket(packet);
    }

    /**
     * Send keyboard data with queued execution.
     */
    public static void sendKeyBoardPressQueued(String functionKey, String keyName) {
        keyboardExecutor.execute(() -> sendKeyBoardPress(functionKey, keyName));
    }

    /**
     * Send keyboard release with queued execution.
     */
    public static void sendKeyBoardReleaseQueued() {
        keyboardExecutor.execute(KeyBoardManagerCore::sendKeyBoardRelease);
    }

    /**
     * Send press+release atomically.
     */
    public static void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
        if (keyName == null || usbDeviceManager == null) return;

        int modifiers = parseModifiers(functionKey);
        int hidCode = getHidCode(keyName);
        if (hidCode < 0) return;

        byte[] packet = KeymodJNI.buildPressRelease(modifiers, hidCode);
        sendPacket(packet);
    }

    // ====== Helper Methods ======

    private static int parseModifiers(String functionKey) {
        if (functionKey == null || functionKey.equals("00")) {
            return KeymodJNI.MOD_NONE;
        }

        int modifiers = KeymodJNI.MOD_NONE;
        try {
            int value = Integer.parseInt(functionKey.replace("0x", ""), 16);
            if ((value & 0x01) != 0) modifiers |= KeymodJNI.MOD_CTRL;
            if ((value & 0x02) != 0) modifiers |= KeymodJNI.MOD_SHIFT;
            if ((value & 0x04) != 0) modifiers |= KeymodJNI.MOD_ALT;
            if ((value & 0x08) != 0) modifiers |= KeymodJNI.MOD_GUI;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid modifier: " + functionKey);
        }
        return modifiers;
    }

    private static int getHidCode(String keyName) {
        // Map Android key names to Core key names
        String coreName = KeymodJNI.toCoreKeyName(keyName);
        return KeymodJNI.hidCode(coreName);
    }

    private static void sendPacket(byte[] packet) {
        if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
            Log.w(TAG, "USB device not connected");
            return;
        }

        boolean result = usbDeviceManager.writeData(packet);
        if (!result) {
            Log.e(TAG, "Failed to send packet");
        }
    }

    // ====== Legacy Compatibility Methods ======

    public static String getFunctionKey(android.view.KeyEvent event, int keyCode) {
        // Determine modifier byte from event
        boolean isCtrl = (event.getMetaState() & android.view.KeyEvent.META_CTRL_ON) != 0;
        boolean isShift = (event.getMetaState() & android.view.KeyEvent.META_SHIFT_ON) != 0;
        boolean isAlt = (event.getMetaState() & android.view.KeyEvent.META_ALT_ON) != 0;

        int modifier = 0;
        if (isCtrl) modifier |= 0x01;
        if (isShift) modifier |= 0x02;
        if (isAlt) modifier |= 0x04;

        return String.format("%02X", modifier);
    }

    public static String getKeyName(int keyCode) {
        String name = android.view.KeyEvent.keyCodeToString(keyCode);
        if (name.startsWith("KEYCODE_")) {
            return name.substring("KEYCODE_".length());
        }
        return null;
    }

    public static void EmptyKeyboard() {
        sendKeyBoardRelease();
    }

    public static void sendKeyBoardDataEnhanced(UsbDeviceManager manager, String functionKey, String keyName) {
        setUsbDeviceManager(manager);
        sendKeyBoardData(functionKey, keyName);
    }

    public static void sendKeyboardMultiple(String keyName) {
        // Not implemented in Core
        Log.w(TAG, "sendKeyboardMultiple not implemented");
    }

    public static void sendKeyBoardShortCut(String modifier, String key) {
        // Use Core to build combined modifier packet
        int mod = KeymodJNI.MOD_NONE;
        if (modifier.contains("Ctrl")) mod |= KeymodJNI.MOD_CTRL;
        if (modifier.contains("Shift")) mod |= KeymodJNI.MOD_SHIFT;
        if (modifier.contains("Alt")) mod |= KeymodJNI.MOD_ALT;
        if (modifier.contains("Win")) mod |= KeymodJNI.MOD_GUI;

        int hidCode = getHidCode(key);
        if (hidCode >= 0) {
            byte[] packet = KeymodJNI.buildPressRelease(mod, hidCode);
            sendPacket(packet);
        }
    }

    public static void sendKeyBoardFunctionCtrlAltDel() {
        // Ctrl+Alt+Del: modifiers = 0x05 (Ctrl | Alt), keys = Delete
        int modifiers = KeymodJNI.MOD_CTRL | KeymodJNI.MOD_ALT;
        int hidCode = KeymodJNI.hidCode("Delete");
        byte[] packet = KeymodJNI.buildPressRelease(modifiers, hidCode);
        sendPacket(packet);
    }

    public static void sendKeyBoardFunction(String ctrl, String shift, String alt, String win, String keyName) {
        int modifiers = KeymodJNI.MOD_NONE;
        if (!"00".equals(ctrl)) modifiers |= KeymodJNI.MOD_CTRL;
        if (!"00".equals(shift)) modifiers |= KeymodJNI.MOD_SHIFT;
        if (!"00".equals(alt)) modifiers |= KeymodJNI.MOD_ALT;
        if (!"00".equals(win)) modifiers |= KeymodJNI.MOD_GUI;

        int hidCode = getHidCode(keyName);
        if (hidCode >= 0) {
            byte[] packet = KeymodJNI.buildPressRelease(modifiers, hidCode);
            sendPacket(packet);
        }
    }

    public static void sendKeyBoardFunctionPress(String ctrl, String shift, String alt, String win, String keyName) {
        sendKeyBoardFunction(ctrl, shift, alt, win, keyName);
    }

    public static void sendKeyboardRequest(String functionKey, String keyName) {
        sendKeyBoardData(functionKey, keyName);
    }

    public KeyBoardManagerCore(Context context) {
        KeyBoardManagerCore.context = context;
    }
}
