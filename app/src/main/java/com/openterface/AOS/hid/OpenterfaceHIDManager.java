/**
 * @Title: OpenterfaceHIDManager
 * @Package com.openterface.AOS.hid
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
package com.openterface.AOS.hid;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;

public class OpenterfaceHIDManager {
    private static final String TAG = "OpenterfaceHIDManager";
    
    // Openterface HID device VID/PID combinations
    private static final int[][] OPENTERFACE_HID_DEVICES = {
        {0x534D, 0x2109},  // Openterface Mini-KVM v1
        {0x345F, 0x2109},  // KVMGO-VGA
        {0x345F, 0x2132},  // KVMGO
    };
    
    private UsbManager usbManager;
    private Context context;
    private boolean isConnected = false;
    private UsbDevice connectedDevice = null;
    
    // Load native library for HID operations
    static {
        try {
            System.loadLibrary("openterface_hid");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Native HID library not found, HID functionality will be limited");
        }
    }
    
    public interface OnHIDDeviceListener {
        void onHIDDeviceConnected(UsbDevice device);
        void onHIDDeviceDisconnected(UsbDevice device);
        void onHIDDataReceived(byte[] data);
    }
    
    private OnHIDDeviceListener deviceListener;
    
    public OpenterfaceHIDManager(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }
    
    public void setOnHIDDeviceListener(OnHIDDeviceListener listener) {
        this.deviceListener = listener;
    }
    
    public void initialize() {
        Log.d(TAG, "Openterface HID Manager initialized");
        logSupportedDevices();
        scanForOpenterfaceHIDDevices();
    }
    
    private void logSupportedDevices() {
        Log.d(TAG, "Supported Openterface HID devices:");
        for (int[] deviceIds : OPENTERFACE_HID_DEVICES) {
            Log.d(TAG, String.format("  %04X:%04X", deviceIds[0], deviceIds[1]));
        }
    }
    
    private boolean isOpenterfaceHIDDevice(UsbDevice device) {
        if (device == null) return false;
        
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        for (int[] deviceIds : OPENTERFACE_HID_DEVICES) {
            if (deviceIds[0] == vid && deviceIds[1] == pid) {
                Log.d(TAG, "Found Openterface HID device: " + String.format("%04X:%04X", vid, pid));
                return true;
            }
        }
        return false;
    }
    
    private String getDeviceName(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        if (vid == 0x534D && pid == 0x2109) {
            return "Openterface Mini-KVM v1";
        } else if (vid == 0x345F && pid == 0x2109) {
            return "KVMGO-VGA";
        } else if (vid == 0x345F && pid == 0x2132) {
            return "KVMGO";
        }
        
        return "Openterface HID Device";
    }
    
    public void scanForOpenterfaceHIDDevices() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        
        for (UsbDevice device : deviceList.values()) {
            if (isOpenterfaceHIDDevice(device)) {
                String deviceName = getDeviceName(device);
                Log.d(TAG, "Found Openterface HID device: " + deviceName + 
                    " [" + String.format("%04X:%04X", device.getVendorId(), device.getProductId()) + "]");
                
                if (connectToHIDDevice(device)) {
                    connectedDevice = device;
                    isConnected = true;
                    
                    if (deviceListener != null) {
                        deviceListener.onHIDDeviceConnected(device);
                    }
                    
                    Log.d(TAG, "Successfully connected to Openterface HID device: " + deviceName);
                    break; // Connect to first found device
                }
            }
        }
        
        if (!isConnected) {
            Log.d(TAG, "No Openterface HID devices found or failed to connect");
        }
    }
    
    private boolean connectToHIDDevice(UsbDevice device) {
        try {
            // For now, we'll use Java-level HID handling
            // In the future, this can be enhanced with native libusb calls
            
            // Check if device has HID interface
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                if (device.getInterface(i).getInterfaceClass() == 3) { // HID class
                    Log.d(TAG, "Found HID interface on device");
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to HID device", e);
            return false;
        }
    }
    
    /**
     * Check if an Openterface HID device is connected
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && connectedDevice != null;
    }
    
    /**
     * Get the connected HID device
     * @return UsbDevice instance if connected, null otherwise
     */
    public UsbDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * Send HID data to the connected Openterface device
     * @param data byte array to send
     * @return true if successful, false otherwise
     */
    public boolean sendHIDData(byte[] data) {
        if (!isConnected()) {
            Log.w(TAG, "No Openterface HID device connected");
            return false;
        }
        
        // For now, log the data that would be sent
        Log.d(TAG, "Sending HID data: " + bytesToHex(data));
        
        // TODO: Implement actual HID communication
        // This would typically involve:
        // 1. Getting USB device connection
        // 2. Claiming HID interface
        // 3. Sending data via interrupt transfer
        
        return true;
    }
    
    /**
     * Read HID data from the connected Openterface device
     * @return byte array of received data, null if no data or error
     */
    public byte[] readHIDData() {
        if (!isConnected()) {
            Log.w(TAG, "No Openterface HID device connected");
            return null;
        }
        
        // TODO: Implement actual HID communication
        // This would typically involve:
        // 1. Getting USB device connection
        // 2. Claiming HID interface
        // 3. Reading data via interrupt transfer
        
        return null;
    }
    
    /**
     * Disconnect from the current HID device
     */
    public void disconnect() {
        if (isConnected && connectedDevice != null) {
            Log.d(TAG, "Disconnecting from Openterface HID device");
            
            if (deviceListener != null) {
                deviceListener.onHIDDeviceDisconnected(connectedDevice);
            }
            
            connectedDevice = null;
            isConnected = false;
        }
    }
    
    /**
     * Release resources and cleanup
     */
    public void release() {
        disconnect();
        Log.d(TAG, "Openterface HID Manager released");
    }
    
    // Helper method to convert bytes to hex string for logging
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    // Native methods for future libusb integration
    // These would be implemented in JNI if native HID support is added
    
    /**
     * Open HID device using VID/PID (native implementation)
     * @param vid Vendor ID
     * @param pid Product ID
     * @return true if successful, false otherwise
     */
    public native boolean openHIDDeviceNative(int vid, int pid);
    
    /**
     * Close HID device (native implementation)
     */
    public native void closeHIDDeviceNative();
    
    /**
     * Read HID data (native implementation)
     * @return byte array of received data
     */
    public native byte[] readHIDDataNative();
    
    /**
     * Write HID data (native implementation)
     * @param data byte array to send
     * @return true if successful, false otherwise
     */
    public native boolean writeHIDDataNative(byte[] data);
}