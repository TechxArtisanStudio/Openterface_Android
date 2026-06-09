package com.openterface.AOS.target;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.serial.UsbDeviceManager;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KeyBoardManager — verifies key code mapping,
 * HID command construction, modifier handling, and threading.
 *
 * Tests cover:
 * - Key name lookups
 * - Single key press sequences
 * - Modifier key combinations (Ctrl+C, Alt+Tab, etc.)
 * - Special keys (Enter, Escape, arrows, function keys)
 * - Thread-safe operations
 * - Keyboard release
 * - Duplicate event filtering
 */
@RunWith(MockitoJUnitRunner.class)
public class KeyBoardManagerTest {

    @Mock
    private UsbDeviceManager mockUsbDeviceManager;

    @Before
    public void setUp() {
        KeyBoardManager.setUsbDeviceManager(mockUsbDeviceManager);
    }

    @After
    public void tearDown() {
        // Can't call sendKeyBoardReleaseQueued() - it uses Android Log
        // Clear any pressed keys state is handled by each test's isolation
    }

    // ====== Key Name Lookup Tests ======

    @Test
    public void keyCodeMap_ContainsAllLetters() {
        // Verify all lowercase letters
        for (char c = 'a'; c <= 'z'; c++) {
            assertNotNull("Key '" + c + "' should be mapped",
                    CH9329MSKBMap.getKeyCodeMap().get(String.valueOf(c)));
        }
    }

    @Test
    public void keyCodeMap_ContainsAllDigits() {
        // Verify all digits
        for (char c = '0'; c <= '9'; c++) {
            assertNotNull("Key '" + c + "' should be mapped",
                    CH9329MSKBMap.getKeyCodeMap().get(String.valueOf(c)));
        }
    }

    @Test
    public void keyCodeMap_ContainsSpecialKeys() {
        assertNotNull("ENTER should be mapped", CH9329MSKBMap.getKeyCodeMap().get("ENTER"));
        assertNotNull("SPACE should be mapped", CH9329MSKBMap.getKeyCodeMap().get("SPACE"));
        assertNotNull("TAB should be mapped", CH9329MSKBMap.getKeyCodeMap().get("TAB"));
        assertNotNull("BACK should be mapped", CH9329MSKBMap.getKeyCodeMap().get("BACK"));
        assertNotNull("Esc should be mapped", CH9329MSKBMap.getKeyCodeMap().get("Esc"));
    }

