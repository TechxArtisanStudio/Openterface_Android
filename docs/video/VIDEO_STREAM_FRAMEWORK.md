# Video Stream Display Framework - Portrait Mode Implementation

## Overview

This document describes the video stream display framework in the Openterface Android app, focusing on the portrait mode implementation that enables zoom/pan functionality while maintaining video aspect ratio and display quality.

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  - Surface lifecycle management                                  │
│  - Camera connection and preview control                         │
│  - Portrait/Landscape mode detection                             │
└─────────────────────────────────────────────────────────────────┘
                                │
            ┌───────────────────┴───────────────────┐
            ▼                                       ▼
┌─────────────────────────┐           ┌─────────────────────────┐
│  AspectRatioTextureView │           │  CustomTouchListener    │
│  (viewMainPreview)      │           │  - Pinch-to-zoom        │
│  - Main video display   │           │  - Matrix transforms    │
│  - SurfaceTexture mgmt  │           │  - Touch event handling │
└─────────────────────────┘           └─────────────────────────┘
            │                                       │
            ▼                                       ▼
┌─────────────────────────┐           ┌─────────────────────────┐
│   RendererHolder        │           │   ZoomLayoutDeal        │
│   - Frame distribution  │           │   - PiP window control  │
│   - Primary/Slave       │           │   - Indicator sync      │
│     surfaces            │           │   - Pan position calc   │
└─────────────────────────┘           └─────────────────────────┘
            │
            ▼
┌─────────────────────────┐
│   AspectRatioSurfaceView│
│   (cameraViewSecond)    │
│   - PiP video display   │
└─────────────────────────┘
```

## Video Frame Flow

```
UVC Camera
    │
    ▼
CameraInternal.mRendererHolder (OpenGL ES context)
    │
    ├──► Primary Surface (viewMainPreview)
    │         │
    │         ├──► GPU Texture (1920x1080 buffer)
    │         │
    │         └──► Matrix Transform (stretch correction + zoom + pan)
    │                    │
    │                    └──► Display on screen
    │
    └──► Slave Surface (cameraViewSecond - PiP)
              │
              └──► Same texture rendered at smaller size
```

## Portrait Mode Implementation

### Problem Statement

In portrait mode, the video stream (typically 16:9 aspect ratio) needs to:
1. Display correctly without stretching
2. Support pinch-to-zoom with smooth scaling
3. Allow panning via PiP indicator
4. Maintain video quality when zoomed
5. Handle keyboard pop-up without crash or visual glitches

### Solution: Matrix-Based Transform

Instead of dynamically resizing the TextureView (which causes surface recreation and flickering), we use a fixed-size TextureView with Matrix transforms:

```java
// TextureView is always MATCH_PARENT in portrait mode
// Matrix handles all visual transformations:
//   1. Stretch correction (SurfaceTexture aspect ratio mismatch)
//   2. Zoom scaling
//   3. Pan translation
```

### Key Files Modified

#### 1. AspectRatioTextureView.java

Added ability to toggle aspect ratio enforcement:

```java
private boolean mAspectRatioEnabled = true;

public void setAspectRatioEnabled(boolean enabled) {
    mAspectRatioEnabled = enabled;  // Does NOT call requestLayout()
}

@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mRequestedAspect > 0 && mAspectRatioEnabled) {
        // Original aspect ratio enforcement logic
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
}
```

#### 2. CustomTouchListener.java

**Stretch Correction Logic:**

When TextureView dimensions don't match camera buffer aspect ratio, SurfaceTexture stretches the content. Matrix corrects this:

```java
public void applyPortraitZoomTransform() {
    float surfaceAspect = surfaceWidth / surfaceHeight;
    float bufferAspect = bufferWidth / bufferHeight;
    boolean needsCorrection = Math.abs(surfaceAspect - bufferAspect) > 0.01f;

    if (needsCorrection) {
        // Correct stretch: maintain aspect ratio, fill width, center vertically
        float correctionScaleY = (bufferHeight * surfaceWidth) / (surfaceHeight * bufferWidth);
        matrix.setScale(1.0f, correctionScaleY);
        
        float correctedHeight = surfaceHeight * correctionScaleY;
        float translateY = (surfaceHeight - correctedHeight) / 2f;
        matrix.postTranslate(0, translateY);
    }

    // Apply zoom (pivot at center)
    matrix.postScale(zoomScale, zoomScale, pivotX, pivotY);
    
    // Apply pan (user drag)
    matrix.postTranslate(mPortraitTranslateX, mPortraitTranslateY);

    textureView.setTransform(matrix);
}
```

**Dynamic MAX_SCALE Calculation:**

```java
final float MAX_DISPLAY_HEIGHT_RATIO = 0.5f;  // Max 50% of screen height
float naturalHeight = screenWidth / bufferAspect;
float maxHeight = screenHeight * MAX_DISPLAY_HEIGHT_RATIO;
MAX_SCALE = maxHeight / naturalHeight;
```

#### 3. MainActivity.java

**Surface Lifecycle in Portrait Mode:**

```java
@Override
public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    if (isPortraitMode) {
        // Skip surface recreation during keyboard animation
        // Just update buffer size and Matrix
        setSurfaceTextureBufferSize(surface, mPreviewWidth, mPreviewHeight);
        mBinding.viewMainPreview.post(() -> 
            mCustomTouchListener.applyPortraitZoomTransform());
    } else {
        // Landscape: full surface recreation with aspect ratio update
        // ... (existing logic)
    }
}
```

**Conditional Aspect Ratio Setting:**

All `setAspectRatio()` calls are wrapped with portrait mode check:

```java
if (!isPortraitMode) {
    mBinding.viewMainPreview.setAspectRatio(mPreviewWidth, mPreviewHeight);
}
```

#### 4. ZoomLayoutDeal.java

**PiP Indicator to Pan Translation:**

```java
private static void syncMainViewPosition(float thumbX, float thumbY) {
    if (drawerLayout == null) {
        // Portrait mode: use Matrix-based panning
        float currentZoomScale = CustomTouchListener.getPortraitZoomScale();
        
        if (currentZoomScale <= 1.0f) return;

        // Calculate actual content dimensions after stretch correction
        float contentHeight = viewWidth * bufferHeight / bufferWidth;
        
        // Use content dimensions (not view dimensions) for accurate edge mapping
        float maxTranslateX = (contentWidth * (currentZoomScale - 1f)) / 2f;
        float maxTranslateY = (contentHeight * (currentZoomScale - 1f)) / 2f;

        // Map indicator position to translation values
        float normalizedX = indicatorX / maxIndicatorX;
        float newTranslateX = (0.5f - normalizedX) * 2f * maxTranslateX;
        
        CustomTouchListener.setPortraitPan(newTranslateX, newTranslateY);
        CustomTouchListener.applyCurrentPortraitTransform();
    }
}
```

## Surface Management

### Buffer Size Configuration

Critical for zoom quality - sets SurfaceTexture buffer to camera native resolution:

```java
private void setSurfaceTextureBufferSize(SurfaceTexture surface, int bufferWidth, int bufferHeight) {
    // Use camera resolution for buffer, not the small view size
    surface.setDefaultBufferSize(bufferWidth, bufferHeight);
}
```

This ensures GPU has enough source pixels when zooming, preventing blur.

### RendererHolder Frame Distribution

```
Primary Surface (viewMainPreview)
    │
    ├──► handleDraw() - updateTexImage()
    │
    └──► handleDrawSlaveSurfaces()
              │
              └──► For each slave surface:
                      onDrawSlaveSurface() → surface.draw(mDrawer, texId, texMatrix, mvpMatrix)
