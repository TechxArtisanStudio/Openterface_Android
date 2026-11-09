package com.openterface.AOS.serial;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.openterface.AOS.activity.MainActivity;
import com.openterface.AOS.target.HidManager;
import com.openterface.AOS.target.MouseManager;
import com.openterface.AOS.target.MouseModeManager;

/**
 * Handles Bluetooth mouse input events.
 * Responsible for:
 * - Processing GenericMotionEvent from BT mouse
 * - Converting BT mouse coordinates to HID coordinates
 * - Tracking virtual HID state for edge accumulation
 * - Button press/release handling
 * - Connection state management
 */
public class BtMouseInputHandler {
    private static final String TAG = "OP-BT-MOUSE";

    // ===== Constants =====
    public static final int HID_LOGICAL_MAX = 4095;  // CH9329 uses 12-bit coordinates
    private static final float EDGE_THRESHOLD = 80f;  // pixels from edge to consider "at edge"
    private static final long BT_MOUSE_DISCONNECT_TIMEOUT_MS = 10000;
    private static final long CURSOR_RESTORE_DELAY_MS = 2000;
    private static final long MIN_BUTTON_HOLD_MS = 50;

    // ===== State =====
    private static boolean btMouseConnected = false;
    private static OnBtMouseConnectionListener btMouseConnectionListener;

    // Button state
    private static boolean btMouseLeftDown = false;
    private static boolean btMouseRightDown = false;
    private static boolean btMouseMiddleDown = false;
    private static boolean btMouseDragging = false;
    private static long btMouseLastPressTime = 0;

    // Position tracking
    private static float btMouseLastX = 0f;
    private static float btMouseLastY = 0f;
    private static float btMouseLastScreenX = 0f;
    private static float btMouseLastScreenY = 0f;
    private static boolean wasAtEdge = false;

    // Device ID tracking to distinguish BT mouse from system cursor
    private static int btMouseDeviceId = -1;
    private static boolean btMouseDeviceIdSet = false;

    // Virtual HID coordinate system
    private static float virtualHidX = HID_LOGICAL_MAX / 2f;
    private static float virtualHidY = HID_LOGICAL_MAX / 2f;
    private static boolean virtualHidInitialized = false;

    // Track last sent HID coordinates to avoid duplicates
    private static float lastSentHidX = -1f;
    private static float lastSentHidY = -1f;

    // Relative mode tracking
    private static float btMouseRelPrevX = 0f;
    private static float btMouseRelPrevY = 0f;
    private static boolean btMouseRelInitialized = false;

    // Touch ignore window
    private static volatile long btMouseLastEventTime = 0;
    private static final long TOUCH_IGNORE_WINDOW_MS = 300;

    // Disconnect/cursor restore handlers
    private static Handler btMouseDisconnectHandler;
    private static Runnable btMouseDisconnectRunnable;
    private static Handler btMouseCursorRestoreHandler;
    private static Runnable btMouseCursorRestoreRunnable;

    // Callback for UI updates (black border, cursor visibility)
    private static Callbacks callbacks;

    // Reference to activity for view lookups
    private static MainActivity activity;

    // ===== Callback interface =====
    public interface Callbacks {
        void onBlackBorderVisibilityChanged(boolean show);
        void onSystemGestureExclusionChanged(boolean enabled);
        void onBlackBorderHintUpdate(float cursorX, float cursorY);
    }

    /**
     * Callback interface for Bluetooth mouse connection state changes.
     */
    public interface OnBtMouseConnectionListener {
        void onBtMouseConnected(boolean connected);
    }

    // ===== Initialization =====

    public static void init(MainActivity mainActivity, Callbacks cb) {
        activity = mainActivity;
        callbacks = cb;
    }

    // ===== Public API =====

    public static void setOnBtMouseConnectionListener(OnBtMouseConnectionListener listener) {
        btMouseConnectionListener = listener;
    }

    public static boolean isBtMouseConnected() {
        return btMouseConnected;
    }

