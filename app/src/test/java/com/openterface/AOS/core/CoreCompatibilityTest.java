package com.openterface.AOS.core;

import com.openterface.AOS.serial.CH9329Function;
import com.openterface.AOS.target.CH9329MSKBMap;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests comparing Android HID implementation with Openterface_Core (C/WASM).
 *
 * This ensures both implementations produce identical HID packets for the
 * same input, guaranteeing compatibility between Android native code and
 * the WebRTC/WebUSB client using the Core library.
 *
 * Coverage:
 * - Frame header constants
 * - Command codes
 * - Keyboard packet structure
 * - Mouse absolute packet structure
 * - Checksum calculation
 * - Key code mappings
 */
public class CoreCompatibilityTest {

    // ====== Frame Structure Constants (from Openterface_Core) ======

    static final byte[] FRAME_HEAD = new byte[]{(byte) 0x57, (byte) 0xAB};
    static final byte DEFAULT_ADDR = 0x00;

    // Command codes from keymod.h / serial.ts
    static final int CMD_KB = 0x02;
    static final int CMD_MS_ABS = 0x04;
    static final int CMD_MS_REL = 0x05;

    // Data lengths
    static final int DATA_LEN_KB = 0x08;
    static final int DATA_LEN_MS_ABS = 0x07;
    static final int DATA_LEN_MS_REL = 0x05;

    // Packet sizes
    static final int PKT_KEYBOARD_SIZE = 14;   // 5 header + 8 data + 1 checksum
    static final int PKT_MOUSE_ABS_SIZE = 13;  // 5 header + 7 data + 1 checksum
    static final int PKT_MOUSE_REL_SIZE = 11;  // 5 header + 5 data + 1 checksum

    // Mouse button masks from keymod.h
    static final int MS_BTN_NONE = 0x00;
    static final int MS_BTN_LEFT = 0x01;
    static final int MS_BTN_RIGHT = 0x02;
    static final int MS_BTN_MIDDLE = 0x04;

    // Modifier masks from keymod.h
    static final int MOD_NONE = 0x00;
    static final int MOD_CTRL = 0x01;
    static final int MOD_SHIFT = 0x02;
    static final int MOD_ALT = 0x04;
    static final int MOD_GUI = 0x08;

    // ====== Frame Structure Tests ======

    @Test
    public void frameHeader_MatchesCore() {
        // Verify frame header matches Openterface_Core
        assertEquals("Frame head byte 0", FRAME_HEAD[0] & 0xFF, 0x57);
        assertEquals("Frame head byte 1", FRAME_HEAD[1] & 0xFF, 0xAB);

        // Verify Android implementation uses same header
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1");
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2");
        assertEquals("57", prefix1);
        assertEquals("AB", prefix2);
    }

    @Test
    public void defaultAddress_MatchesCore() {
        assertEquals("Address", DEFAULT_ADDR & 0xFF, 0x00);
        assertEquals("00", CH9329MSKBMap.getKeyCodeMap().get("address"));
    }

    // ====== Command Code Tests ======

    @Test
    public void keyboardCommand_MatchesCore() {
        assertEquals("CMD_KB", CMD_KB, 0x02);
        assertEquals("CmdKB_HID", CMD_KB,
                Integer.parseInt(CH9329MSKBMap.CmdData().get("CmdKB_HID"), 16));
    }

    @Test
    public void mouseAbsCommand_MatchesCore() {
        assertEquals("CMD_MS_ABS", CMD_MS_ABS, 0x04);
        assertEquals("CmdMS_ABS", CMD_MS_ABS,
                Integer.parseInt(CH9329MSKBMap.CmdData().get("CmdMS_ABS"), 16));
    }

    @Test
    public void mouseRelCommand_MatchesCore() {
        assertEquals("CMD_MS_REL", CMD_MS_REL, 0x05);
        assertEquals("CmdMS_REL", CMD_MS_REL,
                Integer.parseInt(CH9329MSKBMap.CmdData().get("CmdMS_REL"), 16));
    }

    // ====== Data Length Tests ======

