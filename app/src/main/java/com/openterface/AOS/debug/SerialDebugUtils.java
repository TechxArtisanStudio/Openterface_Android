/**
 * @Title: SerialDebugUtils
 * @Package com.openterface.AOS.debug
 * @Description: Debugging utilities for serial communication issues
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
package com.openterface.AOS.debug;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.openterface.AOS.serial.UsbDeviceManager;

import java.util.HashMap;
import java.util.List;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;

public class SerialDebugUtils {
    private static final String TAG = "SerialDebug";
    
    // Common USB issues and solutions
    private static final String[] COMMON_ISSUES = {
        "1. USB Permission not granted",
        "2. Incorrect baudrate (try 9600 or 115200)",
        "3. Device not properly connected",
        "4. USB driver not loaded",
        "5. Hardware flow control issues (DTR/RTS)",
        "6. Device in use by another app",
        "7. USB hub power issues",
        "8. Cable/connection problems"
    };
    
    /**
     * Run comprehensive diagnostics on serial connection
     * @param context application context
     * @param serialManager the UsbDeviceManager instance
     */
    public static void runDiagnostics(Context context, UsbDeviceManager serialManager) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "    OPENTERFACE SERIAL DIAGNOSTICS");
        Log.d(TAG, "========================================");
        
        // Check USB manager
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.e(TAG, "❌ USB Manager is null - critical error");
            return;
        }
        Log.d(TAG, "✅ USB Manager available");
        
        // List all USB devices
        listAllUsbDevices(usbManager);
        
        // Check for Openterface devices specifically
        checkOpenterfaceDevices(usbManager);
        
        // Check serial manager status
        checkSerialManagerStatus(serialManager);
        
        // Check permissions
        checkUsbPermissions(usbManager);
        
        // Print troubleshooting tips
        printTroubleshootingTips();
        
        Log.d(TAG, "========================================");
        Log.d(TAG, "    DIAGNOSTICS COMPLETE");
        Log.d(TAG, "========================================");
    }
    
    /**
     * List all connected USB devices
     */
    private static void listAllUsbDevices(UsbManager usbManager) {
        Log.d(TAG, "\n--- USB DEVICES DETECTED ---");
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        
        if (deviceList.isEmpty()) {
            Log.w(TAG, "⚠️  No USB devices detected");
            return;
        }
        
        Log.d(TAG, "Found " + deviceList.size() + " USB device(s):");
        
        for (UsbDevice device : deviceList.values()) {
            // Safely get device info without requiring permissions
            String productName = "Unknown";
            String manufacturerName = "Unknown";
            String serialNumber = "Permission required";
            
            try {
                productName = device.getProductName() != null ? device.getProductName() : "Unknown";
                manufacturerName = device.getManufacturerName() != null ? device.getManufacturerName() : "Unknown";
                
                // Only try to get serial number if we have permission
                if (usbManager.hasPermission(device)) {
                    serialNumber = device.getSerialNumber() != null ? device.getSerialNumber() : "None";
                } else {
                    serialNumber = "Permission required";
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Permission denied for device " + device.getDeviceName() + ": " + e.getMessage());
                serialNumber = "Permission denied";
            }
            
            String info = String.format(
                "Device: %s\n" +
                "  VID:PID: %04X:%04X\n" +
                "  Product: %s\n" +
                "  Manufacturer: %s\n" +
                "  Serial: %s\n" +
                "  Class: %d, Subclass: %d, Protocol: %d\n" +
                "  Interfaces: %d\n" +
                "  Has Permission: %s",
                device.getDeviceName(),
                device.getVendorId(), device.getProductId(),
                productName,
                manufacturerName,
                serialNumber,
                device.getDeviceClass(), device.getDeviceSubclass(), device.getDeviceProtocol(),
                device.getInterfaceCount(),
                usbManager.hasPermission(device) ? "YES" : "NO"
            );
            Log.d(TAG, info);
            
            // Check if this looks like an Openterface device
            if (isOpenterfaceDevice(device)) {
                Log.d(TAG, "🎯 This appears to be an Openterface device!");
                if (!usbManager.hasPermission(device)) {
                    Log.w(TAG, "⚠️ Openterface device found but no permission to access it!");
                }
            }
        }
    }
    
    /**
     * Check specifically for Openterface devices
     */
    private static void checkOpenterfaceDevices(UsbManager usbManager) {
        Log.d(TAG, "\n--- OPENTERFACE DEVICE CHECK ---");
        
        // Check for known Openterface VID/PIDs
        int[] openterfaceVids = {0x1A86, 0x534D, 0x345F};
        int[] openterfacePids = {0x7523, 0xFE0C, 0x2109, 0x2132};
        
        boolean foundOpenterfaceDevice = false;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        
        for (UsbDevice device : deviceList.values()) {
            if (isOpenterfaceDevice(device)) {
                foundOpenterfaceDevice = true;
                Log.d(TAG, "✅ Found Openterface device: " + 
                    String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                
                // Check permissions for this device
                boolean hasPermission = usbManager.hasPermission(device);
                Log.d(TAG, "   Permission: " + (hasPermission ? "✅ Granted" : "❌ Not granted"));
                
                // Check if it's a serial interface
                if (device.getVendorId() == 0x1A86) {
                    Log.d(TAG, "   Type: Serial interface (CH340/CH341)");
                } else {
                    Log.d(TAG, "   Type: UVC/HID interface");
                }
            }
        }
        
        if (!foundOpenterfaceDevice) {
            Log.w(TAG, "❌ No Openterface devices found");
            Log.w(TAG, "   Make sure device is connected and powered");
        }
    }
    
    /**
     * Check if device appears to be an Openterface device
     */
    private static boolean isOpenterfaceDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        // Known Openterface VID/PID combinations
        return (vid == 0x1A86 && (pid == 0x7523 || pid == 0xFE0C)) ||  // Serial
               (vid == 0x534D && pid == 0x2109) ||  // UVC/HID v1
               (vid == 0x345F && pid == 0x2132);    // UVC/HID v2
    }
    
    /**
     * Check serial manager status
     */
    private static void checkSerialManagerStatus(UsbDeviceManager serialManager) {
        Log.d(TAG, "\n--- SERIAL MANAGER STATUS ---");
        
        if (serialManager == null) {
            Log.e(TAG, "❌ Serial manager is null");
            return;
        }
        
        boolean isConnected = serialManager.isConnected();
        Log.d(TAG, "Connected: " + (isConnected ? "✅ Yes" : "❌ No"));
        
        if (isConnected) {
            int baudrate = serialManager.getCurrentBaudrate();
            Log.d(TAG, "Baudrate: " + baudrate);
            
            // Print detailed connection info
            String connectionInfo = serialManager.getConnectionInfo();
            Log.d(TAG, "Connection Details:\n" + connectionInfo);
        } else {
            Log.w(TAG, "Serial port is not connected");
            Log.d(TAG, "Preferred Baudrate: " + 
                (serialManager.getPreferredBaudrate() == -1 ? "Auto" : serialManager.getPreferredBaudrate()));
        }
    }
    
    /**
     * Check USB permissions for Openterface devices
     */
    private static void checkUsbPermissions(UsbManager usbManager) {
        Log.d(TAG, "\n--- USB PERMISSIONS ---");
        
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        boolean foundPermissionIssues = false;
        
        for (UsbDevice device : deviceList.values()) {
            if (isOpenterfaceDevice(device)) {
                boolean hasPermission = usbManager.hasPermission(device);
                if (!hasPermission) {
                    foundPermissionIssues = true;
                    Log.w(TAG, "❌ No permission for device: " + 
                        String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                } else {
                    Log.d(TAG, "✅ Permission granted for device: " + 
                        String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                }
            }
        }
        
        if (foundPermissionIssues) {
            Log.w(TAG, "⚠️  Permission issues detected - this may prevent communication");
        }
    }
    
    /**
     * Print troubleshooting tips
     */
    private static void printTroubleshootingTips() {
        Log.d(TAG, "\n--- TROUBLESHOOTING TIPS ---");
        Log.d(TAG, "If serial communication is not working, try:");
        
        for (String tip : COMMON_ISSUES) {
            Log.d(TAG, tip);
        }
        
        Log.d(TAG, "\nDEBUG COMMANDS TO TRY:");
        Log.d(TAG, "// Test communication");
        Log.d(TAG, "serialManager.testCommunication();");
        Log.d(TAG, "");
        Log.d(TAG, "// Try different baudrate");
        Log.d(TAG, "serialManager.reconnectWithBaudrate(9600);");
        Log.d(TAG, "// or");
        Log.d(TAG, "serialManager.reconnectWithBaudrate(115200);");
        Log.d(TAG, "");
        Log.d(TAG, "// Send test data");
        Log.d(TAG, "byte[] testData = {0x01, 0x02, 0x03};");
        Log.d(TAG, "boolean success = serialManager.writeData(testData);");
        Log.d(TAG, "");
        Log.d(TAG, "// Enable detailed logging");
        Log.d(TAG, "serialManager.enableDebugLogging();");
    }
    
    /**
     * Test basic serial communication with common patterns
     */
    public static void testSerialCommunication(UsbDeviceManager serialManager) {
        Log.d(TAG, "\n--- SERIAL COMMUNICATION TEST ---");
        
        if (!serialManager.isConnected()) {
            Log.e(TAG, "❌ Cannot test: Serial port not connected");
            return;
        }
        
        // Test 1: Send single byte
        Log.d(TAG, "Test 1: Sending single byte (0xAA)");
        byte[] singleByte = {(byte) 0xAA};
        boolean test1 = serialManager.sendCommand(singleByte, "Single byte test");
        
        // Small delay between tests
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Test 2: Send ASCII string
        Log.d(TAG, "Test 2: Sending ASCII string");
        String testString = "HELLO";
        boolean test2 = serialManager.sendCommand(testString.getBytes(), "ASCII string test");
        
        // Small delay between tests
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Test 3: Send byte sequence
        Log.d(TAG, "Test 3: Sending byte sequence");
        byte[] sequence = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        boolean test3 = serialManager.sendCommand(sequence, "Byte sequence test");
        
        // Small delay between tests
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Test 4: Test flow control signals
        Log.d(TAG, "Test 4: Testing flow control signals");
        boolean dtrTest = serialManager.setDTR(true);
        boolean rtsTest = serialManager.setRTS(true);
        
        Log.d(TAG, "DTR set: " + (dtrTest ? "✅" : "❌"));
        Log.d(TAG, "RTS set: " + (rtsTest ? "✅" : "❌"));
        
        // Summary
        int passedTests = 0;
        if (test1) passedTests++;
        if (test2) passedTests++;
        if (test3) passedTests++;
        if (dtrTest && rtsTest) passedTests++;
        
        Log.d(TAG, "=== TEST RESULTS ===");
        Log.d(TAG, "Passed: " + passedTests + "/4 tests");
        
        if (passedTests == 4) {
            Log.d(TAG, "🎉 All tests passed - Serial communication appears to be working!");
        } else if (passedTests > 0) {
            Log.w(TAG, "⚠️  Some tests passed - Partial communication working");
        } else {
            Log.e(TAG, "❌ All tests failed - Serial communication not working");
            Log.e(TAG, "Check connection, baudrate, and device compatibility");
        }
    }
    
    /**
     * Recommend optimal settings based on device detection
     */
    public static void recommendSettings(Context context) {
        Log.d(TAG, "\n--- RECOMMENDED SETTINGS ---");
        
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        
        for (UsbDevice device : deviceList.values()) {
            if (isOpenterfaceDevice(device)) {
                int vid = device.getVendorId();
                int pid = device.getProductId();
                
                Log.d(TAG, "Device: " + String.format("%04X:%04X", vid, pid));
                
                if (vid == 0x1A86) {
                    Log.d(TAG, "Recommended Settings:");
                    Log.d(TAG, "  - Baudrate: Try 115200 first, fallback to 9600");
                    Log.d(TAG, "  - Data bits: 8");
                    Log.d(TAG, "  - Stop bits: 1");
                    Log.d(TAG, "  - Parity: None");
                    Log.d(TAG, "  - Flow control: None initially, try RTS/CTS if issues");
                    Log.d(TAG, "  - Driver: CH34x (already configured)");
                    
                    if (pid == 0x7523) {
                        Log.d(TAG, "  - Device: Openterface v1 Serial");
                    } else if (pid == 0xFE0C) {
                        Log.d(TAG, "  - Device: Openterface v2 Serial");
                    }
                }
            }
        }
    }
}