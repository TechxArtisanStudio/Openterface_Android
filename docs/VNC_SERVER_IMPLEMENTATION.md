# VNC Server Implementation Documentation

## Overview

This document describes the VNC server implementation added to the Openterface Android KVM app. The VNC server captures UVC camera frames from the target device and streams them to remote VNC clients. Keyboard and mouse input from VNC clients is routed through the existing USB serial (CH9329) HID path to the target device.

**Status**: Phase 1 complete (VNC). Phase 2 (WebRTC) complete - see [WebRTC Server Documentation](WEBRTC_SERVER.md).

---

## Architecture

```
┌─────────────┐     RFB      ┌─────────────────────────────────────────┐
│  VNC Client │◄────────────►│  Phone (Openterface App)                │
│             │   Video      │                                         │
│             │◄─────────────│  ┌─────────────┐  ┌──────────────────┐  │
│             │   Input      │  │  UVC Camera │  │  VNC Server      │  │
│             │   Events     │  │  (HDMI In)  │  │  (libvncserver)  │  │
└─────────────┘              │  └──────┬──────┘  └────────┬─────────┘  │
                             │         │                  │            │
                             │         ▼                  ▼            │
                             │  ┌─────────────┐  ┌──────────────────┐  │
                             │  │FrameCapture │  │  Input Handler   │  │
                             │  │(IFrameCallback)                  │  │
                             │  └─────────────┘  └────────┬─────────┘  │
                             │                            │            │
                             │                            ▼            │
                             │  ┌─────────────────────────────────────┐ │
                             │  │  CH9329 USB Serial (HID to Target) │ │
                             │  └─────────────────────────────────────┘ │
                             └─────────────────────────────────────────┘
```

---

## File Structure

### Native Layer (libvncserver)

| File | Description |
|------|-------------|
| `libvncserver/` | Git submodule with libvncserver source |
| `libvncserver/src/main/jni/libvnc/` | libvncserver source code |
| `libvncserver/src/main/jni/libvnc/include/rfb/rfbconfig.h` | Generated config header (cmake→Android.mk) |
| `libvncserver/src/main/jni/Android.mk` | Top-level NDK build entry point |
| `libvncserver/src/main/jni/Application.mk` | NDK platform/ABI config (android-21, arm64-v8a etc.) |
| `libvncserver/src/main/jni/vnc_server/Android.mk` | Builds `vncserver.so` with libvncserver + JNI sources |
| `libvncserver/src/main/jni/vnc_server/vnc_server_jni.c` | JNI wrapper bridging Java ↔ libvncserver |

### Java Layer

| File | Description |
|------|-------------|
| `libvncserver/src/main/java/com/openterface/AOS/vnc/VncServerNative.java` | JNI method declarations |
| `libvncserver/src/main/java/com/openterface/AOS/vnc/VncServerCallback.java` | Callback interface for VNC events |
| `app/src/main/java/com/openterface/AOS/vnc/VncServerService.java` | Foreground service managing VNC lifecycle |
| `app/src/main/java/com/openterface/AOS/vnc/VncFrameCapture.java` | Bridges UVC IFrameCallback to VNC server |
| `app/src/main/java/com/openterface/AOS/vnc/VncKeyMap.java` | Maps VNC keysyms to CH9329 key names |
| `app/src/main/java/com/openterface/AOS/vnc/VncServerConfig.java` | SharedPreferences persistence for VNC settings |
| `app/src/main/java/com/openterface/AOS/fragment/VncServerSettingsDialogFragment.java` | Settings dialog UI |
| `app/src/main/res/layout/dialog_vnc_server_settings.xml` | Dialog layout |
| `app/src/main/res/drawable/baseline_cast_24.xml` | VNC icon (cast icon) |

### Integration Points

