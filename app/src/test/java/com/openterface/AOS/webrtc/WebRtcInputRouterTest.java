package com.openterface.AOS.webrtc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for WebRtcInputRouter — input routing logic.
 * Uses a mock HidInputSender to verify that the router sends the correct
 * HID commands for keyboard/mouse events without needing Android runtime.
 */
public class WebRtcInputRouterTest {

    private MockHidInputSender mockSender;
    private WebRtcInputRouter router;

    @Before
    public void setUp() {
        mockSender = new MockHidInputSender();
        router = new WebRtcInputRouter(mockSender);
    }

    // ====== Mouse routing tests ======

    @Test
    public void setFramebufferSizeStoresDimensions() {
        router.setFramebufferSize(1920, 1080);
        // Trigger a mouse event to verify dimensions are passed through
        router.onMouseEvent(0, 100, 200, false);
        assertEquals(1920, mockSender.lastWidth);
        assertEquals(1080, mockSender.lastHeight);
    }

    @Test
    public void mousePureMovementSendsAbsMove() {
        // buttonMask=0 means no buttons pressed — pure cursor movement
        router.onMouseEvent(0, 100, 200, false);
        assertEquals("sendAbsMove", mockSender.lastAction);
        assertEquals(100, mockSender.lastX);
        assertEquals(200, mockSender.lastY);
        assertNull(mockSender.lastClickType);
    }

    @Test
    public void mouseLeftButtonPressSendsLeftClick() {
        router.onMouseEvent(1, 500, 300, true);
        assertEquals("sendAbsButtonClick", mockSender.lastAction);
        assertEquals("SecLeftData", mockSender.lastClickType);
        assertEquals(500, mockSender.lastX);
        assertEquals(300, mockSender.lastY);
    }

    @Test
    public void mouseMiddleButtonPressSendsMiddleClick() {
        router.onMouseEvent(2, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockSender.lastAction);
        assertEquals("SecMiddleData", mockSender.lastClickType);
    }

    @Test
    public void mouseRightButtonPressSendsRightClick() {
        router.onMouseEvent(4, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockSender.lastAction);
        assertEquals("SecRightData", mockSender.lastClickType);
    }

    @Test
    public void mouseButtonReleaseSendsMove() {
        // First press a button
        router.onMouseEvent(1, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockSender.lastAction);

        // Then release (buttonMask=0) — should send move
        router.onMouseEvent(0, 100, 200, false);
        assertEquals("sendAbsMove", mockSender.lastAction);
    }

    @Test
    public void mouseMovementOnlyUpdatesPosition() {
        // Press then move: the second call with same buttonMask is movement
        router.onMouseEvent(1, 100, 200, true); // press left
        mockSender.reset();

        // Move with button still pressed (buttonMask=1) — button state didn't change
        router.onMouseEvent(1, 200, 300, true);
        // The router always calls setMouseDimensions first, then checks button state.
        // When buttonMask hasn't changed from lastButtonMask, it should NOT call
        // any mouse HID method (click or move).
        // Note: setMouseDimensions IS called (for dimension normalization), but
        // the actual mouse action (click/move) should NOT happen.
        assertNotEquals("sendAbsButtonClick", mockSender.lastAction);
        assertNotEquals("sendAbsMove", mockSender.lastAction);
    }

    // ====== Keyboard routing tests ======

    @Test
    public void keyboardLetterSendsKeyboardKey() {
        router.onKeyboardEvent(0x61, true); // 'a' press
        assertEquals("sendKeyboardKey", mockSender.lastAction);
        assertEquals("a", mockSender.lastKeyName);

        router.onKeyboardEvent(0x61, false); // release
        assertEquals("sendKeyboardRelease", mockSender.lastAction);
    }

