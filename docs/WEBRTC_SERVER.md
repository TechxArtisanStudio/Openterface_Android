# WebRTC Server Implementation

## Overview

The WebRTC server enables browser-based remote control of the target device without requiring a VNC client. It captures UVC camera video and streams it via WebRTC, while receiving keyboard and mouse input from the browser through a WebRTC data channel.

**Key Features:**
- Browser-based access (no VNC client needed)
- Low-latency video streaming via WebRTC
- Keyboard and mouse control via data channel
- Automatic signaling via built-in HTTP server

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│ Browser                                                             │
│ ┌─────────────┐  WebRTC  ┌─────────────┐   HTTP    ┌─────────────┐  │
│ │  Web Client │◄────────►│   PeerConn  │◄─────────►│  Signaling  │  │
│ │   (Vue.js)  │   Video  │   (DataCh)  │  /sdp     │   Server    │  │
│ └─────────────┘          └─────────────┘           └─────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         ▲                                         ▲
         │ WebRTC (ICE/STUN)                       │ HTTP (REST)
         │                                         │
         ▼                                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Phone (Openterface App)                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │
│  │   WebRTC    │  │   Nano      │  │   UVC       │  │   CH9329   │  │
│  │  PeerConn   │  │   HTTPD     │  │  Camera     │  │   USB HID  │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬──────┘  │
│         │                │                │               │         │
│         └────────────────┴────────────────┴───────────────┘         │
│                              │                                      │
│                              ▼                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    WebRtcServerService                       │   │
│  │              (Foreground Service + WebRTC)                   │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## File Structure

### Core WebRTC Module

| File | Description |
|------|-------------|
| `app/src/main/java/com/openterface/AOS/webrtc/WebRtcServerService.java` | Foreground service managing WebRTC lifecycle |
| `app/src/main/java/com/openterface/AOS/webrtc/WebRtcSignalingServer.java` | NanoHTTPD-based signaling server for SDP exchange |
| `app/src/main/java/com/openterface/AOS/webrtc/WebRtcInputRouter.java` | Routes WebRTC data channel input to HID |
| `app/src/main/java/com/openterface/AOS/webrtc/AndroidHidInputSender.java` | Android-specific HID implementation |
| `app/src/main/java/com/openterface/AOS/webrtc/HidInputSender.java` | Interface for HID operations (testable) |

### Supporting Infrastructure

| File | Description |
|------|-------------|
| `app/src/main/java/com/openterface/AOS/vnc/VncKeyMap.java` | Maps VNC keysyms to CH9329 key names (reused from VNC) |
| `app/src/main/java/com/openterface/AOS/target/KeyBoardManager.java` | Enhanced with synchronized keyboard executor |

### Web Client (Openterface_WebUI submodule)

| Package | Description |
|---------|-------------|
| `@openterface/webrtc` | WebRTC client application (builds to dist/webrtc-android/) |
| `@openterface/webrtc-core` | WebRTC transport layer with CH9329→JSON translation |
| `@openterface/control-ui` | Shared Vue UI components (Layout, VideoStream, etc.) |

The web client is sourced from the `Openterface_WebUI` submodule, not `Openterface_Web`. This allows Android to consume only the necessary web assets without pulling in the full web monorepo.

---

## Building

### Prerequisites

The WebRTC web client must be built and copied to Android assets. It lives in the `Openterface_WebUI` submodule:

```bash
# Build web client
cd Openterface_WebUI
npm install
npm run build:webrtc:android

# Or use Gradle task (automatic during build)
./gradlew :app:buildWebClient
```

The Gradle `buildWebClient` task automatically runs before `preBuild`, so web assets are always up to date.

### Build APK

```bash
# Full build
./gradlew :app:assembleDebug

# With WebRTC web assets
./gradlew :app:copyWebRtcWeb :app:assembleDebug
```

### Clean Build

```bash
# Clear old web assets and rebuild
rm -rf app/src/main/assets/webrtc/*
./gradlew :app:copyWebRtcWeb :app:assembleDebug
```

---

## Usage

1. Open the Openterface app
2. Ensure the Openterface device is connected
3. Connect to the phone's WiFi network
4. Open browser to `http://<phone-ip>:8080`
5. Click "Start Connection"
6. Wait for "Connected" status
7. Video should appear automatically

---

## Keyboard Implementation

### Problem

Early WebRTC keyboard implementation had a race condition:

```
Browser sends keydown 'a' → Android spawns Thread A for key press
Browser sends keyup 'a'   → Android spawns Thread B for key release

Thread A: sends press command (57 AB 00 02 08 00 00 04 00...)
Thread B: sends release command (57 AB 00 02 08 00 00 00 00...)

Result: Race condition - release could execute before press was fully
written to the serial port, causing missed keystrokes.
```

### Solution

**Single-threaded executor with sequential press+release:**