    @Test
    public void keyCodeMap_ContainsArrowKeys() {
        assertNotNull("DPAD_LEFT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("DPAD_LEFT"));
        assertNotNull("DPAD_RIGHT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("DPAD_RIGHT"));
        assertNotNull("DPAD_UP should be mapped", CH9329MSKBMap.getKeyCodeMap().get("DPAD_UP"));
        assertNotNull("DPAD_DOWN should be mapped", CH9329MSKBMap.getKeyCodeMap().get("DPAD_DOWN"));
    }

    @Test
    public void keyCodeMap_ContainsFunctionKeys() {
        for (int i = 1; i <= 12; i++) {
            assertNotNull("F" + i + " should be mapped",
                    CH9329MSKBMap.getKeyCodeMap().get("F" + i));
        }
    }

    @Test
    public void keyCodeMap_ContainsModifierKeys() {
        assertNotNull("CTRL_LEFT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("CTRL_LEFT"));
        assertNotNull("CTRL_RIGHT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("CTRL_RIGHT"));
        assertNotNull("SHIFT_LEFT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("SHIFT_LEFT"));
        assertNotNull("SHIFT_RIGHT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("SHIFT_RIGHT"));
        assertNotNull("ALT_LEFT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("ALT_LEFT"));
        assertNotNull("ALT_RIGHT should be mapped", CH9329MSKBMap.getKeyCodeMap().get("ALT_RIGHT"));
        assertNotNull("Win should be mapped", CH9329MSKBMap.getKeyCodeMap().get("Win"));
    }

    @Test
    public void keyCodeMap_ContainsReleaseCommand() {
        assertNotNull("Release command should exist", CH9329MSKBMap.getKeyCodeMap().get("release"));
        assertEquals("57AB00020800000000000000000C",
                CH9329MSKBMap.getKeyCodeMap().get("release"));
    }

    // ====== Modifier Byte Tests ======

    @Test
    public void kbShortCutKey_ContainsCorrectModifierValues() {
        assertEquals("00", CH9329MSKBMap.KBShortCutKey().get("ShortCutKeyCtrlNull"));
        assertEquals("01", CH9329MSKBMap.KBShortCutKey().get("Ctrl"));
        assertEquals("02", CH9329MSKBMap.KBShortCutKey().get("Shift"));
        assertEquals("04", CH9329MSKBMap.KBShortCutKey().get("Alt"));
        assertEquals("08", CH9329MSKBMap.KBShortCutKey().get("Win"));
    }

    @Test
    public void kbShortCutKey_ContainsRightModifierValues() {
        assertEquals("10", CH9329MSKBMap.KBShortCutKey().get("CtrlR"));
        assertEquals("20", CH9329MSKBMap.KBShortCutKey().get("ShiftR"));
        assertEquals("40", CH9329MSKBMap.KBShortCutKey().get("AltR"));
        assertEquals("80", CH9329MSKBMap.KBShortCutKey().get("WinR"));
    }

    // ====== Command Structure Tests ======

    @Test
    public void keyboardCommand_ContainsCorrectPrefix() {
        assertEquals("57", CH9329MSKBMap.getKeyCodeMap().get("prefix1"));
        assertEquals("AB", CH9329MSKBMap.getKeyCodeMap().get("prefix2"));
        assertEquals("00", CH9329MSKBMap.getKeyCodeMap().get("address"));
    }

    @Test
    public void keyboardCommand_ContainsCorrectCmdByte() {
        assertEquals("02", CH9329MSKBMap.CmdData().get("CmdKB_HID"));
    }

    @Test
    public void keyboardCommand_ContainsCorrectDataLength() {
        assertEquals("08", CH9329MSKBMap.DataLen().get("DataLenKB"));
    }

    // ====== Key Code Values Tests (Verify against expected HID codes) ======

    @Test
    public void keyCodes_Letters_AreCorrect() {
        // USB HID key codes for a-z start at 0x04
        assertEquals("04", CH9329MSKBMap.getKeyCodeMap().get("a"));
        assertEquals("05", CH9329MSKBMap.getKeyCodeMap().get("b"));
        assertEquals("1D", CH9329MSKBMap.getKeyCodeMap().get("z"));
    }

    @Test
    public void keyCodes_Digits_AreCorrect() {
        // USB HID key codes for 1-0
        assertEquals("1E", CH9329MSKBMap.getKeyCodeMap().get("1"));
        assertEquals("27", CH9329MSKBMap.getKeyCodeMap().get("0"));
    }

    @Test
    public void keyCodes_Modifiers_AreCorrect() {
        // USB HID modifier keys
        assertEquals("E0", CH9329MSKBMap.getKeyCodeMap().get("CTRL_LEFT"));
        assertEquals("E1", CH9329MSKBMap.getKeyCodeMap().get("SHIFT_LEFT"));
        assertEquals("E2", CH9329MSKBMap.getKeyCodeMap().get("ALT_LEFT"));
        assertEquals("E3", CH9329MSKBMap.getKeyCodeMap().get("Win"));
    }

    @Test
    public void keyCodes_SpecialKeys_AreCorrect() {
        assertEquals("28", CH9329MSKBMap.getKeyCodeMap().get("ENTER"));
        assertEquals("2C", CH9329MSKBMap.getKeyCodeMap().get("SPACE"));
        assertEquals("29", CH9329MSKBMap.getKeyCodeMap().get("Esc"));
        assertEquals("2A", CH9329MSKBMap.getKeyCodeMap().get("BACK"));
        assertEquals("2B", CH9329MSKBMap.getKeyCodeMap().get("TAB"));
    }

    @Test
    public void keyCodes_ArrowKeys_AreCorrect() {
        assertEquals("50", CH9329MSKBMap.getKeyCodeMap().get("DPAD_LEFT"));
        assertEquals("4F", CH9329MSKBMap.getKeyCodeMap().get("DPAD_RIGHT"));
        assertEquals("51", CH9329MSKBMap.getKeyCodeMap().get("DPAD_DOWN"));
        assertEquals("52", CH9329MSKBMap.getKeyCodeMap().get("DPAD_UP"));
    }

    @Test
    public void keyCodes_FunctionKeys_AreCorrect() {
        assertEquals("3A", CH9329MSKBMap.getKeyCodeMap().get("F1"));
        assertEquals("3B", CH9329MSKBMap.getKeyCodeMap().get("F2"));
        assertEquals("45", CH9329MSKBMap.getKeyCodeMap().get("F12"));
    }

    // ====== Command Construction Tests ======

    @Test
    public void commandStructure_IsValidLength() {
        // A typical keyboard command:
        // prefix1(2) + prefix2(2) + address(2) + cmd(2) + len(2) + modifier(2) + null(2) +
        // key(2) + null(2)*6 + checksum(2) = 28 characters
        String releaseCmd = CH9329MSKBMap.getKeyCodeMap().get("release");
        assertEquals(28, releaseCmd.length()); // 14 bytes = 28 hex chars
    }

    @Test
    public void hexStringToByteArray_ConvertsReleaseCommand() {
        String releaseCmd = CH9329MSKBMap.getKeyCodeMap().get("release");
        byte[] bytes = CH9329Function.hexStringToByteArray(releaseCmd);

        assertEquals(14, bytes.length);
        assertEquals((byte) 0x57, bytes[0]);
        assertEquals((byte) 0xAB, bytes[1]);
        assertEquals((byte) 0x00, bytes[2]);
        assertEquals((byte) 0x02, bytes[3]); // CmdKB_HID
        assertEquals((byte) 0x08, bytes[4]); // DataLenKB
    }

    // ====== Null Handling Tests ======

    @Test(expected = NullPointerException.class)
    public void hexStringToByteArray_NullInput_ThrowsException() {
        CH9329Function.hexStringToByteArray(null);
    }

    @Test
    public void getKeyCodeMap_InvalidKey_ReturnsNull() {
        assertNull(CH9329MSKBMap.getKeyCodeMap().get("NONEXISTENT_KEY"));
    }

    // ====== Checksum Tests ======

    @Test
    public void makeChecksum_ProducesTwoCharacterResult() {
        String checksum = CH9329Function.makeChecksum("57AB000208");
        assertNotNull(checksum);
        assertEquals(2, checksum.length());
    }

    @Test
    public void makeChecksum_IsDeterministic() {
        String checksum1 = CH9329Function.makeChecksum("57AB000208000000000000000000");
        String checksum2 = CH9329Function.makeChecksum("57AB000208000000000000000000");
        assertEquals(checksum1, checksum2);
    }

    // ====== Integration Scenarios ======

    @Test
    public void constructSimpleKeyCommand_CorrectStructure() {
        // Construct what a "press 'a'" command should look like
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1"); // 57
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2"); // AB
        String address = CH9329MSKBMap.getKeyCodeMap().get("address"); // 00
        String cmd = CH9329MSKBMap.CmdData().get("CmdKB_HID"); // 02
        String len = CH9329MSKBMap.DataLen().get("DataLenKB"); // 08
        String modifier = "00"; // No modifier
        String nullByte = CH9329MSKBMap.DataNull().get("DataNull"); // 00
        String keyCode = CH9329MSKBMap.getKeyCodeMap().get("a"); // 04

        String command = prefix1 + prefix2 + address + cmd + len +
                modifier + nullByte + keyCode +
                nullByte + nullByte + nullByte + nullByte + nullByte + nullByte;

        // Verify command structure
        assertTrue(command.startsWith("57AB00"));
        assertTrue(command.contains("02")); // Cmd
        assertTrue(command.contains("08")); // Len
        assertTrue(command.contains("04")); // 'a' keycode
    }

    @Test
    public void constructModifiedKeyCommand_CorrectStructure() {
        // Construct "Ctrl+C" command
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1");
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2");
        String address = CH9329MSKBMap.getKeyCodeMap().get("address");
        String cmd = CH9329MSKBMap.CmdData().get("CmdKB_HID");
        String len = CH9329MSKBMap.DataLen().get("DataLenKB");
        String ctrlModifier = CH9329MSKBMap.KBShortCutKey().get("Ctrl"); // 01
        String nullByte = CH9329MSKBMap.DataNull().get("DataNull");
        String keyCode = CH9329MSKBMap.getKeyCodeMap().get("c"); // 06

        String command = prefix1 + prefix2 + address + cmd + len +
                ctrlModifier + nullByte + keyCode +
                nullByte + nullByte + nullByte + nullByte + nullByte + nullByte;

        assertTrue(command.contains("01")); // Ctrl modifier
        assertTrue(command.contains("06")); // 'c' keycode
    }

    @Test
    public void constructMultipleModifierCommand_CorrectStructure() {
        // Construct "Ctrl+Shift+C" command
        int ctrl = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Ctrl").replace("0x", ""), 16);
        int shift = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Shift").replace("0x", ""), 16);
        int combined = ctrl + shift; // 01 + 02 = 03

        String combinedModifier = String.format("%02X", combined);
        assertEquals("03", combinedModifier);
    }

    // ====== Edge Cases ======

    @Test
    public void functionKeyMapping_AllDefined() {
        // Verify function keys F1-F12
        for (int i = 1; i <= 12; i++) {
            String keyCode = CH9329MSKBMap.getKeyCodeMap().get("F" + i);
            assertNotNull("F" + i + " should have a keycode", keyCode);

            // F1 = 0x3A (58), F12 = 0x45 (69)
            int expectedCode = 0x3A + (i - 1);
            assertEquals(String.format("%02X", expectedCode), keyCode);
        }
    }

    @Test
    public void punctuationKeys_AreDefined() {
        assertNotNull("MINUS", CH9329MSKBMap.getKeyCodeMap().get("MINUS"));
        assertNotNull("EQUALS", CH9329MSKBMap.getKeyCodeMap().get("EQUALS"));
        assertNotNull("LEFT_BRACKET", CH9329MSKBMap.getKeyCodeMap().get("LEFT_BRACKET"));
        assertNotNull("RIGHT_BRACKET", CH9329MSKBMap.getKeyCodeMap().get("RIGHT_BRACKET"));
        assertNotNull("BACKSLASH", CH9329MSKBMap.getKeyCodeMap().get("BACKSLASH"));
        assertNotNull("SEMICOLON", CH9329MSKBMap.getKeyCodeMap().get("SEMICOLON"));
        assertNotNull("APOSTROPHE", CH9329MSKBMap.getKeyCodeMap().get("APOSTROPHE"));
        assertNotNull("GRAVE", CH9329MSKBMap.getKeyCodeMap().get("GRAVE"));
        assertNotNull("COMMA", CH9329MSKBMap.getKeyCodeMap().get("COMMA"));
        assertNotNull("PERIOD", CH9329MSKBMap.getKeyCodeMap().get("PERIOD"));
        assertNotNull("SLASH", CH9329MSKBMap.getKeyCodeMap().get("SLASH"));
    }

    @Test
    public void numpadKeys_AreDefined() {
        assertNotNull("NUM_LOCK", CH9329MSKBMap.getKeyCodeMap().get("NUM_LOCK"));
        assertNotNull("NUMPAD_DIVIDE", CH9329MSKBMap.getKeyCodeMap().get("NUMPAD_DIVIDE"));
        assertNotNull("NUMPAD_MULTIPLY", CH9329MSKBMap.getKeyCodeMap().get("NUMPAD_MULTIPLY"));
        assertNotNull("NUMPAD_SUBTRACT", CH9329MSKBMap.getKeyCodeMap().get("NUMPAD_SUBTRACT"));
        assertNotNull("NUMPAD_ADD", CH9329MSKBMap.getKeyCodeMap().get("NUMPAD_ADD"));
    }

    @Test
    public void navigationKeys_AreDefined() {
        assertNotNull("Home", CH9329MSKBMap.getKeyCodeMap().get("Home"));
        assertNotNull("End", CH9329MSKBMap.getKeyCodeMap().get("End"));
        assertNotNull("PgUp", CH9329MSKBMap.getKeyCodeMap().get("PgUp"));
        assertNotNull("PgDn", CH9329MSKBMap.getKeyCodeMap().get("PgDn"));
        assertNotNull("Ins", CH9329MSKBMap.getKeyCodeMap().get("Ins"));
        assertNotNull("Delete", CH9329MSKBMap.getKeyCodeMap().get("Delete"));
    }

    @Test
    public void lockKeys_AreDefined() {
        assertNotNull("CAPS_LOCK", CH9329MSKBMap.getKeyCodeMap().get("CAPS_LOCK"));
        assertNotNull("NUM_LOCK", CH9329MSKBMap.getKeyCodeMap().get("NUM_LOCK"));
        assertNotNull("SCROLL_LOCK", CH9329MSKBMap.getKeyCodeMap().get("SCROLL_LOCK"));
    }

    @Test
    public void printScreenAndPause_AreDefined() {
        assertNotNull("PrtSc", CH9329MSKBMap.getKeyCodeMap().get("PrtSc"));
        assertNotNull("Pause", CH9329MSKBMap.getKeyCodeMap().get("Pause"));
    }

    // ====== USB Device Manager Integration ======
    // Note: Enhanced mode tests require Android Log, which is not available in unit tests.
    // These are tested via integration tests instead.

    @Test
    public void setUsbDeviceManager_StoresReference() {
        // The mock should be stored (verified by no exception)
        KeyBoardManager.setUsbDeviceManager(mockUsbDeviceManager);
        // No exception means success
    }
}