| File | Changes |
|------|---------|
| `app/src/main/java/com/openterface/AOS/activity/MainActivity.java` | VNC service binding, frame capture, input routing |
| `app/src/main/java/com/openterface/AOS/drawerLayout/DrawerLayoutDeal.java` | VNC button handler |
| `app/src/main/AndroidManifest.xml` | Added INTERNET, FOREGROUND_SERVICE, POST_NOTIFICATIONS permissions + service declaration |
| `app/build.gradle` | Added `project(':libvncserver')` dependency |
| `settings.gradle` | Added `include ':libvncserver'` |
| `app/src/main/java/com/openterface/AOS/target/KeyBoardManager.java` | Fixed Handler Looper for non-main thread calls |

---

## Configuration

VNC server settings are persisted in SharedPreferences via `VncServerConfig`:

| Setting | Default | Description |
|---------|---------|-------------|
| Port | 5900 | VNC server listen port |
| Password | "" (empty) | VNC authentication password (max 8 chars) |
| Bind Address | "" (all interfaces) | Network interface to bind to |
| Encoding | Tight | Video encoding method (Tight recommended) |
| Quality Level | 5 (Medium) | JPEG quality for Tight encoding (0-100). Controls image quality vs bandwidth tradeoff |
| Compression Level | 2 (Medium) | zlib compression level for Tight encoding (0-3). Controls CPU vs bandwidth tradeoff |
| Auto-start | false | Automatically start VNC server when camera connects |

### Quality and Compression (Tight Encoding)

The Tight encoding uses two independent compression parameters:

- **Quality Level (`turboQualityLevel`)**: JPEG quality factor, range 0-100. Higher values produce better image quality but use more bandwidth. Mapped to libjpeg-turbo's quality setting.
- **Compression Level (`tightCompressLevel`)**: zlib compression level, range 0-3 internally. The `tightConf[]` array in libvncserver's `tight.c` only has 4 entries (indices 0-3). Higher values compress more aggressively but use more CPU.

**Important:** `TIGHT_DEFAULT_COMPRESSION` is defined as 6 in libvncserver, which is out-of-bounds for the `tightConf[]` array. The app clamps any compress level > 3 down to 3 at two levels:
1. JNI entry point (`vnc_server_jni.c`)
2. SetEncodings handler (`rfbserver.c`) — after client negotiations

Server-forced settings are applied via `forceQualityLevel` and `forceCompressLevel` fields in the `rfbScreenInfo` struct. These override any client-preferred values during the SetEncodings negotiation phase. Settings are only applied at server startup and cannot be changed while the server is running.

---

## Configuration Details

---

## Key Implementation Details

### 1. libvncserver Build Configuration