```java
// KeyBoardManager.java - Single-threaded executor
private static final ExecutorService keyboardExecutor =
    Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Keyboard-Executor");
        t.setDaemon(true);
        return t;
    });

public static void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
    keyboardExecutor.execute(() -> {
        // Step 1: Send press
        sendKeyBoardPressSync(functionKey, keyName);

        // Step 2: Wait for target to register
        Thread.sleep(50);

        // Step 3: Send release
        sendKeyBoardReleaseSync();
    });
}
```

This ensures:
1. All keyboard operations execute on a single thread (no races)
2. Press always completes before release
3. 50ms delay allows CH9329/target to register the key press

### Data Flow

```
Browser Key Event
    ↓ (JSON over WebRTC DataChannel)
WebRtcServerService.handleDataChannelMessage()
    ↓ (keysym, down)
WebRtcInputRouter.onKeyboardEvent()
    ↓ (keyName)
AndroidHidInputSender.sendKeyboardKey()
    ↓
KeyBoardManager.sendKeyBoardPressAndRelease()
    ↓ (single-threaded executor)
CH9329 USB Serial Command
    ↓
Target Device
```

---

## Signaling Protocol

The signaling server uses HTTP REST endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Serve web client HTML |
| `/sdp` | POST | Browser sends offer SDP |
| `/sdp` | GET | Browser polls for answer SDP |
| `/status` | GET | Connection status |

### SDP Exchange Flow

1. Browser loads web client from `http://<phone>:8080/`
2. Browser creates peer connection, generates offer SDP
3. Browser POSTs offer to `/sdp`
4. Android creates peer connection, sets remote description
5. Android generates answer SDP
6. Browser GETs `/sdp` repeatedly until answer available
7. Browser sets local description, connection established

### ICE Candidate Handling

Android-side ICE candidates are collected and returned with the answer:

```json
// GET /sdp response
{
  "sdp": "v=0\r\n...",
  "type": "answer",
  "status": "ready",
  "candidates": [
    {"candidate": "candidate:1 1 udp...", "sdpMid": "0", "sdpMLineIndex": 0}
  ]
}
```

---

## Keyboard Message Format

WebRTC data channel uses JSON messages:

### Keyboard Event

```json
{
  "type": "keyboard",
  "keysym": 97,      // VNC keysym (ASCII for 'a')
  "down": true       // true = press, false = release
}
```

### Mouse Event

```json
{
  "type": "mouse",
  "buttonMask": 1,   // 1=left, 2=middle, 4=right
  "x": 500,          // Absolute X coordinate
  "y": 300,          // Absolute Y coordinate
  "pressed": true    // Button pressed state
}
```

---

## Known Issues and Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| Keyboard not working | Race condition between press/release threads | Single-threaded executor with sequential press+release |
| ICE connection failed | Android candidates not sent to browser | Queue candidates in JSONArray, include in /sdp response |
| No video track in SDP | Browser didn't request video | Add `addTransceiver('video', {direction: 'recvonly'})` on browser side |
| DataChannel label mismatch | Android created "input", browser created "hid" | Remove Android `createDataChannel()`, wait for browser's channel |
| Gradle build cache issues | Old web assets persisted in APK | Clear `app/src/main/assets/webrtc/` before rebuild |

---

## Debugging

### Enable Verbose Logging

```bash
adb logcat -s WebRtcServerService:V WebRtcSignalingServer:V WebRtcInputRouter:V KeyBoardManager:E
```

### Key Keyboard Log Tags

| Tag | Level | Description |
|-----|-------|-------------|
| `KeyBoardManager` | Error | All keyboard operations logged at ERROR level for visibility |
| `WebRtcInputRouter` | Info | Keysym to keyName mapping |
| `WebRtcServerService` | Info | DataChannel messages received |

### Expected Keyboard Log Sequence

```
D WebRtcServerService: Keyboard event received: keysym=97, down=true
I WebRtcInputRouter: keyName=a
E KeyBoardManager: 🟣 ========== KEY PRESS+RELEASE START ==========
E KeyBoardManager: 🟣 Step 1: Sending PRESS...
E KeyBoardManager: 🟣 Sync press command: 57AB00020208000004000000000010
E KeyBoardManager: ✅ Sync press sent in Xms
E KeyBoardManager: 🟣 Step 2: Waiting 50ms for target to register...
E KeyBoardManager: 🟣 Step 3: Sending RELEASE...
E KeyBoardManager: 🟣 Sync release command: 57AB00020800000000000000000C
E KeyBoardManager: ✅ Sync release sent in Xms
E KeyBoardManager: ✅ Key press+release completed in Xms
E KeyBoardManager: 🟣 ========== KEY PRESS+RELEASE END ==========
```

---

## Future Improvements

- Add modifier key support (Shift, Ctrl, Alt combinations)
- Implement key repeat for held keys
- Add touch support for mobile browsers
- Support multiple simultaneous browser connections
- Add authentication/password protection
- Implement statistics/monitoring UI
