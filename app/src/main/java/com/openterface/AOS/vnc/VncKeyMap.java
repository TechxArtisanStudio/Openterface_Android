package com.openterface.AOS.vnc;

/**
 * Maps VNC/RFB keysyms to CH9329 key names.
 * The CH9329MSKBMap uses String key names (e.g. "A", "ENTER", "F1")
 * as keys in its keycode map, returning hex strings as values.
 *
 * VNC keysyms follow the X11 keysym convention.
 */
public class VncKeyMap {

    // --- Modifier keysyms ---
    public static final int XK_Shift_L = 0xFFE1;
    public static final int XK_Shift_R = 0xFFE2;
    public static final int XK_Control_L = 0xFFE3;
    public static final int XK_Control_R = 0xFFE4;
    public static final int XK_Alt_L = 0xFFE9;
    public static final int XK_Alt_R = 0xFFEA;
    public static final int XK_Meta_L = 0xFFE7;
    public static final int XK_Meta_R = 0xFFE8;
    public static final int XK_Super_L = 0xFFEB;
    public static final int XK_Super_R = 0xFFEC;

    // --- Navigation keys ---
    public static final int XK_BackSpace = 0xFF08;
    public static final int XK_Tab = 0xFF09;
    public static final int XK_Return = 0xFF0D;
    public static final int XK_Escape = 0xFF1B;
    public static final int XK_Delete = 0xFFFF;
    public static final int XK_Insert = 0xFF63;
    public static final int XK_Home = 0xFF50;
    public static final int XK_End = 0xFF57;
    public static final int XK_Page_Up = 0xFF55;
    public static final int XK_Page_Down = 0xFF56;

    // --- Arrow keys ---
    public static final int XK_Left = 0xFF51;
    public static final int XK_Up = 0xFF52;
    public static final int XK_Right = 0xFF53;
    public static final int XK_Down = 0xFF54;

    // --- Function keys ---
    public static final int XK_F1 = 0xFFBE;
    public static final int XK_F2 = 0xFFBF;
    public static final int XK_F3 = 0xFFC0;
    public static final int XK_F4 = 0xFFC1;
    public static final int XK_F5 = 0xFFC2;
    public static final int XK_F6 = 0xFFC3;
    public static final int XK_F7 = 0xFFC4;
    public static final int XK_F8 = 0xFFC5;
    public static final int XK_F9 = 0xFFC6;
    public static final int XK_F10 = 0xFFC7;
    public static final int XK_F11 = 0xFFC8;
    public static final int XK_F12 = 0xFFC9;

    // --- Special keys ---
    public static final int XK_Print = 0xFF61;
    public static final int XK_Scroll_Lock = 0xFF14;
    public static final int XK_Pause = 0xFF13;
    public static final int XK_Num_Lock = 0xFF7F;
    public static final int XK_Caps_Lock = 0xFFE5;

    // --- Numpad ---
    public static final int XK_KP_0 = 0xFFB0;
    public static final int XK_KP_1 = 0xFFB1;
    public static final int XK_KP_2 = 0xFFB2;
    public static final int XK_KP_3 = 0xFFB3;
    public static final int XK_KP_4 = 0xFFB4;
    public static final int XK_KP_5 = 0xFFB5;
    public static final int XK_KP_6 = 0xFFB6;
    public static final int XK_KP_7 = 0xFFB7;
    public static final int XK_KP_8 = 0xFFB8;
    public static final int XK_KP_9 = 0xFFB9;
    public static final int XK_KP_Enter = 0xFF8D;
    public static final int XK_KP_Add = 0xFFAB;
    public static final int XK_KP_Subtract = 0xFFAD;
    public static final int XK_KP_Multiply = 0xFFAA;
    public static final int XK_KP_Divide = 0xFFAF;
    public static final int XK_KP_Decimal = 0xFFAE;

