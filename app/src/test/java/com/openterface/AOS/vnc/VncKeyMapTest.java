package com.openterface.AOS.vnc;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for VncKeyMap — keysym-to-CH9329 key name mapping.
 *
 * Tests cover:
 * - Letter keysyms (uppercase → lowercase key names)
 * - Lowercase ASCII keysyms
 * - Special keys (navigation, arrows, modifiers, function, numpad)
 * - Modifier detection
 * - Unknown/unmapped keysyms
 */
public class VncKeyMapTest {

    // ====== Letter keysyms ======

    @Test
    public void uppercaseAMapsToLowercaseA() {
        // Uppercase A is 0x41
        assertEquals("a", VncKeyMap.vncKeysymToKeyName(0x41));
    }

    @Test
    public void uppercaseZMapsToLowercaseZ() {
        // Uppercase Z is 0x5A
        assertEquals("z", VncKeyMap.vncKeysymToKeyName(0x5A));
    }

    @Test
    public void uppercaseMMapsToLowercaseM() {
        assertEquals("m", VncKeyMap.vncKeysymToKeyName(0x4D));
    }

    @Test
    public void lowercaseLetterA() {
        assertEquals("a", VncKeyMap.vncKeysymToKeyName(0x61));
    }

    @Test
    public void lowercaseLetterZ() {
        assertEquals("z", VncKeyMap.vncKeysymToKeyName(0x7A));
    }

    // ====== Special printable ASCII ======

    @Test
    public void spaceMapsToSpace() {
        assertEquals("SPACE", VncKeyMap.vncKeysymToKeyName(0x20));
    }

    @Test
    public void digitsMapToStringDigits() {
        assertEquals("0", VncKeyMap.vncKeysymToKeyName(0x30));
        assertEquals("5", VncKeyMap.vncKeysymToKeyName(0x35));
        assertEquals("9", VncKeyMap.vncKeysymToKeyName(0x39));
    }

    @Test
    public void punctuationMapsCorrectly() {
        assertEquals("MINUS", VncKeyMap.vncKeysymToKeyName(0x2D));   // -
        assertEquals("EQUALS", VncKeyMap.vncKeysymToKeyName(0x3D));   // =
        assertEquals("COMMA", VncKeyMap.vncKeysymToKeyName(0x2C));    // ,
        assertEquals("PERIOD", VncKeyMap.vncKeysymToKeyName(0x2E));   // .
        assertEquals("SLASH", VncKeyMap.vncKeysymToKeyName(0x2F));    // /
        assertEquals("SEMICOLON", VncKeyMap.vncKeysymToKeyName(0x3B)); // ;
        assertEquals("APOSTROPHE", VncKeyMap.vncKeysymToKeyName(0x27)); // '
        assertEquals("LEFT_BRACKET", VncKeyMap.vncKeysymToKeyName(0x5B)); // [
        assertEquals("RIGHT_BRACKET", VncKeyMap.vncKeysymToKeyName(0x5D)); // ]
        assertEquals("BACKSLASH", VncKeyMap.vncKeysymToKeyName(0x5C)); // \
        assertEquals("GRAVE", VncKeyMap.vncKeysymToKeyName(0x60));    // `
    }

    // ====== Navigation keys ======

    @Test
    public void navigationKeysMapCorrectly() {
        assertEquals("ENTER", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Return));
        assertEquals("BACK", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_BackSpace));
        assertEquals("TAB", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Tab));
        assertEquals("Esc", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Escape));
        assertEquals("Delete", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Delete));
        assertEquals("Ins", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Insert));
        assertEquals("Home", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Home));
        assertEquals("End", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_End));
        assertEquals("PgUp", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Page_Up));
        assertEquals("PgDn", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Page_Down));
    }

    // ====== Arrow keys ======

    @Test
    public void arrowKeysMapCorrectly() {
        assertEquals("DPAD_LEFT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Left));
        assertEquals("DPAD_RIGHT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Right));
        assertEquals("DPAD_UP", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Up));
        assertEquals("DPAD_DOWN", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Down));
    }

    // ====== Modifier keys ======

    @Test
    public void modifierKeysMapCorrectly() {
        assertEquals("SHIFT_LEFT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Shift_L));
        assertEquals("SHIFT_RIGHT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Shift_R));
        assertEquals("CTRL_LEFT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Control_L));
        assertEquals("CTRL_RIGHT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Control_R));
        assertEquals("ALT_LEFT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Alt_L));
        assertEquals("ALT_RIGHT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Alt_R));
        assertEquals("Win", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Meta_L));
        assertEquals("Win", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Super_L));
        assertEquals("Win", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Super_R));
    }

    @Test
    public void allModifiersDetected() {
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Shift_L));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Shift_R));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Control_L));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Control_R));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Alt_L));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Alt_R));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Meta_L));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Meta_R));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Super_L));
        assertTrue(VncKeyMap.isModifier(VncKeyMap.XK_Super_R));

        // Non-modifier keys should return false
        assertFalse(VncKeyMap.isModifier(VncKeyMap.XK_Return));
        assertFalse(VncKeyMap.isModifier(0x61));
        assertFalse(VncKeyMap.isModifier(VncKeyMap.XK_F1));
        assertFalse(VncKeyMap.isModifier(0));
    }

    // ====== Function keys ======

    @Test
    public void functionKeysMapCorrectly() {
        assertEquals("F1", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_F1));
        assertEquals("F2", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_F2));
        assertEquals("F6", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_F6));
        assertEquals("F10", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_F10));
        assertEquals("F12", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_F12));
    }

    // ====== Special keys ======

    @Test
    public void specialKeysMapCorrectly() {
        assertEquals("CAPS_LOCK", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Caps_Lock));
        assertEquals("SCROLL_LOCK", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Scroll_Lock));
        assertEquals("NUM_LOCK", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Num_Lock));
        assertEquals("PrtSc", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Print));
        assertEquals("Pause", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_Pause));
    }

    // ====== Numpad keys ======

    @Test
    public void numpadKeysMapCorrectly() {
        assertEquals("0", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_0));
        assertEquals("5", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_5));
        assertEquals("9", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_9));
        assertEquals("ENTER", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Enter));
        assertEquals("NUMPAD_ADD", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Add));
        assertEquals("NUMPAD_SUBTRACT", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Subtract));
        assertEquals("NUMPAD_MULTIPLY", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Multiply));
        assertEquals("NUMPAD_DIVIDE", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Divide));
        assertEquals("PERIOD", VncKeyMap.vncKeysymToKeyName(VncKeyMap.XK_KP_Decimal));
    }

    // ====== Unknown keysyms ======

    @Test
    public void unknownKeysymReturnsNull() {
        assertNull(VncKeyMap.vncKeysymToKeyName(0x1A));  // Below printable range
        assertNull(VncKeyMap.vncKeysymToKeyName(0x7F));  // Above printable ASCII, not mapped
        assertNull(VncKeyMap.vncKeysymToKeyName(0x100)); // Out of range
        assertNull(VncKeyMap.vncKeysymToKeyName(0));
    }
}
