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

    static {
        currentKeyCodeMap = CH9329MSKBMap.getKeyCodeMap();
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
                    if (UsbDeviceManager.port == null){
                        Log.d(TAG, "sendKeyBoardFunction port is null");
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

    public static void sendKeyBoardFunction(String FunctionKeyPress, String keyName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (UsbDeviceManager.port == null){
                        Log.d(TAG, "sendKeyBoardFunction port is null");
                        return;
                    }

                    String sendKBData = "";
                    sendKBData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdKB_HID") +
                            CH9329MSKBMap.DataLen().get("DataLenKB") +
                            CH9329MSKBMap.KBShortCutKey().get(FunctionKeyPress) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            currentKeyCodeMap.get(keyName) +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull") +
                            CH9329MSKBMap.DataNull().get("DataNull");
                    System.out.println("FunctionKeyPress: " + CH9329MSKBMap.KBShortCutKey().get(FunctionKeyPress));
                    Log.e(TAG, "successful send keyboard data: " + keyName);
                    Log.e(TAG, "successful send keyboard data currentKeyCodeMap: " + currentKeyCodeMap);
                    sendKBData = sendKBData + CH9329Function.makeChecksum(sendKBData);

                    CH9329Function.checkSendLogData(sendKBData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendKBData);

                    try {
                        UsbDeviceManager.port.write(sendKBDataBytes, 200);
                        Log.e(TAG, "successful send keyboard data ");
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
                        UsbDeviceManager.port.write(sendKBDataBytes, 10);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

//                    KeyBoardManager.EmptyKeyboard();

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
                    if (UsbDeviceManager.port == null){
                        Log.d(TAG, "sendKeyboardMultiple port is null");
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
                        UsbDeviceManager.port.write(sendKBDataBytes, 10);
                    } catch (IOException e) {
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
        if (UsbDeviceManager.port == null) {
            Log.d(TAG, "EmptyKeyboard port is null");
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
            UsbDeviceManager.port.write(releaseKBDataBytes, 200);
        } catch (IOException e) {
            Toast.makeText(context, "Please Restart APP", Toast.LENGTH_SHORT).show();
//            Log.e(TAG, "EmptyKeyboard: IOException while writing to port", e);
        }
    }
}
