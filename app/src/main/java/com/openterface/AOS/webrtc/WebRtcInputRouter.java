package com.openterface.AOS.webrtc;

import com.openterface.AOS.vnc.VncKeyMap;

/**
 * Routes WebRTC DataChannel input events to the target machine
 * via the existing CH9329 HID path.
 *
 * Reuses VncKeyMap for keysym → CH9329 key name mapping,
 * and delegates HID operations to HidInputSender for testability.
 *
 * Note: This class has no Android dependencies (no Log, no Context) so it
 * can be unit-tested with pure JVM tests.
 */
public class WebRtcInputRouter {

    private final HidInputSender hidSender;

    private int framebufferWidth = 1920;
    private int framebufferHeight = 1080;

    // Track last button state for release detection
    private int lastButtonMask = 0;

    /**
     * Create router with the Android HID sender (production use).
     */
    public WebRtcInputRouter() {
        this(new AndroidHidInputSender());
    }

    /**
     * Create router with a custom HID sender (for testing).
     */
    public WebRtcInputRouter(HidInputSender hidSender) {
        this.hidSender = hidSender;
    }

    /**
     * Set the framebuffer dimensions for coordinate normalization.
     */
    public void setFramebufferSize(int width, int height) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
    }

    /**
     * Handle mouse event from WebRTC DataChannel.
     * Uses the same routing logic as VNC mouse handling in MainActivity.
     *
     * @param buttonMask RFB button mask (1=left, 2=middle, 4=right)
     * @param x          X coordinate
     * @param y          Y coordinate
     */
    public void onMouseEvent(int buttonMask, int x, int y, boolean pressed) {
        // Set mouse dimensions for proper normalization
        if (framebufferWidth > 0 && framebufferHeight > 0) {
            hidSender.setMouseDimensions(framebufferWidth, framebufferHeight);
        }

        // Button state change detection (same pattern as VNC mouse handling)
        if (buttonMask != lastButtonMask) {
            // Button state changed
            if (buttonMask != 0) {
                // Button press - send click
                String mouseClick = "SecNullData";
                if ((buttonMask & 0x01) != 0) mouseClick = "SecLeftData";
                else if ((buttonMask & 0x02) != 0) mouseClick = "SecMiddleData";
                else if ((buttonMask & 0x04) != 0) mouseClick = "SecRightData";
                hidSender.sendAbsButtonClick(mouseClick, x, y);
            } else {
                // Button release
                hidSender.sendAbsMove(x, y);
            }
            lastButtonMask = buttonMask;
        } else if (buttonMask == 0) {
            // Pure movement - just move cursor
            hidSender.sendAbsMove(x, y);
        }
    }

    /**
     * Handle keyboard event from WebRTC DataChannel.
     * Uses the same routing logic as VNC keyboard handling in MainActivity.
     *
     * @param keysym VNC/RFB keysym value
     * @param down   true = key pressed, false = key released
     */
    public void onKeyboardEvent(int keysym, boolean down) {
        String keyName = VncKeyMap.vncKeysymToKeyName(keysym);
        if (keyName == null) {
            return; // Unknown keysym, silently ignore
        }

        if (VncKeyMap.isModifier(keysym)) {
            // Modifier key
            String functionKey = getModifierFunctionKey(keysym);
            if (down) {
                hidSender.sendKeyboardPress(functionKey, keyName);
            } else {
                hidSender.sendKeyboardRelease();
            }
        } else {
            // Regular key
            if (down) {
                hidSender.sendKeyboardKey(keyName);
            } else {
                hidSender.sendKeyboardRelease();
            }
        }
    }

    private String getModifierFunctionKey(int keysym) {
        switch (keysym) {
            case VncKeyMap.XK_Shift_L:
            case VncKeyMap.XK_Shift_R: return "Shift";
            case VncKeyMap.XK_Control_L:
            case VncKeyMap.XK_Control_R: return "Ctrl";
            case VncKeyMap.XK_Alt_L:
            case VncKeyMap.XK_Alt_R: return "Alt";
            case VncKeyMap.XK_Meta_L:
            case VncKeyMap.XK_Meta_R:
            case VncKeyMap.XK_Super_L:
            case VncKeyMap.XK_Super_R: return "Win";
            default: return "00";
        }
    }
}