    /**
     * Convert a VNC keysym to CH9329 key name string.
     *
     * @param keysym VNC/RFB keysym value
     * @return String key name for CH9329MSKBMap, or null if not mapped
     */
    public static String vncKeysymToKeyName(int keysym) {
        // Printable ASCII (0x20-0x7E)
        // Uppercase A-Z keysyms (0x41-0x5A) should map to lowercase key names
        // since CH9329MSKBMap uses lowercase keys and handles shift separately.
        if (keysym >= 0x0041 && keysym <= 0x005A) {
            // Uppercase letter: return lowercase key name, caller adds Shift if needed
            return String.valueOf((char)(keysym + 0x20));
        }
        if (keysym >= 0x0020 && keysym <= 0x007E) {
            return asciiToKeyName((char) keysym);
        }

        switch (keysym) {
            // Navigation
            case XK_Return:       return "ENTER";
            case XK_BackSpace:    return "BACK";
            case XK_Tab:          return "TAB";
            case XK_Escape:       return "Esc";
            case XK_Delete:       return "Delete";
            case XK_Insert:       return "Ins";
            case XK_Home:         return "Home";
            case XK_End:          return "End";
            case XK_Page_Up:      return "PgUp";
            case XK_Page_Down:    return "PgDn";

            // Arrows
            case XK_Left:         return "DPAD_LEFT";
            case XK_Right:        return "DPAD_RIGHT";
            case XK_Up:           return "DPAD_UP";
            case XK_Down:         return "DPAD_DOWN";

            // Modifiers
            case XK_Shift_L:      return "SHIFT_LEFT";
            case XK_Shift_R:      return "SHIFT_RIGHT";
            case XK_Control_L:    return "CTRL_LEFT";
            case XK_Control_R:    return "CTRL_RIGHT";
            case XK_Alt_L:        return "ALT_LEFT";
            case XK_Alt_R:        return "ALT_RIGHT";
            case XK_Meta_L:
            case XK_Super_L:      return "Win";
            case XK_Meta_R:
            case XK_Super_R:      return "Win";

            // Function keys
            case XK_F1:  return "F1";
            case XK_F2:  return "F2";
            case XK_F3:  return "F3";
            case XK_F4:  return "F4";
            case XK_F5:  return "F5";
            case XK_F6:  return "F6";
            case XK_F7:  return "F7";
            case XK_F8:  return "F8";
            case XK_F9:  return "F9";
            case XK_F10: return "F10";
            case XK_F11: return "F11";
            case XK_F12: return "F12";

            // Special
            case XK_Caps_Lock:    return "CAPS_LOCK";
            case XK_Scroll_Lock:  return "SCROLL_LOCK";
            case XK_Num_Lock:     return "NUM_LOCK";
            case XK_Print:        return "PrtSc";
            case XK_Pause:        return "Pause";

            // Numpad
            case XK_KP_0: return "0";
            case XK_KP_1: return "1";
            case XK_KP_2: return "2";
            case XK_KP_3: return "3";
            case XK_KP_4: return "4";
            case XK_KP_5: return "5";
            case XK_KP_6: return "6";
            case XK_KP_7: return "7";
            case XK_KP_8: return "8";
            case XK_KP_9: return "9";
            case XK_KP_Enter: return "ENTER";
            case XK_KP_Add: return "NUMPAD_ADD";
            case XK_KP_Subtract: return "NUMPAD_SUBTRACT";
            case XK_KP_Multiply: return "NUMPAD_MULTIPLY";
            case XK_KP_Divide: return "NUMPAD_DIVIDE";
            case XK_KP_Decimal: return "PERIOD";

            default:
                return null;
        }
    }

    /**
     * Check if a keysym represents a modifier key.
     */
    public static boolean isModifier(int keysym) {
        return keysym == XK_Shift_L || keysym == XK_Shift_R
            || keysym == XK_Control_L || keysym == XK_Control_R
            || keysym == XK_Alt_L || keysym == XK_Alt_R
            || keysym == XK_Meta_L || keysym == XK_Meta_R
            || keysym == XK_Super_L || keysym == XK_Super_R;
    }

    /**
     * Map ASCII character to CH9329 key name.
     * Uppercase letters return the uppercase key name and caller must
     * apply shift modifier via functionKey parameter.
     */
    private static String asciiToKeyName(char c) {
        if (c == ' ') return "SPACE";

        // Lowercase a-z
        if (c >= 'a' && c <= 'z') {
            return String.valueOf(c);
        }
        // Uppercase A-Z - return uppercase key name
        if (c >= 'A' && c <= 'Z') {
            return String.valueOf(c);
        }
        // Digits 0-9
        if (c >= '0' && c <= '9') {
            return String.valueOf(c);
        }

        // Common punctuation - match CH9329MSKBMap key names
        switch (c) {
            case '-':  return "MINUS";
            case '=':  return "EQUALS";
            case '[':  return "LEFT_BRACKET";
            case ']':  return "RIGHT_BRACKET";
            case '\\': return "BACKSLASH";
            case ';':  return "SEMICOLON";
            case '\'': return "APOSTROPHE";
            case '`':  return "GRAVE";
            case ',':  return "COMMA";
            case '.':  return "PERIOD";
            case '/':  return "SLASH";
            default:   return null;
        }
    }
}
