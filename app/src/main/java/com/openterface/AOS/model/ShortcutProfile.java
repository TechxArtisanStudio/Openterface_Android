package com.openterface.AOS.model;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shortcut profile containing multiple shortcuts.
 * Used for customizable keyboard shortcut strips in portrait mode.
 */
public class ShortcutProfile {
    public String id;
    public String name;
    public String description;
    public String icon;
    public boolean builtIn;
    public List<Shortcut> shortcuts;
    public long createdAt;
    public long updatedAt;

    public ShortcutProfile() {
        shortcuts = new ArrayList<>();
    }

    /**
     * Returns total count of shortcuts in this profile.
     */
    public int getShortcutCount() {
        return shortcuts != null ? shortcuts.size() : 0;
    }

    /**
     * Represents a single shortcut with key code and modifiers.
     */
    public static class Shortcut {
        public String id;
        public String name;
        public String label;      // Display label like "Ctrl+C"
        public int modifiers;     // HID modifier bitmask
        public int keyCode;       // HID usage code
        public String icon;       // Icon resource name
        public int displayOrder;  // Sort order

        public Shortcut() {}

        public Shortcut(String id, String name, String label, int modifiers, int keyCode) {
            this.id = id;
            this.name = name;
            this.label = label;
            this.modifiers = modifiers;
            this.keyCode = keyCode;
            this.icon = "";
            this.displayOrder = 0;
        }

        public Shortcut(String id, String name, String label, int modifiers, int keyCode, String icon, int displayOrder) {
            this(id, name, label, modifiers, keyCode);
            this.icon = icon;
            this.displayOrder = displayOrder;
        }
    }

    /**
     * HID Modifier bitmasks (USB HID Specification)
     */
    public static final int MOD_NONE = 0;
    public static final int MOD_CTRL = 1;
    public static final int MOD_SHIFT = 2;
    public static final int MOD_ALT = 4;
    public static final int MOD_CTRL_SHIFT = 3;
    public static final int MOD_CTRL_ALT = 5;
    public static final int MOD_SHIFT_ALT = 6;
}
