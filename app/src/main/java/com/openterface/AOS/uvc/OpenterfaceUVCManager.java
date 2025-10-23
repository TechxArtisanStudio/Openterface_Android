/**
 * @Title: OpenterfaceUVCManager
 * @Package com.openterface.AOS.uvc
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
package com.openterface.AOS.uvc;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCControl;
import com.serenegiant.usb.USBVendorId;

public class OpenterfaceUVCManager {
    private static final String TAG = "OpenterfaceUVCManager";
    
    // Openterface UVC device VID/PID combinations
    private static final int[][] OPENTERFACE_UVC_DEVICES = {
        {0x534D, 0x2109},  // Openterface Mini-KVM v1
        {0x345F, 0x2109},  // KVMGO-VGA
        {0x345F, 0x2132},  // KVMGO
    };
    
    private USBMonitor usbMonitor;
    private UVCControl uvcControl;
    private Context context;
    private boolean isConnected = false;
    
    public interface OnUVCDeviceListener {
        void onUVCDeviceAttached(UsbDevice device);
        void onUVCDeviceDetached(UsbDevice device);
        void onUVCDeviceConnected(UsbDevice device);
        void onUVCDeviceDisconnected(UsbDevice device);
    }
    
    private OnUVCDeviceListener deviceListener;
    
    public OpenterfaceUVCManager(Context context) {
        this.context = context;
    }
    
    public void setOnUVCDeviceListener(OnUVCDeviceListener listener) {
        this.deviceListener = listener;
    }
    
    public void initialize() {
        usbMonitor = new USBMonitor(context, mOnDeviceConnectListener);
        usbMonitor.register();
        Log.d(TAG, "Openterface UVC Manager initialized");
        logSupportedDevices();
    }
    
    private void logSupportedDevices() {
        Log.d(TAG, "Supported Openterface UVC devices:");
        for (int[] deviceIds : OPENTERFACE_UVC_DEVICES) {
            Log.d(TAG, String.format("  %04X:%04X", deviceIds[0], deviceIds[1]));
        }
    }
    
    private boolean isOpenterfaceUVCDevice(UsbDevice device) {
        if (device == null) return false;
        
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        for (int[] deviceIds : OPENTERFACE_UVC_DEVICES) {
            if (deviceIds[0] == vid && deviceIds[1] == pid) {
                Log.d(TAG, "Found Openterface UVC device: " + String.format("%04X:%04X", vid, pid));
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
        
        // Fallback to vendor name if available
        String vendorName = USBVendorId.vendorName(vid);
        return vendorName != null ? vendorName + " Device" : "Openterface Device";
    }
    
    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = 
        new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(UsbDevice device) {
                if (isOpenterfaceUVCDevice(device)) {
                    String deviceName = getDeviceName(device);
                    Log.d(TAG, "Openterface UVC device attached: " + deviceName + 
                        " [" + String.format("%04X:%04X", device.getVendorId(), device.getProductId()) + "]");
                    
                    if (deviceListener != null) {
                        deviceListener.onUVCDeviceAttached(device);
                    }
                    
                    // Request permission to connect
                    usbMonitor.requestPermission(device);
                }
            }
            
            @Override
            public void onDetach(UsbDevice device) {
                if (isOpenterfaceUVCDevice(device)) {
                    Log.d(TAG, "Openterface UVC device detached");
                    isConnected = false;
                    
                    if (deviceListener != null) {
                        deviceListener.onUVCDeviceDetached(device);
                    }
                    
                    if (uvcControl != null) {
                        uvcControl.release();
                        uvcControl = null;
                    }
                }
            }
            
            @Override
            public void onDeviceOpen(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                if (isOpenterfaceUVCDevice(device)) {
                    Log.d(TAG, "Openterface UVC device connected and ready for use");
                    isConnected = true;
                    
                    if (deviceListener != null) {
                        deviceListener.onUVCDeviceConnected(device);
                    }
                    
                    // Initialize UVC control - note: UVCControl constructor requires native handle
                    // For now, we'll skip creating UVCControl as it needs native uvc_device_handle_t
                    // This will be implemented when native integration is added
                }
            }
            
            @Override
            public void onDeviceClose(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if (isOpenterfaceUVCDevice(device)) {
                    Log.d(TAG, "Openterface UVC device disconnected");
                    isConnected = false;
                    
                    if (deviceListener != null) {
                        deviceListener.onUVCDeviceDisconnected(device);
                    }
                }
            }
            
            @Override
            public void onCancel(UsbDevice device) {
                if (isOpenterfaceUVCDevice(device)) {
                    Log.d(TAG, "Openterface UVC device permission cancelled");
                }
            }
        };
    
    /**
     * Check if an Openterface UVC device is connected
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && uvcControl != null;
    }
    
    /**
     * Get the UVC control instance
     * @return UVCControl instance if connected, null otherwise
     */
    public UVCControl getUVCControl() {
        return uvcControl;
    }
    
    /**
     * Release resources and cleanup
     */
    public void release() {
        if (usbMonitor != null) {
            usbMonitor.unregister();
            usbMonitor = null;
        }
        if (uvcControl != null) {
            uvcControl.release();
            uvcControl = null;
        }
        isConnected = false;
        Log.d(TAG, "Openterface UVC Manager released");
    }
}