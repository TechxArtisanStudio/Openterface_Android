package com.openterface.AOS.serial;

import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.openterface.AOS.target.HidManager;
import com.openterface.AOS.target.MouseModeManager;

/**
 * Handles finger touch input events on the video view.
 * Implements KeyCmd-style gesture mapping:
 * - 1-finger tap → left click
 * - 1-finger double tap → double left click
 * - 1-finger double tap and hold → drag mode
 * - 1-finger move → mouse move
 * - 2-finger tap → right click
 * - 2-finger drag → scroll
 * - 2-finger pinch → zoom
 */
public class TouchInputHandler {
    private static final String TAG = "OP-TOUCH";

    // ===== Timing constants =====
    private static final long TAP_DELAY_MS = 150;
    private static final long TAP_TIMEOUT_MS = 400;
    private static final long DOUBLE_TAP_TIMEOUT_MS = 300;

    // ===== Distance thresholds =====
    private static final float TAP_MOVE_THRESHOLD_PX = 10f;
    private static final float DOUBLE_TAP_SLOP_PX = 15f;
    private static final float TWO_FINGER_TAP_MAX_PRESS_GAP_MS = 100f;
    // ===== Two-finger classification =====
    // A gesture is classified once, from the deltas accumulated since the first MOVE.
    // Classifying on per-frame deltas made slow scrolling impossible: the centre never
    // moved far within a single frame, so the gesture stayed "undecided" and the finger
    // lift was reported as a right click.
    private static final int TWO_FINGER_UNCLASSIFIED = 0;
    private static final int TWO_FINGER_SCROLL = 1;
    private static final int TWO_FINGER_PINCH = 2;

    private static final int AXIS_FREE = 0;
    private static final int AXIS_HORIZONTAL = 1;
    private static final int AXIS_VERTICAL = 2;

    private static final float SCROLL_MIN_TRAVEL_PX = 5f;   // Δcenter must exceed this
    private static final float PINCH_MIN_TRAVEL_PX = 8f;    // Δdist must exceed this
    private static final float GESTURE_DOMINANCE_RATIO = 1.8f;
    private static final float AXIS_LOCK_ANGLE_DEG = 30f;

    // ===== State =====
    private final Handler handler = new Handler();

    // Coordinate tracking
    private float startMoveMSX, startMoveMSY;
    private static float lastMoveMSX, lastMoveMSY;
    private float lastMoveX, lastMoveY;

    // Gesture state
    private boolean pendingDoubleTapDrag = false;

    // One-finger tap tracking
    private float tapDownX, tapDownY;
    private long tapDownTime;
    private boolean tapCancelled = false;
    private Runnable pendingSingleTap = null;
    private long lastSingleTapTime = 0;
    private float lastSingleTapX, lastSingleTapY;

    // Two-finger scroll tracking
    private float twoFingerDownCenterX, twoFingerDownCenterY;
    private float twoFingerPrevX, twoFingerPrevY;
    private float twoFingerScrollAccumX = 0f;
    private float twoFingerScrollAccumY = 0f;
    private int twoFingerMode = TWO_FINGER_UNCLASSIFIED;
    private int twoFingerAxis = AXIS_FREE;
    private boolean inTwoFingerSequence = false;
    private boolean isInitialTwoFingerMove = false;

    // Pending initial absolute position send
    private Runnable pendingInitialAbsDown = null;
    private boolean initialAbsDownSent = false;

    // Two-finger right-click state
    private boolean isTwoFingerClick = false;

    // Pinch-to-zoom state
    private float twoFingerStartDist = 0f;
    private float twoFingerPrevDist = 0f;

    // Drag mode state — entered only by double-tap-and-drag
    private boolean isDragMode = false;

    // Callback for zoom operations
    private ZoomCallback zoomCallback;

    public interface ZoomCallback {
        void onZoomAdjust(float zoomFactor);
    }

    // ===== Initialization =====

    public void setZoomCallback(ZoomCallback callback) {
        this.zoomCallback = callback;
    }

    // ===== Public API =====

