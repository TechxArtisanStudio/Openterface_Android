# Video Stream Zoom and Mouse Cursor Architecture

## Overview

This document describes how video stream zooming and mouse cursor movement work in the Openterface Android app, with emphasis on the portrait mode implementation.

## Core Modules

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainActivity                             │
│  (Layout management, touch listener binding, orientation, init)  │
└─────────────────────────────────────────────────────────────────┘
           │                          │
           ▼                          ▼
┌───────────────────────┐     ┌──────────────────────────────────┐
│  CustomTouchListener  │     │         ZoomLayoutDeal            │
│                       │     │                                  │
│ • Touch gesture       │     │ • PiP thumbnail management       │
│   - Single finger:    │     │ • Green indicator (viewport pos) │
│     tap/drag/double   │     │ • PiP drag → syncMainViewPos     │
│   - Two finger:       │◄────│ • setMouseLocation on drag end   │
│     scroll/right-click│     │                                  │
│ • Zoom/pan state      │     └──────────────────────────────────┘
│   - portrait: Matrix  │
│   - landscape: scaleX │
│ • mapToContentCoords  │
│   (touch → content)   │
└───────────────────────┘
           │
           ▼
┌───────────────────────┐
│       HidManager      │  ← Unified HID interface layer
│  (Java / Core JNI)    │
└───────────────────────┘
           │
           ▼
┌───────────────────────┐
│     MouseManager      │  ← CH9329 protocol packet building
└───────────────────────┘
           │
           ▼
   UsbDeviceManager → CH9329 chip → Target machine
```

## Key Files

| File | Role |
|------|------|
| `CustomTouchListener.java` | Primary touch handler: pinch-to-zoom, pan, mouse gestures, coordinate mapping |
| `ZoomLayoutDeal.java` | PiP thumbnail/indicator management, cursor centering after PiP drag |
| `HidManager.java` | Unified facade switching between Java and Core JNI implementations |
| `MouseManager.java` | Java HID mouse implementation (CH9329 serial protocol) |
| `MouseManagerCore.java` | JNI-based HID mouse implementation |
| `MainActivity.java` | Main activity: layout inflation, touch listener attachment |

## Zoom/Pan State Storage

| Mode | Scale Factor | Translation | Storage Location |
|------|-------------|-------------|------------------|
| Portrait | `mPortraitZoomScale` | `mPortraitTranslateX/Y` | `CustomTouchListener` static fields |
| Landscape | `drawerLayout.getScaleX()` | `drawerLayout.getScrollX/Y()` | `DrawerLayout` view properties |

## Coordinate Transformation Chain

```
Touch point (viewX, viewY)
    │
    │  mapToContentCoords inverse transform
    ▼
Content coords (contentX, contentY)  ←  Range: [0, viewWidth] × [0, viewHeight]
    │
    │  Proportional mapping
    ▼
HID coords (hidX, hidY)  ←  Range: [0, screenWidth] × [0, screenHeight]
    │
    │  MouseManager normalizes to 0-4095
    ▼
