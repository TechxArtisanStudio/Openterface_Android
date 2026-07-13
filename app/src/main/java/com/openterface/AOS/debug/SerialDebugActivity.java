/**
 * @Title: SerialDebugActivity
 * @Package com.openterface.AOS.debug
 * @Description: Activity for debugging serial communication issues
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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.openterface.AOS.R;
import com.openterface.AOS.activity.BaseActivity;
import com.openterface.AOS.serial.UsbDeviceManager;

import java.util.HashMap;

public class SerialDebugActivity extends BaseActivity {
    private static final String TAG = "OP-DEBUG";
    private static final String ACTION_USB_PERMISSION = "com.openterface.AOS.USB_PERMISSION";
    
    private UsbDeviceManager serialManager;
    private UsbManager usbManager;
    private TextView statusText;
    private TextView logText;
    private EditText sendDataEdit;
    private ScrollView logScrollView;
    private Handler mainHandler;
    
    private final StringBuilder logBuffer = new StringBuilder();
    
    // USB permission broadcast receiver
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            log("✅ USB permission granted for device: " + device.getDeviceName());
                            // Now we can run diagnostics safely
                            runInitialDiagnostics();
                        }
                    } else {
                        log("❌ USB permission denied for device: " + (device != null ? device.getDeviceName() : "unknown"));
                    }
                }
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_debug);
        
        mainHandler = new Handler(Looper.getMainLooper());
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
        // Register USB permission receiver
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, filter);
        }
        
        initViews();
        initSerialManager();
        
        // Request USB permissions first, then run diagnostics
        requestUsbPermissionsForOpenterfaceDevices();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        logText = findViewById(R.id.logText);
        sendDataEdit = findViewById(R.id.sendDataEdit);
        logScrollView = findViewById(R.id.logScrollView);
        
        Button connectBtn = findViewById(R.id.connectBtn);
        Button diagnosticsBtn = findViewById(R.id.diagnosticsBtn);
        Button testCommBtn = findViewById(R.id.testCommBtn);
        Button sendBtn = findViewById(R.id.sendBtn);
        Button clearLogBtn = findViewById(R.id.clearLogBtn);
        
        connectBtn.setOnClickListener(v -> connectToDevice());
        diagnosticsBtn.setOnClickListener(v -> runDiagnostics());
        testCommBtn.setOnClickListener(v -> runCommunicationTest());
        sendBtn.setOnClickListener(v -> sendCustomData());
        clearLogBtn.setOnClickListener(v -> clearLog());
        
        updateStatus(getString(R.string.loading));
        sendDataEdit.setText(R.string.hello_openterface);
    }
    
    private void initSerialManager() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        serialManager = new UsbDeviceManager(this, usbManager);
        
        // Set up connection status listener
        serialManager.setOnConnectionStatusListener(new UsbDeviceManager.OnConnectionStatusListener() {
            @Override
            public void onConnected(int baudrate) {
                runOnUiThread(() -> {
                    updateStatus("✅ Connected");
                    appendLog(getString(R.string.serial_port_connected_successfully, baudrate));
                });
            }
            
            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    updateStatus("❌ Disconnected");
                    appendLog(getString(R.string.serial_port_disconnected));
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateStatus("❌ Connection Failed");
                    appendLog(getString(R.string.connection_failed, error));
                });
            }
        });
        
        // Set up data read listener
        serialManager.setOnDataReadListener(new UsbDeviceManager.OnDataReadListener() {
            @Override
            public void onDataRead(byte[] data, int length) {
                String dataStr = getString(R.string.received_bytes, length);
                StringBuilder hex = new StringBuilder();
                for (int i = 0; i < length && i < data.length; i++) {
                    hex.append(String.format("%02X ", data[i]));
                }
                final String finalDataStr = dataStr + hex.toString();
                
                runOnUiThread(() -> appendLog("📥 " + finalDataStr));
            }
        });
        
        // Enable detailed logging
        serialManager.enableDebugLogging();
    }
    
    private void runInitialDiagnostics() {
        appendLog(getString(R.string.initial_diagnostics));
        SerialDebugUtils.runDiagnostics(this, serialManager);
        SerialDebugUtils.recommendSettings(this);
        appendLog(getString(R.string.diagnostics_complete));
    }
    
    private void connectToDevice() {
        appendLog(getString(R.string.connecting_to_device));
        updateStatus("🔄 Connecting...");

        // Try to connect in background thread
        // SerialDebugActivity is serial-only (no camera), so detect + connect immediately
        new Thread(() -> {
            serialManager.detectDeviceOnly();
            serialManager.connect();
        }).start();
    }
    
    private void runDiagnostics() {
        appendLog("\n=== RUNNING DIAGNOSTICS ===");
        SerialDebugUtils.runDiagnostics(this, serialManager);
        appendLog(getString(R.string.diagnostics_complete));
    }
    
    private void runCommunicationTest() {
        if (!serialManager.isConnected()) {
            appendLog("❌ Cannot run test: Not connected");
            return;
        }
        
        appendLog(getString(R.string.communication_test));
        SerialDebugUtils.testSerialCommunication(serialManager);
        appendLog(getString(R.string.test_complete));
    }
    
    private void sendCustomData() {
        if (!serialManager.isConnected()) {
            appendLog("❌ Cannot send: Not connected");
            return;
        }
        
        String dataStr = sendDataEdit.getText().toString();
        if (dataStr.isEmpty()) {
            appendLog(getString(R.string.no_data_to_send));
            return;
        }
        
        byte[] data = dataStr.getBytes();
        appendLog(getString(R.string.sending_data, dataStr, String.valueOf(data.length)));
        
        boolean success = serialManager.sendCommand(data, "Manual send");
        
        if (success) {
            appendLog("✅ Data sent successfully");
        } else {
            appendLog("❌ Failed to send data");
        }
    }
    
    private void clearLog() {
        logBuffer.setLength(0);
        logText.setText("");
    }
    
    private void updateStatus(String status) {
        statusText.setText(getString(R.string.status_label, status));
    }
    
    private void appendLog(String message) {
        Log.d(TAG, message);
        
        mainHandler.post(() -> {
            String timestamp = String.format("%tT", System.currentTimeMillis());
            logBuffer.append("[").append(timestamp).append("] ").append(message).append("\n");
            
            // Keep log buffer reasonable size
            if (logBuffer.length() > 50000) {
                logBuffer.delete(0, 10000); // Remove first 10k characters
            }
            
            logText.setText(logBuffer.toString());
            
            // Auto-scroll to bottom
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
    
    private void requestUsbPermissionsForOpenterfaceDevices() {
        if (usbManager == null) {
            log("❌ USB Manager is null");
            runInitialDiagnostics(); // Run anyway to show the issue
            return;
        }
        
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        boolean foundOpenterfaceDevice = false;
        boolean needsPermission = false;
        
        for (UsbDevice device : deviceList.values()) {
            if (isOpenterfaceDevice(device)) {
                foundOpenterfaceDevice = true;
                if (!usbManager.hasPermission(device)) {
                    needsPermission = true;
                    log("📋 Requesting permission for Openterface device: " + 
                        String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                    
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        this, 0, new Intent(ACTION_USB_PERMISSION), 
                        PendingIntent.FLAG_IMMUTABLE);
                    usbManager.requestPermission(device, permissionIntent);
                } else {
                    log("✅ Already have permission for Openterface device: " + 
                        String.format("%04X:%04X", device.getVendorId(), device.getProductId()));
                }
            }
        }
        
        if (!foundOpenterfaceDevice) {
            log("⚠️ No Openterface devices detected, running diagnostics anyway");
            runInitialDiagnostics();
        } else if (!needsPermission) {
            log("✅ All Openterface devices have permission, running diagnostics");
            runInitialDiagnostics();
        } else {
            log("⏳ Waiting for USB permission...");
        }
    }
    
    private boolean isOpenterfaceDevice(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();
        
        // Known Openterface VID/PID combinations
        return (vid == 0x1A86 && (pid == 0x7523 || pid == 0xFE0C)) ||  // Serial
               (vid == 0x534D && pid == 0x2109) ||  // UVC/HID v1
               (vid == 0x345F && pid == 0x2132);    // UVC/HID v2
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        appendLog(message);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(usbReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
        if (serialManager != null) {
            serialManager.release();
        }
    }
}