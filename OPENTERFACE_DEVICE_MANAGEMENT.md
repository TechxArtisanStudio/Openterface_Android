# Openterface Device Management Implementation

This implementation provides VID/PID-based device detection and management for **Openterface Mini-KVM devices only**.

## 🎯 Supported Devices

### Serial Interfaces
- **0x1A86:0x7523** - Openterface Mini-KVM v1 Serial
- **0x1A86:0xFE0C** - Openterface Mini-KVM v2 Serial

### UVC Camera + HID Interfaces
- **0x534D:0x2109** - Openterface Mini-KVM v1 (UVC + HID)
- **0x345F:0x2132** - Openterface Mini-KVM v2 (UVC + HID)

## 📁 Files Created/Modified

### 1. Updated Serial Manager
- **File**: `app/src/main/java/com/openterface/AOS/serial/UsbDeviceManager.java`
- **Changes**: Added support for both Openterface serial devices (0x1A86:0x7523 and 0x1A86:0xFE0C)

### 2. New UVC Manager
- **File**: `app/src/main/java/com/openterface/AOS/uvc/OpenterfaceUVCManager.java`
- **Purpose**: Manages UVC camera devices (0x534D:0x2109, 0x345F:0x2132)
- **Features**: Device detection, connection management, event callbacks

### 3. New HID Manager
- **File**: `app/src/main/java/com/openterface/AOS/hid/OpenterfaceHIDManager.java`
- **Purpose**: Manages HID interfaces (0x534D:0x2109, 0x345F:0x2132)
- **Features**: Device detection, basic HID framework, native method stubs

### 4. Unified Device Manager
- **File**: `app/src/main/java/com/openterface/AOS/device/OpenterfaceDeviceManager.java`
- **Purpose**: Coordinates all three interfaces (Serial, UVC, HID)
- **Features**: Connection status tracking, unified event handling, device identification

### 5. Usage Example
- **File**: `app/src/main/java/com/openterface/AOS/example/OpenterfaceDeviceExample.java`
- **Purpose**: Shows how to use the unified device manager

## 🚀 Quick Start

```java
public class MainActivity extends AppCompatActivity {
    private OpenterfaceDeviceManager deviceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize device manager for Openterface devices only
        deviceManager = new OpenterfaceDeviceManager(this);
        
        // Set up event listener
        deviceManager.setOnOpenterfaceDeviceListener(new OpenterfaceDeviceManager.OnOpenterfaceDeviceListener() {
            @Override
            public void onSerialConnected() {
                Log.d("MainActivity", "Serial connected!");
            }
            
            @Override
            public void onUVCConnected(UsbDevice device) {
                Log.d("MainActivity", "UVC camera connected!");
            }
            
            @Override
            public void onHIDConnected(UsbDevice device) {
                Log.d("MainActivity", "HID interface connected!");
            }
            
            @Override
            public void onConnectionStatusChanged(OpenterfaceDeviceManager.ConnectionStatus status) {
                if (status == OpenterfaceDeviceManager.ConnectionStatus.FULLY_CONNECTED) {
                    Log.d("MainActivity", "All interfaces ready!");
                }
            }
        });
        
        // Start device detection
        deviceManager.initialize();
    }
    
    @Override
    protected void onDestroy() {
        if (deviceManager != null) {
            deviceManager.release();
        }
        super.onDestroy();
    }
}
```

## 🔧 Key Features

### 1. **Openterface-Only Focus**
- Only detects and manages Openterface Mini-KVM devices
- No support for other USB devices (as requested)

### 2. **Composite Device Handling**
- Properly manages devices where one VID/PID handles UVC+HID
- Another VID/PID handles Serial interface
- Tracks individual interface connection status

### 3. **Connection Status Tracking**
- `DISCONNECTED` - No interfaces connected
- `SERIAL_ONLY` - Only serial interface connected
- `UVC_ONLY` - Only UVC camera connected
- `HID_ONLY` - Only HID interface connected
- `PARTIAL_CONNECTED` - Some interfaces connected
- `FULLY_CONNECTED` - All three interfaces connected

### 4. **Easy Access to Individual Managers**
```java
// Access serial manager
UsbDeviceManager serial = deviceManager.getSerialManager();

// Access UVC manager
OpenterfaceUVCManager uvc = deviceManager.getUVCManager();

// Access HID manager
OpenterfaceHIDManager hid = deviceManager.getHIDManager();
```

### 5. **Device Identification**
```java
// Get device name by VID/PID
String name = deviceManager.getDeviceName(0x534D, 0x2109);
// Returns: "Openterface Mini-KVM v1"

// Find device info
OpenterfaceDeviceManager.OpenterfaceDevice device = 
    deviceManager.findDevice(0x1A86, 0x7523);
```

## 🔍 Implementation Notes

### Serial Interface
- Uses existing `UsbDeviceManager` with CH340/CH341 driver support
- Supports both baudrates: 115200 and 9600
- Auto-detection and fallback between baudrates

### UVC Camera Interface
- Uses existing libuvccamera framework
- Device detection and permission handling
- Ready for UVC control integration (requires native handle)

### HID Interface
- Framework for HID communication
- Native method stubs for future libusb integration
- Device detection and basic Java-level HID handling

## 🎯 Benefits

1. **Clean Architecture**: Modular design with separate managers
2. **Easy Extension**: Simple to add new Openterface device VID/PIDs
3. **Unified Interface**: Single entry point for all device management
4. **Event-Driven**: Real-time connection status updates
5. **Thread-Safe**: Proper handling of USB events and callbacks
6. **Build Success**: ✅ All code compiles successfully

## 📝 Future Enhancements

1. **Native HID Integration**: Implement JNI bindings for libusb HID functions
2. **UVC Control**: Add native UVC device handle integration
3. **Data Transfer**: Implement actual data communication methods
4. **More Devices**: Easy to add new Openterface device VID/PIDs

This implementation provides a solid foundation for Openterface device management with proper VID/PID filtering and modular architecture.