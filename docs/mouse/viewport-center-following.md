# Viewport Center Mouse Following

## Problem Statement

When zooming into the video stream in portrait mode and panning the viewport, the mouse cursor on the target machine should automatically move to the center of the new viewport, not remain at its previous position.

## Previous Behavior (Bug)

The `setMouseLocation()` method in `ZoomLayoutDeal.java` always sent the cursor to the geometric center of the screen `(screenWidth/2, screenHeight/2)`, ignoring the current pan/zoom state. This meant:

- After panning the viewport left, the cursor would jump to screen center instead of viewport center
- The `setMaxViewX/setMaxViewY` parameters were passed but never used

## Solution

### New Method: `moveMouseToViewportCenter()`

Added to `CustomTouchListener.java`:

```java
/**
 * Move the mouse cursor to the center of the currently visible viewport.
 * Used after two-finger pan ends or PiP indicator drag ends, so the mouse follows the view.
 *
 * How it works: Uses mapToContentCoords to inverse-transform the viewport center
 * (viewWidth/2, viewHeight/2) back to content coordinates, then scales to HID coordinates.
 */
public static void moveMouseToViewportCenter() {
    if (activity == null || activity.mBinding == null
        || activity.mBinding.viewMainPreview == null) {
        return;
    }

    float viewWidth = activity.mBinding.viewMainPreview.getWidth();
    float viewHeight = activity.mBinding.viewMainPreview.getHeight();
    if (viewWidth <= 0 || viewHeight <= 0) return;

    // Use mapToContentCoords to inverse-transform the viewport center to content coordinates
    float[] content = mapToContentCoords(viewWidth / 2f, viewHeight / 2f, viewWidth, viewHeight);

    // Content coords are in range [0, viewWidth] x [0, viewHeight]
    // HidManager.sendHexAbsData expects HID reference coordinates
    // Scale proportionally to convert
    int hidWidth = HidManager.getScreenWidth();
    int hidHeight = HidManager.getScreenHeight();
    float hidX = content[0] / viewWidth * hidWidth;
    float hidY = content[1] / viewHeight * hidHeight;

    HidManager.sendHexAbsData(hidX, hidY);
    Log.d(TAG, "moveMouseToViewportCenter: content=(" + content[0] + "," + content[1]
        + ") hid=(" + hidX + "," + hidY + ")");
}
```

### Coordinate Math

The forward transform (content → view) in portrait mode:
```
viewX = (contentX - pivotX) * zoomScale + pivotX + translateX
```

Inverse (view → content):
```
contentX = (viewX - translateX - pivotX) / zoomScale + pivotX
```

For the viewport center (`viewX = viewWidth/2`, `pivotX = viewWidth/2`):
```
contentCenterX = (viewWidth/2 - translateX - viewWidth/2) / zoomScale + viewWidth/2
               = -translateX / zoomScale + viewWidth/2
               = viewWidth/2 - translateX / zoomScale
```

**Verification:**
- When `translateX = 0`: `contentCenterX = viewWidth/2` ✓ (center of content)
- When `translateX > 0`: `contentCenterX < viewWidth/2` ✓ (viewing left portion)
- When `translateX < 0`: `contentCenterX > viewWidth/2` ✓ (viewing right portion)

### Integration Points

#### 1. PiP Indicator Drag (ZoomLayoutDeal.java)

```java
private static void setMouseLocation(int setMaxViewX, int setMaxViewY) {
    // Delegate to CustomTouchListener to calculate viewport center based on current zoom/pan state
    com.openterface.AOS.serial.CustomTouchListener.moveMouseToViewportCenter();
    Log.d(TAG, "setMouseLocation: moved to viewport center");
}
```

Called from `setupThumbnailDrag()` on `ACTION_UP`.

#### 2. Two-Finger Pan End (CustomTouchListener.java)

```java
case MotionEvent.ACTION_UP:
    if (!twoFingerDragConfirmed) {
        // Right click logic (unchanged)
        ...
    } else {
        // Two-finger drag ended — if zoomed in, move cursor to viewport center
        if (isPortraitMode && mPortraitZoomScale > 1.0f
            && KeyMouse_state && !virtualTrackpadMode) {
            moveMouseToViewportCenter();
        }
        Log.d(TAG, "Two-finger drag/pinch ended — no right click");
    }
    ...
```

## Behavior Matrix

| Scenario | Zoom State | Mouse Action |
|----------|-----------|--------------|
| PiP drag ends | Zoomed in | Move to viewport center |
| PiP drag ends | Not zoomed | Move to viewport center (same as center) |
| Two-finger pan ends (portrait) | Zoomed in | Move to viewport center |
| Two-finger pan ends (portrait) | Not zoomed | No change (scroll wheel only) |
| Two-finger pan ends (landscape) | Any | No change (uses different pan mechanism) |

## Why This Design

1. **State locality**: Zoom/pan state lives in `CustomTouchListener`, so the viewport center calculation belongs there too.

2. **Reuse**: `mapToContentCoords` already handles both portrait (Matrix inverse) and landscape (scale + scroll) modes correctly.

3. **Single entry point**: Both PiP drag and two-finger pan use the same method, ensuring consistent behavior.

4. **No duplication**: Eliminates the buggy duplicate logic in `setMouseLocation`.

## Testing Checklist

- [ ] Portrait mode: Pinch to zoom → PiP indicator appears
- [ ] Portrait mode: Drag PiP indicator → cursor follows to new viewport center
- [ ] Portrait mode: Two-finger pan → cursor follows to new viewport center on lift
- [ ] Portrait mode: Not zoomed → two-finger pan → no cursor movement (scroll only)
- [ ] Landscape mode: PiP drag → cursor follows to new viewport center
- [ ] Both modes: Zoom out → cursor returns to center