    public static boolean isBtMouseButtonPressed() {
        return btMouseLeftDown || btMouseRightDown || btMouseMiddleDown || btMouseDragging;
    }

    public static boolean shouldIgnoreTouch() {
        return isBtMouseButtonPressed();
    }

    public static float getBtMouseLastX() {
        return btMouseLastX;
    }

    public static float getBtMouseLastY() {
        return btMouseLastY;
    }

    public static void setBtMouseLastPosition(float x, float y) {
        btMouseLastX = x;
        btMouseLastY = y;
    }

    public static float getVirtualHidX() {
        return virtualHidX;
    }

    public static float getVirtualHidY() {
        return virtualHidY;
    }

    /**
     * Handle GenericMotionEvent from Android input system.
     * @return true if event was consumed
     */
    public static boolean handleGenericMotionEvent(MotionEvent event) {
        int source = event.getSource();
        int action = event.getAction();

        // Check if this event is from a mouse device
        boolean isMouseDevice = (source & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE;

        // Also check tool type
        boolean hasMouseTool = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (event.getToolType(i) == MotionEvent.TOOL_TYPE_MOUSE) {
                hasMouseTool = true;
                break;
            }
        }

        if (!isMouseDevice && !hasMouseTool) {
            return false;
        }

        // Mark BT mouse as connected
        setBtMouseConnected(true);
        resetBtMouseDisconnectTimeout();
        btMouseLastEventTime = System.currentTimeMillis();

        // Log event info
        Log.d(TAG, "BT Mouse event: action=" + action + " source=0x" + Integer.toHexString(source));

        // Handle different event types
        if (action == MotionEvent.ACTION_SCROLL) {
            return handleScroll(event);
        } else if (action == MotionEvent.ACTION_BUTTON_PRESS) {
            Log.d(TAG, "Dispatching BUTTON_PRESS to handleButton");
            return handleButton(event, true);
        } else if (action == MotionEvent.ACTION_BUTTON_RELEASE) {
            Log.d(TAG, "Dispatching BUTTON_RELEASE to handleButton");
            return handleButton(event, false);
        } else if (action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_MOVE) {
            return handleMove(event);
        }

        return true;
    }

    // ===== Event handlers =====

    private static boolean handleScroll(MotionEvent event) {
        float scrollAmount = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        if (scrollAmount != 0) {
            HidManager.handleTwoFingerPanSlideUpDown(scrollAmount);
        }
        return true;
    }

