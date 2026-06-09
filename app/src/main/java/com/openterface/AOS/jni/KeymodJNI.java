package com.openterface.AOS.jni;

/**
 * JNI wrapper for Openterface_Core (keymod library).
 * Provides native HID packet building functions.
 */
public class KeymodJNI {

    static {
        System.loadLibrary("keymod");
    }

    // Modifier masks (match keymod.h)
    public static final int MOD_NONE = 0x00;
    public static final int MOD_CTRL = 0x01;
    public static final int MOD_SHIFT = 0x02;
    public static final int MOD_ALT = 0x04;
    public static final int MOD_GUI = 0x08;

    // Mouse button masks (match keymod.h)
    public static final int MS_BTN_NONE = 0x00;
    public static final int MS_BTN_LEFT = 0x01;
    public static final int MS_BTN_RIGHT = 0x02;
    public static final int MS_BTN_MIDDLE = 0x04;

    // Packet sizes
    public static final int PKT_KEYBOARD_SIZE = 14;
    public static final int PKT_MOUSE_ABS_SIZE = 13;
    public static final int PKT_MOUSE_REL_SIZE = 11;

    /**
     * Get HID usage code for a key name.
     * @param keyName Key name (e.g., "a", "Enter", "F1")
     * @return HID usage code, or -1 if unknown
     */
    public static native int hidCode(String keyName);

    /**
     * Build a keyboard packet.
     * @param modifiers Modifier bitmask (MOD_*)
     * @param keys Array of HID key codes (up to 6)
     * @return 14-byte packet
     */
    public static native byte[] buildKeyboardPacket(int modifiers, int[] keys);

    /**
     * Build a press+release packet (two packets concatenated).
     * @param modifiers Modifier bitmask
     * @param hidCode HID key code
     * @return 28-byte packet (2 x 14 bytes)
     */
    public static native byte[] buildPressRelease(int modifiers, int hidCode);

    /**
     * Build a keyboard release packet.
     * @return 14-byte release packet
     */
    public static native byte[] buildKeyboardRelease();

    /**
     * Build an absolute mouse packet.
     * @param buttons Button bitmask (MS_BTN_*)
     * @param x Absolute X position (0-4095)
     * @param y Absolute Y position (0-4095)
     * @param wheel Scroll wheel delta (-128 to 127)
     * @return 13-byte packet
     */
    public static native byte[] buildMouseAbsPacket(int buttons, int x, int y, int wheel);

    /**
     * Build a relative mouse packet.
     * @param buttons Button bitmask
     * @param dx Delta X (-128 to 127)
     * @param dy Delta Y (-128 to 127)
     * @param wheel Scroll wheel delta
     * @return 11-byte packet
     */
    public static native byte[] buildMouseRelPacket(int buttons, int dx, int dy, int wheel);

    /**
     * Compute CH9329 checksum.
     * @param data Packet data (excluding checksum byte)
     * @return Checksum byte
     */
    public static native int checksum(byte[] data);

    /**
     * Convert a key name to Android format (lowercase with underscores).
     */
    public static String toAndroidKeyName(String coreName) {
        switch (coreName) {
            case "Enter": return "ENTER";
            case "Escape": return "Esc";
            case "Backspace": return "BACK";
            case "Space": return "SPACE";
            case "Tab": return "TAB";
            case "Delete": return "Delete";
            case "Insert": return "Ins";
            case "Home": return "Home";
            case "End": return "End";
            case "PageUp": return "PgUp";
            case "PageDown": return "PgDn";
            case "Right": return "DPAD_RIGHT";
            case "Left": return "DPAD_LEFT";
            case "Up": return "DPAD_UP";
            case "Down": return "DPAD_DOWN";
            case "Ctrl": return "CTRL_LEFT";
            case "Shift": return "SHIFT_LEFT";
            case "Alt": return "ALT_LEFT";
            case "Cmd":
            case "Win": return "Win";
            default:
                // Letter keys: convert "a" to "a"
                if (coreName.length() == 1) {
                    return coreName.toLowerCase();
                }
                // F-keys
                if (coreName.startsWith("F") && coreName.length() <= 3) {
                    return coreName;
                }
                return coreName;
        }
    }

    /**
     * Convert Android key name to Core format.
     */
    public static String toCoreKeyName(String androidName) {
        switch (androidName) {
            case "ENTER": return "Enter";
            case "Esc": return "Escape";
            case "BACK": return "Backspace";
            case "SPACE": return "Space";
            case "TAB": return "Tab";
            case "Delete": return "Delete";
            case "Ins": return "Insert";
            case "Home": return "Home";
            case "End": return "End";
            case "PgUp": return "PageUp";
            case "PgDn": return "PageDown";
            case "DPAD_RIGHT": return "Right";
            case "DPAD_LEFT": return "Left";
            case "DPAD_UP": return "Up";
            case "DPAD_DOWN": return "Down";
            case "CTRL_LEFT":
            case "CTRL_RIGHT": return "Ctrl";
            case "SHIFT_LEFT":
            case "SHIFT_RIGHT": return "Shift";
            case "ALT_LEFT":
            case "ALT_RIGHT": return "Alt";
            case "Win": return "Win";
            default:
                // Single letters to lowercase
                if (androidName.length() == 1) {
                    return androidName.toLowerCase();
                }
                return androidName;
        }
    }

    /**
     * Get HID code for Android key name.
     */
    public static int hidCodeForAndroidKey(String androidKeyName) {
        String coreName = toCoreKeyName(androidKeyName);
        return hidCode(coreName);
    }

    /**
     * Build a keyboard packet from Android key name.
     */
    public static byte[] buildKeyboardPacket(String androidKeyName, boolean ctrl, boolean shift, boolean alt, boolean win) {
        int modifiers = MOD_NONE;
        if (ctrl) modifiers |= MOD_CTRL;
        if (shift) modifiers |= MOD_SHIFT;
        if (alt) modifiers |= MOD_ALT;
        if (win) modifiers |= MOD_GUI;

        int hidCode = hidCodeForAndroidKey(androidKeyName);
        if (hidCode < 0) {
            throw new IllegalArgumentException("Unknown key: " + androidKeyName);
        }

        return buildKeyboardPacket(modifiers, new int[]{hidCode});
    }

    /**
     * Build a press+release packet from Android key name.
     */
    public static byte[] buildPressRelease(String androidKeyName, boolean ctrl, boolean shift, boolean alt, boolean win) {
        int modifiers = MOD_NONE;
        if (ctrl) modifiers |= MOD_CTRL;
        if (shift) modifiers |= MOD_SHIFT;
        if (alt) modifiers |= MOD_ALT;
        if (win) modifiers |= MOD_GUI;

        int hidCode = hidCodeForAndroidKey(androidKeyName);
        if (hidCode < 0) {
            throw new IllegalArgumentException("Unknown key: " + androidKeyName);
        }

        return buildPressRelease(modifiers, hidCode);
    }
}
