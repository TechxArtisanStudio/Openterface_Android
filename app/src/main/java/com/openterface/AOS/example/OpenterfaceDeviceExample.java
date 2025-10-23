/**
 * @Title: OpenterfaceDeviceExample
 * @Package com.openterface.AOS.example
 * @Description: Example usage of Openterface device managers
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
package com.openterface.AOS.example;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.openterface.AOS.device.OpenterfaceDeviceManager;

public class OpenterfaceDeviceExample extends Activity {
    private static final String TAG = "OpenterfaceExample";
    
    private OpenterfaceDeviceManager deviceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize the unified Openterface device manager
        deviceManager = new OpenterfaceDeviceManager(this);
        
        // Set up event listener for device connection events
        deviceManager.setOnOpenterfaceDeviceListener(new OpenterfaceDeviceManager.OnOpenterfaceDeviceListener() {
            @Override
            public void onSerialConnected() {
                Log.d(TAG, "✅ Openterface serial connected");
                showToast("Serial connected at " + deviceManager.getCurrentSerialBaudrate() + " baud");
                checkFullConnection();
            }
            
            @Override
            public void onSerialDisconnected() {
                Log.d(TAG, "❌ Openterface serial disconnected");
                showToast("Serial disconnected");
            }
            
            @Override
            public void onUVCConnected(UsbDevice device) {
                String deviceName = deviceManager.getDeviceName(device.getVendorId(), device.getProductId());
                Log.d(TAG, "📹 Openterface UVC connected: " + deviceName + 
                    " [" + String.format("%04X:%04X", device.getVendorId(), device.getProductId()) + "]");
                showToast("UVC Camera connected: " + deviceName);
                checkFullConnection();
            }
            
            @Override
            public void onUVCDisconnected(UsbDevice device) {
                Log.d(TAG, "❌ Openterface UVC disconnected");
                showToast("UVC Camera disconnected");
            }
            
            @Override
            public void onHIDConnected(UsbDevice device) {
                String deviceName = deviceManager.getDeviceName(device.getVendorId(), device.getProductId());
                Log.d(TAG, "🖱️ Openterface HID connected: " + deviceName + 
                    " [" + String.format("%04X:%04X", device.getVendorId(), device.getProductId()) + "]");
                showToast("HID Interface connected: " + deviceName);
                checkFullConnection();
            }
            
            @Override
            public void onHIDDisconnected(UsbDevice device) {
                Log.d(TAG, "❌ Openterface HID disconnected");
                showToast("HID Interface disconnected");
            }
            
            @Override
            public void onConnectionStatusChanged(OpenterfaceDeviceManager.ConnectionStatus status) {
                Log.d(TAG, "🔄 Connection status changed to: " + status);
                
                switch (status) {
                    case DISCONNECTED:
                        showToast("All Openterface interfaces disconnected");
                        break;
                    case SERIAL_ONLY:
                        showToast("Only serial interface connected");
                        break;
                    case UVC_ONLY:
                        showToast("Only UVC camera connected");
                        break;
                    case HID_ONLY:
                        showToast("Only HID interface connected");
                        break;
                    case PARTIAL_CONNECTED:
                        showToast("Some Openterface interfaces connected");
                        break;
                    case FULLY_CONNECTED:
                        showToast("🎉 All Openterface interfaces connected!");
                        break;
                }
            }
        });
        
        // Initialize all device managers
        deviceManager.initialize();
        
        // Log current status
        logCurrentStatus();
    }
    
    private void checkFullConnection() {
        if (deviceManager.isFullyConnected()) {
            Log.d(TAG, "🎉 All Openterface interfaces are now connected!");
            showToast("Ready to use! All interfaces connected.");
        }
    }
    
    private void logCurrentStatus() {
        Log.d(TAG, "=== Openterface Device Status ===");
        Log.d(TAG, "Serial Connected: " + deviceManager.isSerialConnected());
        Log.d(TAG, "UVC Connected: " + deviceManager.isUVCConnected());
        Log.d(TAG, "HID Connected: " + deviceManager.isHIDConnected());
        Log.d(TAG, "Overall Status: " + deviceManager.getConnectionStatus());
        
        if (deviceManager.isSerialConnected()) {
            Log.d(TAG, "Current Baudrate: " + deviceManager.getCurrentSerialBaudrate());
        }
        
        Log.d(TAG, "================================");
    }
    
    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    protected void onDestroy() {
        if (deviceManager != null) {
            deviceManager.release();
        }
        super.onDestroy();
    }
    
    // Example methods for using individual managers
    
    /**
     * Example of accessing the serial manager directly
     */
    public void sendSerialData(byte[] data) {
        if (deviceManager.isSerialConnected()) {
            // Access the serial manager and send data
            // deviceManager.getSerialManager().sendData(data);
            Log.d(TAG, "Would send " + data.length + " bytes via serial");
        } else {
            Log.w(TAG, "Serial not connected, cannot send data");
        }
    }
    
    /**
     * Example of accessing the UVC manager directly
     */
    public void controlUVCCamera() {
        if (deviceManager.isUVCConnected()) {
            // Access the UVC manager for camera control
            // UVCControl control = deviceManager.getUVCControl();
            // if (control != null) {
            //     // Control camera parameters
            // }
            Log.d(TAG, "Would control UVC camera");
        } else {
            Log.w(TAG, "UVC not connected, cannot control camera");
        }
    }
    
    /**
     * Example of accessing the HID manager directly
     */
    public void sendHIDData(byte[] hidData) {
        if (deviceManager.isHIDConnected()) {
            // Access the HID manager and send HID data
            // deviceManager.getHIDManager().sendHIDData(hidData);
            Log.d(TAG, "Would send HID data: " + hidData.length + " bytes");
        } else {
            Log.w(TAG, "HID not connected, cannot send HID data");
        }
    }
}