    /**
     * Handle touch event.
     * @return true if event was consumed
     */
    public boolean onTouch(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        int toolType = event.getToolType(0);
        int source = event.getSource();
        boolean isMouseEvent = (toolType == MotionEvent.TOOL_TYPE_MOUSE)
                || ((source & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE);

        Log.d(TAG, "TOUCH_EVENT: action=" + action + " x=" + x + " y=" + y
                + " toolType=" + toolType + " source=0x" + Integer.toHexString(source)
                + " isMouseEvent=" + isMouseEvent
                + " btMouseConnected=" + BtMouseInputHandler.isBtMouseConnected());

        // Ignore mouse-generated touch events
        // Android maps mouse button events to both GenericMotionEvent and MotionEvent
        // We only want to process them via the BT mouse path (handleGenericMotionEvent),
        // not the touch path. This applies regardless of BT mouse connection state
        // because the event itself came from a mouse device (toolType=3 or SOURCE_MOUSE).
        if (isMouseEvent) {
            Log.d(TAG, "TOUCH_IGNORED: mouse event (toolType=" + toolType + " source=0x"
                    + Integer.toHexString(source) + ")");
            return false;
        }

        // Ignore touch if BT mouse button is pressed (legacy protection)
        if (BtMouseInputHandler.shouldIgnoreTouch()) {
            Log.d(TAG, "TOUCH_IGNORED: shouldIgnoreTouch=true");
            return false;
        }

        int pointerCount = event.getPointerCount();

        // Handle two-finger gestures first
        if (pointerCount >= 2 || action == MotionEvent.ACTION_POINTER_UP) {
            return handleTwoFingerGesture(event);
        }

        // Handle single-finger gestures
        if (pointerCount == 1) {
            return handleOneFingerGesture(event);
        }

        return false;
    }

    // ===== One-finger gesture handling =====

    private boolean handleOneFingerGesture(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        // A two-finger gesture can leave one finger on the screen. Swallow its
        // remaining events, otherwise a finished pinch would move the mouse or
        // even fire a click when that finger is lifted.
        if (inTwoFingerSequence) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                inTwoFingerSequence = false;
                resetTwoFingerState();
                lastMoveX = 0;
                lastMoveY = 0;
                lastMoveMSX = 0;
                lastMoveMSY = 0;
            }
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // New gesture: clear any stale two-finger state (e.g. previous
                // gesture was cancelled without a POINTER_UP event).
                resetTwoFingerState();
                handleOneFingerDown(event, x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                handleOneFingerMove(event, x, y);
                break;

            case MotionEvent.ACTION_UP:
                handleOneFingerUp(event, x, y);
                break;

            case MotionEvent.ACTION_CANCEL:
                handleOneFingerCancel();
                break;
        }
        return true;
    }

    private void handleOneFingerDown(MotionEvent event, float x, float y) {
        tapDownX = x;
        tapDownY = y;
        tapDownTime = event.getEventTime();
        tapCancelled = false;
        pendingDoubleTapDrag = false;
        initialAbsDownSent = false;

        // Sync the relative-mode baselines to this DOWN position. Several gesture exit
        // paths zero them (cancel, double-tap-drag release, two-finger sequence end), so
        // without this the first MOVE of the next gesture is measured against 0 and jumps.
        lastMoveX = x;
        lastMoveY = y;
        lastMoveMSX = x;
        lastMoveMSY = y;

        initHandActionDownMouse(event);

        // Second press inside the double-tap window → enter the drag *candidate* state.
        // It only becomes a real drag once the finger moves past TAP_MOVE_THRESHOLD_PX
        // (see handleOneFingerMove). A press that stays put is the second click of a
        // double click (see handleOneFingerUp).
        if (lastSingleTapTime > 0) {
            long timeSinceLastTap = event.getEventTime() - lastSingleTapTime;
            float distSinceLastTap = dist(x, y, lastSingleTapX, lastSingleTapY);
            if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && distSinceLastTap < DOUBLE_TAP_SLOP_PX) {
                // Flush the first click now, so both clicks land inside the host's
                // double-click window even when the first one is still queued.
                flushPendingSingleTap();
                pendingDoubleTapDrag = true;
                Log.d(TAG, "Double-tap candidate at (" + x + "," + y + ")");
            }
        }

