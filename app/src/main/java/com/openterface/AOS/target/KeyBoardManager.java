/**
* @Title: KeyBoardManager
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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.openterface.AOS.KeyBoardClick.KeyBoardSystem;
import com.openterface.AOS.KeyBoardClick.KeyMapConfig_De;
import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class KeyBoardManager {
    private static final String TAG = "OP-KB";
    private static Context context;
    private static Map<Object, String> currentKeyCodeMap;
    private static UsbDeviceManager usbDeviceManager; // Add static reference for enhanced FE0C support

    static {
        currentKeyCodeMap = CH9329MSKBMap.getKeyCodeMap();
    }
    
    /**
     * Set the UsbDeviceManager instance for enhanced device support
     * @param manager UsbDeviceManager instance
     */
    public static void setUsbDeviceManager(UsbDeviceManager manager) {
        usbDeviceManager = manager;
    }

    /**
     * Get the static UsbDeviceManager reference for callers that need
     * device-aware writes (FE0C bulk transfer vs 7523 serial port).
     */
    public static UsbDeviceManager getUsbDeviceManager() {
        return usbDeviceManager;
    }

    public static void setKeyBoardLanguage() {
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("de")) {
            currentKeyCodeMap = KeyMapConfig_De.getKeyCodeMap();
//            KeyBoardSystem.setKeyboardLanguage("de");
            Log.d(TAG, "language is de");
        }else {
            currentKeyCodeMap = CH9329MSKBMap.getKeyCodeMap();
//            KeyBoardSystem.setKeyboardLanguage("us");
            Log.d(TAG, "language is us");
        }
    }

    /**
     * Get the current key code map (supports language-specific layouts)
     * @return current key code map
     */
    public static Map<Object, String> getCurrentKeyCodeMap() {
        return currentKeyCodeMap;
    }

    public static String getFunctionKey(KeyEvent event, int keyCode) {
        boolean isCtrlPressed = (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0;
        boolean isShiftPressed = (event.getMetaState() & KeyEvent.META_SHIFT_ON) != 0;
        boolean isAltPressed = (event.getMetaState() & KeyEvent.META_ALT_ON) != 0;

        if (isCtrlPressed) {
            if (keyCode != KeyEvent.KEYCODE_CTRL_LEFT && keyCode != KeyEvent.KEYCODE_CTRL_RIGHT) {
                Log.d(TAG, "click ctrl and other click");
                return "01";
            }
        } else if (isShiftPressed) {
            if (keyCode != KeyEvent.KEYCODE_SHIFT_LEFT && keyCode != KeyEvent.KEYCODE_SHIFT_RIGHT) {
                Log.d(TAG, "click shift and other click");
                return "02";
            }
        } else if (isAltPressed) {
            if (keyCode != KeyEvent.KEYCODE_ALT_LEFT && keyCode != KeyEvent.KEYCODE_ALT_RIGHT) {
                Log.d(TAG, "click alt and other click");
                return "04";
            }
        }
        return "00";
    }

    public static String getKeyName(int keyCode) {
        String keyCodeName = KeyEvent.keyCodeToString(keyCode);
        if (keyCodeName.startsWith("KEYCODE_")) {
            return keyCodeName.substring("KEYCODE_".length());
        }
        return null;
    }

    public static void DetectedCharacter(String functionKey, String pressedChar, String targetChars){
        if (targetChars.contains(pressedChar)) {
            Log.d(TAG, "Detected special character: " + pressedChar);
            sendKeyboardRequest(functionKey, pressedChar);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.mKeyboardRequestSent = true;
                }
            }, 100);
        }
    }

    /**
     * Enhanced keyboard data sending with FE0C device support
     * @param usbDeviceManager UsbDeviceManager instance for enhanced features
     * @param functionKey modifier keys
     * @param keyName key to send
     * @return true if successful using enhanced method, false if fallback needed
     */
    public static boolean sendKeyBoardDataEnhanced(UsbDeviceManager usbDeviceManager, String functionKey, String keyName) {
        if (usbDeviceManager == null || !usbDeviceManager.isConnected() || keyName == null) {
            Log.w(TAG, "Enhanced keyboard: Invalid parameters - usbDeviceManager=" + (usbDeviceManager != null) + 
                  ", connected=" + (usbDeviceManager != null && usbDeviceManager.isConnected()) + 
                  ", keyName=" + keyName);
            return false;
        }
        
        try {
            Log.d(TAG, "=== ENHANCED KEYBOARD DEBUG ===");
            Log.d(TAG, "Input - functionKey: '" + functionKey + "', keyName: '" + keyName + "'");
            Log.d(TAG, "Device Type: " + (usbDeviceManager.isFE0CDevice() ? "FE0C" : "Other"));
            
            // Get the individual key code from the mapping
            String keyCode = currentKeyCodeMap.get(keyName);
            if (keyCode == null) {
                Log.w(TAG, "❌ Key '" + keyName + "' not found in mapping");
                Log.d(TAG, "Available keys in map: " + currentKeyCodeMap.keySet().toString());
                return false;
            }
            
            Log.d(TAG, "✅ Found key code for '" + keyName + "': " + keyCode);
            
            // Build the complete keyboard command like the original method
            String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1");
            String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2");
            String address = CH9329MSKBMap.getKeyCodeMap().get("address");
            String cmdKB = CH9329MSKBMap.CmdData().get("CmdKB_HID");
            String dataLen = CH9329MSKBMap.DataLen().get("DataLenKB");
            String dataNull = CH9329MSKBMap.DataNull().get("DataNull");
            
            Log.d(TAG, "Command components:");
            Log.d(TAG, "  prefix1: " + prefix1);
            Log.d(TAG, "  prefix2: " + prefix2);
            Log.d(TAG, "  address: " + address);
            Log.d(TAG, "  cmdKB: " + cmdKB);
            Log.d(TAG, "  dataLen: " + dataLen);
            Log.d(TAG, "  functionKey: " + functionKey);
            Log.d(TAG, "  keyCode: " + keyCode);
            Log.d(TAG, "  dataNull: " + dataNull);
            
            String sendKBData = prefix1 + prefix2 + address + cmdKB + dataLen +
                    functionKey + dataNull + keyCode + dataNull + dataNull + dataNull + dataNull + dataNull;
            
            Log.d(TAG, "Command before checksum: " + sendKBData);
            
            // Add checksum
            sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);
            
            Log.d(TAG, "✅ Final command with checksum: " + sendKBData);
            CH9329Function.checkSendLogData(sendKBData);
            
            byte[] keyboardDataBytes = CH9329Function.hexStringToByteArray(sendKBData);
            Log.d(TAG, "Command as bytes: " + java.util.Arrays.toString(keyboardDataBytes));
            
            boolean result = usbDeviceManager.sendKeyboardCommandFE0C(keyboardDataBytes);
            Log.d(TAG, "Send result: " + (result ? "SUCCESS" : "FAILED"));
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced keyboard sending: " + e.getMessage(), e);
        }
        
        return false;
    }

    public static void sendKeyBoardData(String functionKey, String keyName) {
        if (keyName != null) {
            Log.d(TAG, "keyName: " + keyName);
            sendKeyboardRequest(functionKey, keyName);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.mKeyboardRequestSent = true;
                }
            }, 10);
        }
    }

    /**
     * Send keyboard press event (key down) - without automatic release
     * @param functionKey modifier keys
     * @param keyName key to press
     */
    public static void sendKeyBoardPress(String functionKey, String keyName) {
        if (keyName == null) {
            Log.w(TAG, "❌ sendKeyBoardPress: keyName is null");
            return;
        }
        
        Log.e(TAG, "🔵 ========== KEY PRESS START ==========");
        Log.e(TAG, "🔵 Thread: " + Thread.currentThread().getName());
        Log.e(TAG, "🔵 functionKey: '" + functionKey + "', keyName: '" + keyName + "'");
        Log.e(TAG, "🔵 Time: " + System.currentTimeMillis());
        
        new Thread(() -> {
            try {
                if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
                    Log.e(TAG, "❌ sendKeyBoardPress USB device not connected");
                    return;
                }

                String sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                        CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                        CH9329MSKBMap.getKeyCodeMap().get("address") +
                        CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                        CH9329MSKBMap.DataLen().get("DataLenKB") +
                        functionKey +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        currentKeyCodeMap.get(keyName) +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull");

                sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                Log.e(TAG, "🔵 Full command: " + sendKBData);
                CH9329Function.checkSendLogData(sendKBData);

                byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                try {
                    long startTime = System.currentTimeMillis();
                    boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                    long endTime = System.currentTimeMillis();
                    if (result) {
                        Log.e(TAG, "✅ Key press sent successfully in " + (endTime - startTime) + "ms (no auto-release)");
                    } else {
                        Log.e(TAG, "❌ Key press write returned false");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error writing key press: " + e.getMessage(), e);
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in sendKeyBoardPress: " + e.getMessage(), e);
            }
            Log.e(TAG, "🔵 ========== KEY PRESS END ==========");
        }).start();
    }

    // Static executor for sequential keyboard operations - ensures press completes before release
    private static final java.util.concurrent.ExecutorService keyboardExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Keyboard-Executor");
            t.setDaemon(true);
            return t;
        });

    // Track currently pressed keys to avoid duplicate presses from browser duplicate events
    private static final java.util.Set<String> currentlyPressedKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Send a complete key press and release sequence for simple key events.
     * Used by WebRTC sendKeyboardKey() for single key events where browser sends
     * both keydown and keyup - this handles the full cycle atomically.
     */
    public static void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
        if (keyName == null) {
            Log.w(TAG, "❌ sendKeyBoardPressAndRelease: keyName is null");
            return;
        }

        // Skip if key is already pressed (prevents duplicate from browser double-firing)
        if (!currentlyPressedKeys.add(keyName)) {
            Log.w(TAG, "🟣 Key '" + keyName + "' already pressed, ignoring duplicate");
            return;
        }

        Log.e(TAG, "🟣 ========== KEY PRESS+RELEASE START ==========");
        Log.e(TAG, "🟣 functionKey: '" + functionKey + "', keyName: '" + keyName + "'");

        keyboardExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Step 1: Send press
                Log.e(TAG, "🟣 Step 1: Sending PRESS...");
                boolean pressSent = sendKeyBoardPressSync(functionKey, keyName);
                if (!pressSent) {
                    Log.e(TAG, "❌ Press failed, aborting release");
                    currentlyPressedKeys.remove(keyName);
                    return;
                }

                // Step 2: Small delay to ensure target registers the press
                Log.e(TAG, "🟣 Step 2: Waiting 50ms for target to register...");
                Thread.sleep(50);

                // Step 3: Send release
                Log.e(TAG, "🟣 Step 3: Sending RELEASE...");
                sendKeyBoardReleaseSync();

                currentlyPressedKeys.remove(keyName);

                long endTime = System.currentTimeMillis();
                Log.e(TAG, "✅ Key press+release completed in " + (endTime - startTime) + "ms");

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in sendKeyBoardPressAndRelease: " + e.getMessage(), e);
                currentlyPressedKeys.remove(keyName);
            }
            Log.e(TAG, "🟣 ========== KEY PRESS+RELEASE END ==========");
        });
    }

    /**
     * Queue a key press on the executor thread.
     * Used by WebRTC when browser sends keydown separately from keyup.
     */
    public static void sendKeyBoardPressQueued(String functionKey, String keyName) {
        if (keyName == null) {
            Log.w(TAG, "❌ sendKeyBoardPressQueued: keyName is null");
            return;
        }

        // Skip if key is already pressed (prevents duplicate from browser double-firing)
        if (!currentlyPressedKeys.add(keyName)) {
            Log.w(TAG, "🟣 Key '" + keyName + "' already pressed, ignoring duplicate");
            return;
        }

        Log.e(TAG, "🔵 ========== QUEUED KEY PRESS START ==========");
        Log.e(TAG, "🔵 functionKey: '" + functionKey + "', keyName: '" + keyName + "'");

        keyboardExecutor.execute(() -> {
            try {
                Log.e(TAG, "🔵 Sending PRESS...");
                boolean result = sendKeyBoardPressSync(functionKey, keyName);
                Log.e(TAG, "🔵 Press result: " + (result ? "SUCCESS" : "FAILED"));
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in sendKeyBoardPressQueued: " + e.getMessage(), e);
                currentlyPressedKeys.remove(keyName);
            }
            Log.e(TAG, "🔵 ========== QUEUED KEY PRESS END ==========");
        });
    }

    /**
     * Queue a key release on the executor thread.
     * Used by WebRTC when browser sends keyup separately from keydown.
     * This ensures release always happens AFTER all queued press operations.
     */
    public static void sendKeyBoardReleaseQueued() {
        Log.e(TAG, "🔴 ========== QUEUED KEY RELEASE START ==========");

        keyboardExecutor.execute(() -> {
            try {
                Log.e(TAG, "🔴 Sending RELEASE...");
                sendKeyBoardReleaseSync();
                currentlyPressedKeys.clear();
                Log.e(TAG, "✅ Release sent, cleared all pressed keys");
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in sendKeyBoardReleaseQueued: " + e.getMessage(), e);
            }
            Log.e(TAG, "🔴 ========== QUEUED KEY RELEASE END ==========");
        });
    }

    /**
     * Synchronous key press - must be called from executor thread
     */
    private static boolean sendKeyBoardPressSync(String functionKey, String keyName) {
        try {
            if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
                Log.e(TAG, "❌ sendKeyBoardPressSync: USB device not connected");
                return false;
            }

            String sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                    CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                    CH9329MSKBMap.DataLen().get("DataLenKB") +
                    functionKey +
                    CH9329MSKBMap.DataNull().get("DataNull") +
                    currentKeyCodeMap.get(keyName) +
                    CH9329MSKBMap.DataNull().get("DataNull") +
                    CH9329MSKBMap.DataNull().get("DataNull") +
                    CH9329MSKBMap.DataNull().get("DataNull") +
                    CH9329MSKBMap.DataNull().get("DataNull") +
                    CH9329MSKBMap.DataNull().get("DataNull");

            sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

            Log.e(TAG, "🟣 Sync press command: " + sendKBData);

            byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

            long writeStart = System.currentTimeMillis();
            boolean result = usbDeviceManager.writeData(sendKBDataBytes);
            long writeEnd = System.currentTimeMillis();
            if (result) {
                Log.e(TAG, "✅ Sync press sent in " + (writeEnd - writeStart) + "ms");
            } else {
                Log.e(TAG, "❌ Sync press write returned false");
            }
            return result;

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in sendKeyBoardPressSync: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Synchronous key release - must be called from executor thread
     */
    private static void sendKeyBoardReleaseSync() {
        try {
            String releaseKBData = CH9329MSKBMap.getKeyCodeMap().get("release");
            if (releaseKBData == null) {
                Log.e(TAG, "❌ sendKeyBoardReleaseSync: releaseKBData is null");
                return;
            }

            Log.e(TAG, "🟣 Sync release command: " + releaseKBData);

            byte[] releaseKBDataBytes = CH9329Function.hexStringToByteArray(releaseKBData);

            // Use usbDeviceManager for proper device routing (serial/FE0C)
            if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
                long writeStart = System.currentTimeMillis();
                boolean result = usbDeviceManager.writeData(releaseKBDataBytes);
                long writeEnd = System.currentTimeMillis();
                if (result) {
                    Log.e(TAG, "✅ Sync release sent in " + (writeEnd - writeStart) + "ms");
                } else {
                    Log.e(TAG, "❌ Sync release write returned false");
                }
            } else {
                Log.e(TAG, "❌ sendKeyBoardReleaseSync: USB device not connected");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in sendKeyBoardReleaseSync: " + e.getMessage(), e);
        }
    }

    /**
     * Send keyboard release event (key up) - releases all keys
     */
    public static void sendKeyBoardRelease() {
        Log.e(TAG, "🔴 ========== KEY RELEASE START ==========");
        Log.e(TAG, "🔴 Thread: " + Thread.currentThread().getName());
        Log.e(TAG, "🔴 Time: " + System.currentTimeMillis());

        new Thread(() -> {
            // Small delay to ensure press was sent before release
            // This prevents release from overtaking press in the serial queue
            try {
                Log.e(TAG, "🔴 Waiting 20ms before release...");
                Thread.sleep(20);
                Log.e(TAG, "🔴 Now sending release command");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "🔴 Release delay interrupted: " + e.getMessage());
            }

            EmptyKeyboard();
            Log.e(TAG, "🔴 ========== KEY RELEASE END ==========");
        }).start();
    }

    /**
     * Send modifier key press (Ctrl, Alt, Shift, Win) — uses modifier byte, not key byte.
     * @param modifierKey one of "Ctrl", "Shift", "Alt", "Win" (from KBShortCutKey mapping)
     */
    public static void sendModifierPress(String modifierKey) {
        if (modifierKey == null) {
            Log.w(TAG, "sendModifierPress: modifierKey is null");
            return;
        }

        Log.d(TAG, "Modifier press: " + modifierKey);

        new Thread(() -> {
            try {
                // Use usbDeviceManager.writeData() like other keyboard methods
                if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
                    Log.e(TAG, "sendModifierPress USB device not connected");
                    return;
                }

                String modifierValue = CH9329MSKBMap.KBShortCutKey().get(modifierKey);
                if (modifierValue == null) {
                    Log.e(TAG, "sendModifierPress: unknown modifier " + modifierKey);
                    return;
                }

                String sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                        CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                        CH9329MSKBMap.getKeyCodeMap().get("address") +
                        CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                        CH9329MSKBMap.DataLen().get("DataLenKB") +
                        modifierValue +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull");

                sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);
                Log.d(TAG, "Modifier press command: " + sendKBData);
                CH9329Function.checkSendLogData(sendKBData);

                byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);
                boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                Log.d(TAG, "Modifier press sent: " + modifierKey + " result=" + result);
            } catch (Exception e) {
                Log.e(TAG, "Error sending modifier press: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Send modifier key release — sends release command to clear all modifiers.
     */
    public static void sendModifierRelease() {
        Log.d(TAG, "Modifier release");
        sendKeyBoardRelease();
    }

    public static void sendKeyBoardShortCut(String modifier, String key) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
                        Log.d(TAG, "sendKeyBoardShortCut USB device not connected");
                        return;
                    }

                    //Press ShortCut
                    String sendKBData = "";
                    sendKBData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                                    CH9329MSKBMap.DataLen().get("DataLenKB") +
                                    CH9329MSKBMap.KBShortCutKey().get(modifier) +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.getKeyCodeMap().get(key) +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull");

                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (!result) {
                            Log.e(TAG, "Failed to write shortcut key data");
                        }
                        EmptyKeyboard();
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing shortcut key: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sendKeyBoardFunctionCtrlAltDel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()){
                        Log.d(TAG, "sendKeyBoardFunction USB device not connected");
                        return;
                    }

                    //Press Ctrl+Alt+Del
                    String sendKBData = "";
                    sendKBData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            CH9329MSKBMap.KBFunctionKey().get("FunctionKey") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            "E0" + "E2" + "4C"+
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (result) {
                            EmptyKeyboard();
                        } else {
                            Log.e(TAG, "Failed to write Ctrl+Alt+Del keyboard data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sendKeyBoardFunction(String FunctionKeyCtrlPress, String FunctionKeyShiftPress, String FunctionKeyAltPress, String FunctionKeyWinPress, String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()){
                        Log.d(TAG, "sendKeyBoardFunction USB device not connected");
                        return;
                    }

                    // Gets the control key mapping value
                    String ctrlValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyCtrlPress);
                    String shiftValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyShiftPress);
                    String altValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyAltPress);
                    String winValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyWinPress);

                    // Resolve to integers
                    int ctrl = ctrlValue != null ? Integer.parseInt(ctrlValue.replace("0x", ""), 16) : 0;
                    int shift = shiftValue != null ? Integer.parseInt(shiftValue.replace("0x", ""), 16) : 0;
                    int alt = altValue != null ? Integer.parseInt(altValue.replace("0x", ""), 16) : 0;
                    int win = winValue != null ? Integer.parseInt(winValue.replace("0x", ""), 16) : 0;

                    // Direct accumulation
                    int combinedValue = ctrl + shift + alt + win;

                    // Convert to a two-digit hexadecimal string
                    String combinationFunctionKey = String.format("%02X", combinedValue);

                    String sendKBData = "";
                    sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            combinationFunctionKey +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            currentKeyCodeMap.get(keyName) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");
                    Log.d(TAG, "FunctionKeyPress: " + CH9329MSKBMap.KBShortCutKey().get(combinationFunctionKey));
                    Log.e(TAG, "successful send keyboard data: " + keyName);
                    Log.e(TAG, "successful send keyboard data currentKeyCodeMap: " + currentKeyCodeMap);
                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (result) {
                            Log.e(TAG, "successful send keyboard data ");
                            EmptyKeyboard();
                        } else {
                            Log.e(TAG, "Failed to write keyboard function data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Send keyboard function key press (without automatic release)
     * @param FunctionKeyCtrlPress Ctrl key state
     * @param FunctionKeyShiftPress Shift key state
     * @param FunctionKeyAltPress Alt key state
     * @param FunctionKeyWinPress Win key state
     * @param keyName the key to press
     */
    public static void sendKeyBoardFunctionPress(String FunctionKeyCtrlPress, String FunctionKeyShiftPress, String FunctionKeyAltPress, String FunctionKeyWinPress, String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()){
                        Log.d(TAG, "sendKeyBoardFunctionPress USB device not connected");
                        return;
                    }

                    // Gets the control key mapping value
                    String ctrlValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyCtrlPress);
                    String shiftValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyShiftPress);
                    String altValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyAltPress);
                    String winValue = CH9329MSKBMap.KBShortCutKey().get(FunctionKeyWinPress);

                    // Resolve to integers
                    int ctrl = ctrlValue != null ? Integer.parseInt(ctrlValue.replace("0x", ""), 16) : 0;
                    int shift = shiftValue != null ? Integer.parseInt(shiftValue.replace("0x", ""), 16) : 0;
                    int alt = altValue != null ? Integer.parseInt(altValue.replace("0x", ""), 16) : 0;
                    int win = winValue != null ? Integer.parseInt(winValue.replace("0x", ""), 16) : 0;

                    // Direct accumulation
                    int combinedValue = ctrl + shift + alt + win;

                    // Convert to a two-digit hexadecimal string
                    String combinationFunctionKey = String.format("%02X", combinedValue);

                    String sendKBData = "";
                    sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            combinationFunctionKey +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            currentKeyCodeMap.get(keyName) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");
                    
                    Log.d(TAG, "=== KEY PRESS === FunctionKey: " + combinationFunctionKey + ", keyName: " + keyName);
                    
                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (result) {
                            Log.d(TAG, "Key press sent successfully (no auto-release)");
                        } else {
                            Log.e(TAG, "Failed to write keyboard function press data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Exception in sendKeyBoardFunctionPress: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    public static void sendKeyboardRequest(String function_key, String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()) {
                        Log.d(TAG, "sendKeyboardRequest USB device not connected");
                        return;
                    }

                    Log.d(TAG, "keyName: " + keyName);
                    Log.d(TAG, "currentKeyCodeMap class: " + currentKeyCodeMap.getClass().getName());
                    Log.d(TAG, "currentKeyCodeMap size: " + currentKeyCodeMap.size());
                    String lookedUpCode = currentKeyCodeMap.get(keyName);
                    Log.d(TAG, "lookedUpCode for '" + keyName + "': " + lookedUpCode);

                    String sendKBData = "";
                    sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            function_key +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            currentKeyCodeMap.get(keyName) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (result) {
                            Log.d(TAG, "Key press sent successfully");
                        } else {
                            Log.e(TAG, "Key press write returned false");
                        }

                        // Add small delay before releasing key to ensure proper key press registration
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }

                        // Release the key immediately after
                        KeyBoardManager.EmptyKeyboard();
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing key data: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sendKeyboardMultiple(String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (usbDeviceManager == null || !usbDeviceManager.isConnected()){
                        Log.d(TAG, "sendKeyboardMultiple USB device not connected");
                        return;
                    }

                    Log.d(TAG, "keyName: " + keyName);

                    String sendKBData = "";
                    sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.getKeyCodeMap().get(keyName) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        boolean result = usbDeviceManager.writeData(sendKBDataBytes);
                        if (!result) {
                            Log.e(TAG, "Failed to write keyboard multiple data");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                    KeyBoardManager.EmptyKeyboard();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public KeyBoardManager(Context context) {
        this.context = context;
    }

    //release all keyboard button
    public static void EmptyKeyboard() {
        Log.e(TAG, "🔴 EmptyKeyboard called - checking conditions...");
        
        // Check using usbDeviceManager first (consistent with press)
        if (usbDeviceManager != null && usbDeviceManager.isConnected()) {
            Log.e(TAG, "🔴 Using usbDeviceManager.writeData() for release");
            
            String releaseKBData = CH9329MSKBMap.getKeyCodeMap().get("release");
            if (releaseKBData == null) {
                Log.e(TAG, "❌ EmptyKeyboard: releaseKBData is null");
                return;
            }

            Log.e(TAG, "🔴 EmptyKeyboard: Release command: " + releaseKBData);
            CH9329Function.ReleaseSendLogData(releaseKBData);

            byte[] releaseKBDataBytes = CH9329Function.hexStringToByteArray(releaseKBData);
            if (releaseKBDataBytes == null || releaseKBDataBytes.length == 0) {
                Log.e(TAG, "❌ EmptyKeyboard: releaseKBDataBytes is null or empty");
                return;
            }

            Log.e(TAG, "🔴 EmptyKeyboard: Writing " + releaseKBDataBytes.length + " bytes: " + Arrays.toString(releaseKBDataBytes));

            try {
                long startTime = System.currentTimeMillis();
                boolean result = usbDeviceManager.writeData(releaseKBDataBytes);
                long endTime = System.currentTimeMillis();
                if (result) {
                    Log.e(TAG, "✅ EmptyKeyboard: Key release sent successfully via usbDeviceManager in " + (endTime - startTime) + "ms");
                } else {
                    Log.e(TAG, "❌ EmptyKeyboard: usbDeviceManager.writeData() returned false");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ EmptyKeyboard: Exception while writing via usbDeviceManager: " + e.getMessage(), e);
            }
            return;
        }

        // No fallback to direct port access — UsbDeviceManager.port is a static reference
        // that may not reflect the actual connection state, and port.write() uses
        // testConnection() which fails on some CH340 chips.
        Log.e(TAG, "❌ EmptyKeyboard: usbDeviceManager is null or not connected, cannot send key release");
    }
}
