package com.openterface.AOS.webrtc;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for WebRtcInputRouter — input routing logic.
 * Uses mock senders to verify that the router sends the correct
 * HID commands for keyboard/mouse events without needing Android runtime.
 */
public class WebRtcInputRouterTest {

    private MockHidInputSender mockHidSender;
    private MockKeyboardSender mockKeyboardSender;
    private WebRtcInputRouter router;

    @Before
    public void setUp() {
        mockHidSender = new MockHidInputSender();
        mockKeyboardSender = new MockKeyboardSender();
        router = new WebRtcInputRouter(mockHidSender, mockKeyboardSender);
    }

    // ====== Mouse routing tests ======

    @Test
    public void setFramebufferSizeStoresDimensions() {
        router.setFramebufferSize(1920, 1080);
        // Trigger a mouse event to verify dimensions are passed through
        router.onMouseEvent(0, 100, 200, false);
        assertEquals(1920, mockHidSender.lastWidth);
        assertEquals(1080, mockHidSender.lastHeight);
    }

    @Test
    public void mousePureMovementSendsAbsMove() {
        // buttonMask=0 means no buttons pressed — pure cursor movement
        router.onMouseEvent(0, 100, 200, false);
        assertEquals("sendAbsMove", mockHidSender.lastAction);
        assertEquals(100, mockHidSender.lastX);
        assertEquals(200, mockHidSender.lastY);
        assertNull(mockHidSender.lastClickType);
    }

    @Test
    public void mouseLeftButtonPressSendsLeftClick() {
        router.onMouseEvent(1, 500, 300, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertEquals("SecLeftData", mockHidSender.lastClickType);
        assertEquals(500, mockHidSender.lastX);
        assertEquals(300, mockHidSender.lastY);
    }

    @Test
    public void mouseMiddleButtonPressSendsMiddleClick() {
        router.onMouseEvent(2, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertEquals("SecMiddleData", mockHidSender.lastClickType);
    }

    @Test
    public void mouseRightButtonPressSendsRightClick() {
        router.onMouseEvent(4, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertEquals("SecRightData", mockHidSender.lastClickType);
    }

    @Test
    public void mouseButtonReleaseSendsMove() {
        // First press a button
        router.onMouseEvent(1, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);

        // Then release (buttonMask=0) — should send move
        router.onMouseEvent(0, 100, 200, false);
        assertEquals("sendAbsMove", mockHidSender.lastAction);
    }

    @Test
    public void mouseMovementOnlyUpdatesPosition() {
        // Press then move: the second call with same buttonMask is movement
        router.onMouseEvent(1, 100, 200, true); // press left
        mockHidSender.reset();

        // Move with button still pressed (buttonMask=1) — button state didn't change
        router.onMouseEvent(1, 200, 300, true);
        // The router always calls setMouseDimensions first, then checks button state.
        // When buttonMask hasn't changed from lastButtonMask, it should NOT call
        // any mouse HID method (click or move).
        // Note: setMouseDimensions IS called (for dimension normalization), but
        // the actual mouse action (click/move) should NOT happen.
        assertNotEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertNotEquals("sendAbsMove", mockHidSender.lastAction);
    }

    // ====== Keyboard routing tests ======

    @Test
    public void keyboardLetterSendsKeyboardKey() {
        router.onKeyboardEvent(0x61, true); // 'a' press
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("00", mockKeyboardSender.lastFunctionKey);
        assertEquals("a", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0x61, false); // release
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardEnterSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF0D, true); // Return press
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("ENTER", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardEscapeSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF1B, true); // Escape press
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("Esc", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardShiftSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE1, true); // Shift_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Shift", mockKeyboardSender.lastFunctionKey);
        assertEquals("SHIFT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardShiftReleaseSendsKeyboardRelease() {
        router.onKeyboardEvent(0xFFE1, true);  // press
        router.onKeyboardEvent(0xFFE1, false); // release
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardControlSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE3, true); // Control_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Ctrl", mockKeyboardSender.lastFunctionKey);
        assertEquals("CTRL_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardAltSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE9, true); // Alt_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Alt", mockKeyboardSender.lastFunctionKey);
        assertEquals("ALT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardWinSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFEB, true); // Super_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
    }

    @Test
    public void keyboardFunctionKeySendsKeyboardKey() {
        router.onKeyboardEvent(0xFFBE, true); // F1
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("F1", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0xFFC9, true); // F12
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("F12", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardUnknownKeysymDoesNothing() {
        router.onKeyboardEvent(0x9999, true);
        assertNull(mockKeyboardSender.lastAction); // No action for unknown keysym
    }

    @Test
    public void keyboardZeroKeysymDoesNothing() {
        router.onKeyboardEvent(0, true);
        assertNull(mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardArrowKeysSendKeyboardKey() {
        router.onKeyboardEvent(0xFF51, true); // Left
        assertEquals("DPAD_LEFT", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0xFF52, true); // Up
        assertEquals("DPAD_UP", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardNumpadSendsKeyboardKey() {
        router.onKeyboardEvent(0xFFB0, true); // KP_0
        assertEquals("0", mockKeyboardSender.lastKeyName);
    }

    // ====== Combined scenario tests ======

    @Test
    public void modifierCombo_CtrlCSendsCorrectCommands() {
        // Press Ctrl (modifier held)
        router.onKeyboardEvent(0xFFE3, true);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        mockKeyboardSender.reset();

        // Press 'c' while Ctrl held
        router.onKeyboardEvent(0x63, true);
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);
        assertEquals("c", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void mixedMouseAndKeyboardEvents() {
        // Mouse click
        router.onMouseEvent(1, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);

        // Type a key
        router.onKeyboardEvent(0x61, true);
        assertEquals("sendKeyBoardPressAndRelease", mockKeyboardSender.lastAction);

        // Release mouse
        router.onMouseEvent(0, 100, 200, false);
        assertEquals("sendAbsMove", mockHidSender.lastAction);
    }

    // ====== Mock implementations ======

    /**
     * Simple mock HidInputSender that records the last action for verification.
     */
    private static class MockHidInputSender implements HidInputSender {

        String lastAction;
        String lastClickType;
        int lastX, lastY, lastWidth, lastHeight;

        void reset() {
            lastAction = null;
            lastClickType = null;
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
            // Not used - keyboard operations go through KeyboardSender
        }

        @Override
        public void sendKeyboardKey(String keyName) {
            // Not used - keyboard operations go through KeyboardSender
        }

        @Override
        public void sendKeyboardRelease() {
            // Not used - keyboard operations go through KeyboardSender
        }
    }

    /**
     * Simple mock KeyboardSender that records the last action for verification.
     */
    private static class MockKeyboardSender implements KeyboardSender {

        String lastAction;
        String lastFunctionKey;
        String lastKeyName;

        void reset() {
            lastAction = null;
            lastFunctionKey = null;
            lastKeyName = null;
        }

        @Override
        public void sendKeyBoardPressQueued(String functionKey, String keyName) {
            lastAction = "sendKeyBoardPressQueued";
            lastFunctionKey = functionKey;
            lastKeyName = keyName;
        }

        @Override
        public void sendKeyBoardPressAndRelease(String functionKey, String keyName) {
            lastAction = "sendKeyBoardPressAndRelease";
            lastFunctionKey = functionKey;
            lastKeyName = keyName;
        }

        @Override
        public void sendKeyBoardReleaseQueued() {
            lastAction = "sendKeyBoardReleaseQueued";
        }
    }
}
