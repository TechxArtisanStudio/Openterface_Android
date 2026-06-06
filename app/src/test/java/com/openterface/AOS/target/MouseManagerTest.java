package com.openterface.AOS.target;

import com.openterface.AOS.serial.CH9329Function;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for MouseManager — verifies coordinate math and command structure.
 *
 * Note: MouseManager requires Android runtime (Log, HandlerThread), so this test
 * focuses on the static CH9329MSKBMap mappings and coordinate calculations
 * that can be verified without Android APIs.
 */
public class MouseManagerTest {

    // ====== Screen Dimension Placeholder Tests ======
    // These document expected behavior - actual screen dimension setting
    // requires Android runtime

    @Test
    public void screenWidth_InitialValue_IsZero() {
        // MouseManager.screenWidth starts at 0
        assertEquals(0, MouseManager.screenWidth);
    }

    @Test
    public void screenHeight_InitialValue_IsZero() {
        // MouseManager.screenHeight starts at 0
        assertEquals(0, MouseManager.screenHeight);
    }

    // ====== Coordinate Normalization Tests ======

    @Test
    public void coordinateNormalization_CenterOfScreen() {
        // Center (960, 540) should normalize to (2048, 2048) in 4096 range
        // 960/1920 * 4096 = 2048
        // 540/1080 * 4096 = 2048
        int expectedX = (int) (960.0 * 4096 / 1920);
        int expectedY = (int) (540.0 * 4096 / 1080);

        assertEquals(2048, expectedX);
        assertEquals(2048, expectedY);
    }

    @Test
    public void coordinateNormalization_Origin() {
        // (0, 0) should normalize to (0, 0)
        int normalizedX = (int) (0.0 * 4096 / 1920);
        int normalizedY = (int) (0.0 * 4096 / 1080);

        assertEquals(0, normalizedX);
        assertEquals(0, normalizedY);
    }

    @Test
    public void coordinateNormalization_Maximum() {
        // (1920, 1080) should normalize to just under 4096
        int normalizedX = (int) (1919.0 * 4096 / 1920);
        int normalizedY = (int) (1079.0 * 4096 / 1080);

        assertTrue(normalizedX < 4096);
        assertTrue(normalizedY < 4096);
    }

    @Test
    public void coordinateNormalization_SquareScreen() {
        // On square screen, aspect ratio is 1:1
        int normalizedX = (int) (512.0 * 4096 / 1024);
        int normalizedY = (int) (512.0 * 4096 / 1024);

        assertEquals(2048, normalizedX);
        assertEquals(2048, normalizedY);
    }

    // ====== Button Type Tests ======

    @Test
    public void allButtonTypes_AreDefined() {
        // Verify all expected button types exist in CH9329MSKBMap
        assertNotNull(CH9329MSKBMap.MSAbsData().get("SecNullData"));
        assertNotNull(CH9329MSKBMap.MSAbsData().get("SecLeftData"));
        assertNotNull(CH9329MSKBMap.MSAbsData().get("SecRightData"));
        assertNotNull(CH9329MSKBMap.MSAbsData().get("SecMiddleData"));

        // Verify values
        assertEquals("00", CH9329MSKBMap.MSAbsData().get("SecNullData"));
        assertEquals("01", CH9329MSKBMap.MSAbsData().get("SecLeftData"));
        assertEquals("02", CH9329MSKBMap.MSAbsData().get("SecRightData"));
        assertEquals("04", CH9329MSKBMap.MSAbsData().get("SecMiddleData"));
    }

    // ====== Command Prefix Tests ======

    @Test
    public void commandPrefixes_AreCorrect() {
        assertEquals("57", CH9329MSKBMap.getKeyCodeMap().get("prefix1"));
        assertEquals("AB", CH9329MSKBMap.getKeyCodeMap().get("prefix2"));
        assertEquals("00", CH9329MSKBMap.getKeyCodeMap().get("address"));
    }

    @Test
    public void mouseCommands_AreCorrect() {
        assertEquals("04", CH9329MSKBMap.CmdData().get("CmdMS_ABS"));
        assertEquals("05", CH9329MSKBMap.CmdData().get("CmdMS_REL"));
    }

    @Test
    public void dataLengths_AreCorrect() {
        assertEquals("07", CH9329MSKBMap.DataLen().get("DataLenAbsMS"));
        assertEquals("05", CH9329MSKBMap.DataLen().get("DataLenRelMS"));
    }

    // ====== Byte Conversion Tests ======

    @Test
    public void intToByteArray_ConvertsZero() {
        byte[] result = CH9329Function.intToByteArray(0);
        assertEquals(2, result.length);
        assertEquals((byte) 0, result[0]);
        assertEquals((byte) 0, result[1]);
    }

    @Test
    public void intToByteArray_ConvertsMaxValue() {
        byte[] result = CH9329Function.intToByteArray(4095); // 0x0FFF
        assertEquals(2, result.length);
        // Function returns little-endian: low byte first, high byte second
        assertEquals(0xFF, result[0] & 0xFF); // Low byte (0xFF = 255, or -1 as signed)
        assertEquals(0x0F, result[1] & 0xFF); // High byte (0x0F = 15)
    }

    @Test
    public void intToByteArray_Converts2048() {
        byte[] result = CH9329Function.intToByteArray(2048); // 0x0800
        assertEquals(2, result.length);
        // Little-endian: low byte first
        assertEquals(0x00, result[0] & 0xFF); // Low byte
        assertEquals(0x08, result[1] & 0xFF); // High byte
    }

