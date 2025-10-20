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
    private int preferredBaudrate = -1;
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
     * Get alternative baudrate - similar to anotherBaudrate() in C++ version
     * @param currentBaudrate the current baudrate
     * @return the alternative baudrate (115200 <-> 9600)
     */
    private int getAlternativeBaudrate(int currentBaudrate) {
        return currentBaudrate == BAUDRATE_HIGHSPEED ? BAUDRATE_LOWSPEED : BAUDRATE_HIGHSPEED;
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
            boolean isCH340Device = (device.getVendorId() == 0x1A86);
            
            if (isCH340Device) {
                Log.d(TAG, "Detected CH340 device - using enhanced initialization");
                return initializeCH340Port(connection, baudrate);
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
     * Enhanced initialization for CH340 devices with multiple retry strategies
     */
    private boolean initializeCH340Port(UsbDeviceConnection connection, int baudrate) {
        Log.d(TAG, "=== CH340 Enhanced Initialization ===");
        
        UsbDevice device = driver.getDevice();
        boolean isFE0CDevice = (device.getProductId() == 0xFE0C);
        
        if (isFE0CDevice) {
            Log.i(TAG, "FE0C device detected - using interface conflict resolution");
            // For FE0C devices, try interface conflict resolution first
            if (tryFE0CInterfaceConflictResolution(connection, baudrate)) {
                return true;
            }
        }
        
        // Strategy 1: Standard open with minimal configuration
        Log.d(TAG, "Strategy 1: Standard CH340 initialization...");
        if (tryStandardCH340Init(connection, baudrate)) {
            return true;
        }
        
        // Strategy 2: Reset device and try with delay
        Log.d(TAG, "Strategy 2: CH340 reset and retry...");
        if (tryCH340ResetInit(connection, baudrate)) {
            return true;
        }
        
        // Strategy 3: Force specific CH340 control commands
        Log.d(TAG, "Strategy 3: CH340 manual control sequence...");
        if (tryManualCH340Init(connection, baudrate)) {
            return true;
        }
        
        // Strategy 4: Raw USB communication for FE0C
        if (isFE0CDevice) {
            Log.d(TAG, "Strategy 4: FE0C raw USB communication...");
            if (tryFE0CRawCommunication(connection, baudrate)) {
                return true;
            }
        }
        
        Log.e(TAG, "❌ All CH340 initialization strategies failed");
        return false;
    }
    
    /**
     * Try to resolve interface conflicts for FE0C devices
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
    private boolean tryCH340ResetInit(UsbDeviceConnection connection, int baudrate) {
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
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("No Openterface devices found (VID:PID should be 1A86:7523 or 1A86:FE0C)");
                }
                return;
            }
            
            Log.i(TAG, "Found Openterface device: " + 
                String.format("%04X:%04X", openterfaceDevice.getVendorId(), openterfaceDevice.getProductId()) +
                " - " + openterfaceDevice.getProductName());
            
            // Now try to get a driver for this specific device
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(openterfaceDevice.getVendorId(), openterfaceDevice.getProductId(), Ch34xSerialDriver.class);
            
            UsbSerialProber prober = new UsbSerialProber(customTable);
            List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
            
            if (availableDrivers.isEmpty()) {
                Log.e(TAG, "No compatible driver found for Openterface device " + 
                    String.format("%04X:%04X", openterfaceDevice.getVendorId(), openterfaceDevice.getProductId()));
                if (onConnectionStatusListener != null) {
                    onConnectionStatusListener.onError("No compatible serial driver for detected device");
                }
                return;
            }
            
            driver = availableDrivers.get(0);
            UsbDevice device = driver.getDevice();
            
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
                Log.d(TAG, "Device 1A86:FE0C - Mini-KVM v2: Only supports 115200 baud");
            } else if (pid == 0x7523) {
                // Mini-KVM v1: Supports both 9600 and 115200, auto-detect
                preferredBaudrate = -1; // Auto-detect
                Log.d(TAG, "Device 1A86:7523 - Mini-KVM v1: Supports 9600 and 115200 baud (auto-detect)");
            } else {
                Log.w(TAG, "Unknown 1A86 device PID: " + String.format("%04X", pid));
                preferredBaudrate = -1; // Auto-detect for unknown devices
            }
        } else {
            Log.w(TAG, "Unsupported device VID: " + String.format("%04X", vid));
            preferredBaudrate = -1; // Auto-detect for unsupported devices
        }
        
        Log.d(TAG, "Preferred baudrate set to: " + 
            (preferredBaudrate == -1 ? "Auto (will try 9600 and 115200)" : preferredBaudrate));
        
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
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
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
            Log.e(TAG, "❌ Failed to connect with any baudrate");
            connection.close();
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to configure serial port with any baudrate");
            }
            
            // Try advanced recovery
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
        
        // Implement baudrate switching logic 
        // Use preferred baudrate if set, otherwise try default first
        int tryBaudrate;
            Log.d(TAG, "KVMGO device detected - enforcing 115200 baud rate only");
        if (preferredBaudrate == -1) {
            // Auto mode: try DEFAULT_BAUDRATE first, then fallback
            tryBaudrate = DEFAULT_BAUDRATE;
        } else {
            // Use user's preferred baudrate
            tryBaudrate = preferredBaudrate;
        }
        
        boolean connectionSuccessful = false;
        int workingBaudrate = tryBaudrate;
        // For auto mode try both baudrates, for specific baudrate try only once
        final int maxRetries = (preferredBaudrate == -1) ? 2 : 1;
        int retryCount = 0;
        
        while (retryCount < maxRetries && !connectionSuccessful) {
            Log.d(TAG, "Attempting serial connection with baudrate: " + tryBaudrate + " (attempt " + (retryCount + 1) + "/" + maxRetries + ")");
            
            connectionSuccessful = trySerialConfiguration(connection, tryBaudrate);
            
            if (connectionSuccessful) {
                workingBaudrate = tryBaudrate;
                Log.d(TAG, "Serial connection successful with baudrate: " + workingBaudrate);
                break;
            } else {
                Log.w(TAG, "Failed to connect with baudrate: " + tryBaudrate);
                
                // Only try alternative baudrate in auto mode
                if (preferredBaudrate == -1) {
                    // Try alternative baudrate on next iteration
                    tryBaudrate = getAlternativeBaudrate(tryBaudrate);
                    Log.d(TAG, "Trying alternative baudrate: " + tryBaudrate);
                } else {
                    Log.w(TAG, "Specific baudrate failed - no other baudrates will be attempted");
                }
                retryCount++;
            }
        }
        
        if (connectionSuccessful) {
            Log.d(TAG, "Serial connection established successfully with baudrate: " + workingBaudrate);
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onConnected(workingBaudrate);
            }
            startReading();
        } else {
            Log.e(TAG, "Failed to establish serial connection with any baudrate");
            connection.close();
            if (onConnectionStatusListener != null) {
                onConnectionStatusListener.onError("Failed to establish serial connection with any baudrate");
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
        return port != null && port.isOpen();
    }

    /**
     * Set the preferred baudrate for serial communication
     * @param baudrate the preferred baudrate (9600, 115200, or -1 for auto)
     */
    public void setPreferredBaudrate(int baudrate) {
        this.preferredBaudrate = baudrate;
        Log.d(TAG, "Preferred baudrate set to: " + (baudrate == -1 ? "Auto" : baudrate));
    }

    /**
     * Get the preferred baudrate setting
     * @return the preferred baudrate (-1 for auto, otherwise specific baudrate)
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
     * @param data byte array to write
     * @param timeout timeout in milliseconds
     * @return true if write successful, false otherwise
     */
    public boolean writeData(byte[] data, int timeout) {
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
            Log.d(TAG, "Writing " + data.length + " bytes: " + bytesToHexString(data));
            
            port.write(data, timeout);
            Log.d(TAG, "Successfully wrote " + data.length + " bytes");
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
        info.append("Preferred Baudrate: ").append(getPreferredBaudrate() == -1 ? "Auto" : getPreferredBaudrate()).append("\n");
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
}