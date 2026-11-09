package com.openterface.AOS.target;

/**
 * Mouse control mode.
 * <p>
 * ABSOLUTE: Click to position cursor, long press to drag (default).
 * RELATIVE: Slide to move cursor like a traditional mouse.
 */
public enum MouseMode {
    ABSOLUTE,   // Absolute mode: tap to position, long press to drag
    RELATIVE    // Relative mode: slide to move cursor
}