    private static boolean handleMove(MotionEvent event) {
        // Track the BT mouse device ID on first move event
        // BUT only if not already set by button event (button event is more reliable)
        if (!btMouseDeviceIdSet) {
            btMouseDeviceId = event.getDeviceId();
            btMouseDeviceIdSet = true;
            Log.d(TAG, "BT mouse device ID set from move event: " + btMouseDeviceId);
        } else if (event.getDeviceId() != btMouseDeviceId) {
            // Ignore move events from other devices (e.g., system cursor)
            Log.d(TAG, "Ignoring move from device " + event.getDeviceId() + " (expected " + btMouseDeviceId + ")");
            return false;
        }

        // ===== Process historical positions =====
        // Android batches move events: multiple hardware reports are merged into one MotionEvent.
        // Historical positions (getHistoricalX/Y) must be processed to avoid cursor jumping.
        // This is especially critical during drag — without it, only the final position is sent,
        // making window resize/drag show only start and end with no intermediate feedback.
        boolean isDragging = btMouseDragging && (btMouseLeftDown || btMouseRightDown || btMouseMiddleDown);
        int historySize = event.getHistorySize();

        if (isDragging && historySize > 0) {
            int buttonState = event.getButtonState();
            boolean anyButtonDown = (buttonState & MotionEvent.BUTTON_PRIMARY) != 0
                    || (buttonState & MotionEvent.BUTTON_SECONDARY) != 0
                    || (buttonState & MotionEvent.BUTTON_TERTIARY) != 0;
            int dragButtonState = buttonState;
            if (!anyButtonDown) {
                dragButtonState = 0;
                if (btMouseLeftDown) dragButtonState |= MotionEvent.BUTTON_PRIMARY;
                if (btMouseRightDown) dragButtonState |= MotionEvent.BUTTON_SECONDARY;
                if (btMouseMiddleDown) dragButtonState |= MotionEvent.BUTTON_TERTIARY;
            }

            for (int i = 0; i < historySize; i++) {
                float histScreenX = event.getHistoricalX(i);
                float histScreenY = event.getHistoricalY(i);
                float[] histViewCoords = toViewCoords(histScreenX, histScreenY);
                sendBtMouseDragMove(histViewCoords[0], histViewCoords[1], dragButtonState);
                btMouseLastX = histViewCoords[0];
                btMouseLastY = histViewCoords[1];
                btMouseLastScreenX = histScreenX;
                btMouseLastScreenY = histScreenY;
            }
            Log.d(TAG, "handleMove: processed " + historySize + " historical drag positions");
        }

        float screenX = event.getX();
        float screenY = event.getY();

        // Get screen dimensions for edge detection
        float screenWidthPx = 0, screenHeightPx = 0;
        if (activity != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            screenWidthPx = metrics.widthPixels;
            screenHeightPx = metrics.heightPixels;
        }

        // Detect cursor at screen edges
        boolean atLeft = screenX <= EDGE_THRESHOLD;
        boolean atRight = screenX >= screenWidthPx - EDGE_THRESHOLD;
        boolean atTop = screenY <= EDGE_THRESHOLD;
        boolean atBottom = screenY >= screenHeightPx - EDGE_THRESHOLD;
        boolean nowAtEdge = atLeft || atRight || atTop || atBottom;

        // Get relative axis values
        float relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
        float relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);

        // Convert to view-local coordinates
        float[] viewCoords = toViewCoords(screenX, screenY);
        float cursorX = viewCoords[0];
        float cursorY = viewCoords[1];

        // Update tracked position
        btMouseLastX = cursorX;
        btMouseLastY = cursorY;

        int buttonState = event.getButtonState();
        boolean anyButtonDown = (buttonState & MotionEvent.BUTTON_PRIMARY) != 0
                || (buttonState & MotionEvent.BUTTON_SECONDARY) != 0
                || (buttonState & MotionEvent.BUTTON_TERTIARY) != 0;

        // If dragging (any button held down), send drag move
        // Some BT mice don't report button state in move events during drag,
        // so we also check cached button state
        isDragging = btMouseDragging && (btMouseLeftDown || btMouseRightDown || btMouseMiddleDown);
        Log.d(TAG, "handleMove drag check: btMouseDragging=" + btMouseDragging
                + " leftDown=" + btMouseLeftDown + " rightDown=" + btMouseRightDown
                + " middleDown=" + btMouseMiddleDown + " isDragging=" + isDragging);

        if (isDragging) {
            // Reconstruct buttonState from cached state if event doesn't have it
            int dragButtonState = buttonState;
            if (!anyButtonDown) {
                dragButtonState = 0;
                if (btMouseLeftDown) dragButtonState |= MotionEvent.BUTTON_PRIMARY;
                if (btMouseRightDown) dragButtonState |= MotionEvent.BUTTON_SECONDARY;
                if (btMouseMiddleDown) dragButtonState |= MotionEvent.BUTTON_TERTIARY;
            }
            sendBtMouseDragMove(cursorX, cursorY, dragButtonState);
            wasAtEdge = nowAtEdge;
            btMouseLastScreenX = screenX;
            btMouseLastScreenY = screenY;
            return true;
        }

        // Normal move
        Log.d(TAG, "handleMove: NORMAL move path");
        float viewW = CoordinateConverter.getTouchViewWidth();
        float viewH = CoordinateConverter.getTouchViewHeight();

        // Get screen dimensions for debugging
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        if (activity != null && activity.getWindowManager() != null) {
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        }
        Log.d(TAG, "handleMove: screenDimensions=(" + metrics.widthPixels + "," + metrics.heightPixels
                + ") viewDimensions=(" + viewW + "," + viewH + ")");

