/**
* @Title: UsbDeviceManager
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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
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
    private int preferredBaudrate = DEFAULT_BAUDRATE; // User's preferred baudrate (-1 for auto)

    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;
    private static final String ACTION_USB_PERMISSION = "com.example.openterface.USB_PERMISSION";
    
    // Baudrate constants - similar to C++ version
    private static final int BAUDRATE_HIGHSPEED = 115200;
    private static final int BAUDRATE_LOWSPEED = 9600;
    private static final int DEFAULT_BAUDRATE = BAUDRATE_HIGHSPEED; // Try high speed first
    private UsbDeviceManager usbDeviceManager;

    public interface OnDataReadListener {
        void onDataRead();
    }

    private OnDataReadListener onDataReadListener;

    public void setOnDataReadListener(OnDataReadListener listener) {
        this.onDataReadListener = listener;
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
     * Attempt to configure serial port with specified baudrate
     * @param connection the USB device connection
     * @param baudrate the baudrate to try
     * @return true if successful, false if failed
     */
    private boolean trySerialConfiguration(UsbDeviceConnection connection, int baudrate) {
        try {
            port = driver.getPorts().get(0); // Most devices have just one port (port 0)
            port.open(connection);
            port.setParameters(baudrate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            currentBaudrate = baudrate; // Track the working baudrate
            Log.d(TAG, "Serial port configured successfully with baudrate: " + baudrate);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure serial port with baudrate " + baudrate + ": " + e.getMessage());
            // Close port if it was opened but configuration failed
            if (port != null && port.isOpen()) {
                try {
                    port.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Failed to close port after configuration error", closeException);
                }
            }
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
            Log.d(TAG, "onReceive data successful");
            init();
//            handleUsbDevice(intent);
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
        mSerialAsyncHandler.post(() -> {
            ProbeTable customTable = new ProbeTable();
            customTable.addProduct(0x1A86, 0x7523, Ch34xSerialDriver.class);
            UsbSerialProber prober = new UsbSerialProber(customTable);
            List<UsbSerialDriver> availableDrivers = prober.findAllDrivers(usbManager);
            if (availableDrivers.isEmpty()) {
                return;
            }
            driver = availableDrivers.get(0);
            Log.d("serial", "find available drivers:" + driver.getDevice());
            requestUsbPermission(driver.getDevice());
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
        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        usbManager.requestPermission(serialDevice, permissionIntent);
        Log.e(TAG, "requestUsbPermission serialDevice");

        UsbDeviceConnection connection = usbManager.openDevice(serialDevice);
        Log.d("serial", "open port successful11 ");
        if (connection != null) {
            Log.d("serial", "open port successful22 ");
            
            // Implement baudrate switching logic similar to C++ version
            // Use preferred baudrate if set, otherwise try DEFAULT_BAUDRATE (115200) first
            int tryBaudrate;
            if (preferredBaudrate == -1) {
                // Auto mode: try DEFAULT_BAUDRATE first, then fallback
                tryBaudrate = DEFAULT_BAUDRATE;
            } else {
                // Use user's preferred baudrate
                tryBaudrate = preferredBaudrate;
            }
            
            boolean connectionSuccessful = false;
            int workingBaudrate = tryBaudrate;
            final int maxRetries = (preferredBaudrate == -1) ? 2 : 1; // Auto mode tries both, manual mode tries only preferred
            int retryCount = 0;
            
            while (retryCount < maxRetries && !connectionSuccessful) {
                Log.d(TAG, "Attempting serial connection with baudrate: " + tryBaudrate + " (attempt " + (retryCount + 1) + ")");
                
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
                    }
                    retryCount++;
                }
            }
            
            if (connectionSuccessful) {
                Log.d("serial", "open port successful33 with baudrate: " + workingBaudrate);
                startReading();
            } else {
                Log.e(TAG, "Failed to establish serial connection with any baudrate");
                // Close the USB connection if we couldn't establish serial communication
                connection.close();
            }
        } else {
            Log.e(TAG, "Failed to open serialDevice");
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
                            onDataReadListener.onDataRead();
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
            } catch (Exception e) {
                Log.e(TAG, "Error closing port for reconnection", e);
            }
        }
        
        // Set the preferred baudrate and reinitialize
        setPreferredBaudrate(baudrate);
        init();
        return true;
    }
}