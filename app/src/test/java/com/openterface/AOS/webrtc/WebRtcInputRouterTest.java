package com.openterface.AOS.webrtc;

import com.openterface.AOS.vnc.VncKeyMap;
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
        router.onKeyboardEvent(0x61, true, 0); // 'a' press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("00", mockKeyboardSender.lastFunctionKey);
        assertEquals("a", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0x61, false, 0); // release
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardEnterSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF0D, true, 0); // Return press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("ENTER", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardEscapeSendsKeyboardKey() {
        router.onKeyboardEvent(0xFF1B, true, 0); // Escape press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Esc", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardShiftSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE1, true, 0); // Shift_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Shift", mockKeyboardSender.lastFunctionKey);
        assertEquals("SHIFT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardShiftReleaseSendsKeyboardRelease() {
        router.onKeyboardEvent(0xFFE1, true, 0);  // press
        router.onKeyboardEvent(0xFFE1, false, 0); // release
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardControlSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE3, true, 0); // Control_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Ctrl", mockKeyboardSender.lastFunctionKey);
        assertEquals("CTRL_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardAltSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFE9, true, 0); // Alt_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Alt", mockKeyboardSender.lastFunctionKey);
        assertEquals("ALT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardWinSendsKeyboardPress() {
        router.onKeyboardEvent(0xFFEB, true, 0); // Super_L press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
    }

    @Test
    public void keyboardFunctionKeySendsKeyboardKey() {
        router.onKeyboardEvent(0xFFBE, true, 0); // F1
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("F1", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0xFFC9, true, 0); // F12
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("F12", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardUnknownKeysymDoesNothing() {
        router.onKeyboardEvent(0x9999, true, 0);
        assertNull(mockKeyboardSender.lastAction); // No action for unknown keysym
    }

    @Test
    public void keyboardZeroKeysymDoesNothing() {
        router.onKeyboardEvent(0, true, 0);
        assertNull(mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardArrowKeysSendKeyboardKey() {
        router.onKeyboardEvent(0xFF51, true, 0); // Left
        assertEquals("DPAD_LEFT", mockKeyboardSender.lastKeyName);

        router.onKeyboardEvent(0xFF52, true, 0); // Up
        assertEquals("DPAD_UP", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void keyboardNumpadSendsKeyboardKey() {
        router.onKeyboardEvent(0xFFB0, true, 0); // KP_0
        assertEquals("0", mockKeyboardSender.lastKeyName);
    }

    // ====== Release-all signal ======

    @Test
    public void releaseAllSignal_sendsKeyboardRelease() {
        // When keysym=0 and down=false, it's a release-all signal
        router.onKeyboardEvent(0, false, 0);
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void releaseAllSignal_afterKeyPress_clearsAllModifiers() {
        // Press Shift
        router.onKeyboardEvent(VncKeyMap.XK_Shift_L, true, 0);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        
        // Send release-all
        router.onKeyboardEvent(0, false, 0);
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    // ====== Modifier function key mapping ======

    @Test
    public void modifierFunctionKey_shiftLeft_mapsToShift() {
        router.onKeyboardEvent(VncKeyMap.XK_Shift_L, true, 0);
        assertEquals("Shift", mockKeyboardSender.lastFunctionKey);
        assertEquals("SHIFT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_shiftRight_mapsToShift() {
        router.onKeyboardEvent(VncKeyMap.XK_Shift_R, true, 0);
        assertEquals("Shift", mockKeyboardSender.lastFunctionKey);
        assertEquals("SHIFT_RIGHT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_controlLeft_mapsToCtrl() {
        router.onKeyboardEvent(VncKeyMap.XK_Control_L, true, 0);
        assertEquals("Ctrl", mockKeyboardSender.lastFunctionKey);
        assertEquals("CTRL_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_controlRight_mapsToCtrl() {
        router.onKeyboardEvent(VncKeyMap.XK_Control_R, true, 0);
        assertEquals("Ctrl", mockKeyboardSender.lastFunctionKey);
        assertEquals("CTRL_RIGHT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_altLeft_mapsToAlt() {
        router.onKeyboardEvent(VncKeyMap.XK_Alt_L, true, 0);
        assertEquals("Alt", mockKeyboardSender.lastFunctionKey);
        assertEquals("ALT_LEFT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_altRight_mapsToAlt() {
        router.onKeyboardEvent(VncKeyMap.XK_Alt_R, true, 0);
        assertEquals("Alt", mockKeyboardSender.lastFunctionKey);
        assertEquals("ALT_RIGHT", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_metaLeft_mapsToWin() {
        router.onKeyboardEvent(VncKeyMap.XK_Meta_L, true, 0);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
        assertEquals("Win", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_metaRight_mapsToWin() {
        router.onKeyboardEvent(VncKeyMap.XK_Meta_R, true, 0);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
        assertEquals("Win", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_superLeft_mapsToWin() {
        router.onKeyboardEvent(VncKeyMap.XK_Super_L, true, 0);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
        assertEquals("Win", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void modifierFunctionKey_superRight_mapsToWin() {
        router.onKeyboardEvent(VncKeyMap.XK_Super_R, true, 0);
        assertEquals("Win", mockKeyboardSender.lastFunctionKey);
        assertEquals("Win", mockKeyboardSender.lastKeyName);
    }

    // ====== Keyboard press vs release events ======

    @Test
    public void keyboardPress_sendsPressQueued() {
        router.onKeyboardEvent(0x61, true, 0); // 'a' press
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void keyboardRelease_sendsReleaseQueued() {
        router.onKeyboardEvent(0x61, false, 0); // 'a' release
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void modifierKeyPress_sendsPressQueued() {
        router.onKeyboardEvent(VncKeyMap.XK_Shift_L, true, 0);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
    }

    @Test
    public void modifierKeyRelease_sendsReleaseQueued() {
        router.onKeyboardEvent(VncKeyMap.XK_Shift_L, false, 0);
        assertEquals("sendKeyBoardReleaseQueued", mockKeyboardSender.lastAction);
    }

    // ====== Modifier byte handling ======

    @Test
    public void regularKey_withModifierByte_passesCorrectFunctionKey() {
        // Simulate Shift+A: modifier byte is 0x02 (left shift), keysym is 0x41 (uppercase A)
        router.onKeyboardEvent(0x41, true, 0x02);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("02", mockKeyboardSender.lastFunctionKey);
        assertEquals("a", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void regularKey_withCtrlModifier_passesCorrectFunctionKey() {
        // Simulate Ctrl+C: modifier byte is 0x01 (left ctrl), keysym is 0x63 (lowercase c)
        router.onKeyboardEvent(0x63, true, 0x01);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("01", mockKeyboardSender.lastFunctionKey);
        assertEquals("c", mockKeyboardSender.lastKeyName);
    }

    @Test
    public void regularKey_noModifier_passesZeroFunctionKey() {
        // Simulate plain 'a': modifier byte is 0x00
        router.onKeyboardEvent(0x61, true, 0x00);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("00", mockKeyboardSender.lastFunctionKey);
        assertEquals("a", mockKeyboardSender.lastKeyName);
    }

    // ====== Framebuffer size handling ======

    @Test
    public void framebufferSize_default_is1920x1080() {
        // Default framebuffer size should be 1920x1080
        router.onMouseEvent(0, 100, 200, false);
        assertEquals(1920, mockHidSender.lastWidth);
        assertEquals(1080, mockHidSender.lastHeight);
    }

    @Test
    public void framebufferSize_custom_resolution() {
        router.setFramebufferSize(2560, 1440);
        router.onMouseEvent(0, 100, 200, false);
        assertEquals(2560, mockHidSender.lastWidth);
        assertEquals(1440, mockHidSender.lastHeight);
    }

    @Test
    public void framebufferSize_zeroDimensions_doesNotCallSetMouseDimensions() {
        router.setFramebufferSize(0, 0);
        router.onMouseEvent(0, 100, 200, false);
        // When dimensions are 0, setMouseDimensions should not be called
        assertNotEquals("setMouseDimensions", mockHidSender.lastAction);
    }

    // ====== Mouse button combinations ======

    @Test
    public void mouseButtons_simultaneousLeftAndRight_prioritizesLeft() {
        // Button mask 0x05 = left (0x01) + right (0x04)
        // Should prioritize left button
        router.onMouseEvent(0x05, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertEquals("SecLeftData", mockHidSender.lastClickType);
    }

    @Test
    public void mouseButtons_simultaneousMiddleAndRight_prioritizesMiddle() {
        // Button mask 0x06 = middle (0x02) + right (0x04)
        // Should prioritize middle button
        router.onMouseEvent(0x06, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        assertEquals("SecMiddleData", mockHidSender.lastClickType);
    }

    @Test
    public void mouseButtonRelease_fromLeftClick_sendsMove() {
        // Press left button
        router.onMouseEvent(0x01, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);
        
        // Release all buttons
        router.onMouseEvent(0x00, 100, 200, false);
        assertEquals("sendAbsMove", mockHidSender.lastAction);
    }

    // ====== Unknown keysyms ======

    @Test
    public void unknownKeysym_0x9999_doesNothing() {
        router.onKeyboardEvent(0x9999, true, 0);
        assertNull(mockKeyboardSender.lastAction);
    }

    @Test
    public void unknownKeysym_0x0100_doesNothing() {
        router.onKeyboardEvent(0x0100, true, 0);
        assertNull(mockKeyboardSender.lastAction);
    }

    @Test
    public void unknownKeysym_0xFFFF_mapsToDeleteKey() {
        // 0xFFFF is XK_Delete, not unknown
        router.onKeyboardEvent(0xFFFF, true, 0);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("Delete", mockKeyboardSender.lastKeyName);
    }

    // ====== Combined scenario tests ======

    @Test
    public void modifierCombo_CtrlCSendsCorrectCommands() {
        // Press Ctrl (modifier held)
        router.onKeyboardEvent(0xFFE3, true, 0);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        mockKeyboardSender.reset();

        // Press 'c' while Ctrl held
        router.onKeyboardEvent(0x63, true, 0x01); // Ctrl modifier byte
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);
        assertEquals("c", mockKeyboardSender.lastKeyName);
        assertEquals("01", mockKeyboardSender.lastFunctionKey);
    }

    @Test
    public void mixedMouseAndKeyboardEvents() {
        // Mouse click
        router.onMouseEvent(1, 100, 200, true);
        assertEquals("sendAbsButtonClick", mockHidSender.lastAction);

        // Type a key
        router.onKeyboardEvent(0x61, true, 0);
        assertEquals("sendKeyBoardPressQueued", mockKeyboardSender.lastAction);

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
