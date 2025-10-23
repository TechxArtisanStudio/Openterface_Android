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
    private static final String TAG = KeyBoardManager.class.getSimpleName();
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

    public static void setKeyBoardLanguage() {
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("de")) {
            currentKeyCodeMap = KeyMapConfig_De.getKeyCodeMap();
//            KeyBoardSystem.setKeyboardLanguage("de");
            Log.d("setKeyBoardLanguage", "language is de");
        }else {
            currentKeyCodeMap = CH9329MSKBMap.getKeyCodeMap();
//            KeyBoardSystem.setKeyboardLanguage("us");
            Log.d("setKeyBoardLanguage", "language is us");
        }
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

    public static void sendKeyBoardShortCut(String modifier, String key) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (UsbDeviceManager.port == null){
                        Log.d(TAG, "sendKeyBoardFunction port is null");
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
                        UsbDeviceManager.port.write(sendKBDataBytes, 200);
                        EmptyKeyboard();
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
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
                    System.out.println("FunctionKeyPress: " + CH9329MSKBMap.KBShortCutKey().get(combinationFunctionKey));
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

    public static void sendKeyboardRequest(String function_key, String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (UsbDeviceManager.port == null){
                        Log.d(TAG, "sendKeyboardRequest port is null");
                        return;
                    }

                    System.out.println("keyName: " + keyName);

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
                        // Use direct port write for reliable key press at any baudrate
                        UsbDeviceManager.port.write(sendKBDataBytes, 10);
                        Log.d(TAG, "Key press sent successfully");
                        
                        // Add small delay before releasing key to ensure proper key press registration
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        
                        // Release the key immediately after
                        KeyBoardManager.EmptyKeyboard();
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
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

                    System.out.println("keyName: " + keyName);

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
        // For 7523 devices at 9600 baud, use direct port access for reliable key release
        if (UsbDeviceManager.port == null) {
            Log.d(TAG, "EmptyKeyboard: port is null");
            return;
        }

        String releaseKBData = CH9329MSKBMap.getKeyCodeMap().get("release");
        if (releaseKBData == null) {
            Log.e(TAG, "EmptyKeyboard: releaseKBData is null");
            return;
        }

        CH9329Function.ReleaseSendLogData(releaseKBData);

        byte[] releaseKBDataBytes = CH9329Function.hexStringToByteArray(releaseKBData);
        if (releaseKBDataBytes == null || releaseKBDataBytes.length == 0) {
            Log.e(TAG, "EmptyKeyboard: releaseKBDataBytes is null or empty");
            return;
        }

        Log.d(TAG, "EmptyKeyboard: Writing " + releaseKBDataBytes.length + " bytes: " + Arrays.toString(releaseKBDataBytes));

        try {
            // Direct port write for all devices - most reliable method
            UsbDeviceManager.port.write(releaseKBDataBytes, 200);
            Log.d(TAG, "EmptyKeyboard: Key release sent successfully");
        } catch (IOException e) {
            Log.e(TAG, "EmptyKeyboard: IOException while writing to port: " + e.getMessage());
            if (context != null) {
                Toast.makeText(context, "Please Restart APP", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
