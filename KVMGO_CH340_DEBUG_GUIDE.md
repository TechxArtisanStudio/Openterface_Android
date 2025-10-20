# KVMGO CH340 Device Testing Guide

This guide explains how to test the KVMGO device with CH340 chipset (VID: 0x1A86, PID: 0xFE0C) with the updated code that supports proper baudrate detection.

## Setup

1. Make sure you have built the latest app version with the updated USB device detection code:
   ```
   ./gradlew assembleDebug
   ```

2. Install the app on your Android device:
   ```
   adb install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk
   ```

## Testing Steps

1. Clear the Android logs:
   ```
   adb logcat -c
   ```

2. Start monitoring logs with specific filters for our USB detection:
   ```
   adb logcat -v time UsbDeviceManager:D *:S
   ```
   
   Alternative filtering for broader USB-related logs:
   ```
   adb logcat -v time | findstr "UsbDeviceManager|KVMGO|CH34x|FE0C|baudrate|detectOpenterface"
   ```

3. Connect the KVMGO device with CH340 chipset (VID: 0x1A86, PID: 0xFE0C) to the Android device.

4. Launch the app if not already running:
   ```
   adb shell am start -n com.openterface.AOS/.activity.MainActivity
   ```

5. Observe the logs to verify that:
   - The device is detected as a KVMGO device
   - The baudrate is set to 115200 only (no attempts at 9600)
   - Special initialization for CH340 is applied
   - Connection is successful

## Expected Log Messages

When successful, you should see log messages similar to:

```
UsbDeviceManager: Supported KVMGO devices (115200 baudrate only):
UsbDeviceManager:   - 345F:2130/2132 (KVMGO UVC/HID)
UsbDeviceManager:   - 2109:8110/7523 (KVMGO Serial)
UsbDeviceManager:   - 1A86:FE0C (KVMGO CH340 Serial)
UsbDeviceManager: ⭐ MATCH: Found KVMGO device 1A86:FE0C
UsbDeviceManager: KVMGO device detected - enforcing 115200 baud rate only
UsbDeviceManager: Applying special initialization for CH34x KVMGO device
UsbDeviceManager: Serial connection established successfully with baudrate: 115200
```

## Troubleshooting

If the connection fails, check the logs for:

1. Device detection issues:
   - Is the device detected at all?
   - Is it recognized as a KVMGO device?

2. Driver issues:
   - Is the correct driver (Ch34xSerialDriver) being used?
   - Are there any driver initialization errors?

3. Baudrate issues:
   - Is the code trying to use baudrates other than 115200?
   - Are there any errors during baudrate configuration?

4. CH340 initialization:
   - Is the special initialization for CH340 being applied?

## Collecting Data for Further Analysis

If issues persist, collect full logs for analysis:

```
adb logcat -v time > kvmgo_ch340_debug.log
```