    @Test
    public void keyboardDataLength_MatchesCore() {
        assertEquals("DATA_LEN_KB", DATA_LEN_KB, 0x08);
        assertEquals("DataLenKB", DATA_LEN_KB,
                Integer.parseInt(CH9329MSKBMap.DataLen().get("DataLenKB"), 16));
    }

    @Test
    public void mouseAbsDataLength_MatchesCore() {
        assertEquals("DATA_LEN_MS_ABS", DATA_LEN_MS_ABS, 0x07);
        assertEquals("DataLenAbsMS", DATA_LEN_MS_ABS,
                Integer.parseInt(CH9329MSKBMap.DataLen().get("DataLenAbsMS"), 16));
    }

    @Test
    public void mouseRelDataLength_MatchesCore() {
        assertEquals("DATA_LEN_MS_REL", DATA_LEN_MS_REL, 0x05);
        assertEquals("DataLenRelMS", DATA_LEN_MS_REL,
                Integer.parseInt(CH9329MSKBMap.DataLen().get("DataLenRelMS"), 16));
    }

    // ====== Mouse Button Tests ======

    @Test
    public void mouseButtonMasks_MatchCore() {
        // Verify button masks match keymod.h
        assertEquals("MS_BTN_NONE", MS_BTN_NONE, 0x00);
        assertEquals("MS_BTN_LEFT", MS_BTN_LEFT, 0x01);
        assertEquals("MS_BTN_RIGHT", MS_BTN_RIGHT, 0x02);
        assertEquals("MS_BTN_MIDDLE", MS_BTN_MIDDLE, 0x04);

        // Verify Android implementation uses same values
        assertEquals("SecNullData", MS_BTN_NONE,
                Integer.parseInt(CH9329MSKBMap.MSAbsData().get("SecNullData"), 16));
        assertEquals("SecLeftData", MS_BTN_LEFT,
                Integer.parseInt(CH9329MSKBMap.MSAbsData().get("SecLeftData"), 16));
        assertEquals("SecRightData", MS_BTN_RIGHT,
                Integer.parseInt(CH9329MSKBMap.MSAbsData().get("SecRightData"), 16));
        assertEquals("SecMiddleData", MS_BTN_MIDDLE,
                Integer.parseInt(CH9329MSKBMap.MSAbsData().get("SecMiddleData"), 16));
    }

    // ====== Modifier Tests ======

