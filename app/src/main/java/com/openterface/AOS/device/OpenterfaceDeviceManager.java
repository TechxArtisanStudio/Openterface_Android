/**
 * @Title: OpenterfaceDeviceManager
 * @Package com.openterface.AOS.device
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
package com.openterface.AOS.device;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.openterface.AOS.serial.UsbDeviceManager;
import com.openterface.AOS.uvc.OpenterfaceUVCManager;
import com.openterface.AOS.hid.OpenterfaceHIDManager;
import com.serenegiant.usb.UVCControl;

public class OpenterfaceDeviceManager {
    private static final String TAG = "OpenterfaceDeviceManager";
    
    // Define all Openterface device configurations
    public static class OpenterfaceDevice {
        public final int vid;
        public final int pid;
        public final String name;
        public final DeviceType type;
        public final String description;
        
        public OpenterfaceDevice(int vid, int pid, String name, DeviceType type, String description) {
            this.vid = vid;
            this.pid = pid;
            this.name = name;
            this.type = type;
            this.description = description;
        }
    }
    
    public enum DeviceType {
        SERIAL, UVC, HID
    }
    
    public enum ConnectionStatus {
        DISCONNECTED, SERIAL_ONLY, UVC_ONLY, HID_ONLY, PARTIAL_CONNECTED, FULLY_CONNECTED
    }
    
    // Define all Openterface devices and their interfaces
    private static final OpenterfaceDevice[] OPENTERFACE_DEVICES = {
        // Openterface Mini-KVM v1
        new OpenterfaceDevice(0x534D, 0x2109, "Openterface Mini-KVM v1", DeviceType.UVC, "UVC Camera + HID Interface"),
        new OpenterfaceDevice(0x1A86, 0x7523, "Openterface Mini-KVM v1", DeviceType.SERIAL, "Serial Interface"),
        
        // KVMGO-VGA
        new OpenterfaceDevice(0x345F, 0x2109, "KVMGO-VGA", DeviceType.UVC, "UVC Camera + HID Interface"),
        
        // KVMGO
        new OpenterfaceDevice(0x345F, 0x2132, "KVMGO", DeviceType.UVC, "UVC Camera + HID Interface"),
        new OpenterfaceDevice(0x1A86, 0xFE0C, "KVMGO", DeviceType.SERIAL, "Serial Interface"),
    };
    
    private UsbDeviceManager serialManager;
    private OpenterfaceUVCManager uvcManager;
    private OpenterfaceHIDManager hidManager;
    private Context context;
    
    // Connection status tracking
    private boolean serialConnected = false;
    private boolean uvcConnected = false;
    private boolean hidConnected = false;
    
    public interface OnOpenterfaceDeviceListener {
        void onSerialConnected();
        void onSerialDisconnected();
        void onUVCConnected(UsbDevice device);
        void onUVCDisconnected(UsbDevice device);
        void onHIDConnected(UsbDevice device);
        void onHIDDisconnected(UsbDevice device);
        void onConnectionStatusChanged(ConnectionStatus status);
    }
    
    private OnOpenterfaceDeviceListener deviceListener;
    
    public OpenterfaceDeviceManager(Context context) {
        this.context = context;
        initializeManagers();
    }
    
    private void initializeManagers() {
        // Initialize Serial Manager
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        serialManager = new UsbDeviceManager(context, usbManager);
        serialManager.setOnDataReadListener(new UsbDeviceManager.OnDataReadListener() {
            @Override
            public void onDataRead(byte[] data, int length) {
                onSerialDataRead(data);
            }
        });
        
        // Initialize UVC Manager
        uvcManager = new OpenterfaceUVCManager(context);
        uvcManager.setOnUVCDeviceListener(new OpenterfaceUVCManager.OnUVCDeviceListener() {
            @Override
            public void onUVCDeviceAttached(UsbDevice device) {
                Log.d(TAG, "UVC device attached");
            }
            
            @Override
            public void onUVCDeviceDetached(UsbDevice device) {
                Log.d(TAG, "UVC device detached");
                uvcConnected = false;
                updateConnectionStatus();
                if (deviceListener != null) {
                    deviceListener.onUVCDisconnected(device);
                }
            }
            
            @Override
            public void onUVCDeviceConnected(UsbDevice device) {
                Log.d(TAG, "UVC device connected");
                uvcConnected = true;
                updateConnectionStatus();
                if (deviceListener != null) {
                    deviceListener.onUVCConnected(device);
                }
            }
            
            @Override
            public void onUVCDeviceDisconnected(UsbDevice device) {
                Log.d(TAG, "UVC device disconnected");
                uvcConnected = false;
                updateConnectionStatus();
                if (deviceListener != null) {
                    deviceListener.onUVCDisconnected(device);
                }
            }
        });
        
        // Initialize HID Manager
        hidManager = new OpenterfaceHIDManager(context);
        hidManager.setOnHIDDeviceListener(new OpenterfaceHIDManager.OnHIDDeviceListener() {
            @Override
            public void onHIDDeviceConnected(UsbDevice device) {
                Log.d(TAG, "HID device connected");
                hidConnected = true;
                updateConnectionStatus();
                if (deviceListener != null) {
                    deviceListener.onHIDConnected(device);
                }
            }
            
            @Override
            public void onHIDDeviceDisconnected(UsbDevice device) {
                Log.d(TAG, "HID device disconnected");
                hidConnected = false;
                updateConnectionStatus();
                if (deviceListener != null) {
                    deviceListener.onHIDDisconnected(device);
                }
            }
            
            @Override
            public void onHIDDataReceived(byte[] data) {
                Log.d(TAG, "HID data received: " + data.length + " bytes");
            }
        });
    }
    
    public void setOnOpenterfaceDeviceListener(OnOpenterfaceDeviceListener listener) {
        this.deviceListener = listener;
    }
    
    public void initialize() {
        Log.d(TAG, "Initializing Openterface Device Manager");
        logSupportedDevices();
        
        // Initialize all managers
        serialManager.init();
        uvcManager.initialize();
        hidManager.initialize();
        
        // Initial connection status check
        updateConnectionStatus();
    }
    
    private void logSupportedDevices() {
        Log.d(TAG, "Supported Openterface devices:");
        for (OpenterfaceDevice device : OPENTERFACE_DEVICES) {
            Log.d(TAG, String.format("  %04X:%04X - %s (%s) - %s", 
                device.vid, device.pid, device.name, device.type, device.description));
        }
    }
    
    private void onSerialDataRead(byte[] data) {
        // Handle serial data read events
        Log.d(TAG, "Serial data read: " + data.length + " bytes");
    }
    
    private void updateConnectionStatus() {
        // Update individual connection status
        serialConnected = serialManager.isConnected();
        // uvcConnected and hidConnected are updated by their respective listeners
        
        ConnectionStatus status = getConnectionStatus();
        Log.d(TAG, "Connection status: " + status + 
            " (Serial: " + serialConnected + 
            ", UVC: " + uvcConnected + 
            ", HID: " + hidConnected + ")");
        
        if (deviceListener != null) {
            deviceListener.onConnectionStatusChanged(status);
        }
    }
    
    public ConnectionStatus getConnectionStatus() {
        int connectedCount = 0;
        if (serialConnected) connectedCount++;
        if (uvcConnected) connectedCount++;
        if (hidConnected) connectedCount++;
        
        switch (connectedCount) {
            case 0:
                return ConnectionStatus.DISCONNECTED;
            case 1:
                if (serialConnected) return ConnectionStatus.SERIAL_ONLY;
                if (uvcConnected) return ConnectionStatus.UVC_ONLY;
                if (hidConnected) return ConnectionStatus.HID_ONLY;
                break;
            case 2:
                return ConnectionStatus.PARTIAL_CONNECTED;
            case 3:
                return ConnectionStatus.FULLY_CONNECTED;
        }
        return ConnectionStatus.DISCONNECTED;
    }
    
    // Public methods to access individual managers
    
    /**
     * Get the serial device manager
     * @return UsbDeviceManager instance
     */
    public UsbDeviceManager getSerialManager() {
        return serialManager;
    }
    
    /**
     * Get the UVC device manager
     * @return OpenterfaceUVCManager instance
     */
    public OpenterfaceUVCManager getUVCManager() {
        return uvcManager;
    }
    
    /**
     * Get the HID device manager
     * @return OpenterfaceHIDManager instance
     */
    public OpenterfaceHIDManager getHIDManager() {
        return hidManager;
    }
    
    // Convenience methods for common operations
    
    /**
     * Check if serial interface is connected
     * @return true if serial is connected
     */
    public boolean isSerialConnected() {
        return serialConnected;
    }
    
    /**
     * Check if UVC interface is connected
     * @return true if UVC is connected
     */
    public boolean isUVCConnected() {
        return uvcConnected;
    }
    
    /**
     * Check if HID interface is connected
     * @return true if HID is connected
     */
    public boolean isHIDConnected() {
        return hidConnected;
    }
    
    /**
     * Check if all interfaces are connected
     * @return true if fully connected
     */
    public boolean isFullyConnected() {
        return getConnectionStatus() == ConnectionStatus.FULLY_CONNECTED;
    }
    
    /**
     * Get current serial baudrate
     * @return baudrate or -1 if not connected
     */
    public int getCurrentSerialBaudrate() {
        return serialManager.getCurrentBaudrate();
    }
    
    /**
     * Get UVC control instance
     * @return UVCControl instance if UVC is connected, null otherwise
     */
    public UVCControl getUVCControl() {
        return uvcManager.getUVCControl();
    }
    
    /**
     * Find Openterface device by VID/PID
     * @param vid Vendor ID
     * @param pid Product ID
     * @return OpenterfaceDevice if found, null otherwise
     */
    public OpenterfaceDevice findDevice(int vid, int pid) {
        for (OpenterfaceDevice device : OPENTERFACE_DEVICES) {
            if (device.vid == vid && device.pid == pid) {
                return device;
            }
        }
        return null;
    }
    
    /**
     * Get device name by VID/PID
     * @param vid Vendor ID
     * @param pid Product ID
     * @return device name or "Unknown Openterface Device"
     */
    public String getDeviceName(int vid, int pid) {
        OpenterfaceDevice device = findDevice(vid, pid);
        return device != null ? device.name : "Unknown Openterface Device";
    }
    
    /**
     * Release all resources and cleanup
     */
    public void release() {
        Log.d(TAG, "Releasing Openterface Device Manager");
        
        if (serialManager != null) {
            serialManager.release();
        }
        if (uvcManager != null) {
            uvcManager.release();
        }
        if (hidManager != null) {
            hidManager.release();
        }
        
        serialConnected = false;
        uvcConnected = false;
        hidConnected = false;
        
        Log.d(TAG, "Openterface Device Manager released");
    }
}