        // Update black border hint
        if (callbacks != null) {
            callbacks.onBlackBorderHintUpdate(cursorX, cursorY);
        }

        if (MouseModeManager.isAbsoluteMode()) {
            handleAbsoluteMode(cursorX, cursorY, viewW, viewH,
                    screenX, screenY, screenWidthPx, screenHeightPx,
                    relX, relY, atLeft, atRight, atTop, atBottom);
        } else {
            handleRelativeMode(cursorX, cursorY, relX, relY, nowAtEdge);
        }

        wasAtEdge = nowAtEdge;
        btMouseLastScreenX = screenX;
        btMouseLastScreenY = screenY;
        return true;
    }

    private static void handleAbsoluteMode(float cursorX, float cursorY,
            float viewW, float viewH, float screenX, float screenY,
            float screenW, float screenH, float relX, float relY,
            boolean atLeft, boolean atRight, boolean atTop, boolean atBottom) {

        // Step 1: Apply inverse portrait zoom/pan transform to get content coordinates
        // This handles the case where video is zoomed/panned in portrait mode
        float[] contentCoords = CoordinateConverter.mapToContentCoords(
                cursorX, cursorY, viewW, viewH,
                CoordinateConverter.isPortraitMode(),
                1.0f, 0f, 0f);
        float contentX = contentCoords[0];
        float contentY = contentCoords[1];

        // Step 2: Get video rect for coordinate conversion
        float bufferW = 0, bufferH = 0;
        if (activity != null) {
            bufferW = activity.getPreviewWidth();
            bufferH = activity.getPreviewHeight();
        }
        float[] videoRect = CoordinateConverter.getVideoRectInView(viewW, viewH, bufferW, bufferH);

        // Step 3: Map content coordinates to HID [0, 4095].
        // Content coords are used so that only the video area maps to the target.
        // Black bar areas are clamped to edges — cursor stays at boundary, no jump.
        float[] hidCoords = CoordinateConverter.normalizeBtMouseToHid(contentX, contentY, viewW, viewH, videoRect);
        virtualHidX = hidCoords[0];
        virtualHidY = hidCoords[1];
        virtualHidInitialized = true;

        // Send HID data if changed
        int hidX = (int) virtualHidX;
        int hidY = (int) virtualHidY;

        // DETAILED DEBUG: Log full coordinate chain
        Log.d(TAG, "handleAbsoluteMode: "
                + "screen=(" + screenX + "," + screenY + ")"
                + " viewCoords=(" + cursorX + "," + cursorY + ")"
                + " contentCoords=(" + contentX + "," + contentY + ")"
                + " viewSize=(" + viewW + "," + viewH + ")"
                + " videoRect=(" + videoRect[0] + "," + videoRect[1] + "," + videoRect[2] + "," + videoRect[3] + ")"
                + " hidCoords=(" + hidX + "," + hidY + ")");

        if (hidX != lastSentHidX || hidY != lastSentHidY) {
            HidManager.sendBtMouseHidAbsData(hidX, hidY);
            lastSentHidX = hidX;
            lastSentHidY = hidY;
        }
    }

    private static void handleRelativeMode(float cursorX, float cursorY,
            float relX, float relY, boolean nowAtEdge) {

        Log.d(TAG, "handleRelativeMode: cursor=(" + cursorX + "," + cursorY
                + ") rel=(" + relX + "," + relY + ") atEdge=" + nowAtEdge);

        // First sample after (re)connect: only learn the baseline. With 0f as the
        // previous position the absolute cursor coordinate became a delta and the
        // host cursor jumped up to 127px on the first movement.
        if (!btMouseRelInitialized) {
            btMouseRelPrevX = cursorX;
            btMouseRelPrevY = cursorY;
            btMouseRelInitialized = true;
            Log.d(TAG, "handleRelativeMode: initialized baseline at (" + cursorX + "," + cursorY + ")");
            return;
        }

        if (nowAtEdge && (relX != 0 || relY != 0)) {
            float scaledRelX = relX * 2.0f;
            float scaledRelY = relY * 2.0f;
            HidManager.sendHexRelData("SecNullData",
                    btMouseRelPrevX + scaledRelX,
                    btMouseRelPrevY + scaledRelY,
                    btMouseRelPrevX, btMouseRelPrevY);
            btMouseRelPrevX += scaledRelX;
            btMouseRelPrevY += scaledRelY;
        } else {
            HidManager.sendHexRelData("SecNullData", cursorX, cursorY,
                    btMouseRelPrevX, btMouseRelPrevY);
            btMouseRelPrevX = cursorX;
            btMouseRelPrevY = cursorY;
        }
    }

    private static boolean handleButton(MotionEvent event, boolean pressed) {
        Log.d(TAG, "handleButton ENTER: pressed=" + pressed + " deviceId=" + event.getDeviceId()
                + " btMouseDeviceId=" + btMouseDeviceId + " btMouseDeviceIdSet=" + btMouseDeviceIdSet
                + " buttonState=" + event.getButtonState()
                + " relX=" + event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                + " relY=" + event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                + " pointerCount=" + event.getPointerCount());

        // Track the BT mouse device ID from button events (reliable source)
        // Button events only come from actual mouse devices, not system cursor
        if (!btMouseDeviceIdSet) {
            btMouseDeviceId = event.getDeviceId();
            btMouseDeviceIdSet = true;
            Log.d(TAG, "BT mouse device ID set from button event: " + btMouseDeviceId);
        }
        // Only accept button events from the BT mouse device
        else if (event.getDeviceId() != btMouseDeviceId) {
            Log.d(TAG, "Ignoring button event from device " + event.getDeviceId() + " (expected " + btMouseDeviceId + ")");
            return false;
        }

        int buttonState = event.getButtonState();

        // Always use event's own coordinates - convert to view coordinates
        // This avoids pollution from system cursor movements in black bars
        float[] viewCoords = toViewCoords(event.getX(), event.getY());
        float x = viewCoords[0];
        float y = viewCoords[1];

        if (pressed) {
            if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
                btMouseLeftDown = true;
                btMouseDragging = true;
            }
            if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
                btMouseRightDown = true;
                btMouseDragging = true;
            }
            if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
                btMouseMiddleDown = true;
                btMouseDragging = true;
            }

            // Send button press with position
            sendBtMouseButtonClick(x, y);
        } else {
            btMouseLeftDown = false;
            btMouseRightDown = false;
            btMouseMiddleDown = false;
            btMouseDragging = false;

            // Send button release
            releaseBtMouseButtons(x, y);
        }

        return true;
    }

    // ===== Send methods =====

    private static void sendBtMouseButtonClick(float x, float y) {
        String buttonKey;
        if (btMouseLeftDown) {
            buttonKey = "SecLeftData";
        } else if (btMouseRightDown) {
            buttonKey = "SecRightData";
        } else if (btMouseMiddleDown) {
            buttonKey = "SecMiddleData";
        } else {
            return;
        }

        // Apply portrait zoom/pan transform first, then normalize to HID
        float viewW = CoordinateConverter.getTouchViewWidth();
        float viewH = CoordinateConverter.getTouchViewHeight();

        // Step 1: Apply inverse portrait zoom/pan transform
        float[] contentCoords = CoordinateConverter.mapToContentCoords(
                x, y, viewW, viewH,
                CoordinateConverter.isPortraitMode(),
                1.0f, 0f, 0f);

        // Step 2: Normalize to HID coordinates
        float bufferW = 0, bufferH = 0;
        if (activity != null) {
            bufferW = activity.getPreviewWidth();
            bufferH = activity.getPreviewHeight();
        }
        float[] videoRect = CoordinateConverter.getVideoRectInView(viewW, viewH, bufferW, bufferH);
        float[] hidCoords = CoordinateConverter.normalizeBtMouseToHid(contentCoords[0], contentCoords[1], viewW, viewH, videoRect);
        float hidX = hidCoords[0];
        float hidY = hidCoords[1];

        // Use calculated HID coordinates from button event
        if (MouseModeManager.isAbsoluteMode()) {
            HidManager.sendBtMouseHidAbsButtonClickData(buttonKey, hidX, hidY);
        } else {
            HidManager.sendHexRelData(buttonKey, 0, 0, 0, 0);
        }
    }

    private static void sendBtMouseDragMove(float x, float y, int buttonState) {
        String buttonKey;
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            buttonKey = "SecLeftData";
        } else if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            buttonKey = "SecRightData";
        } else if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            buttonKey = "SecMiddleData";
        } else {
            return;
        }

        float viewW = CoordinateConverter.getTouchViewWidth();
        float viewH = CoordinateConverter.getTouchViewHeight();

        if (MouseModeManager.isAbsoluteMode()) {
            // Apply portrait zoom/pan transform first
            float[] contentCoords = CoordinateConverter.mapToContentCoords(
                    x, y, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0f, 0f);

            // Then normalize to HID coordinates
            float bufferW = 0, bufferH = 0;
            if (activity != null) {
                bufferW = activity.getPreviewWidth();
                bufferH = activity.getPreviewHeight();
            }
            float[] videoRect = CoordinateConverter.getVideoRectInView(viewW, viewH, bufferW, bufferH);
            float[] hidCoords = CoordinateConverter.normalizeBtMouseToHid(contentCoords[0], contentCoords[1], viewW, viewH, videoRect);
            virtualHidX = hidCoords[0];
            virtualHidY = hidCoords[1];

            HidManager.sendBtMouseHidAbsButtonClickData(buttonKey, virtualHidX, virtualHidY);
        } else {
            // A click carries no movement. Prev stays untouched because the pointer
            // did not move; the next motion frame keeps subtracting the same baseline.
            HidManager.sendHexRelData(buttonKey, 0, 0, 0, 0);
        }
    }

    private static void releaseBtMouseButtons(float x, float y) {
        Log.d(TAG, "releaseBtMouseButtons CALLED with x=" + x + " y=" + y, new Exception("Stack trace"));
        if (MouseModeManager.isAbsoluteMode()) {
            // Apply portrait zoom/pan transform first, then normalize to HID
            float viewW = CoordinateConverter.getTouchViewWidth();
            float viewH = CoordinateConverter.getTouchViewHeight();

            // Step 1: Apply inverse portrait zoom/pan transform
            float[] contentCoords = CoordinateConverter.mapToContentCoords(
                    x, y, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0f, 0f);

            // Step 2: Normalize to HID coordinates
            float bufferW = 0, bufferH = 0;
            if (activity != null) {
                bufferW = activity.getPreviewWidth();
                bufferH = activity.getPreviewHeight();
            }
            float[] videoRect = CoordinateConverter.getVideoRectInView(viewW, viewH, bufferW, bufferH);
            float[] hidCoords = CoordinateConverter.normalizeBtMouseToHid(contentCoords[0], contentCoords[1], viewW, viewH, videoRect);

            Log.d(TAG, "releaseBtMouseButtons: viewCoords=(" + x + "," + y
                    + ") contentCoords=(" + contentCoords[0] + "," + contentCoords[1]
                    + ") hidCoords=(" + hidCoords[0] + "," + hidCoords[1] + ")");

            HidManager.sendBtMouseHidAbsData(hidCoords[0], hidCoords[1]);
        } else {
            HidManager.sendHexRelData("SecNullData", 0, 0, 0, 0);
        }
    }

    // ===== Connection state =====

    private static void setBtMouseConnected(boolean connected) {
        if (btMouseConnected == connected) {
            return;
        }
        btMouseConnected = connected;
        Log.d(TAG, "BT Mouse connection state changed: " + connected);

        if (callbacks != null) {
            callbacks.onBlackBorderVisibilityChanged(connected);
            callbacks.onSystemGestureExclusionChanged(connected);
        }

        if (connected) {
            if (btMouseCursorRestoreHandler != null && btMouseCursorRestoreRunnable != null) {
                btMouseCursorRestoreHandler.removeCallbacks(btMouseCursorRestoreRunnable);
            }
            if (btMouseConnectionListener != null) {
                btMouseConnectionListener.onBtMouseConnected(true);
            }
        } else {
            // Delayed disconnect notification
            if (btMouseCursorRestoreHandler == null) {
                btMouseCursorRestoreHandler = new Handler(android.os.Looper.getMainLooper());
            }
            if (btMouseCursorRestoreRunnable == null) {
                btMouseCursorRestoreRunnable = () -> {
                    if (btMouseConnectionListener != null) {
                        btMouseConnectionListener.onBtMouseConnected(false);
                    }
                };
            }
            btMouseCursorRestoreHandler.removeCallbacks(btMouseCursorRestoreRunnable);
            btMouseCursorRestoreHandler.postDelayed(btMouseCursorRestoreRunnable, CURSOR_RESTORE_DELAY_MS);
        }
    }

    public static void cancelBtMouseCursorRestore() {
        if (btMouseCursorRestoreHandler != null && btMouseCursorRestoreRunnable != null) {
            btMouseCursorRestoreHandler.removeCallbacks(btMouseCursorRestoreRunnable);
        }
    }

    private static void resetBtMouseDisconnectTimeout() {
        if (btMouseDisconnectHandler == null) {
            btMouseDisconnectHandler = new Handler(android.os.Looper.getMainLooper());
            btMouseDisconnectRunnable = () -> {
                setBtMouseConnected(false);
                btMouseLeftDown = false;
                btMouseRightDown = false;
                btMouseMiddleDown = false;
                btMouseDragging = false;
                btMouseRelInitialized = false;
                CoordinateConverter.resetLearnedRange();
                virtualHidInitialized = false;
                lastSentHidX = -1f;
                lastSentHidY = -1f;
                // Reset device ID tracking
                btMouseDeviceId = -1;
                btMouseDeviceIdSet = false;
            };
        }
        btMouseDisconnectHandler.removeCallbacks(btMouseDisconnectRunnable);
        btMouseDisconnectHandler.postDelayed(btMouseDisconnectRunnable, BT_MOUSE_DISCONNECT_TIMEOUT_MS);

        if (btMouseCursorRestoreHandler != null && btMouseCursorRestoreRunnable != null) {
            btMouseCursorRestoreHandler.removeCallbacks(btMouseCursorRestoreRunnable);
        }
    }

    // ===== Coordinate conversion =====

    private static float[] toViewCoords(float screenX, float screenY) {
        if (activity != null && activity.mBinding != null
                && activity.mBinding.viewMainPreview != null) {
            // Use getLocationInWindow - returns position relative to window content area
            // This matches the absolute screen position (verified via dumpsys)
            int[] loc = new int[2];
            activity.mBinding.viewMainPreview.getLocationInWindow(loc);

            // Calculate view-relative coordinates
            float viewX = screenX - loc[0];
            float viewY = screenY - loc[1];

            // Detailed logging for coordinate debugging
            Log.d(TAG, "toViewCoords: screen=(" + screenX + "," + screenY
                    + ") viewLocInWindow=(" + loc[0] + "," + loc[1]
                    + ") viewCoords=(" + viewX + "," + viewY + ")");
            return new float[]{viewX, viewY};
        }
        return new float[]{screenX, screenY};
    }

    // ===== Reset =====

    public static void reset() {
        btMouseLeftDown = false;
        btMouseRightDown = false;
        btMouseMiddleDown = false;
        btMouseDragging = false;
        virtualHidX = HID_LOGICAL_MAX / 2f;
        virtualHidY = HID_LOGICAL_MAX / 2f;
        virtualHidInitialized = false;
        lastSentHidX = -1f;
        lastSentHidY = -1f;
        CoordinateConverter.resetLearnedRange();
        btMouseDeviceId = -1;
        btMouseDeviceIdSet = false;
    }
}
