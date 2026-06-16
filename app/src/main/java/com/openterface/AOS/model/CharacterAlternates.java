package com.openterface.AOS.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Character alternatives mapping for long-press functionality
 * Maps each letter to its possible alternatives (uppercase, symbols, etc.)
 */
public class CharacterAlternates {

    private static final Map<Integer, List<String>> ALTERNATES_MAP = new HashMap<>();

    static {
        // Row 1: Q-P - Format: [uppercase, number, symbol] - NO lowercase
        ALTERNATES_MAP.put(0x14, Arrays.asList("Q", "1", "!")); // q
        ALTERNATES_MAP.put(0x1A, Arrays.asList("W", "2", "@")); // w
        ALTERNATES_MAP.put(0x08, Arrays.asList("E", "3", "#")); // e
        ALTERNATES_MAP.put(0x15, Arrays.asList("R", "4", "$")); // r
        ALTERNATES_MAP.put(0x17, Arrays.asList("T", "5", "%")); // t
        ALTERNATES_MAP.put(0x1C, Arrays.asList("Y", "6", "^")); // y
        ALTERNATES_MAP.put(0x18, Arrays.asList("U", "7", "&")); // u
        ALTERNATES_MAP.put(0x0C, Arrays.asList("I", "8", "*")); // i
        ALTERNATES_MAP.put(0x12, Arrays.asList("O", "9", "(")); // o
        ALTERNATES_MAP.put(0x13, Arrays.asList("P", "0", ")")); // p

        // Row 2: A-L - Format: [uppercase, symbol]
        ALTERNATES_MAP.put(0x04, Arrays.asList("A", "@")); // a
        ALTERNATES_MAP.put(0x16, Arrays.asList("S", "#")); // s
        ALTERNATES_MAP.put(0x07, Arrays.asList("D", "$")); // d
        ALTERNATES_MAP.put(0x09, Arrays.asList("F", "%")); // f
        ALTERNATES_MAP.put(0x0A, Arrays.asList("G", "^")); // g
        ALTERNATES_MAP.put(0x0B, Arrays.asList("H", "&")); // h
        ALTERNATES_MAP.put(0x0D, Arrays.asList("J", "*")); // j
        ALTERNATES_MAP.put(0x0E, Arrays.asList("K", "(")); // k
        ALTERNATES_MAP.put(0x0F, Arrays.asList("L", ")")); // l

        // Row 3: Z-M, / - Format: [uppercase, symbol]
        ALTERNATES_MAP.put(0x1D, Arrays.asList("Z", "!")); // z
        ALTERNATES_MAP.put(0x1B, Arrays.asList("X", "=")); // x
        ALTERNATES_MAP.put(0x06, Arrays.asList("C", "+")); // c
        ALTERNATES_MAP.put(0x19, Arrays.asList("V", "-")); // v
        ALTERNATES_MAP.put(0x05, Arrays.asList("B", "_")); // b
        ALTERNATES_MAP.put(0x11, Arrays.asList("N", "[")); // n
        ALTERNATES_MAP.put(0x10, Arrays.asList("M", "]")); // m
        ALTERNATES_MAP.put(0x38, Arrays.asList("?", "\\")); // /
    }

    /**
     * Get character alternatives for a given HID usage code
     * @param hidUsageCode HID usage code (e.g., 0x14 for 'q')
     * @return List of alternative characters, or null if none available
     */
    public static List<String> getAlternates(int hidUsageCode) {
        return ALTERNATES_MAP.get(hidUsageCode);
    }

    /**
     * Check if a key has alternatives
     * @param hidUsageCode HID usage code
     * @return true if alternatives are available
     */
    public static boolean hasAlternates(int hidUsageCode) {
        return ALTERNATES_MAP.containsKey(hidUsageCode);
    }

    /**
     * Get the corner hint text for a key (number/symbol only, not uppercase)
     * For letter keys q-p, returns the number (e.g., "1" for q)
     * For other keys, returns the first symbol (e.g., "@" for a)
     * @param hidUsageCode HID usage code
     * @return hint string or null
     */
    public static String getCornerHint(int hidUsageCode) {
        List<String> alternates = ALTERNATES_MAP.get(hidUsageCode);
        if (alternates == null || alternates.isEmpty()) return null;
        // Skip the first item (uppercase letter), return the next one
        if (alternates.size() > 1) {
            return alternates.get(1);
        }
        return null;
    }
}