```

Both surfaces receive identical frames from the same OpenGL texture.

## PiP (Picture-in-Picture) System

### Layout Structure

```xml
<RelativeLayout android:id="@+id/video_area_container">
    <AspectRatioTextureView android:id="@+id/viewMainPreview" />
    
    <RelativeLayout android:id="@+id/thumbnail_container">
        <AspectRatioSurfaceView android:id="@+id/cameraViewSecond" />
        <View android:id="@+id/view_indicator" />  <!-- Green box -->
    </RelativeLayout>
    
    <ImageButton android:id="@+id/drag_button" />
</RelativeLayout>
```

### Drag Operations

1. **Drag Button**: Moves PiP window position on screen (visual only)
2. **Thumbnail Drag**: Pans the zoomed main view via indicator position

### Bidirectional Sync

- **PiP → Main View**: `syncMainViewPosition()` → `setPortraitPan()` → `applyCurrentPortraitTransform()`
- **Main View → PiP**: `applyPortraitZoomTransform()` → `ZoomLayoutDeal.updateIndicatorFromMainView()`

## Issues Fixed

### 1. Video Stretching in Portrait Mode

**Problem**: SurfaceTexture stretches content when view aspect ratio differs from buffer.

**Solution**: Matrix stretch correction that scales Y-axis to maintain aspect ratio.

### 2. Crash During Keyboard Pop-up

**Problem**: `onSurfaceTextureSizeChanged` fired repeatedly during keyboard animation, each time removing/re-adding surfaces, causing crash.

**Solution**: Skip surface recreation in portrait mode, only update buffer size and Matrix.

### 3. Flickering During Pinch Zoom

**Problem**: Multiple `requestLayout()` calls per touch event.

**Solution**: 
- Removed `requestLayout()` from `setAspectRatioEnabled()`
- Added thresholds to `setLayoutParams()` calls
- TextureView stays fixed size, Matrix handles all transforms

### 4. Video Blur When Zoomed

**Problem**: SurfaceTexture buffer defaulted to view size (smaller than camera resolution).

**Solution**: Explicitly set buffer size to camera native resolution (1920x1080).

### 5. PiP Drag Showing Black Bars

**Problem**: `maxTranslateY` calculated using view height instead of corrected content height.

**Solution**: Calculate max translation based on actual content dimensions after stretch correction.

### 6. PiP Drag Causing Video Distortion

**Problem**: `applyCurrentPortraitTransform()` lacked stretch correction.

**Solution**: Added same stretch correction logic as `applyPortraitZoomTransform()`.

## Code Statistics

| File | Lines Changed | Key Additions |
|------|---------------|---------------|
| MainActivity.java | ~100 | Surface lifecycle, conditional aspect ratio |
| CustomTouchListener.java | ~200 | Matrix transforms, stretch correction |
| ZoomLayoutDeal.java | ~30 | Content-based translation calculation |
| AspectRatioTextureView.java | ~20 | Aspect ratio toggle |
| layout-port/activity_main.xml | 2 | clipChildren, clipToPadding |

## Testing Checklist

- [x] Initialization shows video correctly at natural aspect ratio
- [x] Pinch-to-zoom works smoothly without stretching
- [x] Zoom caps at 50% screen height
- [x] Opening keyboard doesn't compress video or crash
- [x] PiP indicator drag pans main view correctly
- [x] No black bars when dragging PiP to edges
- [x] Video remains clear when zoomed (no blur)
- [x] Screen rotation works correctly
- [x] PiP window can be dragged without affecting main video

## Future Improvements

1. **Smooth Animation**: Add interpolation for zoom/pan transitions
2. **Gesture Refinement**: Better double-tap to zoom to specific area
3. **Performance**: Optimize Matrix calculations for lower-end devices
4. **Accessibility**: Add keyboard shortcuts for zoom/pan control
