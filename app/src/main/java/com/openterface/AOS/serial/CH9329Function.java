/**
* @Title: CH9329Function
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

import android.util.Log;

public class CH9329Function {
    private static final String TAG = CH9329Function.class.getSimpleName();
    private static UsbDeviceManager usbDeviceManager;

    public static String makeChecksum(String data) {
        int total = 0;

        for (int i = 0; i < data.length(); i += 2) {
            String byteStr = data.substring(i, Math.min(i + 2, data.length()));
            total += Integer.parseInt(byteStr, 16);
        }

        int mod = total % 256;

        return String.format("%02X", mod);
    }

    public static byte[] hexStringToByteArray(String ByteData) {
        if (ByteData.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }

        int len = ByteData.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(ByteData.charAt(i), 16) << 4)
                    + Character.digit(ByteData.charAt(i + 1), 16));
        }
        return data;
    }

    public static void checkSendLogData(String sendKBData) {
        StringBuilder check_send_data = new StringBuilder();
        for (int i = 0; i < sendKBData.length(); i += 2) {
            if (i + 2 <= sendKBData.length()) {
                check_send_data.append(sendKBData.substring(i, i + 2)).append(" ");
            } else {
                check_send_data.append(sendKBData.substring(i)).append(" ");
            }
        }
        Log.d(TAG, "sendKBData: " + check_send_data.toString().trim());
    }

    public static void ReleaseSendLogData(String releaseKBData) {
        StringBuilder check_release_data = new StringBuilder();
        for (int i = 0; i < releaseKBData.length(); i += 2) {
            check_release_data.append(releaseKBData.substring(i, i + 2)).append(" ");
        }
        Log.d(TAG, "release data: " + check_release_data.toString().trim());
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
    }
}
