package com.openterface.AOS.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Character alternatives mapping for long-press functionality.
 * References KeyCMD KeyboardMouse Pro layout (keyboard_lower_portrait_no_gui.xml).
 *
 * Each entry's first element is the "corner hint" — the symbol shown on the
 * key cap (top-right) and the first / default option in the long-press popup.
 * The last element is the uppercase letter for that key.
 */
public class CharacterAlternates {

    private static final Map<Integer, List<String>> ALTERNATES_MAP = new HashMap<>();

    static {
        // Row 1: Q-P — [symbol, number, uppercase]
        ALTERNATES_MAP.put(0x14, Arrays.asList("!", "1", "Q")); // q
        ALTERNATES_MAP.put(0x1A, Arrays.asList("@", "2", "W")); // w
        ALTERNATES_MAP.put(0x08, Arrays.asList("#", "3", "E")); // e
        ALTERNATES_MAP.put(0x15, Arrays.asList("$", "4", "R")); // r
        ALTERNATES_MAP.put(0x17, Arrays.asList("%", "5", "T")); // t
        ALTERNATES_MAP.put(0x1C, Arrays.asList("^", "6", "Y")); // y
        ALTERNATES_MAP.put(0x18, Arrays.asList("&", "7", "U")); // u
        ALTERNATES_MAP.put(0x0C, Arrays.asList("*", "8", "I")); // i
        ALTERNATES_MAP.put(0x12, Arrays.asList(",", "9", "O")); // o
        ALTERNATES_MAP.put(0x13, Arrays.asList(".", "0", "P")); // p

        // Row 2: A-L — [symbol, symbol..., uppercase]
        ALTERNATES_MAP.put(0x04, Arrays.asList("¥", "£", "€", "A")); // a
        ALTERNATES_MAP.put(0x16, Arrays.asList("`", "~", "S"));       // s
        ALTERNATES_MAP.put(0x07, Arrays.asList("-", "_", "D"));       // d
        ALTERNATES_MAP.put(0x09, Arrays.asList("+", "=", "F"));       // f
        ALTERNATES_MAP.put(0x0A, Arrays.asList("/", "?", "G"));       // g
        ALTERNATES_MAP.put(0x0B, Arrays.asList("<", ">", "H"));       // h
        ALTERNATES_MAP.put(0x0D, Arrays.asList("[", "]", "J"));       // j
        ALTERNATES_MAP.put(0x0E, Arrays.asList("{", "}", "K"));       // k
        ALTERNATES_MAP.put(0x0F, Arrays.asList("(", ")", "L"));       // l

        // Row 3: Z-M — [symbol, uppercase]
        ALTERNATES_MAP.put(0x1D, Arrays.asList("'", "Z"));           // z
        ALTERNATES_MAP.put(0x1B, Arrays.asList("\"", "X"));          // x
        ALTERNATES_MAP.put(0x06, Arrays.asList(";", "C"));           // c
        ALTERNATES_MAP.put(0x19, Arrays.asList(":", "V"));           // v
        ALTERNATES_MAP.put(0x05, Arrays.asList("/", "B"));           // b
        ALTERNATES_MAP.put(0x11, Arrays.asList("|", "N"));           // n
        ALTERNATES_MAP.put(0x10, Arrays.asList("\\", "M"));          // m

        // / key — KeyCMD: keyAlternates="@string/.../?", cornerHint="?"
        ALTERNATES_MAP.put(0x38, Arrays.asList("?"));
    }

    /**
     * Get character alternatives for a given HID usage code.
     * The first element is the corner-hint / default selection.
     *
     * @param hidUsageCode HID usage code (e.g., 0x14 for 'q')
     * @return List of alternative characters, or null if none available
     */
    public static List<String> getAlternates(int hidUsageCode) {
        return ALTERNATES_MAP.get(hidUsageCode);
    }

    /**
     * Check if a key has alternatives.
     */
    public static boolean hasAlternates(int hidUsageCode) {
        return ALTERNATES_MAP.containsKey(hidUsageCode);
    }

    /**
     * Get the corner hint text for a key — the symbol shown on the key cap
     * (top-right) and the first / default popup option.
     * Always equals alternates[0].
     *
     * @param hidUsageCode HID usage code
     * @return hint string or null
     */
    public static String getCornerHint(int hidUsageCode) {
        List<String> alternates = ALTERNATES_MAP.get(hidUsageCode);
        if (alternates == null || alternates.isEmpty()) return null;
        return alternates.get(0);
    }
}
