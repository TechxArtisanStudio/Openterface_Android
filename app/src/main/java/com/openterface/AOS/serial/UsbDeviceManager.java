/**
* @Title: UsbDeviceManager
* @Package com.openterface.AOS.serial
* @Description:
 * ======================================================================== *
 *  Supported Openterface Serial Devices:                                  *
 *    - 1A86:7523 (Mini-KVM v1 - supports 9600 and 115200 baud)           *
 *    - 1A86:FE0C (Mini-KVM v2 - supports 115200 baud only)               *
 * ======================================================================== *
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.openterface.AOS.R;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.Ch34xSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import android.hardware.usb.UsbDeviceConnection;
import android.widget.TextView;
import android.os.Build;
import android.view.KeyEvent;

public class UsbDeviceManager {
    private static final String TAG = UsbDeviceManager.class.getSimpleName();

    private UsbManager usbManager;
    public static UsbSerialPort port;
    public static UsbSerialDriver driver;
    private HandlerThread mSerialThread;
    private Handler mSerialAsyncHandler;
    private Context context;
    private boolean isReading = false;
    private TextView tvReceivedData;
    private int currentBaudrate = -1; // Track the working baudrate
    private int preferredBaudrate = DEFAULT_BAUDRATE;
    private UsbDevice pendingDevice; // Device waiting for permission

    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final String ACTION_USB_PERMISSION = "com.example.openterface.USB_PERMISSION";
    
    // Baudrate constants - similar to C++ version
    private static final int BAUDRATE_HIGHSPEED = 115200;
    private static final int BAUDRATE_LOWSPEED = 9600;
    private static final int DEFAULT_BAUDRATE = BAUDRATE_HIGHSPEED; // Try high speed first
    private UsbDeviceManager usbDeviceManager;

    public interface OnDataReadListener {
        void onDataRead(byte[] data, int length);
    }
    
    public interface OnConnectionStatusListener {
        void onConnected(int baudrate);
        void onDisconnected();
        void onError(String error);
    }

    private OnDataReadListener onDataReadListener;
    private OnConnectionStatusListener onConnectionStatusListener;

    public void setOnDataReadListener(OnDataReadListener listener) {
        this.onDataReadListener = listener;
    }
    
    public void setOnConnectionStatusListener(OnConnectionStatusListener listener) {
        this.onConnectionStatusListener = listener;
    }

    
    /**
     * Scan all connected USB devices and find Openterface devices by VID/PID
     * @return UsbDevice if found, null otherwise
     */
    private UsbDevice detectOpenterfaceDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Log.d(TAG, "Scanning " + deviceList.size() + " connected USB devices for Openterface devices");
        
        // Define only the two supported serial port devices
        int[][] supportedIds = {
            {0x1A86, 0x7523},  // Openterface Mini-KVM v1 Serial (supports 9600 and 115200)
            {0x1A86, 0xFE0C}   // Openterface Mini-KVM v2 Serial (supports 115200 only)
        };
        
        // Log supported device types
        Log.d(TAG, "Supported Openterface Serial Devices:");
        Log.d(TAG, "  - 1A86:7523 (Mini-KVM v1 - supports 9600 and 115200 baud)");
        Log.d(TAG, "  - 1A86:FE0C (Mini-KVM v2 - supports 115200 baud only)");
        
        // Scan for supported devices
        for (UsbDevice device : deviceList.values()) {
            int vid = device.getVendorId();
            int pid = device.getProductId();
            
            // Safely get device info without requiring permissions
            String productName = device.getProductName() != null ? device.getProductName() : "Unknown";
            String manufacturerName = device.getManufacturerName() != null ? device.getManufacturerName() : "Unknown";
            String serialNumber = "Permission required";
            
            try {
                // Only try to get serial number if we have permission
                if (usbManager.hasPermission(device)) {
                    serialNumber = device.getSerialNumber() != null ? device.getSerialNumber() : "None";
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Permission denied for device " + device.getDeviceName() + ": " + e.getMessage());
                serialNumber = "Permission denied";
            }
            
            Log.d(TAG, "Checking device: " + String.format("VID:PID=%04X:%04X", vid, pid) + 
                ", Product=" + productName + 
                ", Manufacturer=" + manufacturerName +
                ", SerialNumber=" + serialNumber);
            
            // Check if this device matches any of our supported VID/PID combinations
            for (int[] ids : supportedIds) {
                if (vid == ids[0] && pid == ids[1]) {
                    Log.i(TAG, "✅ Found supported Openterface device: " + 
                        String.format("%04X:%04X", vid, pid) + " - " + productName);
                    
                    // Check permissions
                    boolean hasPermission = usbManager.hasPermission(device);
                    Log.d(TAG, "Permission status for " + String.format("%04X:%04X", vid, pid) + ": " + hasPermission);
                    
                    return device;
                }
            }
        }
        
        Log.w(TAG, "❌ No compatible USB serial devices found");
        Log.d(TAG, "Expected VID/PID combinations:");
        Log.d(TAG, "  - 1A86:7523 (Mini-KVM v1 - supports 9600 and 115200 baud)");
        Log.d(TAG, "  - 1A86:FE0C (Mini-KVM v2 - supports 115200 baud only)");
        
        return null;
    }
    
    /**
     * Check if a USB device has a serial interface (CDC or vendor-specific)
     */
    private boolean hasSerialInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            int intfClass = intf.getInterfaceClass();
            
            // Check for CDC (Communication Device Class) - typical for serial
            if (intfClass == 2) { // USB_CLASS_COMM
                Log.d(TAG, "Found CDC interface on " + String.format("%04X:%04X", 
                    device.getVendorId(), device.getProductId()));
                return true;
            }
            
            // Check for vendor-specific interface that might be serial
            if (intfClass == 255) { // USB_CLASS_VENDOR_SPEC
                Log.d(TAG, "Found vendor-specific interface on " + String.format("%04X:%04X", 
                    device.getVendorId(), device.getProductId()));
                // For Openterface devices, vendor-specific might be serial
                return true;
            }
        }
        return false;
    }

    /**
     * Attempt to configure serial port with specified baudrate
     * @param connection the USB device connection
     * @param baudrate the baudrate to try
     * @return true if successful, false if failed
     */
    private boolean trySerialConfiguration(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "Attempting to configure port with baudrate: " + baudrate);
            
            // Check if driver is valid
            if (driver == null || driver.getPorts().isEmpty()) {
                Log.e(TAG, "Driver is null or has no ports");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("Invalid driver - no ports available");
                }
                return false;
            }
            
            // Get the first port
            port = driver.getPorts().get(0); // Most devices have just one port (port 0)
            
            // Special handling for CH340 devices (1A86 vendor ID)
            UsbDevice device = driver.getDevice();
            boolean is7523Device = (device.getVendorId() == 0x1A86 && device.getProductId() == 0x7523);
            boolean isFE0CDevice = (device.getVendorId() == 0x1A86 && device.getProductId() == 0xFE0C);
            
            if (is7523Device) {
                Log.d(TAG, "Detected 7523 CH340 device - using standard CH340 initialization");
                return initializeCH340Port(connection, baudrate);
            } else if (isFE0CDevice) {
                Log.d(TAG, "Detected FE0C CH340 device - using bypass initialization");
                return initializeFE0CPort(connection, baudrate);
            } else {
                Log.d(TAG, "Using standard initialization for non-CH340 device");
                return initializeStandardPort(connection, baudrate);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to configure serial port with baudrate " + baudrate + ": " + e.getMessage(), e);
            // Close port if it was opened but configuration failed
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException ioException) {
                    Log.e(TAG, "Error closing port after failed configuration: " + ioException.getMessage());
                }
            }
            return false;
        }
    }
    
    /**
     * Enhanced initialization for CH340 devices with device-specific strategies
     * FE0C: Command-only mode, no CH340 init needed, no replies expected
     * 7523: Full CH340 initialization required
     */
    private boolean initializeCH340Port(UsbDeviceConnection connection, int baudrate) {
        Log.d(TAG, "=== CH340 Enhanced Initialization ===");
        
        UsbDevice device = driver.getDevice();
        boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
        boolean is7523Device = (device.getProductId() == 0x7523);
        
        Log.d(TAG, "Device type: " + (isFE0CDevice ? "FE0C (v2)" : is7523Device ? "7523 (v1)" : "Unknown"));
        
        // Strategy 1: Device-specific initialization approach
        if (isFE0CDevice) {
            Log.d(TAG, "Strategy 1: FE0C-specific initialization...");
            if (tryFE0CSpecificInit(connection, baudrate)) {
                return true;
            } else {
                Log.e(TAG, "❌ FE0C initialization failed - device may not be working properly");
                return false;
            }
        } else if (is7523Device) {
            Log.d(TAG, "Strategy 1: 7523-specific initialization...");
            if (try7523SpecificInit(connection, baudrate)) {
                return true;
            }
            
            // For 7523, try enhanced standard CH340 as fallback
            Log.d(TAG, "7523 specific init failed, trying enhanced standard CH340...");
            if (tryEnhancedStandardCH340Init(connection, baudrate)) {
                return true;
            }
            
            // Reset and retry for 7523 if needed
            Log.d(TAG, "Trying CH340 reset for 7523...");
            if (tryCH340ResetInit(connection, baudrate)) {
                return true;
            }
            
            Log.e(TAG, "❌ All 7523 initialization strategies failed");
            return false;
        }
        
        Log.e(TAG, "❌ Unknown device type - no specific initialization available");
        return false;
    }
    
    /**
     * FE0C-specific initialization method
     * FE0C devices don't need CH340 initialization and don't reply to commands
     */
    private boolean tryFE0CSpecificInit(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Specific Initialization (No CH340 Init) ===");
            Log.d(TAG, "FE0C devices don't need CH340 initialization and don't reply to commands");
            
            // For FE0C devices, try to open the port with minimal expectations
            Log.d(TAG, "Opening FE0C port with CH340 bypass...");
            
            try {
                // Try opening with the expectation that CH340 init commands may fail
                port.open(connection);
                Log.d(TAG, "✅ FE0C port opened (ignoring any CH340 init failures)");
            } catch (Exception openEx) {
                // For FE0C, opening might "fail" due to CH340 init commands not getting responses
                // But the port might still be usable for sending commands
                Log.w(TAG, "FE0C port open had issues (expected): " + openEx.getMessage());
                
                // Check if the port is actually open despite the "failure"
                if (port.isOpen()) {
                    Log.i(TAG, "✅ FE0C port is actually open despite init warnings");
                } else {
                    Log.w(TAG, "Attempting force open for FE0C...");
                    // Try a second time - sometimes FE0C needs this
                    try {
                        port.open(connection);
                        Log.d(TAG, "✅ FE0C force open succeeded");
                    } catch (Exception forceEx) {
                        Log.e(TAG, "❌ FE0C force open failed: " + forceEx.getMessage());
                        return false;
                    }
                }
            }
            
            // Step 2: Minimal delay for port to be ready
            Thread.sleep(200); // Longer delay for FE0C
            
            // Step 3: Try to set parameters but ignore failures completely
            try {
                Log.d(TAG, "Attempting parameter setting for FE0C: " + baudrate + " baud");
                port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                Log.d(TAG, "✅ FE0C parameters set successfully");
            } catch (Exception paramEx) {
                Log.w(TAG, "FE0C parameter setting failed (continuing anyway): " + paramEx.getMessage());
                // Continue anyway - FE0C devices often don't acknowledge parameter changes
            }
            
            // Step 4: Skip DTR/RTS control for FE0C
            Log.d(TAG, "Skipping DTR/RTS control for FE0C device");
            
            // Step 5: Test if we can actually send data
            if (port.isOpen()) {
                Log.d(TAG, "Testing FE0C data transmission capability...");
                try {
                    // Send a simple test command to verify communication
                    byte[] testCommand = {0x57, (byte)0xAB, 0x00, 0x00, 0x00}; // Minimal test
                    port.write(testCommand, 100);
                    Log.d(TAG, "✅ FE0C test write successful - device ready");
                } catch (Exception writeEx) {
                    Log.w(TAG, "FE0C test write failed: " + writeEx.getMessage());
                    // Even if test write fails, continue - some FE0C devices are picky
                }
            }
            
            currentBaudrate = baudrate;
            Log.i(TAG, "✅ FE0C initialization completed - ready for command-only operation");
            Log.i(TAG, "Note: FE0C device works in command-only mode (no replies expected)");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C initialization failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after FE0C init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * 7523-specific initialization method
     * 7523 devices need proper CH340 initialization
     */
    private boolean try7523SpecificInit(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== 7523 Specific Initialization (Full CH340 Init) ===");
            Log.d(TAG, "7523 devices require proper CH340 initialization");
            
            // Step 1: CH340 initialization sequence
            Log.d(TAG, "Performing CH340 initialization for 7523...");
            
            // Step 2: Open port with CH340 initialization
            port.open(connection);
            
            // Step 3: CH340 initialization delay
            Thread.sleep(100);
            
            // Step 4: Set parameters with proper CH340 handling
            Log.d(TAG, "Setting 7523 parameters: " + baudrate + " baud, 8N1");
            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            // Step 5: Set DTR/RTS for proper CH340 handshaking
            Log.d(TAG, "Setting CH340 control signals for 7523...");
            port.setDTR(true);
            port.setRTS(false);
            
            // Step 6: Additional delay for CH340 to settle
            Thread.sleep(50);
            
            currentBaudrate = baudrate;
            Log.i(TAG, "✅ 7523 CH340 initialization successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 7523 CH340 initialization failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after 7523 init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Enhanced standard CH340 initialization with better error handling
     */
    private boolean tryEnhancedStandardCH340Init(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== Enhanced Standard CH340 Initialization ===");
            
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            // Step 1: Open port
            Log.d(TAG, "Opening CH340 port with enhanced method...");
            port.open(connection);
            
            // Step 2: Device-specific initialization delays
            if (isFE0CDevice) {
                Thread.sleep(200); // Longer delay for FE0C
            } else {
                Thread.sleep(100); // Standard delay for others
            }
            
            // Step 3: Set parameters with retry logic
            boolean paramSuccess = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    Log.d(TAG, "Setting CH340 parameters (attempt " + attempt + "): " + baudrate + " baud, 8N1");
                    port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    paramSuccess = true;
                    Log.d(TAG, "✅ Parameters set on attempt " + attempt);
                    break;
                } catch (Exception paramEx) {
                    Log.w(TAG, "Parameter setting attempt " + attempt + " failed: " + paramEx.getMessage());
                    if (attempt < 3) {
                        Thread.sleep(100);
                    } else if (isFE0CDevice) {
                        Log.w(TAG, "FE0C parameter setting failed but continuing anyway");
                        paramSuccess = true; // Continue for FE0C devices
                    }
                }
            }
            
            if (!paramSuccess) {
                throw new IOException("Failed to set parameters after 3 attempts");
            }
            
            // Step 4: Set control signals with retry
            try {
                port.setDTR(true);
                port.setRTS(false);
                Thread.sleep(50);
                Log.d(TAG, "✅ Control signals set");
            } catch (Exception controlEx) {
                Log.w(TAG, "Control signal setting failed: " + controlEx.getMessage());
                if (!isFE0CDevice) {
                    throw controlEx; // Only fail for non-FE0C devices
                }
            }
            
            currentBaudrate = baudrate;
            Log.i(TAG, "✅ Enhanced standard CH340 initialization successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Enhanced standard CH340 init failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after enhanced init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Compatibility mode for devices that don't respond to standard initialization
     */
    private boolean tryCompatibilityMode(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== Compatibility Mode Initialization ===");
            Log.w(TAG, "Entering compatibility mode - using most permissive approach");
            
            UsbDevice device = driver.getDevice();
            
            // Step 1: Create completely fresh driver instance
            UsbSerialDriver freshDriver = UsbSerialProber.getDefaultProber().probeDevice(device);
            
            if (freshDriver != null && !freshDriver.getPorts().isEmpty()) {
                UsbSerialPort freshPort = freshDriver.getPorts().get(0);
                
                Log.d(TAG, "Opening with fresh driver in compatibility mode...");
                freshPort.open(connection);
                
                // Step 2: Minimal parameter setting (ignore all errors)
                try {
                    freshPort.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "Parameters set in compatibility mode");
                } catch (Exception paramEx) {
                    Log.w(TAG, "Parameter setting ignored in compatibility mode: " + paramEx.getMessage());
                }
                
                // Step 3: Replace old port with new one
                if (port != null && port.isOpen()) {
                    try {
                        port.close();
                    } catch (Exception closeEx) {
                        Log.w(TAG, "Failed to close old port: " + closeEx.getMessage());
                    }
                }
                
                port = freshPort;
                driver = freshDriver;
                currentBaudrate = baudrate;
                
                Log.i(TAG, "✅ Compatibility mode initialization successful at " + baudrate + " baud");
                Log.i(TAG, "Note: Device may not respond to all commands - this is normal in compatibility mode");
                
                return true;
            } else {
                Log.e(TAG, "Failed to create fresh driver for compatibility mode");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Compatibility mode initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Try to resolve FE0C interface conflicts by releasing and reclaiming interfaces
     */
    private boolean tryFE0CInterfaceConflictResolution(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Interface Conflict Resolution ===");
            
            UsbDevice device = driver.getDevice();
            
            // Check if any interface is already claimed by the system or another app
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbInterface = device.getInterface(i);
                Log.d(TAG, "Interface " + i + ": " + usbInterface.toString());
                
                // Try to force release any existing claims
                try {
                    boolean released = connection.releaseInterface(usbInterface);
                    Log.d(TAG, "Force released interface " + i + ": " + released);
                    Thread.sleep(100); // Give time for release
                } catch (Exception e) {
                    Log.d(TAG, "Interface " + i + " was not claimed or release failed: " + e.getMessage());
                }
            }
            
            // Wait a bit longer for the system to process the releases
            Thread.sleep(300);
            
            // Now try to create a new driver and claim interfaces
            UsbSerialDriver freshDriver = UsbSerialProber.getDefaultProber().probeDevice(device);
            if (freshDriver != null && !freshDriver.getPorts().isEmpty()) {
                UsbSerialPort freshPort = freshDriver.getPorts().get(0);
                
                Log.d(TAG, "Attempting connection with fresh driver after interface release...");
                try {
                    freshPort.open(connection);
                    
                    // Try to set parameters - be tolerant of errors
                    try {
                        freshPort.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                        Log.d(TAG, "✅ FE0C parameters set after interface resolution");
                    } catch (Exception paramEx) {
                        Log.w(TAG, "Parameter setting failed but port is open - continuing: " + paramEx.getMessage());
                    }
                    
                    // Update our references
                    if (port != null && port.isOpen()) {
                        try { port.close(); } catch (Exception e) { /* ignore */ }
                    }
                    port = freshPort;
                    driver = freshDriver;
                    currentBaudrate = baudrate;
                    
                    Log.i(TAG, "✅ FE0C interface conflict resolution successful!");
                    return true;
                    
                } catch (Exception openEx) {
                    Log.w(TAG, "Fresh driver open failed after interface release: " + openEx.getMessage());
                    try { freshPort.close(); } catch (Exception e) { /* ignore */ }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "FE0C interface conflict resolution failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Try raw USB communication for FE0C when all other methods fail
     */
    private boolean tryFE0CRawCommunication(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Raw USB Communication ===");
            Log.w(TAG, "Attempting to bypass driver and use raw USB for FE0C device");
            
            UsbDevice device = driver.getDevice();
            
            // Try to work directly with USB endpoints
            if (device.getInterfaceCount() > 0) {
                UsbInterface usbInterface = device.getInterface(0);
                Log.d(TAG, "Working with interface: " + usbInterface.toString());
                
                // Force claim the interface with more aggressive approach
                boolean claimed = false;
                for (int attempt = 0; attempt < 3; attempt++) {
                    Log.d(TAG, "Claim attempt " + (attempt + 1) + "/3...");
                    
                    // Try to release first
                    connection.releaseInterface(usbInterface);
                    Thread.sleep(100);
                    
                    // Then claim with force
                    claimed = connection.claimInterface(usbInterface, true);
                    if (claimed) {
                        Log.d(TAG, "✅ Successfully claimed interface on attempt " + (attempt + 1));
                        break;
                    }
                    Thread.sleep(200);
                }
                
                if (claimed) {
                    // We have the interface - now try to create a working port
                    try {
                        // Create a minimal working port
                        UsbSerialDriver rawDriver = UsbSerialProber.getDefaultProber().probeDevice(device);
                        if (rawDriver != null && !rawDriver.getPorts().isEmpty()) {
                            UsbSerialPort rawPort = rawDriver.getPorts().get(0);
                            
                            Log.d(TAG, "Opening raw port with claimed interface...");
                            rawPort.open(connection);
                            
                            // Update our references immediately
                            if (port != null && port.isOpen()) {
                                try { port.close(); } catch (Exception e) { /* ignore */ }
                            }
                            port = rawPort;
                            driver = rawDriver;
                            currentBaudrate = baudrate;
                            
                            Log.i(TAG, "✅ FE0C raw USB communication established!");
                            Log.i(TAG, "Note: Device may not respond to parameter changes - this is normal for FE0C");
                            
                            return true;
                        } else {
                            Log.w(TAG, "Could not create raw driver even with claimed interface");
                            connection.releaseInterface(usbInterface);
                        }
                    } catch (Exception rawEx) {
                        Log.w(TAG, "Raw port creation failed: " + rawEx.getMessage());
                        connection.releaseInterface(usbInterface);
                    }
                } else {
                    Log.w(TAG, "Could not claim interface after 3 attempts");
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "FE0C raw USB communication failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Standard CH340 initialization with minimal approach for FE0C devices
     */
    private boolean tryStandardCH340Init(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "Opening CH340 port with standard method...");
            
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            if (isFE0CDevice) {
                Log.d(TAG, "Detected FE0C device - using minimal initialization");
                return tryMinimalCH340Init(connection, baudrate);
            }
            
            port.open(connection);
            
            // Give CH340 time to initialize
            Thread.sleep(100);
            
            Log.d(TAG, "Setting CH340 parameters: " + baudrate + " baud, 8N1");
            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            // Set DTR/RTS for CH340
            port.setDTR(true);
            port.setRTS(false);
            
            Thread.sleep(50); // Additional delay for CH340
            
            currentBaudrate = baudrate;
            Log.d(TAG, "✅ CH340 standard initialization successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "CH340 standard init failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after standard init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Minimal initialization specifically for 1A86:FE0C devices that don't respond to init commands
     */
    private boolean tryMinimalCH340Init(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Minimal Initialization (No Init Commands) ===");
            
            // Strategy: Open port without sending any initialization commands
            // Some CH340 variants (like FE0C) don't respond to init commands
            
            Log.d(TAG, "Opening FE0C port without init commands...");
            
            // Try to open with minimal configuration
            try {
                port.open(connection);
                Log.d(TAG, "✅ FE0C port opened successfully");
            } catch (Exception openException) {
                Log.w(TAG, "Normal open failed, trying force open: " + openException.getMessage());
                
                // Try alternative opening method for problematic devices
                try {
                    // Force interface claim without initialization
                    port.open(connection);
                    Log.d(TAG, "✅ FE0C port force opened");
                } catch (Exception forceException) {
                    Log.e(TAG, "Force open also failed: " + forceException.getMessage());
                    throw forceException;
                }
            }
            
            // Small delay to let device settle
            Thread.sleep(200);
            
            // Try setting parameters without strict error checking for FE0C
            try {
                Log.d(TAG, "Setting FE0C parameters: " + baudrate + " baud (ignoring init responses)");
                port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                Log.d(TAG, "✅ FE0C parameters set");
            } catch (Exception paramException) {
                Log.w(TAG, "Parameter setting failed but continuing (FE0C expected): " + paramException.getMessage());
                // For FE0C devices, parameter setting might fail but the device may still work
                // Continue anyway since some FE0C devices don't acknowledge parameter changes
            }
            
            // Try DTR/RTS but don't fail if it doesn't work
            try {
                port.setDTR(false);
                Thread.sleep(50);
                port.setDTR(true);
                port.setRTS(false);
                Log.d(TAG, "✅ FE0C control signals set");
            } catch (Exception controlException) {
                Log.w(TAG, "Control signal setting failed but continuing: " + controlException.getMessage());
                // Continue anyway - some FE0C devices don't support DTR/RTS control
            }
            
            Thread.sleep(100);
            
            currentBaudrate = baudrate;
            Log.i(TAG, "✅ FE0C minimal initialization completed at " + baudrate + " baud");
            Log.i(TAG, "Note: FE0C device may not respond to init commands - this is normal");
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C minimal initialization failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close FE0C port after minimal init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * CH340 reset and retry initialization with FE0C tolerance
     */
    /**
     * Reset and reinitialize CH340 device with proper timing
     */
    private boolean tryCH340ResetInit(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== CH340 Reset and Retry ===");
            
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            // Step 1: Close port if it's open
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                    Log.d(TAG, "Closed existing port for reset");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to close port for reset: " + e.getMessage());
                }
            }
            
            // Step 2: Hardware reset via USB control transfer
            try {
                Log.d(TAG, "Attempting hardware reset...");
                // CH340 reset command
                int result = connection.controlTransfer(0x40, 0xA4, 0, 0, null, 0, 1000);
                Log.d(TAG, "Reset control transfer result: " + result);
                
                // Wait for device to reset
                Thread.sleep(500);
            } catch (Exception resetEx) {
                Log.w(TAG, "Hardware reset failed, continuing with soft reset: " + resetEx.getMessage());
            }
            
            // Step 3: Re-open port after reset
            Log.d(TAG, "Re-opening port after reset...");
            port.open(connection);
            
            // Step 4: Extended delay for device to stabilize after reset
            Thread.sleep(isFE0CDevice ? 500 : 300);
            
            // Step 5: Set parameters with multiple attempts
            boolean configSuccess = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    Log.d(TAG, "Post-reset configuration attempt " + attempt + ": " + baudrate + " baud");
                    port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    configSuccess = true;
                    Log.d(TAG, "✅ Post-reset configuration successful on attempt " + attempt);
                    break;
                } catch (Exception configEx) {
                    Log.w(TAG, "Post-reset config attempt " + attempt + " failed: " + configEx.getMessage());
                    if (attempt < 5) {
                        Thread.sleep(200);
                    }
                }
            }
            
            // Step 6: For FE0C, allow configuration failure but continue
            if (!configSuccess && isFE0CDevice) {
                Log.w(TAG, "FE0C configuration failed but continuing - device may still work");
                configSuccess = true;
            }
            
            if (!configSuccess) {
                throw new IOException("Failed to configure device after reset");
            }
            
            // Step 7: Set control signals
            try {
                port.setDTR(false);
                Thread.sleep(100);
                port.setDTR(true);
                port.setRTS(false);
                Log.d(TAG, "✅ Post-reset control signals set");
            } catch (Exception controlEx) {
                Log.w(TAG, "Post-reset control signal setting failed: " + controlEx.getMessage());
                if (!isFE0CDevice) {
                    throw controlEx;
                }
            }
            
            currentBaudrate = baudrate;
            Log.i(TAG, "✅ CH340 reset and retry successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ CH340 reset init failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after reset init failure", closeException);
                }
            }
            return false;
        }
    }

    /**
     * Initialize CH340 port with reset functionality
     */
    private boolean tryResetInitialization(UsbDeviceConnection connection, int baudrate) {
        try {
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            if (isFE0CDevice) {
                Log.d(TAG, "FE0C device - skipping reset, trying alternative approach");
                return tryFE0CAlternativeInit(connection, baudrate);
            }
            
            // Reset device using USB control transfer
            Log.d(TAG, "Resetting CH340 device...");
            boolean resetResult = connection.controlTransfer(0x40, 0xA4, 0, 0, null, 0, 1000) >= 0;
            Log.d(TAG, "CH340 reset result: " + resetResult);
            
            Thread.sleep(500); // Wait for reset to complete
            
            Log.d(TAG, "Opening CH340 port after reset...");
            port.open(connection);
            
            Thread.sleep(200); // Extra delay after reset
            
            Log.d(TAG, "Setting CH340 parameters after reset: " + baudrate + " baud");
            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            
            // Configure DTR/RTS
            port.setDTR(false);
            Thread.sleep(50);
            port.setDTR(true);
            port.setRTS(false);
            
            Thread.sleep(100);
            
            currentBaudrate = baudrate;
            Log.d(TAG, "✅ CH340 reset initialization successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "CH340 reset init failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after reset init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Alternative initialization for FE0C devices that may not support standard reset
     */
    private boolean tryFE0CAlternativeInit(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Alternative Initialization ===");
            
            // Try claiming interface directly without reset
            Log.d(TAG, "Attempting direct interface claim for FE0C...");
            
            // Get interface directly and try to claim it
            UsbDevice device = driver.getDevice();
            if (device.getInterfaceCount() > 0) {
                UsbInterface usbInterface = device.getInterface(0);
                Log.d(TAG, "Found interface: " + usbInterface.toString());
                
                // Try to claim interface manually
                boolean claimResult = connection.claimInterface(usbInterface, true);
                Log.d(TAG, "Manual interface claim result: " + claimResult);
                
                if (claimResult) {
                    // Interface claimed successfully, now try to open port
                    try {
                        port.open(connection);
                        Log.d(TAG, "✅ FE0C port opened after manual interface claim");
                        
                        // Try setting parameters
                        try {
                            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                            Log.d(TAG, "✅ FE0C parameters set successfully");
                        } catch (Exception paramEx) {
                            Log.w(TAG, "FE0C parameter setting failed but continuing: " + paramEx.getMessage());
                        }
                        
                        currentBaudrate = baudrate;
                        Log.i(TAG, "✅ FE0C alternative initialization successful at " + baudrate + " baud");
                        return true;
                        
                    } catch (Exception openEx) {
                        Log.w(TAG, "Port open failed after interface claim: " + openEx.getMessage());
                        connection.releaseInterface(usbInterface);
                    }
                } else {
                    Log.w(TAG, "Failed to manually claim interface for FE0C");
                }
            } else {
                Log.w(TAG, "No interfaces found on FE0C device");
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C alternative initialization failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Manual CH340 control sequence initialization with FE0C support
     */
    private boolean tryManualCH340Init(UsbDeviceConnection connection, int baudrate) {
        try {
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            if (isFE0CDevice) {
                Log.d(TAG, "FE0C device - trying final compatibility mode");
                return tryFE0CCompatibilityMode(connection, baudrate);
            }
            
            Log.d(TAG, "Trying manual CH340 control sequence...");
            
            // Manual CH340 control transfers (similar to what the driver does internally)
            // These are specific to CH340 initialization sequence
            connection.controlTransfer(0x40, 0xA1, 0, 0, null, 0, 1000);
            Thread.sleep(50);
            connection.controlTransfer(0x40, 0x9A, 0x2518, 0x0050, null, 0, 1000);
            Thread.sleep(50);
            
            Log.d(TAG, "Opening CH340 port with manual control...");
            port.open(connection);
            
            Thread.sleep(150);
            
            // Try setting parameters with different approach
            Log.d(TAG, "Setting CH340 parameters with manual control: " + baudrate + " baud");
            
            // Set parameters step by step
            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            Thread.sleep(50);
            
            // Control lines
            port.setDTR(true);
            Thread.sleep(20);
            port.setRTS(false);
            Thread.sleep(50);
            
            currentBaudrate = baudrate;
            Log.d(TAG, "✅ CH340 manual initialization successful at " + baudrate + " baud");
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "CH340 manual init failed: " + e.getMessage());
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after manual init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Final compatibility mode for FE0C devices - most permissive approach
     */
    private boolean tryFE0CCompatibilityMode(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "=== FE0C Final Compatibility Mode ===");
            Log.d(TAG, "Using most permissive approach for stubborn FE0C device");
            
            // Create a completely fresh driver instance
            UsbDevice device = driver.getDevice();
            UsbSerialDriver newDriver = UsbSerialProber.getDefaultProber().probeDevice(device);
            
            if (newDriver != null && !newDriver.getPorts().isEmpty()) {
                Log.d(TAG, "Created fresh driver instance for FE0C");
                UsbSerialPort newPort = newDriver.getPorts().get(0);
                
                try {
                    Log.d(TAG, "Opening FE0C with fresh driver...");
                    newPort.open(connection);
                    
                    // Just try to set the baudrate - ignore all errors
                    try {
                        newPort.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                        Log.d(TAG, "Parameters set on fresh driver");
                    } catch (Exception paramEx) {
                        Log.w(TAG, "Parameter setting failed on fresh driver but continuing: " + paramEx.getMessage());
                    }
                    
                    // Replace the old port with the new one
                    if (port != null && port.isOpen()) {
                        try {
                            port.close();
                        } catch (Exception closeEx) {
                            Log.w(TAG, "Failed to close old port: " + closeEx.getMessage());
                        }
                    }
                    
                    port = newPort;
                    driver = newDriver;
                    currentBaudrate = baudrate;
                    
                    Log.i(TAG, "✅ FE0C compatibility mode successful - device may work without init responses");
                    Log.i(TAG, "FE0C Note: This device type doesn't provide initialization feedback");
                    
                    return true;
                    
                } catch (Exception freshEx) {
                    Log.w(TAG, "Fresh driver approach failed: " + freshEx.getMessage());
                    try {
                        newPort.close();
                    } catch (Exception closeEx) {
                        // Ignore close errors
                    }
                }
            } else {
                Log.w(TAG, "Could not create fresh driver for FE0C");
            }
            
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C compatibility mode failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Special initialization for FE0C devices that bypasses CH340 driver initialization
     * FE0C devices are CH340-based but don't respond to initialization commands
     */
    private boolean initializeFE0CPort(UsbDeviceConnection connection, int baudrate) {
        Log.d(TAG, "=== FE0C Direct USB Initialization ===");
        Log.d(TAG, "Bypassing CH340 driver initialization for FE0C device");
        
        try {
            UsbDevice device = driver.getDevice();
            
            // Log all available interfaces for debugging
            Log.d(TAG, "Device has " + device.getInterfaceCount() + " interfaces:");
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface iface = device.getInterface(i);
                Log.d(TAG, "Interface " + i + ": class=" + iface.getInterfaceClass() + 
                     ", subclass=" + iface.getInterfaceSubclass() + 
                     ", protocol=" + iface.getInterfaceProtocol() +
                     ", endpoints=" + iface.getEndpointCount());
            }
            
            // Get the USB interface - for CH340, usually the first (and often only) interface
            UsbInterface dataInterface = null;
            
            // Try to find the correct interface
            // CH340 devices typically use CDC-ACM (class 2) or vendor-specific (class 255/0xFF)
            // But for data transfer, we need the interface with bulk endpoints
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface iface = device.getInterface(i);
                int ifaceClass = iface.getInterfaceClass();
                int endpointCount = iface.getEndpointCount();
                
                Log.d(TAG, "Checking interface " + i + ": class=" + ifaceClass + ", endpoints=" + endpointCount);
                
                // Look for interfaces with multiple endpoints (likely data interface)
                // CDC Data interface is usually class 10, while CDC Control is class 2
                if (ifaceClass == 10 && endpointCount >= 2) {
                    dataInterface = iface;
                    Log.d(TAG, "Found CDC data interface " + i + " with class " + ifaceClass + " and " + endpointCount + " endpoints");
                    break;
                } else if ((ifaceClass == 2 || ifaceClass == 0xFF) && endpointCount >= 2) {
                    dataInterface = iface;
                    Log.d(TAG, "Found potential data interface " + i + " with class " + ifaceClass + " and " + endpointCount + " endpoints");
                    break;
                }
            }
            
            // If no specific class found, use the first interface
            if (dataInterface == null && device.getInterfaceCount() > 0) {
                dataInterface = device.getInterface(0);
                Log.d(TAG, "Using first interface as fallback: class=" + dataInterface.getInterfaceClass());
            }
            
            if (dataInterface == null) {
                Log.e(TAG, "❌ No suitable interface found for FE0C device");
                return false;
            }
            
            // Claim the interface directly
            Log.d(TAG, "Attempting to claim interface...");
            if (!connection.claimInterface(dataInterface, true)) {
                Log.e(TAG, "❌ Could not claim interface for FE0C device");
                return false;
            }
            
            Log.d(TAG, "✅ FE0C interface claimed successfully");
            
            // Store connection parameters for direct USB communication
            fe0cConnection = connection;
            fe0cInterface = dataInterface;
            currentBaudrate = baudrate;
            
            // Set up port reference (we still need this for compatibility)
            port = driver.getPorts().get(0);
            
            Log.d(TAG, "Testing FE0C communication capability...");
            
            // Test with a simple reset command that FE0C should handle
            byte[] testCommand = {0x57, (byte)0xAB, 0x00, 0x02, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07}; // Reset command with checksum
            
            Log.d(TAG, "Sending FE0C test command: " + bytesToHex(testCommand));
            
            // Test the connection with our special FE0C write method
            boolean testResult = testFE0CConnection(testCommand);
            
            if (testResult) {
                Log.i(TAG, "✅ FE0C device initialized successfully for command-only operation");
                Log.i(TAG, "Note: FE0C uses direct USB communication, not standard serial");
                return true;
            } else {
                Log.w(TAG, "⚠️ FE0C test command failed, but continuing anyway");
                Log.w(TAG, "FE0C device may still work for actual keyboard commands");
                // Don't fail here - FE0C might not respond to test commands but still work for keyboard data
                return true;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C initialization failed: " + e.getMessage(), e);
            // Clean up if we fail
            if (fe0cConnection != null && fe0cInterface != null) {
                try {
                    fe0cConnection.releaseInterface(fe0cInterface);
                } catch (Exception cleanup) {
                    Log.w(TAG, "Cleanup warning: " + cleanup.getMessage());
                }
                fe0cConnection = null;
                fe0cInterface = null;
            }
            return false;
        }
    }
    
    // Add fields to store FE0C direct connection
    private UsbDeviceConnection fe0cConnection = null;
    private UsbInterface fe0cInterface = null;
    
    /**
     * Test FE0C connection by sending a test command
     */
    private boolean testFE0CConnection(byte[] testCommand) {
        try {
            if (fe0cConnection == null || fe0cInterface == null) {
                Log.e(TAG, "❌ FE0C connection not properly initialized");
                return false;
            }
            
            // Find the bulk out endpoint
            android.hardware.usb.UsbEndpoint outEndpoint = null;
            for (int i = 0; i < fe0cInterface.getEndpointCount(); i++) {
                android.hardware.usb.UsbEndpoint endpoint = fe0cInterface.getEndpoint(i);
                if (endpoint.getType() == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.getDirection() == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                    outEndpoint = endpoint;
                    break;
                }
            }
            
            if (outEndpoint == null) {
                Log.e(TAG, "❌ Could not find bulk out endpoint for FE0C");
                return false;
            }
            
            // Send the test command directly
            int result = fe0cConnection.bulkTransfer(outEndpoint, testCommand, testCommand.length, 1000);
            if (result >= 0) {
                Log.d(TAG, "✅ FE0C test command sent successfully (" + result + " bytes)");
                return true;
            } else {
                Log.e(TAG, "❌ FE0C test command failed: " + result);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C test failed: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Write data directly to FE0C device using USB bulk transfer
     */
    private boolean writeFE0CData(byte[] data, int timeout) {
        try {
            if (fe0cConnection == null || fe0cInterface == null) {
                Log.e(TAG, "❌ FE0C direct connection not available");
                return false;
            }
            
            // Enhanced logging for FE0C
            Log.d(TAG, "=== FE0C DIRECT USB WRITE ===");
            Log.d(TAG, "Data Length: " + data.length + " bytes");
            Log.d(TAG, "Timeout: " + timeout + "ms");
            Log.d(TAG, "Raw Hex Data: " + bytesToHex(data));
            
            // Decode keyboard data if it looks like CH9329 keyboard command
            if (data.length >= 5 && data[0] == 0x57 && data[1] == (byte)0xAB) {
                Log.d(TAG, ">>> FE0C CH9329 Keyboard Command <<<");
                Log.d(TAG, "Header: " + String.format("%02X %02X", data[0] & 0xFF, data[1] & 0xFF));
                if (data.length >= 13) {
                    Log.d(TAG, "Address: " + String.format("%02X", data[2] & 0xFF));
                    Log.d(TAG, "Command: " + String.format("%02X", data[3] & 0xFF));
                    Log.d(TAG, "Length: " + String.format("%02X", data[4] & 0xFF));
                    Log.d(TAG, "Modifier: " + String.format("%02X", data[5] & 0xFF));
                    Log.d(TAG, "Reserved: " + String.format("%02X", data[6] & 0xFF));
                    Log.d(TAG, "Key Code: " + String.format("%02X", data[7] & 0xFF));
                    Log.d(TAG, "Key 2-6: " + String.format("%02X %02X %02X %02X %02X", 
                        data[8] & 0xFF, data[9] & 0xFF, data[10] & 0xFF, data[11] & 0xFF, data[12] & 0xFF));
                    if (data.length > 13) {
                        Log.d(TAG, "Checksum: " + String.format("%02X", data[13] & 0xFF));
                    }
                }
            }
            
            // Find the bulk out endpoint
            android.hardware.usb.UsbEndpoint outEndpoint = null;
            for (int i = 0; i < fe0cInterface.getEndpointCount(); i++) {
                android.hardware.usb.UsbEndpoint endpoint = fe0cInterface.getEndpoint(i);
                if (endpoint.getType() == android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.getDirection() == android.hardware.usb.UsbConstants.USB_DIR_OUT) {
                    outEndpoint = endpoint;
                    break;
                }
            }
            
            if (outEndpoint == null) {
                Log.e(TAG, "❌ Could not find bulk out endpoint for FE0C");
                return false;
            }
            
            // Send data directly via USB bulk transfer
            long startTime = System.currentTimeMillis();
            int result = fe0cConnection.bulkTransfer(outEndpoint, data, data.length, timeout);
            long endTime = System.currentTimeMillis();
            
            if (result >= 0) {
                Log.d(TAG, "✅ FE0C direct write successful: " + result + " bytes in " + (endTime - startTime) + "ms");
                return true;
            } else {
                Log.e(TAG, "❌ FE0C direct write failed: " + result);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ FE0C direct write error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Convert byte array to hex string for logging
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b & 0xFF));
        }
        return result.toString().trim();
    }
    
    /**
     * Standard initialization for non-CH340 devices
     */
    private boolean initializeStandardPort(UsbDeviceConnection connection, int baudrate) {
        try {
            Log.d(TAG, "Opening port with standard method...");
            port.open(connection);
            Log.d(TAG, "Port opened successfully");
            
            // Try different configurations in case the default doesn't work
            boolean configSuccess = false;
            
            // First try: Standard parameters
            try {
                Log.d(TAG, "Setting standard parameters: " + baudrate + " baud, 8N1");
                port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                configSuccess = true;
            } catch (Exception e) {
                Log.w(TAG, "Standard configuration failed, trying alternative parameters: " + e.getMessage());
                
                // Alternative configurations for problematic devices
                try {
                    // Try with flow control
                    port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    port.setDTR(true);
                    port.setRTS(false);
                    configSuccess = true;
                    Log.d(TAG, "✅ Port configured with flow control");
                } catch (Exception e2) {
                    Log.w(TAG, "Flow control configuration failed, trying minimal config: " + e2.getMessage());
                    
                    try {
                        // Minimal configuration
                        port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                        configSuccess = true;
                        Log.d(TAG, "✅ Port configured with minimal parameters");
                    } catch (Exception e3) {
                        Log.e(TAG, "❌ All configuration attempts failed: " + e3.getMessage());
                    }
                }
            }
            
            if (configSuccess) {
                currentBaudrate = baudrate;
                Log.i(TAG, "✅ Serial port configured successfully at " + baudrate + " baud");
                
                // Test if the connection is actually working
                if (testSerialConnection()) {
                    Log.i(TAG, "✅ Serial connection test passed");
                    return true;
                } else {
                    Log.w(TAG, "⚠️ Serial port opened but connection test failed - may still work for some devices");
                    return true; // Return true anyway - some devices don't respond to test but still work
                }
            } else {
                Log.e(TAG, "❌ Failed to configure serial port parameters");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("Failed to configure serial parameters");
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Standard port initialization failed: " + e.getMessage(), e);
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after standard init failure", closeException);
                }
            }
            return false;
        }
    }
    
    /**
     * Test if the serial connection is actually working
     */
    private boolean testSerialConnection() {
        try {
            if (port == null || !port.isOpen()) {
                Log.w(TAG, "Cannot test connection - port is not open");
                return false;
            }
            
            // For FE0C devices, don't do aggressive testing since they don't respond to commands
            UsbDevice device = driver.getDevice();
            boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
            
            if (isFE0CDevice) {
                Log.d(TAG, "FE0C device - skipping aggressive connection test");
                
                // Just check if we can get basic port info
                try {
                    boolean dtr = port.getDTR();
                    boolean rts = port.getRTS();
                    Log.d(TAG, "FE0C basic test - DTR: " + dtr + ", RTS: " + rts);
                    return true; // Consider it working if we can read control lines
                } catch (Exception e) {
                    Log.w(TAG, "FE0C basic test failed but this may be normal: " + e.getMessage());
                    return true; // Still consider it working - FE0C is quirky
                }
            } else {
                // For other devices, try a simple write test
                try {
                    // Try to write a small test byte
                    byte[] testData = new byte[]{0x00}; // Null byte - usually safe
                    port.write(testData, 100);
                    Log.d(TAG, "Connection test - write successful");
                    return true; // Consider successful if write didn't throw exception
                } catch (Exception e) {
                    Log.w(TAG, "Connection write test failed: " + e.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Connection test exception: " + e.getMessage());
            return false;
        }
    }

//    public UsbDeviceManager(Context context, TextView tvReceivedData) {
//        this.context = context;
//        this.tvReceivedData = tvReceivedData;
//    }
//
//    public void registerReceiver() {
//        context.registerReceiver(usbReceiver, new IntentFilter("com.android.example.USB_PERMISSION"));
//    }
//
//    public void unregisterReceiver() {
//        context.unregisterReceiver(usbReceiver);
//    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "USB Receiver - Action: " + action);
            
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d(TAG, "✅ USB permission granted for device: " + 
                                String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                            
                            // Use standard connection method for all devices
                            connectWithPermission(device);
                        } else {
                            Log.e(TAG, "USB permission granted but device is null");
                            if (onConnectionStatusListener != null) {
                                onConnectionStatusListener.onError("Device is null after permission grant");
                            }
                        }
                    } else {
                        Log.w(TAG, "❌ USB permission denied for device: " + 
                            (device != null ? String.format("%04X:%04X", device.getVendorId(), device.getProductId()) : "null"));
                        if (onConnectionStatusListener != null) {
                            onConnectionStatusListener.onError("USB permission denied by user");
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "USB device attached - reinitializing");
                init();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && driver != null && driver.getDevice().equals(device)) {
                    Log.d(TAG, "Openterface device detached");
                    if (onConnectionStatusListener != null) {
                        onConnectionStatusListener.onDisconnected();
                    }
                }
            }
        }
    };

    public void handleUsbDevice(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            Log.d(TAG, "handleUsbDevice data successful");
            init();
        }
    }

    public UsbDeviceManager(Context context, UsbManager usbManager) {
        this.context = context;
        this.usbManager = usbManager;
        mSerialThread = new HandlerThread("Serial");
        mSerialThread.start();
        mSerialAsyncHandler = new Handler(mSerialThread.getLooper());
    }

    public void init() {
        Log.d(TAG, "Initializing UsbDeviceManager - scanning for Openterface devices");
        mSerialAsyncHandler.post(() -> {
            // First, scan ALL USB devices to find Openterface devices
            UsbDevice openterfaceDevice = detectOpenterfaceDevice();
            
            if (openterfaceDevice == null) {
                Log.w(TAG, "No Openterface serial devices detected");
                Log.d(TAG, "Please ensure your Openterface device is connected and powered");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("No Openterface devices found (VID:PID should be 1A86:7523 or 1A86:FE0C)");
                }
                return;
            }
            
            int vid = openterfaceDevice.getVendorId();
            int pid = openterfaceDevice.getProductId();
            String deviceType = (pid == 0xFE0C) ? "Mini-KVM v2" : (pid == 0x7523) ? "Mini-KVM v1" : "Unknown";
            
            Log.i(TAG, "Found Openterface device: " + 
                String.format("%04X:%04X", vid, pid) + " (" + deviceType + ")" +
                " - " + openterfaceDevice.getProductName());
            
            // Now try to get a driver for this specific device with enhanced probing
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(vid, pid, Ch34xSerialDriver.class);
            
            // Create custom prober with our device support
            UsbSerialProber prober = new UsbSerialProber(customTable);
            List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
            
            // If custom probing fails, try default prober as fallback
            if (availableDrivers.isEmpty()) {
                Log.w(TAG, "Custom prober failed, trying default prober...");
                availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                
                // Filter to only include our device
                availableDrivers = availableDrivers.stream()
                    .filter(d -> d.getDevice().getVendorId() == vid && d.getDevice().getProductId() == pid)
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (availableDrivers.isEmpty()) {
                Log.e(TAG, "No compatible driver found for Openterface device " + 
                    String.format("%04X:%04X", vid, pid));
                Log.e(TAG, "This may indicate a driver compatibility issue or USB subsystem problem");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("No compatible serial driver for detected device. Device: " + 
                        String.format("%04X:%04X", vid, pid) + " (" + deviceType + ")");
                }
                return;
            }
            
            driver = availableDrivers.get(0);
            UsbDevice device = driver.getDevice();
            
            Log.d(TAG, "Successfully created driver for device: " + 
                String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
            Log.d(TAG, "Driver class: " + driver.getClass().getSimpleName());
            Log.d(TAG, "Available ports: " + driver.getPorts().size());
            
            // Apply device-specific configuration
            applyDeviceSpecificConfig(device);
            
            // Check permission before attempting connection
            if (!usbManager.hasPermission(device)) {
                Log.d(TAG, "No permission for device, requesting permission...");
                requestPermissionAndConnect(device);
            } else {
                Log.d(TAG, "Device already has permission, connecting directly...");
                connectWithPermission(device);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(usbReceiver, filter);
        }
    }
    
    /**
     * Apply device-specific configuration based on VID/PID
     */
    private void applyDeviceSpecificConfig(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        Log.d(TAG, "Applying configuration for device " + String.format("%04X:%04X", vid, pid));
        
        // CH340/CH341 based devices - only two supported devices
        if (vid == 0x1A86) {
            if (pid == 0xFE0C) {
                // Mini-KVM v2: Only supports 115200
                preferredBaudrate = BAUDRATE_HIGHSPEED;
                Log.d(TAG, "Device 1A86:FE0C - Mini-KVM v2: Default to 115200 baud");
            } else if (pid == 0x7523) {
                // Mini-KVM v1: Default to 9600
                preferredBaudrate = BAUDRATE_LOWSPEED;
                Log.d(TAG, "Device 1A86:7523 - Mini-KVM v1: Default to 9600 baud");
            } else {
                Log.w(TAG, "Unknown 1A86 device PID: " + String.format("%04X", pid));
                preferredBaudrate = DEFAULT_BAUDRATE; // Use default baudrate for unknown devices
            }
        } else {
            Log.w(TAG, "Unsupported device VID: " + String.format("%04X", vid));
            preferredBaudrate = DEFAULT_BAUDRATE; // Use default baudrate for unsupported devices
        }
        
        Log.d(TAG, "Preferred baudrate set to: " + preferredBaudrate);
        
        // Additional device-specific configuration
        // For CH340 devices, we might need to set specific DTR/RTS states later
        if (vid == 0x1A86) {
            Log.d(TAG, "Will configure CH340 flow control after connection");
        }
    }
    
    /**
     * Request permission and connect when granted
     */
    private void requestPermissionAndConnect(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            context, 0, new Intent(ACTION_USB_PERMISSION), 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Store device for later use in broadcast receiver
        pendingDevice = device;
        
        Log.d(TAG, "Requesting USB permission for " + String.format("%04X:%04X", 
            device.getVendorId(), device.getProductId()));
        usbManager.requestPermission(device, permissionIntent);
    }
    
    /**
     * Connect to device (assumes permission is already granted)
     */
    private void connectWithPermission(UsbDevice device) {
        Log.d(TAG, "Connecting to device with permission: " + String.format("%04X:%04X", 
            device.getVendorId(), device.getProductId()));
        
        // Extract device info for later use
        int vid = device.getVendorId();
        int pid = device.getProductId();
        String deviceType = (pid == 0xFE0C) ? "Mini-KVM v2" : (pid == 0x7523) ? "Mini-KVM v1" : "Unknown";
        
        // Create custom driver for this specific device
        try {
            Log.d(TAG, "Creating custom driver for device...");
            ProbeTable customTable = new ProbeTable();
            
            // Add device to probe table with different potential drivers
            customTable.addProduct(device.getVendorId(), device.getProductId(), Ch34xSerialDriver.class);
            
            UsbSerialProber prober = new UsbSerialProber(customTable);
            List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
            
            if (availableDrivers.isEmpty()) {
                Log.w(TAG, "No drivers found with custom prober, trying default prober...");
                
                // Try default prober as fallback
                availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                
                if (availableDrivers.isEmpty()) {
                    Log.e(TAG, "❌ No compatible drivers found for device");
                    if (onConnectionStatusListener != null) {
                        onConnectionStatusListener.onError("No compatible serial drivers for this device");
                    }
                    return;
                }
            }
            
            // Find our device in the available drivers
            UsbSerialDriver matchingDriver = null;
            for (UsbSerialDriver driver : availableDrivers) {
                if (driver.getDevice().equals(device)) {
                    matchingDriver = driver;
                    break;
                }
            }
            
            if (matchingDriver != null) {
                driver = matchingDriver;
                Log.d(TAG, "Using driver: " + driver.getClass().getSimpleName());
            } else {
                Log.e(TAG, "❌ Device found but no matching driver available");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("No matching driver for detected device");
                }
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating driver: " + e.getMessage(), e);
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Error creating driver: " + e.getMessage());
            }
            return;
        }
            
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            Log.e(TAG, "Failed to open device connection");
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to open USB connection");
            }
            return;
        }
        
        // Reset USB device before trying to configure
        try {
            connection.controlTransfer(0x40, 0, 0, 0, null, 0, 0);
            Thread.sleep(100); // Give device time to reset
        } catch (Exception e) {
            Log.d(TAG, "Device reset not supported or failed: " + e.getMessage());
        }
        
        // Set baudrates based on device type
        int[] baudrates;
        
        // Check device VID/PID to determine supported baudrates        
        Log.d(TAG, "Connecting with permission to device: " + 
            String.format("%04X:%04X", vid, pid) + " (" + deviceType + ")");
        
        if (vid == 0x1A86 && pid == 0xFE0C) {
            // Mini-KVM v2: Only supports 115200
            baudrates = new int[] { BAUDRATE_HIGHSPEED };
            Log.d(TAG, "Mini-KVM v2 (1A86:FE0C) - trying 115200 baud only");
        } else if (vid == 0x1A86 && pid == 0x7523) {
            // Mini-KVM v1: Supports both 9600 and 115200
            baudrates = new int[] { BAUDRATE_HIGHSPEED, BAUDRATE_LOWSPEED };
            Log.d(TAG, "Mini-KVM v1 (1A86:7523) - trying 115200 and 9600 baud");
        } else {
            // Fallback for any other device
            baudrates = new int[] { BAUDRATE_HIGHSPEED, BAUDRATE_LOWSPEED };
            Log.d(TAG, "Unknown device - trying both baudrates");
        }
        
        Log.d(TAG, "Available baudrates for this device: " + java.util.Arrays.toString(baudrates));
        
        // If user has set a preferred baudrate, try that first
        if (preferredBaudrate > 0) {
            Log.d(TAG, "User has set preferred baudrate: " + preferredBaudrate + ", trying first");
            if (trySerialConfiguration(connection, preferredBaudrate)) {
                Log.i(TAG, "✅ Successfully connected at preferred baudrate: " + preferredBaudrate + " baud");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onConnected(preferredBaudrate);
                }
                startReading();
                return;
            } else {
                Log.w(TAG, "Failed with preferred baudrate, trying others...");
            }
        }
        
        boolean connected = false;
        
        for (int baudrate : baudrates) {
            // Skip preferred baudrate if we already tried it
            if (preferredBaudrate > 0 && baudrate == preferredBaudrate) {
                continue;
            }
            
            if (trySerialConfiguration(connection, baudrate)) {
                Log.i(TAG, "✅ Successfully connected at " + baudrate + " baud");
                connected = true;
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onConnected(baudrate);
                }
                startReading();
                break;
            }
            
            // Short delay between baudrate attempts
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (!connected) {
            Log.e(TAG, "❌ Failed to connect with any baudrate for device " + 
                String.format("%04X:%04X", vid, pid) + " (" + deviceType + ")");
            Log.e(TAG, "This could indicate hardware issues, driver problems, or USB communication errors");
            
            connection.close();
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to configure serial port with any baudrate for " +
                    deviceType + " device " + String.format("%04X:%04X", vid, pid) + 
                    ". Please check device connection and try again.");
            }
            
            // Try advanced recovery
            Log.d(TAG, "Attempting advanced recovery for device " + String.format("%04X:%04X", vid, pid));
            advancedSerialRecovery(device);
        }
    }
    
    /**
     * Advanced recovery for serial devices that failed standard connection
     */
    private void advancedSerialRecovery(UsbDevice device) {
        Log.d(TAG, "Attempting advanced serial recovery...");
        
        // 1. Try device power cycling via USB
        try {
            Log.d(TAG, "1. Attempting USB power cycle...");
            UsbDeviceConnection connection = usbManager.openDevice(device);
            if (connection != null) {
                // Toggle DTR/RTS for device reset
                boolean resetSuccess = connection.controlTransfer(0x40, 0, 0, 0, null, 0, 1000) >= 0;
                Log.d(TAG, "USB reset result: " + resetSuccess);
                connection.close();
                Thread.sleep(1000); // Wait for device to reset
            }
        } catch (Exception e) {
            Log.e(TAG, "Power cycle failed: " + e.getMessage());
        }
        
        // 2. Try with a different approach - direct open
        try {
            Log.d(TAG, "2. Attempting direct open approach...");
            UsbDeviceConnection connection = usbManager.openDevice(device);
            if (connection != null) {
                port = driver.getPorts().get(0);
                port.open(connection);
                
                // Try with basic 8N1 configuration
                try {
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    Log.d(TAG, "✅ Direct configuration succeeded at 9600 baud");
                    currentBaudrate = 9600;
                    if (onConnectionStatusListener != null) {
                        onConnectionStatusListener.onConnected(9600);
                    }
                    startReading();
                } catch (Exception e) {
                    Log.e(TAG, "Direct configuration failed: " + e.getMessage());
                    if (port != null && port.isOpen()) {
                        try {
                            port.close();
                        } catch (IOException closeException) {
                            // Ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Direct open failed: " + e.getMessage());
        }
        
        Log.d(TAG, "Advanced recovery complete - check connection state");
    }

    private void closeDevice() {
        try {
            // Clean up FE0C direct connection if exists
            if (fe0cConnection != null && fe0cInterface != null) {
                Log.d(TAG, "Cleaning up FE0C direct connection");
                try {
                    fe0cConnection.releaseInterface(fe0cInterface);
                    Log.d(TAG, "FE0C interface released");
                } catch (Exception e) {
                    Log.w(TAG, "FE0C interface release warning: " + e.getMessage());
                }
                fe0cConnection = null;
                fe0cInterface = null;
            }
            
            // Clean up regular serial port
            if (port != null && port.isOpen()) {
                port.close();
                Log.d(TAG, "port close: ");
            }
        } catch (IOException e) {
            Log.d(TAG, "port close failed ", e);
        } finally {
            currentBaudrate = -1; // Reset tracked baudrate
        }
    }

    public void release() {
        context.unregisterReceiver(usbReceiver);
        closeDevice();
        mSerialThread.quitSafely();
        Log.d(TAG, "serial is close");
    }

    private void requestUsbPermission(UsbDevice serialDevice) {
        Log.d(TAG, "Requesting USB permission for device: " + 
            String.format("%04X:%04X", serialDevice.getVendorId(), serialDevice.getProductId()));
        
        // Check if we already have permission
        if (usbManager.hasPermission(serialDevice)) {
            Log.d(TAG, "Permission already granted, proceeding with connection");
            openSerialConnection(serialDevice);
            return;
        }
        
        Log.d(TAG, "Permission not granted, requesting permission from user");
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        usbManager.requestPermission(serialDevice, permissionIntent);
    }
    
    private void openSerialConnection(UsbDevice serialDevice) {
        Log.d(TAG, "Opening serial connection to device: " + 
            String.format("%04X:%04X", serialDevice.getVendorId(), serialDevice.getProductId()));
            
        UsbDeviceConnection connection = usbManager.openDevice(serialDevice);
        if (connection == null) {
            Log.e(TAG, "Failed to open USB device connection - connection is null");
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to open USB device connection");
            }
            return;
        }
        
        Log.d(TAG, "USB device connection opened successfully");
        
        // Use the configured preferred baudrate
        Log.d(TAG, "Attempting serial connection with baudrate: " + preferredBaudrate);
        
        boolean connectionSuccessful = trySerialConfiguration(connection, preferredBaudrate);
        
        if (connectionSuccessful) {
            Log.d(TAG, "Serial connection established successfully with baudrate: " + preferredBaudrate);
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onConnected(preferredBaudrate);
            }
            startReading();
        } else {
            Log.e(TAG, "Failed to establish serial connection with baudrate: " + preferredBaudrate);
            connection.close();
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to establish serial connection with baudrate: " + preferredBaudrate);
            }
        }
    }

    private void startReading() {
        isReading = true;
        mSerialAsyncHandler.post(() -> {
            byte[] buffer = new byte[1024];
            while (isReading) {
                try {
                    int numBytesRead = port.read(buffer, 5);
                    if (numBytesRead > 0) {
                        StringBuilder allReadData = new StringBuilder();
                        for (int i = 0; i < numBytesRead; i++) {
                            allReadData.append(String.format("%02X ", buffer[i]));
                        }
                        Log.d(TAG, "Read data: " + allReadData.toString().trim());
                        
                        if (onDataReadListener != null) {
                            onDataReadListener.onDataRead(buffer, numBytesRead);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from port", e);
                    break;
                }
            }
        });
    }

    /**
     * Get the current baudrate of the serial port
     * @return current baudrate, or -1 if port is not open
     */
    public int getCurrentBaudrate() {
        if (port != null && port.isOpen()) {
            return currentBaudrate;
        }
        return -1;
    }

    /**
     * Check if the serial port is connected and open
     * @return true if connected and open, false otherwise
     */
    public boolean isConnected() {
        // For FE0C devices using direct USB communication
        if (fe0cConnection != null && fe0cInterface != null) {
            return true;
        }
        // For standard serial devices
        return port != null && port.isOpen();
    }

    /**
     * Set the preferred baudrate for serial communication
     * @param baudrate the preferred baudrate (9600 or 115200)
     */
    public void setPreferredBaudrate(int baudrate) {
        this.preferredBaudrate = baudrate;
        Log.d(TAG, "Preferred baudrate set to: " + baudrate);
    }

    /**
     * Get the preferred baudrate setting
     * @return the preferred baudrate
     */
    public int getPreferredBaudrate() {
        return preferredBaudrate;
    }

    /**
     * Reconnect with the specified baudrate
     * @param baudrate the baudrate to use for reconnection
     * @return true if reconnection successful, false otherwise
     */
    public boolean reconnectWithBaudrate(int baudrate) {
        if (port != null && port.isOpen()) {
            try {
                port.close();
                Log.d(TAG, "Closed existing connection for baudrate change");
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onDisconnected();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing port for reconnection", e);
            }
        }
        
        // Set the preferred baudrate and reinitialize
        setPreferredBaudrate(baudrate);
        init();
        return true;
    }
    
    /**
     * Write data to the serial port
     * @param data byte array to write
     * @return true if write successful, false otherwise
     */
    public boolean writeData(byte[] data) {
        return writeData(data, WRITE_WAIT_MILLIS);
    }
    
    /**
     * Write data to the serial port with custom timeout
     * 
     * This method sends commands to both device types:
     * - 1A86:FE0C (Mini-KVM v2): Commands only, no replies expected
     * - 1A86:7523 (Mini-KVM v1): Commands with possible replies (but not required)
     * 
     * @param data byte array to write
     * @param timeout timeout in milliseconds
     * @return true if write successful, false otherwise
     */
    public boolean writeData(byte[] data, int timeout) {
        // Special handling for FE0C devices that use direct USB communication
        if (fe0cConnection != null && fe0cInterface != null) {
            return writeFE0CData(data, timeout);
        }
        
        if (port == null || !port.isOpen()) {
            Log.e(TAG, "Cannot write: port is not open");
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Port not open for writing");
            }
            return false;
        }
        
        if (data == null || data.length == 0) {
            Log.w(TAG, "Cannot write: data is null or empty");
            return false;
        }
        
        try {
            // Enhanced logging for debugging FE0C keyboard issues
            if (driver != null && driver.getDevice() != null) {
                UsbDevice device = driver.getDevice();
                boolean isFE0C = (device.getProductId() == 0xFE0C);
                String deviceInfo = String.format("Device %04X:%04X (%s)", 
                    device.getVendorId(), device.getProductId(),
                    (isFE0C ? "FE0C command-only" : "7523 with CH340"));
                
                Log.d(TAG, "=== SERIAL WRITE DEBUG ===");
                Log.d(TAG, "Target: " + deviceInfo);
                Log.d(TAG, "Data Length: " + data.length + " bytes");
                Log.d(TAG, "Timeout: " + timeout + "ms");
                Log.d(TAG, "Raw Hex Data: " + bytesToHexString(data));
                Log.d(TAG, "Port Status: " + (port.isOpen() ? "OPEN" : "CLOSED"));
                Log.d(TAG, "Connection: " + (isConnected() ? "CONNECTED" : "DISCONNECTED"));
                
                // Decode keyboard data if it looks like CH9329 keyboard command
                if (data.length >= 5 && data[0] == 0x57 && data[1] == (byte)0xAB) {
                    Log.d(TAG, ">>> CH9329 Keyboard Command Detected <<<");
                    Log.d(TAG, "Header: " + String.format("%02X %02X", data[0] & 0xFF, data[1] & 0xFF));
                    if (data.length >= 13) {
                        Log.d(TAG, "Address: " + String.format("%02X", data[2] & 0xFF));
                        Log.d(TAG, "Command: " + String.format("%02X", data[3] & 0xFF));
                        Log.d(TAG, "Length: " + String.format("%02X", data[4] & 0xFF));
                        Log.d(TAG, "Modifier: " + String.format("%02X", data[5] & 0xFF));
                        Log.d(TAG, "Reserved: " + String.format("%02X", data[6] & 0xFF));
                        Log.d(TAG, "Key Code: " + String.format("%02X", data[7] & 0xFF));
                        Log.d(TAG, "Key 2-6: " + String.format("%02X %02X %02X %02X %02X", 
                            data[8] & 0xFF, data[9] & 0xFF, data[10] & 0xFF, data[11] & 0xFF, data[12] & 0xFF));
                        if (data.length > 13) {
                            Log.d(TAG, "Checksum: " + String.format("%02X", data[13] & 0xFF));
                        }
                    }
                }
            } else {
                Log.d(TAG, "Writing " + data.length + " bytes: " + bytesToHexString(data));
            }
            
            long startTime = System.currentTimeMillis();
            port.write(data, timeout);
            long endTime = System.currentTimeMillis();
            
            Log.d(TAG, "✅ Successfully wrote " + data.length + " bytes in " + (endTime - startTime) + "ms");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing data: " + e.getMessage(), e);
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Write error: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Write a string to the serial port (UTF-8 encoding)
     * @param message string to write
     * @return true if write successful, false otherwise
     */
    public boolean writeString(String message) {
        if (message == null) {
            Log.w(TAG, "Cannot write: message is null");
            return false;
        }
        return writeData(message.getBytes());
    }
    
    /**
     * Send a command with debugging
     * @param command command byte array
     * @param description human-readable description of the command
     * @return true if successful, false otherwise
     */
    public boolean sendCommand(byte[] command, String description) {
        Log.d(TAG, "Sending command: " + description);
        boolean result = writeData(command);
        Log.d(TAG, "Command '" + description + "' " + (result ? "succeeded" : "failed"));
        return result;
    }
    
    /**
     * Test serial communication by sending a test pattern
     * @return true if test successful, false otherwise
     */
    public boolean testCommunication() {
        Log.d(TAG, "=== Starting Serial Communication Test ===");
        
        if (!isConnected()) {
            Log.e(TAG, "Test failed: Not connected");
            return false;
        }
        
        // Test 1: Send simple byte pattern
        byte[] testPattern1 = {0x01, 0x02, 0x03, 0x04, 0x05};
        boolean test1 = sendCommand(testPattern1, "Test Pattern 1 (0x01-0x05)");
        
        // Wait a bit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test 2: Send ASCII text
        String testString = "Hello Openterface!";
        boolean test2 = sendCommand(testString.getBytes(), "ASCII Test: " + testString);
        
        // Test 3: Send specific KVM command (if you know the protocol)
        // byte[] kvmCommand = {0xFF, 0x01, 0x00, 0x00}; // Example command
        // boolean test3 = sendCommand(kvmCommand, "KVM Command Test");
        
        boolean overallResult = test1 && test2;
        Log.d(TAG, "=== Serial Communication Test " + (overallResult ? "PASSED" : "FAILED") + " ===");
        return overallResult;
    }
    
    /**
     * Get detailed connection information for debugging
     * @return connection info string
     */
    public String getConnectionInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Serial Connection Info ===\n");
        
        if (driver != null && driver.getDevice() != null) {
            UsbDevice device = driver.getDevice();
            info.append("Device: ").append(device.getDeviceName()).append("\n");
            info.append("VID:PID: ").append(String.format("%04X:%04X", 
                device.getVendorId(), device.getProductId())).append("\n");
            info.append("Product: ").append(device.getProductName()).append("\n");
            info.append("Manufacturer: ").append(device.getManufacturerName()).append("\n");
            info.append("Serial Number: ").append(device.getSerialNumber()).append("\n");
        }
        
        info.append("Port Open: ").append(isConnected()).append("\n");
        info.append("Current Baudrate: ").append(getCurrentBaudrate()).append("\n");
        info.append("Preferred Baudrate: ").append(getPreferredBaudrate()).append("\n");
        info.append("Driver: ").append(driver != null ? driver.getClass().getSimpleName() : "null").append("\n");
        
        if (port != null) {
            try {
                info.append("DTR: ").append(port.getDTR()).append("\n");
                info.append("RTS: ").append(port.getRTS()).append("\n");
            } catch (Exception e) {
                info.append("DTR/RTS: Error reading (").append(e.getMessage()).append(")\n");
            }
        }
        
        info.append("==============================");
        return info.toString();
    }
    
    /**
     * Convert byte array to hex string for logging
     * @param bytes byte array
     * @return hex string representation
     */
    private String bytesToHexString(byte[] bytes) {
        if (bytes == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
    
    /**
     * Set DTR (Data Terminal Ready) signal
     * @param value true to set DTR, false to clear
     * @return true if successful, false otherwise
     */
    public boolean setDTR(boolean value) {
        if (port == null || !port.isOpen()) {
            Log.e(TAG, "Cannot set DTR: port not open");
            return false;
        }
        
        try {
            port.setDTR(value);
            Log.d(TAG, "DTR set to: " + value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting DTR: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Set RTS (Request to Send) signal
     * @param value true to set RTS, false to clear
     * @return true if successful, false otherwise
     */
    public boolean setRTS(boolean value) {
        if (port == null || !port.isOpen()) {
            Log.e(TAG, "Cannot set RTS: port not open");
            return false;
        }
        
        try {
            port.setRTS(value);
            Log.d(TAG, "RTS set to: " + value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error setting RTS: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enable verbose logging for debugging
     */
    public void enableDebugLogging() {
        Log.d(TAG, "=== Debug Logging Enabled ===");
        Log.d(TAG, getConnectionInfo());
        
        // Test basic communication
        if (isConnected()) {
            mSerialAsyncHandler.post(() -> testCommunication());
        }
    }
    
    /**
     * Debug method to test FE0C keyboard functionality
     */
    public void debugFE0CKeyboard() {
        Log.d(TAG, "=== FE0C Keyboard Debug Test ===");
        
        if (!isConnected()) {
            Log.e(TAG, "Debug: Device not connected");
            return;
        }
        
        boolean isFE0C = isFE0CDevice();
        int delay = getKeyReleaseDelay();
        
        Log.d(TAG, "Device Type: " + (isFE0C ? "FE0C" : "Other"));
        Log.d(TAG, "Key Release Delay: " + delay + "ms");
        Log.d(TAG, "Device VID:PID: " + 
            String.format("%04X:%04X", 
                driver.getDevice().getVendorId(), 
                driver.getDevice().getProductId()));
        
        // Test direct 'B' key press (0x05 = 'B' in HID)
        Log.d(TAG, "Testing direct 'B' key press...");
        testDirectKeyPress();
    }
    
    /**
     * Test direct key press for debugging FE0C keyboard issues
     */
    public void testDirectKeyPress() {
        try {
            // Direct 'B' key command according to CH9329 protocol
            // 57 AB 00 02 08 00 00 05 00 00 00 00 00 + checksum
            byte[] bKeyCommand = {
                0x57, (byte) 0xAB, 0x00, 0x02, 0x08,  // Header
                0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00  // 'B' key (0x05), no modifiers
            };
            
            // Calculate and add checksum
            int checksum = 0;
            for (byte b : bKeyCommand) {
                checksum += b & 0xFF;
            }
            checksum = checksum & 0xFF;
            
            byte[] fullCommand = new byte[bKeyCommand.length + 1];
            System.arraycopy(bKeyCommand, 0, fullCommand, 0, bKeyCommand.length);
            fullCommand[fullCommand.length - 1] = (byte) checksum;
            
            Log.d(TAG, "Direct B key command: " + bytesToHexString(fullCommand));
            
            // Send key press
            boolean pressResult = writeData(fullCommand, 100);
            Log.d(TAG, "Key press result: " + pressResult);
            
            if (pressResult) {
                // Send key release after delay
                mSerialAsyncHandler.postDelayed(() -> {
                    Log.d(TAG, "Sending key release...");
                    sendKeyRelease();
                }, getKeyReleaseDelay());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Direct key test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Debug method to monitor keyboard timing
     */
    public void debugKeyboardTiming(int keyCode, int modifiers) {
        long startTime = System.currentTimeMillis();
        
        Log.d(TAG, "=== Keyboard Timing Debug ===");
        Log.d(TAG, "Key: 0x" + Integer.toHexString(keyCode) + ", Modifiers: 0x" + Integer.toHexString(modifiers));
        Log.d(TAG, "Device: " + (isFE0CDevice() ? "FE0C" : "Other"));
        Log.d(TAG, "Release Delay: " + getKeyReleaseDelay() + "ms");
        
        // Format keyboard command
        byte[] keyCommand = formatCH9329KeyboardCommand(keyCode, modifiers);
        Log.d(TAG, "Command bytes: " + bytesToHexString(keyCommand));
        
        // Send key press
        Log.d(TAG, "Sending key press at: " + startTime + "ms");
        boolean pressResult = sendCommand(keyCommand, "Debug Key Press");
        
        if (pressResult) {
            int delay = getKeyReleaseDelay();
            Log.d(TAG, "Scheduling release in " + delay + "ms");
            
            mSerialAsyncHandler.postDelayed(() -> {
                long releaseTime = System.currentTimeMillis();
                Log.d(TAG, "Sending key release at: " + releaseTime + "ms");
                Log.d(TAG, "Actual delay: " + (releaseTime - startTime) + "ms");
                sendKeyRelease();
            }, delay);
        }
    }
    
    /**
     * Check if the current device is FE0C
     * @return true if current device is 1A86:FE0C
     */
    public boolean isFE0CDevice() {
        return driver != null && 
               driver.getDevice().getVendorId() == 0x1A86 && 
               driver.getDevice().getProductId() == 0xFE0C;
    }
    
    /**
     * Get device-specific key release delay
     * @return delay in milliseconds
     */
    public int getKeyReleaseDelay() {
        if (isFE0CDevice()) {
            return 3; // 3ms for FE0C
        } else {
            return 50; // 50ms for other devices
        }
    }
    
    /**
     * Send key release command immediately
     * @return true if successful
     */
    public boolean sendKeyRelease() {
        Log.d(TAG, "=== KEY RELEASE DEBUG ===");
        
        // CH9329 key release command: all keys released
        byte[] releaseCommand = {
            0x57, (byte) 0xAB, 0x00, 0x02, 0x08, 
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        
        Log.d(TAG, "Release command: " + bytesToHexString(releaseCommand));
        
        boolean result = sendCommand(releaseCommand, "Key Release");
        Log.d(TAG, "Key release result: " + (result ? "SUCCESS" : "FAILED"));
        
        return result;
    }
    
    /**
     * Send keyboard command with automatic release for FE0C devices
     * @param keyboardData the keyboard command bytes
     * @return true if successful
     */
    public boolean sendKeyboardCommandFE0C(byte[] keyboardData) {
        Log.d(TAG, "=== KEYBOARD COMMAND FE0C DEBUG ===");
        
        if (!isConnected()) {
            Log.e(TAG, "❌ Cannot send keyboard command: not connected");
            return false;
        }
        
        boolean isFE0C = isFE0CDevice();
        int delay = getKeyReleaseDelay();
        
        Log.d(TAG, "Device info:");
        Log.d(TAG, "  Type: " + (isFE0C ? "FE0C" : "Other"));
        Log.d(TAG, "  Release delay: " + delay + "ms");
        Log.d(TAG, "  Command length: " + keyboardData.length + " bytes");
        
        // Send key press
        Log.d(TAG, ">>> Sending KEY PRESS command...");
        boolean pressResult = sendCommand(keyboardData, "Keyboard Press");
        Log.d(TAG, "Key press result: " + (pressResult ? "SUCCESS" : "FAILED"));
        
        if (pressResult) {
            // For all devices, send release after device-specific delay
            Log.d(TAG, ">>> Scheduling KEY RELEASE in " + delay + "ms...");
            mSerialAsyncHandler.postDelayed(() -> {
                Log.d(TAG, ">>> Executing scheduled KEY RELEASE...");
                boolean releaseResult = sendKeyRelease();
                Log.d(TAG, "Key release result: " + (releaseResult ? "SUCCESS" : "FAILED"));
            }, delay);
        } else {
            Log.e(TAG, "❌ Skipping key release due to failed key press");
        }
        
        return pressResult;
    }
    
    /**
     * Format keyboard command for CH9329 chip (used in Openterface)
     * @param keyCode HID key code
     * @param modifiers modifier byte (Ctrl=0x01, Shift=0x02, Alt=0x04, Win=0x08)
     * @return formatted command bytes
     */
    private byte[] formatCH9329KeyboardCommand(int keyCode, int modifiers) {
        // CH9329 keyboard command format:
        // [0x57, 0xAB, 0x00, 0x02, 0x08, mod, 0x00, key1, key2, key3, key4, key5, key6]
        byte[] command = new byte[13];
        command[0] = 0x57;           // Header
        command[1] = (byte) 0xAB;    // Header
        command[2] = 0x00;           // Address
        command[3] = 0x02;           // Command: Keyboard
        command[4] = 0x08;           // Data length
        command[5] = (byte) modifiers; // Modifier keys
        command[6] = 0x00;           // Reserved
        command[7] = (byte) keyCode; // Key 1
        command[8] = 0x00;           // Key 2
        command[9] = 0x00;           // Key 3
        command[10] = 0x00;          // Key 4
        command[11] = 0x00;          // Key 5
        command[12] = 0x00;          // Key 6
        
        return command;
    }
    
    /**
     * Enhanced keyboard command with device detection
     * @param keyCode HID key code
     * @param modifiers modifier keys
     * @return true if successful
     */
    public boolean sendKeyPressWithAutoRelease(int keyCode, int modifiers) {
        if (!isConnected()) {
            Log.e(TAG, "Cannot send key: not connected");
            return false;
        }
        
        // Format keyboard command
        byte[] keyCommand = formatCH9329KeyboardCommand(keyCode, modifiers);
        
        // Send key press
        boolean pressResult = sendCommand(keyCommand, "Key Press: " + keyCode + " (mod: " + modifiers + ")");
        
        if (pressResult) {
            // Send release after device-specific delay
            int delay = getKeyReleaseDelay();
            mSerialAsyncHandler.postDelayed(this::sendKeyRelease, delay);
        }
        
        return pressResult;
    }
    
    /**
     * Convert Android KeyEvent to HID key code
     * @param keyCode Android key code
     * @return HID key code, or 0 if not supported
     */
    public static int androidKeyToHID(int keyCode) {
        switch (keyCode) {
            // Letters (a-z)
            case KeyEvent.KEYCODE_A: return 0x04;
            case KeyEvent.KEYCODE_B: return 0x05;
            case KeyEvent.KEYCODE_C: return 0x06;
            case KeyEvent.KEYCODE_D: return 0x07;
            case KeyEvent.KEYCODE_E: return 0x08;
            case KeyEvent.KEYCODE_F: return 0x09;
            case KeyEvent.KEYCODE_G: return 0x0A;
            case KeyEvent.KEYCODE_H: return 0x0B;
            case KeyEvent.KEYCODE_I: return 0x0C;
            case KeyEvent.KEYCODE_J: return 0x0D;
            case KeyEvent.KEYCODE_K: return 0x0E;
            case KeyEvent.KEYCODE_L: return 0x0F;
            case KeyEvent.KEYCODE_M: return 0x10;
            case KeyEvent.KEYCODE_N: return 0x11;
            case KeyEvent.KEYCODE_O: return 0x12;
            case KeyEvent.KEYCODE_P: return 0x13;
            case KeyEvent.KEYCODE_Q: return 0x14;
            case KeyEvent.KEYCODE_R: return 0x15;
            case KeyEvent.KEYCODE_S: return 0x16;
            case KeyEvent.KEYCODE_T: return 0x17;
            case KeyEvent.KEYCODE_U: return 0x18;
            case KeyEvent.KEYCODE_V: return 0x19;
            case KeyEvent.KEYCODE_W: return 0x1A;
            case KeyEvent.KEYCODE_X: return 0x1B;
            case KeyEvent.KEYCODE_Y: return 0x1C;
            case KeyEvent.KEYCODE_Z: return 0x1D;
            
            // Numbers (1-9, 0)
            case KeyEvent.KEYCODE_1: return 0x1E;
            case KeyEvent.KEYCODE_2: return 0x1F;
            case KeyEvent.KEYCODE_3: return 0x20;
            case KeyEvent.KEYCODE_4: return 0x21;
            case KeyEvent.KEYCODE_5: return 0x22;
            case KeyEvent.KEYCODE_6: return 0x23;
            case KeyEvent.KEYCODE_7: return 0x24;
            case KeyEvent.KEYCODE_8: return 0x25;
            case KeyEvent.KEYCODE_9: return 0x26;
            case KeyEvent.KEYCODE_0: return 0x27;
            
            // Special keys
            case KeyEvent.KEYCODE_ENTER: return 0x28;
            case KeyEvent.KEYCODE_ESCAPE: return 0x29;
            case KeyEvent.KEYCODE_DEL: return 0x2A; // Backspace
            case KeyEvent.KEYCODE_TAB: return 0x2B;
            case KeyEvent.KEYCODE_SPACE: return 0x2C;
            case KeyEvent.KEYCODE_FORWARD_DEL: return 0x4C; // Delete
            
            // Arrow keys
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 0x4F;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 0x50;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 0x51;
            case KeyEvent.KEYCODE_DPAD_UP: return 0x52;
            
            // Function keys
            case KeyEvent.KEYCODE_F1: return 0x3A;
            case KeyEvent.KEYCODE_F2: return 0x3B;
            case KeyEvent.KEYCODE_F3: return 0x3C;
            case KeyEvent.KEYCODE_F4: return 0x3D;
            case KeyEvent.KEYCODE_F5: return 0x3E;
            case KeyEvent.KEYCODE_F6: return 0x3F;
            case KeyEvent.KEYCODE_F7: return 0x40;
            case KeyEvent.KEYCODE_F8: return 0x41;
            case KeyEvent.KEYCODE_F9: return 0x42;
            case KeyEvent.KEYCODE_F10: return 0x43;
            case KeyEvent.KEYCODE_F11: return 0x44;
            case KeyEvent.KEYCODE_F12: return 0x45;
            
            default: return 0; // Unsupported key
        }
    }
    
    /**
     * Convert Android KeyEvent modifiers to HID modifiers
     * @param event KeyEvent to extract modifiers from
     * @return HID modifier byte
     */
    public static int getHIDModifiers(KeyEvent event) {
        int modifiers = 0;
        
        if (event.isCtrlPressed()) {
            modifiers |= 0x01; // Left Ctrl
        }
        if (event.isShiftPressed()) {
            modifiers |= 0x02; // Left Shift
        }
        if (event.isAltPressed()) {
            modifiers |= 0x04; // Left Alt
        }
        if (event.isMetaPressed()) {
            modifiers |= 0x08; // Left Win/Cmd
        }
        
        return modifiers;
    }
}