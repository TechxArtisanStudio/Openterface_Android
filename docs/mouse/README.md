# Mouse Control Documentation

This directory contains documentation for the mouse control and video stream zoom system in the Openterface Android app.

## Documents

| File | Description |
|------|-------------|
| [zoom-and-cursor-architecture.md](./zoom-and-cursor-architecture.md) | Overall architecture: modules, coordinate transformation, pipelines |
| [viewport-center-following.md](./viewport-center-following.md) | Viewport center mouse following feature: problem, solution, math |

## Quick Reference

### Key Classes

- `CustomTouchListener` — Touch gesture handling, zoom/pan state, coordinate mapping
- `ZoomLayoutDeal` — PiP thumbnail management, viewport indicator
- `HidManager` — Unified HID interface (Java + JNI)
- `MouseManager` — CH9329 protocol implementation

### Mouse Modes

| Mode | Use Case |
|------|----------|
| Absolute | Direct positioning (touch → cursor) |
| Absolute Drag | Drag with left button held |
| Relative | Incremental movement (touchpad style) |
| Virtual Trackpad | Always relative, regardless of setting |

### Coordinate Spaces

1. **View space** — Touch coordinates on `viewMainPreview`
2. **Content space** — Original camera content coordinates `[0, viewWidth] × [0, viewHeight]`
3. **HID space** — Reference coordinates for CH9329 `[0, screenWidth] × [0, screenHeight]`
4. **Normalized space** — 0-4095 range for USB HID protocol