        // Schedule delayed initial abs down
        pendingInitialAbsDown = () -> {
            if (!initialAbsDownSent && MouseModeManager.isAbsoluteMode()) {
                try {
                    float viewW = CoordinateConverter.getTouchViewWidth();
                    float viewH = CoordinateConverter.getTouchViewHeight();
                    float[] content = CoordinateConverter.mapToContentCoords(
                            startMoveMSX, startMoveMSY, viewW, viewH,
                            CoordinateConverter.isPortraitMode(),
                            1.0f, 0, 0);
                    HidManager.sendHexAbsData(content[0], content[1]);
                } catch (Exception e) {
                    Log.e(TAG, "pendingInitialAbsDown error: " + e.getMessage(), e);
                }
                initialAbsDownSent = true;
            }
        };
        handler.postDelayed(pendingInitialAbsDown, 80);
    }

    private void handleOneFingerMove(MotionEvent event, float x, float y) {
        float distMoved = dist(x, y, tapDownX, tapDownY);
        if (distMoved > TAP_MOVE_THRESHOLD_PX && !tapCancelled && !isDragMode) {
            cancelPendingSingleTap();
            tapCancelled = true;

            // Double-tap drag candidate: lock the drag only after the finger actually
            // moves. Baselines are already kept current by handleNormalMove(), so the
            // first drag frame must NOT re-sync them or the movement is applied twice.
            if (pendingDoubleTapDrag) {
                pendingDoubleTapDrag = false;
                isDragMode = true;
                Log.d(TAG, "Double-tap-drag: drag locked after " + distMoved + "px");
            }
        }

        if (isDragMode) {
            handleDragMove(x, y);
        } else {
            handleNormalMove(x, y);
        }
        lastMoveX = x;
        lastMoveY = y;
    }

    private void handleOneFingerUp(MotionEvent event, float x, float y) {
        cancelPendingInitialAbsDown();

        long duration = event.getEventTime() - tapDownTime;
        float distLifted = dist(x, y, tapDownX, tapDownY);

        // Locked drag (entered in handleOneFingerMove) → release the left button.
        // All four baselines are zeroed because the next DOWN re-syncs them.
        if (isDragMode) {
            Log.d(TAG, ">>> DRAG END: releasing left button");
            releaseMouseAndReset();
            isDragMode = false;
            lastMoveX = 0;
            lastMoveY = 0;
            lastMoveMSX = 0;
            lastMoveMSY = 0;
            return;
        }

        // Second press of a double tap that never moved: that press IS the second click.
        // The first click was already flushed when the second DOWN arrived.
        if (pendingDoubleTapDrag) {
            pendingDoubleTapDrag = false;
            Log.d(TAG, "Double tap → second click at (" + tapDownX + "," + tapDownY + ")");
            handleSingleClick(tapDownX, tapDownY);
            lastSingleTapTime = 0;
            lastMoveX = 0;
            lastMoveY = 0;
            lastMoveMSX = 0;
            lastMoveMSY = 0;
            return;
        }

        // Check for tap
        boolean validTap = !tapCancelled
                && duration < TAP_TIMEOUT_MS
                && distLifted <= TAP_MOVE_THRESHOLD_PX;

        if (validTap) {
            // Delay the click so a following second press can flush it as the first
            // click of a double click. The click lands at the DOWN position, not where
            // the finger happened to lift.
            cancelPendingSingleTap();
            pendingSingleTap = () -> {
                handleSingleClick(tapDownX, tapDownY);
                pendingSingleTap = null;
            };
            lastSingleTapTime = event.getEventTime();
            lastSingleTapX = tapDownX;
            lastSingleTapY = tapDownY;
            handler.postDelayed(pendingSingleTap, TAP_DELAY_MS);
        }

        if (!tapCancelled) {
            handActionUpMouse(event);
        } else {
            HidManager.releaseMSRelData();
            lastMoveMSX = 0;
            lastMoveMSY = 0;
        }
    }

    private void handleOneFingerCancel() {
        cancelPendingInitialAbsDown();
        cancelPendingSingleTap();
        pendingDoubleTapDrag = false;
        if (isDragMode) {
            releaseMouseAndReset();
            isDragMode = false;
        } else {
            HidManager.releaseMSRelData();
        }
        lastMoveMSX = 0;
        lastMoveMSY = 0;
        lastMoveX = 0;
        lastMoveY = 0;
    }

    // ===== Move handlers =====

    private void handleNormalMove(float x, float y) {
        if (MouseModeManager.isAbsoluteMode()) {
            firePendingInitialAbsDown();
            float viewW = CoordinateConverter.getTouchViewWidth();
            float viewH = CoordinateConverter.getTouchViewHeight();
            float[] content = CoordinateConverter.mapToContentCoords(
                    x, y, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0, 0);
            HidManager.sendHexAbsData(content[0], content[1]);
            lastMoveMSX = x;
            lastMoveMSY = y;
        } else {
            float zoomScale = ZoomPanManager.getEffectiveZoomScale();
            float effectiveDeltaScale = 1.0f / zoomScale;
            float adjX = lastMoveMSX + (x - lastMoveX) * effectiveDeltaScale;
            float adjY = lastMoveMSY + (y - lastMoveY) * effectiveDeltaScale;
            HidManager.sendHexRelData("SecNullData", adjX, adjY, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    private void handleDragMove(float x, float y) {
        if (MouseModeManager.isAbsoluteMode()) {
            firePendingInitialAbsDown();
            float viewW = CoordinateConverter.getTouchViewWidth();
            float viewH = CoordinateConverter.getTouchViewHeight();
            float[] content = CoordinateConverter.mapToContentCoords(
                    x, y, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0, 0);
            HidManager.sendHexAbsButtonClickData("SecLeftData", content[0], content[1]);
            lastMoveMSX = x;
            lastMoveMSY = y;
        } else {
            float zoomScale = ZoomPanManager.getEffectiveZoomScale();
            float effectiveDeltaScale = 1.0f / zoomScale;
            float adjX = lastMoveMSX + (x - lastMoveX) * effectiveDeltaScale;
            float adjY = lastMoveMSY + (y - lastMoveY) * effectiveDeltaScale;
            HidManager.sendHexRelData("SecLeftData", adjX, adjY, lastMoveMSX, lastMoveMSY);
            lastMoveMSX = x;
            lastMoveMSY = y;
        }
    }

    // ===== Two-finger gesture handling =====

    private boolean handleTwoFingerGesture(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                handleTwoFingerDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                handleTwoFingerMove(event);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                handleTwoFingerPointerUp(event);
                break;

            case MotionEvent.ACTION_CANCEL:
                // Gesture aborted by the system or a parent view. Without this reset
                // the stuck pinch flags turned every later two-finger gesture into
                // a scroll gesture, so pinch appeared to work only once.
                Log.d(TAG, "Two-finger gesture cancelled, resetting state");
                inTwoFingerSequence = false;
                resetTwoFingerState();
                break;
        }
        return true;
    }

    private void handleTwoFingerDown(MotionEvent event) {
        cancelPendingSingleTap();
        // A pinch can start inside the 80ms window of the initial absolute-mode report;
        // leaving it queued sends that report and jumps the cursor to finger 0.
        cancelPendingInitialAbsDown();

        // Until the last finger is lifted, single-finger events belong to this
        // two-finger gesture and must not be handled as mouse input.
        inTwoFingerSequence = true;

        twoFingerDownCenterX = (event.getX(0) + event.getX(1)) / 2f;
        twoFingerDownCenterY = (event.getY(0) + event.getY(1)) / 2f;
        twoFingerPrevX = twoFingerDownCenterX;
        twoFingerPrevY = twoFingerDownCenterY;
        twoFingerScrollAccumX = 0f;
        twoFingerScrollAccumY = 0f;
        twoFingerMode = TWO_FINGER_UNCLASSIFIED;
        twoFingerAxis = AXIS_FREE;
        // A right click needs both fingers to land together. A second finger arriving
        // late (more than 100ms after the first) is a deliberate two-finger gesture.
        isTwoFingerClick = (event.getEventTime() - event.getDownTime()) <= TWO_FINGER_TAP_MAX_PRESS_GAP_MS;

        // Record initial distance for pinch
        twoFingerStartDist = calculateDistance(
                event.getX(0), event.getY(0),
                event.getX(1), event.getY(1));
        twoFingerPrevDist = twoFingerStartDist;

        // Always start a new two-finger gesture unclassified, otherwise flags left
        // over from the previous gesture would disable pinch permanently.
        isInitialTwoFingerMove = true;
    }

    private void handleTwoFingerMove(MotionEvent event) {
        // ACTION_MOVE only reaches this method while two fingers are down; the single
        // finger left over after a POINTER_UP is swallowed by inTwoFingerSequence.
        if (event.getPointerCount() != 2) {
            return;
        }

        float cx = (event.getX(0) + event.getX(1)) / 2f;
        float cy = (event.getY(0) + event.getY(1)) / 2f;
        float currentDist = calculateDistance(
                event.getX(0), event.getY(0),
                event.getX(1), event.getY(1));

        if (isInitialTwoFingerMove) {
            // Re-base on the first MOVE — the DOWN position is the sloppy landing spot,
            // and classifying from it would count hand placement as gesture travel.
            isInitialTwoFingerMove = false;
            twoFingerDownCenterX = cx;
            twoFingerDownCenterY = cy;
            twoFingerStartDist = currentDist;
            twoFingerPrevDist = currentDist;
            twoFingerPrevX = cx;
            twoFingerPrevY = cy;
            return;
        }

        // Classify from gesture-wide travel, not from this frame's delta.
        float totalCenterX = cx - twoFingerDownCenterX;
        float totalCenterY = cy - twoFingerDownCenterY;
        float totalCenterDelta = (float) Math.hypot(totalCenterX, totalCenterY);
        float totalDistDelta = Math.abs(currentDist - twoFingerStartDist);

        if (twoFingerMode == TWO_FINGER_UNCLASSIFIED) {
            if (totalDistDelta > PINCH_MIN_TRAVEL_PX
                    && totalDistDelta > GESTURE_DOMINANCE_RATIO * totalCenterDelta) {
                twoFingerMode = TWO_FINGER_PINCH;
                isTwoFingerClick = false;
                Log.d(TAG, "Gesture → PINCH (Δdist=" + totalDistDelta
                        + " Δcenter=" + totalCenterDelta + ")");
            } else if (totalCenterDelta > SCROLL_MIN_TRAVEL_PX
                    && totalCenterDelta > GESTURE_DOMINANCE_RATIO * totalDistDelta) {
                twoFingerMode = TWO_FINGER_SCROLL;
                isTwoFingerClick = false;
                twoFingerAxis = lockAxis(totalCenterX, totalCenterY);
                Log.d(TAG, "Gesture → SCROLL axis=" + twoFingerAxis
                        + " (Δcenter=" + totalCenterDelta + " Δdist=" + totalDistDelta + ")");
            }
            // Below both thresholds the gesture stays undecided: hysteresis, so a tap
            // with a few pixels of hand jitter is still a right click.
        }

        if (twoFingerMode == TWO_FINGER_PINCH) {
            // Incremental zoom: compare against last frame's distance
            if (twoFingerPrevDist > 0f && currentDist > 0f) {
                float zoomFactor = currentDist / twoFingerPrevDist;
                // Guard against a single huge jump (finger teleport / dropped frame)
                zoomFactor = Math.max(0.7f, Math.min(1.4f, zoomFactor));
                if (zoomCallback != null && Math.abs(zoomFactor - 1f) > 0.001f) {
                    zoomCallback.onZoomAdjust(zoomFactor);
                }
            }
        } else if (twoFingerMode == TWO_FINGER_SCROLL) {
            // Per-frame deltas drive the scroll output; the locked axis discards the
            // other one so diagonal jitter cannot bleed into both directions.
            float dx = cx - twoFingerPrevX;
            float dy = cy - twoFingerPrevY;

            if (twoFingerAxis != AXIS_VERTICAL) {
                twoFingerScrollAccumX += dx / 3f;
            }
            if (twoFingerAxis != AXIS_HORIZONTAL) {
                twoFingerScrollAccumY += -dy / 3f;
            }

            // CH9329 relative mouse reports carry five data bytes — fixed flag, buttons,
            // X, Y, wheel — so there is no AC Pan byte for horizontal scrolling. The
            // horizontal accumulator is drained but never sent; locking the axis still
            // keeps diagonal jitter out of the vertical wheel.
            int scrollX = (int) twoFingerScrollAccumX;
            int scrollY = (int) twoFingerScrollAccumY;

            if (scrollX != 0) twoFingerScrollAccumX -= scrollX;
            if (scrollY != 0) twoFingerScrollAccumY -= scrollY;

            if (scrollX != 0 || scrollY != 0) {
                HidManager.handleTwoFingerPanSlideUpDown(scrollY);
            }
        }

        twoFingerPrevDist = currentDist;
        twoFingerPrevX = cx;
        twoFingerPrevY = cy;
    }

    /**
     * Lock the scroll to the dominant axis when the initial direction is within
     * {@link #AXIS_LOCK_ANGLE_DEG} of it; otherwise keep both axes live.
     */
    private int lockAxis(float totalDx, float totalDy) {
        float absX = Math.abs(totalDx);
        float absY = Math.abs(totalDy);
        if (absX < 0.001f && absY < 0.001f) {
            return AXIS_FREE;
        }
        float angleFromHorizontal = (float) Math.toDegrees(Math.atan2(absY, absX));
        if (angleFromHorizontal <= AXIS_LOCK_ANGLE_DEG) {
            return AXIS_HORIZONTAL;
        }
        if (angleFromHorizontal >= 90f - AXIS_LOCK_ANGLE_DEG) {
            return AXIS_VERTICAL;
        }
        return AXIS_FREE;
    }

    private void handleTwoFingerPointerUp(MotionEvent event) {
        // Capture state before reset for right-click detection
        boolean wasClassified = twoFingerMode != TWO_FINGER_UNCLASSIFIED;
        boolean wasClick = isTwoFingerClick;

        // ACTION_POINTER_UP still reports the lifting pointer in getPointerCount(),
        // so the number of fingers that remain down is count - 1. Using the raw count
        // kept the classification flags alive forever, which made pinch work only once
        // and behave like a scroll afterwards.
        int remaining = event.getPointerCount() - 1;
        if (remaining <= 1) {
            // Reset so the next pinch/scroll gesture can be recognized
            resetTwoFingerState();
        }

        if (wasClassified) {
            // Already scroll/pinch - no right click
        } else if (wasClick) {
            // Two-finger tap - send RIGHT CLICK
            if (MouseModeManager.isAbsoluteMode()) {
                float viewW = CoordinateConverter.getTouchViewWidth();
                float viewH = CoordinateConverter.getTouchViewHeight();
                float[] content = CoordinateConverter.mapToContentCoords(
                        twoFingerDownCenterX, twoFingerDownCenterY, viewW, viewH,
                        CoordinateConverter.isPortraitMode(),
                        1.0f, 0, 0);
                HidManager.sendHexAbsButtonClickData("SecRightData", content[0], content[1]);
                HidManager.sendHexAbsData(content[0], content[1]);
            } else {
                HidManager.sendHexRelData("SecRightData",
                        twoFingerDownCenterX, twoFingerDownCenterY, 0, 0);
                HidManager.releaseMSRelData();
            }
            Log.d(TAG, "Two-finger tap confirmed → RIGHT CLICK");
        }
    }

    /**
     * Clear every two-finger gesture flag so the next gesture starts clean.
     * inTwoFingerSequence is intentionally left untouched - it is cleared when
     * the last finger of the gesture is lifted.
     */
    private void resetTwoFingerState() {
        twoFingerMode = TWO_FINGER_UNCLASSIFIED;
        twoFingerAxis = AXIS_FREE;
        isTwoFingerClick = false;
        twoFingerStartDist = 0f;
        twoFingerPrevDist = 0f;
        twoFingerScrollAccumX = 0f;
        twoFingerScrollAccumY = 0f;
        isInitialTwoFingerMove = false;
    }

    // ===== Helper methods =====

    private void firePendingInitialAbsDown() {
        if (pendingInitialAbsDown != null && !initialAbsDownSent) {
            handler.removeCallbacks(pendingInitialAbsDown);
            pendingInitialAbsDown.run();
            pendingInitialAbsDown = null;
        }
    }

    private void cancelPendingInitialAbsDown() {
        if (pendingInitialAbsDown != null) {
            handler.removeCallbacks(pendingInitialAbsDown);
            pendingInitialAbsDown = null;
        }
    }

    private void cancelPendingSingleTap() {
        if (pendingSingleTap != null) {
            handler.removeCallbacks(pendingSingleTap);
            pendingSingleTap = null;
        }
    }

    /**
     * Run the queued single click immediately instead of waiting for TAP_DELAY_MS.
     * Called when the second press of a double tap arrives, so the first click is
     * already on the wire and the second one lands inside the host's double-click window.
     */
    private void flushPendingSingleTap() {
        if (pendingSingleTap != null) {
            handler.removeCallbacks(pendingSingleTap);
            pendingSingleTap.run();
            pendingSingleTap = null;
        }
    }

    private void handleSingleClick(float x, float y) {
        if (MouseModeManager.isAbsoluteMode()) {
            float viewW = CoordinateConverter.getTouchViewWidth();
            float viewH = CoordinateConverter.getTouchViewHeight();
            float[] content = CoordinateConverter.mapToContentCoords(
                    x, y, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0, 0);
            HidManager.sendHexAbsButtonClickData("SecLeftData", content[0], content[1]);
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            HidManager.sendHexRelData("SecLeftData", x, y, 0, 0);
            HidManager.releaseMSRelData();
        }
        Log.d(TAG, "Single tap → LEFT CLICK");
    }

    private void releaseMouseAndReset() {
        if (MouseModeManager.isAbsoluteMode()) {
            float viewW = CoordinateConverter.getTouchViewWidth();
            float viewH = CoordinateConverter.getTouchViewHeight();
            float[] content = CoordinateConverter.mapToContentCoords(
                    lastMoveMSX, lastMoveMSY, viewW, viewH,
                    CoordinateConverter.isPortraitMode(),
                    1.0f, 0, 0);
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            HidManager.releaseMSRelData();
        }
    }

    private void initHandActionDownMouse(MotionEvent event) {
        startMoveMSX = event.getX();
        startMoveMSY = event.getY();
    }

    private void handActionUpMouse(MotionEvent event) {
        float viewW = CoordinateConverter.getTouchViewWidth();
        float viewH = CoordinateConverter.getTouchViewHeight();
        float[] content = CoordinateConverter.mapToContentCoords(
                startMoveMSX, startMoveMSY, viewW, viewH,
                CoordinateConverter.isPortraitMode(),
                1.0f, 0, 0);

        if (MouseModeManager.isAbsoluteMode()) {
            HidManager.sendHexAbsData(content[0], content[1]);
        } else {
            HidManager.releaseMSRelData();
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ===== Reset =====

    public void reset() {
        isDragMode = false;
        inTwoFingerSequence = false;
        resetTwoFingerState();
        tapCancelled = false;
        pendingDoubleTapDrag = false;
        cancelPendingSingleTap();
        cancelPendingInitialAbsDown();
        lastMoveMSX = 0;
        lastMoveMSY = 0;
        lastMoveX = 0;
        lastMoveY = 0;
    }
}
