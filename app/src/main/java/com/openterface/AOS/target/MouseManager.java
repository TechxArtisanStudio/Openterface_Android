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

import android.util.Log;
import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;
import java.io.IOException;

public class MouseManager {
    private static final String TAG = MouseManager.class.getSimpleName();
    private static UsbDeviceManager usbDeviceManager;
    public static int screenWidth, screenHeight;

    public static void width_height(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    public static void sendHexAbsData(float x, float y) {
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
                            CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
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
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
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

    public static void sendHexAbsDragData(float x, float y) {
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
                            CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecLeftData") + //MS key
                            String.format("%02X", xBytes[0]) +
                            String.format("%02X", xBytes[1]) +
                            String.format("%02X", yBytes[0]) +
                            String.format("%02X", yBytes[1]) +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);

                    CH9329Function.checkSendLogData(sendMSData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
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

    public static void handleDoubleClickAbs(float x, float y) {

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
                            CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
                            CH9329MSKBMap.MSAbsData().get("FirstData") +
                            CH9329MSKBMap.MSAbsData().get("SecLeftData") + //MS key
                            String.format("%02X", xBytes[0]) +
                            String.format("%02X", xBytes[1]) +
                            String.format("%02X", yBytes[0]) +
                            String.format("%02X", yBytes[1]) +
                            CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
                        Log.d(TAG, "send data successful");
                        releaseMSAbsData(xBytes0, xBytes1, yBytes0, yBytes1);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void handleTwoPress() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String sendMSDoubleClickData = "";
                    sendMSDoubleClickData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecRightData") + //MS key
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
                        Log.d(TAG, "send data successful two click");
                        releaseMSRelData();
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void handleDoubleFingerPan(float StartMoveMSY, float LastMoveMSY) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int yMovement = (int) (StartMoveMSY - LastMoveMSY);

                    String yByte = "";
                    if (yMovement == 0) {
                        yByte = "00";
                    }else if(LastMoveMSY == 0){
                        yByte = "00";
                    } else if (yMovement > 0) {
                        yByte = "01";
                        System.out.println("yMovement > 0 data :" + yByte);
//                        yByte = String.format("%02X", Math.min(yMovement, 0x7F));
                    } else {
                        yByte = "FF";
//                        yByte = String.format("%02X", 0x100 + yMovement);
                        System.out.println("yByte < 0 data :" + yByte);
                    }

                    String sendMSData = "";
                    sendMSData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecMiddleData") + //MS key
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    yByte;

                    sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);

                    if (sendMSData.length() % 2 != 0) {
                        sendMSData += "0";
                    }

                    CH9329Function.checkSendLogData(sendMSData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
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

    public static void releaseMSAbsData(String xBytes0, String xBytes1, String yBytes0, String yBytes1) {
        String sendReleaseMSData = "";
        sendReleaseMSData = CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                CH9329MSKBMap.getKeyCodeMap().get("address") +
                CH9329MSKBMap.CmdData().get("CmdMS_ABS") +
                CH9329MSKBMap.DataLen().get("DataLenAbsMS") +
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
            usbDeviceManager.port.write(sendKBDataBytes, 20);
            Log.d(TAG, "release all MS data");
        } catch (IOException e) {
            Log.e(TAG, "Error writing to port: " + e.getMessage());
        }
    }

    public static void handleDoubleClickRel() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String sendMSDoubleClickData = "";
                    sendMSDoubleClickData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecLeftData") + //MS key
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull") +
                                    CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSDoubleClickData = sendMSDoubleClickData + CH9329Function.makeChecksum(sendMSDoubleClickData);

                    CH9329Function.checkSendLogData(sendMSDoubleClickData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSDoubleClickData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
                        Log.d(TAG, "send data successful");
                        releaseMSRelData();
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void sendHexRelData(float StartMoveMSX, float StartMoveMSY, float LastMoveMSX, float LastMoveMSY) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int xMovement = (int) (StartMoveMSX - LastMoveMSX);
                    int yMovement = (int) (StartMoveMSY - LastMoveMSY);

                    String xByte;
                    if (xMovement == 0) {
                        xByte = "00";
                    }else if(LastMoveMSX == 0){
                        xByte = "00";
                    } else if (xMovement > 0) {
                        xByte = String.format("%02X", Math.min(xMovement, 0x7F));
                    } else {
                        xByte = String.format("%02X", 0x100 + xMovement);
                    }

                    String yByte;
                    if (yMovement == 0) {
                        yByte = "00";
                    }else if(LastMoveMSY == 0){
                        yByte = "00";
                    } else if (yMovement > 0) {
                        yByte = String.format("%02X", Math.min(yMovement, 0x7F));
                    } else {
                        yByte = String.format("%02X", 0x100 + yMovement);
                    }

                    String sendMSData = "";
                    sendMSData =
                            CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                                    CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                                    CH9329MSKBMap.getKeyCodeMap().get("address") +
                                    CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                                    CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                                    CH9329MSKBMap.MSRelData().get("FirstData") +
                                    CH9329MSKBMap.MSRelData().get("SecNullData") + //MS key
                                    xByte +
                                    yByte +
                                    CH9329MSKBMap.DataNull().get("DataNull");

                    sendMSData = sendMSData + CH9329Function.makeChecksum(sendMSData);

                    if (sendMSData.length() % 2 != 0) {
                        sendMSData += "0";
                    }
                    CH9329Function.checkSendLogData(sendMSData);

                    byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendMSData);

                    try {
                        usbDeviceManager.port.write(sendKBDataBytes, 20);
//                        Log.d(TAG, "send data successful");
                    } catch (IOException e) {
//                        Log.e(TAG, "Error writing to port: " + e.getMessage());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void releaseMSRelData() {
        String sendReleaseMSData = "";
        sendReleaseMSData =
                CH9329MSKBMap.getKeyCodeMap().get("prefix1") +
                        CH9329MSKBMap.getKeyCodeMap().get("prefix2") +
                        CH9329MSKBMap.getKeyCodeMap().get("address") +
                        CH9329MSKBMap.CmdData().get("CmdMS_REL") +
                        CH9329MSKBMap.DataLen().get("DataLenRelMS") +
                        CH9329MSKBMap.MSRelData().get("FirstData") +
                        CH9329MSKBMap.MSRelData().get("SecNullData") + //MS key
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull") +
                        CH9329MSKBMap.DataNull().get("DataNull");

        sendReleaseMSData = sendReleaseMSData + CH9329Function.makeChecksum(sendReleaseMSData);

        CH9329Function.checkSendLogData(sendReleaseMSData);

        byte[] sendKBDataBytes = CH9329Function.hexStringToByteArray(sendReleaseMSData);

        try {
            usbDeviceManager.port.write(sendKBDataBytes, 20);
            Log.d(TAG, "release all MS data");
        } catch (IOException e) {
            Log.e(TAG, "Error writing to port: " + e.getMessage());
        }
    }
}
