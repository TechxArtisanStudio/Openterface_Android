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
        // Row 1: Q-P
        ALTERNATES_MAP.put(0x14, Arrays.asList("q", "Q", "1")); // q
        ALTERNATES_MAP.put(0x1A, Arrays.asList("w", "W", "2")); // w
        ALTERNATES_MAP.put(0x08, Arrays.asList("e", "E", "3")); // e
        ALTERNATES_MAP.put(0x15, Arrays.asList("r", "R", "4")); // r
        ALTERNATES_MAP.put(0x17, Arrays.asList("t", "T", "5")); // t
        ALTERNATES_MAP.put(0x1C, Arrays.asList("y", "Y", "6")); // y
        ALTERNATES_MAP.put(0x18, Arrays.asList("u", "U", "7")); // u
        ALTERNATES_MAP.put(0x0C, Arrays.asList("i", "I", "8")); // i
        ALTERNATES_MAP.put(0x12, Arrays.asList("o", "O", "9")); // o
        ALTERNATES_MAP.put(0x13, Arrays.asList("p", "P", "0")); // p

        // Row 2: A-L
        ALTERNATES_MAP.put(0x04, Arrays.asList("a", "A", "@")); // a
        ALTERNATES_MAP.put(0x16, Arrays.asList("s", "S", "#")); // s
        ALTERNATES_MAP.put(0x07, Arrays.asList("d", "D", "$")); // d
        ALTERNATES_MAP.put(0x09, Arrays.asList("f", "F", "%")); // f
        ALTERNATES_MAP.put(0x0A, Arrays.asList("g", "G", "^")); // g
        ALTERNATES_MAP.put(0x0B, Arrays.asList("h", "H", "&")); // h
        ALTERNATES_MAP.put(0x0D, Arrays.asList("j", "J", "*")); // j
        ALTERNATES_MAP.put(0x0E, Arrays.asList("k", "K", "(")); // k
        ALTERNATES_MAP.put(0x0F, Arrays.asList("l", "L", ")")); // l

        // Row 3: Z-M, /
        ALTERNATES_MAP.put(0x1D, Arrays.asList("z", "Z", "!")); // z
        ALTERNATES_MAP.put(0x1B, Arrays.asList("x", "X", "=")); // x
        ALTERNATES_MAP.put(0x06, Arrays.asList("c", "C", "+")); // c
        ALTERNATES_MAP.put(0x19, Arrays.asList("v", "V", "-")); // v
        ALTERNATES_MAP.put(0x05, Arrays.asList("b", "B", "_")); // b
        ALTERNATES_MAP.put(0x11, Arrays.asList("n", "N", "[")); // n
        ALTERNATES_MAP.put(0x10, Arrays.asList("m", "M", "]")); // m
        ALTERNATES_MAP.put(0x38, Arrays.asList("/", "?", "\\")); // /
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
}