    @Test
    public void keyboardEnterSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF0D, true); // Return press
        assertEquals("sendKeyboardKey", mockSender.lastAction);
        assertEquals("ENTER", mockSender.lastKeyName);
    }

    @Test
    public void keyboardEscapeSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF1B, true); // Escape press
        assertEquals("sendKeyboardKey", mockSender.lastAction);
        assertEquals("Esc", mockSender.lastKeyName);
    }

    @Test
    public void keyboardShiftSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE1, true); // Shift_L press
        assertEquals("sendKeyboardPress", mockSender.lastAction);
        assertEquals("Shift", mockSender.lastFunctionKey);
        assertEquals("SHIFT_LEFT", mockSender.lastKeyName);
    }

    @Test
    public void keyboardShiftReleaseSendsKeyboardRelease() {
        router.onKeyboardEvent(0xFFE1, true);  // press
        router.onKeyboardEvent(0xFFE1, false); // release
        assertEquals("sendKeyboardRelease", mockSender.lastAction);
    }

    @Test
    public void keyboardControlSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE3, true); // Control_L press
        assertEquals("sendKeyboardPress", mockSender.lastAction);
        assertEquals("Ctrl", mockSender.lastFunctionKey);
        assertEquals("CTRL_LEFT", mockSender.lastKeyName);
    }

    @Test
    public void keyboardAltSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE9, true); // Alt_L press
        assertEquals("sendKeyboardPress", mockSender.lastAction);
        assertEquals("Alt", mockSender.lastFunctionKey);
        assertEquals("ALT_LEFT", mockSender.lastKeyName);
    }

    @Test
    public void keyboardWinSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFEB, true); // Super_L press
        assertEquals("sendKeyboardPress", mockSender.lastAction);
        assertEquals("Win", mockSender.lastFunctionKey);
    }

    @Test
    public void keyboardFunctionKeySendsKeyboardKey() {
        router.onKeyboardEvent(0xFFBE, true); // F1
        assertEquals("sendKeyboardKey", mockSender.lastAction);
        assertEquals("F1", mockSender.lastKeyName);

        router.onKeyboardEvent(0xFFC9, true); // F12
        assertEquals("sendKeyboardKey", mockSender.lastAction);
        assertEquals("F12", mockSender.lastKeyName);
    }

    @Test
    public void keyboardUnknownKeysymDoesNothing() {
        router.onKeyboardEvent(0x9999, true);
        assertNull(mockSender.lastAction); // No action for unknown keysym
    }

    @Test
    public void keyboardZeroKeysymDoesNothing() {
        router.onKeyboardEvent(0, true);
        assertNull(mockSender.lastAction);
    }

    @Test
    public void keyboardArrowKeysSendKeyboardKey() {
        router.onKeyboardEvent(0xFF51, true); // Left
        assertEquals("DPAD_LEFT", mockSender.lastKeyName);

        router.onKeyboardEvent(0xFF52, true); // Up
        assertEquals("DPAD_UP", mockSender.lastKeyName);
    }

    @Test
    public void keyboardNumpadSendsKeyboardKey() {
        router.onKeyboardEvent(0xFFB0, true); // KP_0
        assertEquals("0", mockSender.lastKeyName);
    }

    /**
     * Simple mock HidInputSender that records the last action for verification.
     */
    private static class MockHidInputSender implements HidInputSender {

        String lastAction;
        String lastClickType;
        String lastFunctionKey;
        String lastKeyName;
        int lastX, lastY, lastWidth, lastHeight;

        void reset() {
            lastAction = null;
            lastClickType = null;
            lastFunctionKey = null;
            lastKeyName = null;
            lastX = 0;
            lastY = 0;
            lastWidth = 0;
            lastHeight = 0;
        }

        @Override
        public void setMouseDimensions(int width, int height) {
            lastAction = "setMouseDimensions";
            lastWidth = width;
            lastHeight = height;
        }

        @Override
        public void sendAbsMove(int x, int y) {
            lastAction = "sendAbsMove";
            lastX = x;
            lastY = y;
        }

        @Override
        public void sendAbsButtonClick(String clickType, int x, int y) {
            lastAction = "sendAbsButtonClick";
            lastClickType = clickType;
            lastX = x;
            lastY = y;
        }

        @Override
        public void sendKeyboardPress(String functionKey, String keyName) {
            lastAction = "sendKeyboardPress";
            lastFunctionKey = functionKey;
            lastKeyName = keyName;
        }

        @Override
        public void sendKeyboardKey(String keyName) {
            lastAction = "sendKeyboardKey";
            lastKeyName = keyName;
        }

        @Override
        public void sendKeyboardRelease() {
            lastAction = "sendKeyboardRelease";
        }
    }
}
