/**
* @Title: MouseManager
* @Package com.example.openterface.target
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
package com.herohan.uvcapp.target;

import android.util.Log;
import com.herohan.uvcapp.serial.CH9329Function;
import com.herohan.uvcapp.serial.UsbDeviceManager;
import java.io.IOException;

public class MouseManager {
    private static final String TAG = MouseManager.class.getSimpleName();
    private static UsbDeviceManager usbDeviceManager;
    public static int screenWidth, screenHeight;

    public static void width_height(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    public static void sendHexData(float x, float y) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "screenWidth: " + screenWidth + " screenHeight: " + screenHeight);
                    // Calculate position
                    int x1 = (int) ((x * 4096) / screenWidth);
                    int y1 = (int) ((y * 4096) / screenHeight);

                    byte[] xBytes = CH9329Function.intToByteArray((int) x1);
                    byte[] yBytes = CH9329Function.intToByteArray((int) y1);

                    String sendMSData = "";
                    sendMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                            CH9329MSKBMap.DataLen().get("DataLenMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecNullData") + //MS key
                            String.format("%02X", xBytes[0]) +
                            String.format("%02X", xBytes[1]) +
                            String.format("%02X", yBytes[0]) +
                            String.format("%02X", yBytes[1]) +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);

                    CH9329Function.checkSendLogData(sendMSData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 200);
                        Log.d(TAG, "send data successful");
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void handleDoubleClick(float x, float y) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Calculate position
                    int x1 = (int) ((x * 4096) / screenWidth);
                    int y1 = (int) ((y * 4096) / screenHeight);

                    byte[] xBytes = CH9329Function.intToByteArray((int) x1);
                    byte[] yBytes = CH9329Function.intToByteArray((int) y1);

                    String xBytes0 = String.format("%02X", xBytes[0]);
                    String xBytes1 = String.format("%02X", xBytes[1]);
                    String yBytes0 = String.format("%02X", yBytes[0]);
                    String yBytes1 = String.format("%02X", yBytes[1]);

                    String sendMSDoubleClickData = "";
                    sendMSDoubleClickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                            CH9329MSKBMap.DataLen().get("DataLenMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecLeftData") + //MS key
                            xBytes0 +
                            xBytes1 +
                            yBytes0 +
                            yBytes1 +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 200);
                        Log.d(TAG, "send data successful");
                        releaseMSData(xBytes0, xBytes1, yBytes0, yBytes1);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void handleLongPress(float x, float y) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Calculate position
                    int x1 = (int) ((x * 4096) / screenWidth);
                    int y1 = (int) ((y * 4096) / screenHeight);

                    byte[] xBytes = CH9329Function.intToByteArray((int) x1);
                    byte[] yBytes = CH9329Function.intToByteArray((int) y1);

                    String sendMSDoubleClickData = "";
                    sendMSDoubleClickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                            CH9329MSKBMap.DataLen().get("DataLenMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecRightData") + //MS key
                            String.format("%02X", xBytes[0]) +
                            String.format("%02X", xBytes[1]) +
                            String.format("%02X", yBytes[0]) +
                            String.format("%02X", yBytes[1]) +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 200);
                        Log.d(TAG, "send data successful");
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void handleDoubleFingerPan(float x, float y, String rollingGearY) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 计算位置
                    int x1 = (int) ((x * 4096) / screenWidth);
                    int y1 = (int) ((y * 4096) / screenHeight);

                    // 将x和y转换为低字节在前，高字节在后的格式
                    byte[] xBytes = CH9329Function.intToByteArray((int) x1);
                    byte[] yBytes = CH9329Function.intToByteArray((int) y1);

                    String sendMSDoubleFingerClickData = "";
                    sendMSDoubleFingerClickData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                            CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                            CH9329MSKBMap.getKeyCodeMap().get("address") +
                            CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                            CH9329MSKBMap.DataLen().get("DataLenMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecNullData") + //MS key
                            String.format("%02X", xBytes[0]) +
                            String.format("%02X", xBytes[1]) +
                            String.format("%02X", yBytes[0]) +
                            String.format("%02X", yBytes[1]) +
                            CH9329MSKBMap.MSAbsData().get(rollingGearY);

                    Log.d(TAG, "sendMSDoubleClickData: " + sendMSDoubleFingerClickData);

                    sendMSDoubleFingerClickData = sendMSDoubleFingerClickData + CH9329Function.makeChecksum(sendMSDoubleFingerClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleFingerClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleFingerClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 200);
                        Log.d(TAG, "send data successful");
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void releaseMSData(String xBytes0, String xBytes1, String yBytes0, String yBytes1) {
        String sendReleaseMSData = "";
        sendReleaseMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenMS") +
                CH9329MSKBMap.MSAbsData().get("FirstData") +
                CH9329MSKBMap.MSAbsData().get("SecNullData") + //MS key
                xBytes0 +
                xBytes1 +
                yBytes0 +
                yBytes1 +
                CH9329MSKBMap.DataNull().get("DataNull");

        sendReleaseMSData = sendReleaseMSData + CH9329Function.makeChecksum(sendReleaseMSData);

        CH9329Function.checkSendLogData(sendReleaseMSData);

        byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendReleaseMSData);

        try {
            usbDeviceManager.port.write(sendKBDataBytes, 200);
            Log.d(TAG, "release all MS data");
        } catch (IOException e) {
            Log.e(TAG, "Error writing to port: " + e.getMessage());
        }
    }
}