- **Excluded encodings**: tight.c (requires libjpeg/png), notls.c (requires openssl/gnutls)
- **Included dependencies**: minilzo.c (for ultra encoding)
- **Template files excluded**: tableinit24.c, tableinittctemplate.c etc. (these are #included by translate.c, not standalone)
- **Build standard**: gnu99 for C compatibility
- **API note**: `rfbSetPassword()` doesn't exist in this version; password auth uses `authPasswdData` + `rfbCheckPasswordByList`

### 2. Frame Capture

- Uses `IFrameCallback` from libuvccamera with `PIXEL_FORMAT_RGBX`
- Rate-limited to 15 FPS to avoid overwhelming the encoder
- Frame resolution matches actual camera preview size (not screen dimensions)
- Frame buffer size: `width × height × 4` bytes (RGBA)

### 3. Input Event Routing

**Mouse Events:**
- VNC client sends absolute coordinates (0 to framebuffer width/height)
- `MouseManager` normalizes to 0-4096 range using `screenWidth/screenHeight`
- `MouseManager.width_height()` must be set to VNC server resolution for correct mapping
- Uses `sendHexAbsData()` for movement, `sendHexAbsButtonClickData()` for clicks

**Keyboard Events:**
- VNC keysyms mapped to CH9329 key names via `VncKeyMap`
- Uppercase letter keysyms (0x41-0x5A) return lowercase key names to match CH9329MSKBMap
- Regular keys use `sendKeyBoardData("00", keyName)` which calls `sendKeyboardRequest()`
- Modifier keys use `sendKeyBoardPress()` / `sendKeyBoardRelease()`
- All keyboard writes use `usbDeviceManager.writeData()` for correct FE0C endpoint routing

### 4. Auto-start Behavior

Auto-start triggers in `onCameraOpen()` (not `onServiceConnected`) to ensure the camera is fully initialized before starting the VNC server and frame capture.

---

## Bugs Fixed During Implementation

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| `UnsatisfiedLinkError: vncServerStart` | vnc_server_jni.c not in LOCAL_SRC_FILES | Added to Android.mk |
| `rfbSetPassword` undefined | Function doesn't exist in this libvncserver version | Use `authPasswdData` + `rffbCheckPasswordByList` |
| `clientGoneHook` assignment failed | Forward declaration missing | Added `static void client_gone(rfbClientPtr cl);` |
| Frame buffer size mismatch | VNC server used hardcoded 1920x1080, camera used different resolution | Use actual camera preview size for both server and frame capture |
| Mouse coordinate misalignment | Double normalization: VNC normalized to 0-4096, MouseManager normalized again | Pass raw VNC coordinates, set MouseManager dimensions to VNC resolution |
| Mouse not moving | Only click command sent, no movement command | Split: `sendHexAbsData()` for movement, `sendHexAbsButtonClickData()` for clicks |
| Keyboard data corrupted with "null" | Uppercase keysym → uppercase keyName → CH9329MSKBMap has lowercase keys | Return lowercase key names from VncKeyMap |
| Keyboard not reaching target | `port.write()` goes to wrong endpoint on FE0C devices | Use `usbDeviceManager.writeData()` for correct routing |
| Handler crash in sendKeyBoardData | `new Handler()` on JNI callback thread (no Looper) | Use `new Handler(Looper.getMainLooper())` |
| Auto-start crash on app launch | VNC started before camera connected | Move auto-start to `onCameraOpen` |
| Dialog didn't start frame capture | VNC dialog started server but didn't trigger frame capture | Add listener callback to start frame capture |
| Crash with max compression level | `tightConf[]` array only has 4 entries (0-3), but level 9 caused OOB access | Changed Java constants to valid range (1-3), added clamping at JNI and SetEncodings handler |
| Crash when stopping server with client connected | Same OOB issue corrupted heap + double-free in client_gone callback | Unconditional clamp in SetEncodings, set `cl->clientData = NULL` before freeing |
| Client couldn't connect after changes | Stale build on device | Clean rebuild resolves |

---

## Testing

### Building

```bash
# Clean build native library
./gradlew :libvncserver:clean :libvncserver:assembleDebug

# Build full app
./gradlew :app:assembleDebug
```

### Installing

```bash
# Via USB
adb install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk

# Via WiFi (enable first)
adb tcpip 5555
adb connect <phone-ip>:5555
adb -s <phone-ip>:5555 install -r app/build/outputs/apk/debug/OpenterfaceAndroid-debug.apk
```

### Testing VNC

1. Open the Openterface app
2. Ensure the Openterface device (USB camera/HID) is connected
3. Tap the VNC icon in the drawer to open settings
4. Configure port, password, and auto-start as needed
5. Tap "Start Server"
6. Connect from a VNC client: `vnc://<phone-ip>:5900`
7. Verify video streaming and keyboard/mouse input on the target device

### Monitoring Logs

```bash
adb -s <phone-ip>:5555 logcat -s VncServerJNI:V VncServerService:V VncFrameCapture:V
```

---

## Future Work (Phase 2)

- WebRTC server support for browser-based remote access
- Multi-client support
- TLS encryption
- Improved encoding (tight, zrle) with external dependencies