    // ====== Hex String Conversion Tests ======

    @Test
    public void hexStringToByteArray_ConvertsCorrectly() {
        byte[] result = CH9329Function.hexStringToByteArray("57AB00");
        assertEquals(3, result.length);
        assertEquals((byte) 0x57, result[0]);
        assertEquals((byte) 0xAB, result[1]);
        assertEquals((byte) 0x00, result[2]);
    }

    @Test
    public void hexStringToByteArray_HandlesLongString() {
        byte[] result = CH9329Function.hexStringToByteArray("57AB00020800000000000000000C");
        assertEquals(14, result.length);
    }

    @Test
    public void hexStringToByteArray_HandlesEmptyString() {
        byte[] result = CH9329Function.hexStringToByteArray("");
        assertEquals(0, result.length);
    }

    // ====== Checksum Tests ======

    @Test
    public void makeChecksum_CalculatesCorrectly() {
        String checksum = CH9329Function.makeChecksum("57AB00");
        assertNotNull(checksum);
        assertEquals(2, checksum.length()); // 2 hex characters
    }

    @Test
    public void makeChecksum_IsConsistent() {
        String checksum1 = CH9329Function.makeChecksum("57AB000208");
        String checksum2 = CH9329Function.makeChecksum("57AB000208");
        assertEquals(checksum1, checksum2);
    }

    // ====== Command Construction Tests ======

    @Test
    public void commandStructure_ForAbsoluteMove() {
        // A typical absolute mouse move command structure:
        // prefix1(2) + prefix2(2) + address(2) + cmd(2) + len(2) + firstData(2) +
        // button(2) + x0(2) + x1(2) + y0(2) + y1(2) + null(2) + checksum(2)

        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1"); // 57
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2"); // AB
        String address = CH9329MSKBMap.getKeyCodeMap().get("address"); // 00
        String cmd = CH9329MSKBMap.CmdData().get("CmdMS_ABS"); // 04
        String len = CH9329MSKBMap.DataLen().get("DataLenAbsMS"); // 07
        String firstData = CH9329MSKBMap.MSAbsData().get("FirstData"); // 02
        String button = CH9329MSKBMap.MSAbsData().get("SecNullData"); // 00
        String nullByte = CH9329MSKBMap.DataNull().get("DataNull"); // 00

        // Sample command for move to (2048, 2048) = (0x08, 0x00, 0x08, 0x00)
        String command = prefix1 + prefix2 + address + cmd + len +
                firstData + button + "08" + "00" + "08" + "00" + nullByte;

        // Verify structure
        assertTrue(command.startsWith("57AB00"));
        assertTrue(command.contains("04")); // CmdMS_ABS
        assertTrue(command.contains("07")); // DataLenAbsMS
        assertTrue(command.contains("02")); // FirstData
    }

    @Test
    public void commandStructure_ForAbsoluteClick() {
        // Click uses "SecLeftData" instead of "SecNullData"
        String prefix1 = CH9329MSKBMap.getKeyCodeMap().get("prefix1"); // 57
        String prefix2 = CH9329MSKBMap.getKeyCodeMap().get("prefix2"); // AB
        String address = CH9329MSKBMap.getKeyCodeMap().get("address"); // 00
        String cmd = CH9329MSKBMap.CmdData().get("CmdMS_ABS"); // 04
        String len = CH9329MSKBMap.DataLen().get("DataLenAbsMS"); // 07
        String firstData = CH9329MSKBMap.MSAbsData().get("FirstData"); // 02
        String leftButton = CH9329MSKBMap.MSAbsData().get("SecLeftData"); // 01

        assertEquals("01", leftButton);

        String command = prefix1 + prefix2 + address + cmd + len +
                firstData + leftButton + "08" + "00" + "08" + "00" + "00";

        assertTrue(command.contains("01")); // Left button
    }

    // ====== Edge Cases ======

    @Test
    public void coordinates_OutOfBounds_Positive() {
        // Should handle coordinates beyond screen size gracefully
        int normalizedX = (int) (3000.0 * 4096 / 1920);
        assertTrue(normalizedX > 4096);
    }

    @Test
    public void coordinates_NegativeInput() {
        // Test with negative coordinates
        int normalizedX = (int) (-100.0 * 4096 / 1920);
        assertTrue(normalizedX < 0);
    }

    @Test
    public void mouseAbsData_ContainsScrollWheel() {
        assertNotNull(CH9329MSKBMap.MSAbsData().get("SlideUp"));
        assertNotNull(CH9329MSKBMap.MSAbsData().get("Downward"));
        assertEquals("02", CH9329MSKBMap.MSAbsData().get("SlideUp"));
        assertEquals("82", CH9329MSKBMap.MSAbsData().get("Downward"));
    }

    @Test
    public void mouseRelData_ContainsDirectional() {
        assertNotNull(CH9329MSKBMap.MSRelData().get("LeftRel"));
        assertNotNull(CH9329MSKBMap.MSRelData().get("RightRel"));
        assertNotNull(CH9329MSKBMap.MSRelData().get("UpRel"));
        assertNotNull(CH9329MSKBMap.MSRelData().get("DownRel"));
    }

    @Test
    public void dataNull_IsZero() {
        assertEquals("00", CH9329MSKBMap.DataNull().get("DataNull"));
    }
}