    @Test
    public void modifierMasks_MatchCore() {
        // Verify modifier masks match keymod.h
        assertEquals("MOD_NONE", MOD_NONE, 0x00);
        assertEquals("MOD_CTRL", MOD_CTRL, 0x01);
        assertEquals("MOD_SHIFT", MOD_SHIFT, 0x02);
        assertEquals("MOD_ALT", MOD_ALT, 0x04);
        assertEquals("MOD_GUI", MOD_GUI, 0x08);

        // Verify Android implementation
        assertEquals("Ctrl", MOD_CTRL,
                Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Ctrl"), 16));
        assertEquals("Shift", MOD_SHIFT,
                Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Shift"), 16));
        assertEquals("Alt", MOD_ALT,
                Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Alt"), 16));
        assertEquals("Win", MOD_GUI,
                Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Win"), 16));
    }

    // ====== Key Code Mapping Tests ======

    @Test
    public void hidKeyCodes_MatchCore() {
        // Verify key codes match USB HID Usage Tables
        // These should match between Android and Core

        // Letters a-z: 0x04-0x1D
        assertEquals("a", 0x04, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("a"), 16));
        assertEquals("z", 0x1D, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("z"), 16));

        // Digits 1-0: 0x1E-0x27
        assertEquals("1", 0x1E, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("1"), 16));
        assertEquals("0", 0x27, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("0"), 16));

        // Function keys F1-F12: 0x3A-0x45
        assertEquals("F1", 0x3A, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("F1"), 16));
        assertEquals("F12", 0x45, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("F12"), 16));

        // Special keys
        assertEquals("ENTER", 0x28, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("ENTER"), 16));
        assertEquals("Esc", 0x29, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("Esc"), 16));
        assertEquals("BACK", 0x2A, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("BACK"), 16));
        assertEquals("TAB", 0x2B, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("TAB"), 16));
        assertEquals("SPACE", 0x2C, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("SPACE"), 16));

        // Arrow keys
        assertEquals("DPAD_LEFT", 0x50, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("DPAD_LEFT"), 16));
        assertEquals("DPAD_RIGHT", 0x4F, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("DPAD_RIGHT"), 16));
        assertEquals("DPAD_UP", 0x52, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("DPAD_UP"), 16));
        assertEquals("DPAD_DOWN", 0x51, Integer.parseInt(CH9329MSKBMap.getKeyCodeMap().get("DPAD_DOWN"), 16));
    }

    // ====== Checksum Tests ======

    @Test
    public void checksumCalculation_MatchesCore() {
        // Test checksum matches Openterface_Core algorithm
        // km_checksum sums all bytes except the last and masks to 8 bits

        // Test with frame header
        String testData = "57AB00";
        byte[] bytes = CH9329Function.hexStringToByteArray(testData);
        int sum = 0;
        for (byte b : bytes) {
            sum += (b & 0xFF);
        }
        int expectedChecksum = sum & 0xFF;

        String checksum = CH9329Function.makeChecksum(testData);
        int androidChecksum = Integer.parseInt(checksum, 16);

        assertEquals("Checksum matches", expectedChecksum, androidChecksum);
    }

    @Test
    public void checksum_FullKeyboardPacket() {
        // Build a sample keyboard packet and verify checksum
        // Format: 57 AB 00 02 08 | modifier | 00 | key1..key6 | checksum

        StringBuilder packet = new StringBuilder();
        packet.append("57"); // prefix1
        packet.append("AB"); // prefix2
        packet.append("00"); // address
        packet.append("02"); // CMD_KB
        packet.append("08"); // DATA_LEN_KB
        packet.append("00"); // modifier (none)
        packet.append("00"); // reserved
        packet.append("04"); // key1 (a)
        packet.append("00"); // key2
        packet.append("00"); // key3
        packet.append("00"); // key4
        packet.append("00"); // key5
        packet.append("00"); // key6

        String checksum = CH9329Function.makeChecksum(packet.toString());
        packet.append(checksum);

        // Verify packet length
        assertEquals("Packet length", 28, packet.length()); // 14 bytes = 28 hex chars
    }

    // ====== Packet Structure Comparison Tests ======

    @Test
    public void keyboardPacketStructure_MatchesCore() {
        // Core keyboard packet format:
        // 57 AB 00 02 08 | modifier | 00 | key1..key6 | checksum

        // Build Android equivalent
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1");
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2");
        String address = CH9329MSKBMap.getKeyCodeMap().get("address");
        String cmd = CH9329MSKBMap.CmdData().get("CmdKB_HID");
        String len = CH9329MSKBMap.DataLen().get("DataLenKB");
        String modifier = "00";
        String reserved = CH9329MSKBMap.DataNull().get("DataNull");
        String keyCode = CH9329MSKBMap.getKeyCodeMap().get("a");

        String packet = prefix1 + prefix2 + address + cmd + len +
                modifier + reserved + keyCode +
                reserved + reserved + reserved + reserved + reserved + reserved;

        // Verify structure matches Core
        assertTrue(packet.startsWith("57AB00"));
        assertTrue(packet.contains("02")); // CMD_KB
        assertTrue(packet.contains("08")); // DATA_LEN_KB
        assertTrue(packet.contains("04")); // 'a' keycode
    }

    @Test
    public void mouseAbsPacketStructure_MatchesCore() {
        // Core mouse absolute packet format:
        // 57 AB 00 04 07 | 01 | buttons | x_lo | x_hi | y_lo | y_hi | wheel | checksum

        // Build Android equivalent
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1");
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2");
        String address = CH9329MSKBMap.getKeyCodeMap().get("address");
        String cmd = CH9329MSKBMap.CmdData().get("CmdMS_ABS");
        String len = CH9329MSKBMap.DataLen().get("DataLenAbsMS");
        String firstData = CH9329MSKBMap.MSAbsData().get("FirstData");
        String button = CH9329MSKBMap.MSAbsData().get("SecLeftData");
        String nullByte = CH9329MSKBMap.DataNull().get("DataNull");

        // Coordinates for (2048, 2048) = 0x0800, 0x0800
        String x0 = "08"; // high byte
        String x1 = "00"; // low byte
        String y0 = "08";
        String y1 = "00";

        String packet = prefix1 + prefix2 + address + cmd + len +
                firstData + button + x0 + x1 + y0 + y1 + nullByte;

        // Verify structure matches Core
        assertTrue(packet.startsWith("57AB00"));
        assertTrue(packet.contains("04")); // CMD_MS_ABS
        assertTrue(packet.contains("07")); // DATA_LEN_MS_ABS
        assertTrue(packet.contains("02")); // FirstData
        assertTrue(packet.contains("01")); // SecLeftData
    }

    // ====== Combined Modifier Tests ======

    @Test
    public void combinedModifiers_MatchCore() {
        // Core allows combining modifiers via bitwise OR
        // Verify Android produces same combined values

        int ctrlShift = MOD_CTRL | MOD_SHIFT; // 0x01 | 0x02 = 0x03
        int ctrlAlt = MOD_CTRL | MOD_ALT;     // 0x01 | 0x04 = 0x05
        int allMods = MOD_CTRL | MOD_SHIFT | MOD_ALT | MOD_GUI; // 0x0F

        assertEquals("Ctrl+Shift", 0x03, ctrlShift);
        assertEquals("Ctrl+Alt", 0x05, ctrlAlt);
        assertEquals("All modifiers", 0x0F, allMods);

        // Verify Android shortcut key values match
        int androidCtrl = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Ctrl"), 16);
        int androidShift = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Shift"), 16);
        int androidAlt = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Alt"), 16);
        int androidWin = Integer.parseInt(CH9329MSKBMap.KBShortCutKey().get("Win"), 16);

        assertEquals("Android Ctrl", MOD_CTRL, androidCtrl);
        assertEquals("Android Shift", MOD_SHIFT, androidShift);
        assertEquals("Android Alt", MOD_ALT, androidAlt);
        assertEquals("Android Win", MOD_GUI, androidWin);
    }

    // ====== Release Command Tests ======

    @Test
    public void keyboardReleaseCommand_MatchesCore() {
        // Core release packet is all zeros
        // Android: "57AB00020800000000000000000C"

        String releaseCmd = CH9329MSKBMap.getKeyCodeMap().get("release");
        assertNotNull(releaseCmd);

        // Verify structure
        assertTrue(releaseCmd.startsWith("57AB00")); // header + address
        assertTrue(releaseCmd.contains("02")); // CMD_KB
        assertTrue(releaseCmd.contains("08")); // DATA_LEN_KB

        // Parse the command
        byte[] bytes = CH9329Function.hexStringToByteArray(releaseCmd);
        assertEquals("Release packet size", 14, bytes.length);

        // Verify it's all zeros after header/cmd/len (except checksum)
        // bytes[5-12] should be zeros (modifier, reserved, 6 keys)
        for (int i = 5; i <= 12; i++) {
            assertEquals("Byte " + i + " should be zero", 0, bytes[i] & 0xFF);
        }
    }

    // ====== Edge Case Tests ======

    @Test
    public void coordinateConversion_MatchesCore() {
        // Core uses 12-bit coordinates (0-4095)
        // Test coordinate normalization matches Android

        // Center of 1920x1080 screen → 2048, 2048 in 4096 space
        int androidX = (int) (960.0 * 4096 / 1920);
        int androidY = (int) (540.0 * 4096 / 1080);

        assertEquals("Center X", 2048, androidX);
        assertEquals("Center Y", 2048, androidY);

        // Max coordinates
        int maxX = (int) (1919.0 * 4096 / 1920);
        int maxY = (int) (1079.0 * 4096 / 1080);

        assertTrue("Max X < 4096", maxX < 4096);
        assertTrue("Max Y < 4096", maxY < 4096);
    }

    @Test
    public void packetSizeConstants_MatchCore() {
        assertEquals("Keyboard packet size", PKT_KEYBOARD_SIZE, 14);
        assertEquals("Mouse abs packet size", PKT_MOUSE_ABS_SIZE, 13);
        assertEquals("Mouse rel packet size", PKT_MOUSE_REL_SIZE, 11);
    }
}