CH9329 protocol packet
```

### Inverse Transform Formula (Portrait Mode)

```
contentX = (touchX - translateX - pivotX) / zoomScale + pivotX
contentY = (touchY - translateY - pivotY) / zoomScale + pivotY
```

Where:
- `pivotX = viewWidth / 2`
- `pivotY = viewHeight / 2`
- `translateX/Y` = current pan translation

### Viewport Center Calculation

The viewport center in content coordinates:
```
centerX = viewWidth/2 - translateX / zoomScale
centerY = viewHeight/2 - translateY / zoomScale
```

This is what `moveMouseToViewportCenter()` calculates and sends to the HID.

## Viewport Panning Paths

| Path | Trigger | State Update | Mouse Follow |
|------|---------|--------------|--------------|
| **PiP indicator drag** | User drags green square | `syncMainViewPosition` → `setPortraitPan` | `setMouseLocation` → `moveMouseToViewportCenter` |
| **Two-finger pan** | Two-finger swipe on main view | `handleTwoFingerGesture` → sends scroll events | `moveMouseToViewportCenter` on ACTION_UP |

## Mouse Modes

| Mode | KeyMouse_state | keyMouseAbsCtrl | virtualTrackpadMode | Behavior |
|------|---------------|-----------------|---------------------|----------|
| Absolute (default) | true | false | false | Touch point → absolute coordinates |
| Absolute drag | true | true | false | Left button held during drag |
| Relative | false | false | false | Delta → relative displacement |
| Virtual trackpad | false | false | true | Always uses relative mode |

## Mouse Gesture Handling

### Single Finger (handleOneFingerGesture)
- **Tap** (quick press + release): Left click
- **Double tap**: Double click
- **Drag** (movement beyond threshold): Mouse move
- **Long press** (>500ms): Enter drag mode (left button held toggle)

### Two Finger (handleTwoFingerGesture)
- **Pinch**: Zoom in/out (distance change > center movement)
- **Scroll** (both fingers moving together): Vertical scroll wheel
- **Tap** (no significant movement): Right click
- **Drag end** (if zoomed in portrait mode): Move cursor to viewport center

## Viewport Center Following (moveMouseToViewportCenter)

This method ensures the mouse cursor follows the viewport when the view is panned.

### Implementation

```java
public static void moveMouseToViewportCenter() {
    // 1. Get current viewport dimensions
    float viewWidth = viewMainPreview.getWidth();
    float viewHeight = viewMainPreview.getHeight();

    // 2. Use mapToContentCoords to inverse-transform viewport center
    float[] content = mapToContentCoords(viewWidth/2, viewHeight/2, viewWidth, viewHeight);

    // 3. Scale to HID coordinates
    float hidX = content[0] / viewWidth * hidWidth;
    float hidY = content[1] / viewHeight * hidHeight;

    // 4. Send to target machine
    HidManager.sendHexAbsData(hidX, hidY);
}
```

### Usage

Called from:
1. `ZoomLayoutDeal.setMouseLocation()` — after PiP indicator drag ends
2. `CustomTouchListener.handleTwoFingerGesture()` — after two-finger pan ends (portrait mode, zoomed in)

## Video Input Pipeline

```
USB Camera (UVC)
    │
    ▼
OpenterfaceUVCManager.java    ← USB device monitor, VID/PID matching
    │
    ▼
CameraConnectionService.java  ← UVC camera connection management
    │
    ▼
CameraHelper.java / VideoCapture.java  ← Camera preview config, frame capture
    │
    ▼
AspectRatioTextureView (viewMainPreview)  ← Main display
    │
    ▼
AspectRatioSurfaceView (cameraViewSecond) ← PiP thumbnail
```

## HID Output Pipeline

```
Touch gestures on viewMainPreview
    │
    ▼
CustomTouchListener.onTouch()
    │
    │  Routes based on mode: absolute/relative/trackpad
    │  Maps coords via mapToContentCoords for zoom-awareness
    ▼
HidManager.java (unified facade)
    │
    ├── MouseManager.java (Java: CH9329 serial protocol)
    │       └── CH9329Function.java (packet building, checksum)
    │
    └── MouseManagerCore.java (JNI: KeymodJNI native)
            │
            ▼
    UsbDeviceManager.writeData() → CH9329 HID chip → Target machine
```

## Alternative Input Routes

- **VNC**: `VncServerService` → `VncFrameCapture` → `VncKeyMap`
- **WebRTC**: `WebRtcServerService` → `WebRtcFrameCapture` → `WebRtcInputRouter` → `HidManager`

## Design Principles

1. **State Cohesion**: Zoom/pan state lives in `CustomTouchListener`; `ZoomLayoutDeal` reads/writes via static methods.

2. **Inverse Transform Reuse**: `mapToContentCoords` handles both portrait (Matrix inverse) and landscape (scale + scroll inverse) modes.

3. **Unified Entry Point**: `moveMouseToViewportCenter()` is the single method for "mouse follows viewport center" behavior, shared by PiP drag and two-finger pan.

4. **Mode Detection**: Portrait vs landscape is detected by checking for `R.id.module_selector_bar` view existence in the layout.

## Related Documentation

- [Touch Gesture Design](./touch-gestures.md) — detailed gesture recognition logic
- [HID Protocol Reference](./hid-protocol.md) — CH9329 packet format
