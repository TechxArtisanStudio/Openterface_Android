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
    public List<ShortcutCategory> categories;
    public long createdAt;
    public long updatedAt;

    public ShortcutProfile() {
        shortcuts = new ArrayList<>();
        categories = new ArrayList<>();
    }

    /**
     * Returns all shortcuts flattened across categories (and flat list).
     */
    public List<Shortcut> getAllShortcutsFlat() {
        List<Shortcut> all = new ArrayList<>();
        if (shortcuts != null) {
            all.addAll(shortcuts);
        }
        if (categories != null) {
            for (ShortcutCategory cat : categories) {
                if (cat != null && cat.shortcuts != null) {
                    all.addAll(cat.shortcuts);
                }
            }
        }
        return all;
    }

    /**
     * Returns total count of shortcuts in this profile.
     */
    public int getShortcutCount() {
        int count = shortcuts != null ? shortcuts.size() : 0;
        if (categories != null) {
            for (ShortcutCategory cat : categories) count += cat.shortcuts.size();
        }
        return count;
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
     * Category grouping shortcuts within a profile
     */
    public static class ShortcutCategory {
        public String id;
        public String name;
        public List<Shortcut> shortcuts;

        public ShortcutCategory() { shortcuts = new ArrayList<>(); }

        public ShortcutCategory(String id, String name) {
            this.id = id;
            this.name = name;
            this.shortcuts = new ArrayList<>();
        }
    }

    /**
     * HID Modifier bitmasks (USB HID Specification)
     */
    public static final int MOD_NONE = 0;
    public static final int MOD_CTRL = 1;
    public static final int MOD_SHIFT = 2;
    public static final int MOD_ALT = 4;
    public static final int MOD_WIN = 8;
    public static final int MOD_CTRL_SHIFT = 3;
    public static final int MOD_CTRL_ALT = 5;
    public static final int MOD_SHIFT_ALT = 6;
    public static final int MOD_WIN_SHIFT = 10;
    public static final int MOD_CTRL_WIN = 9;
    public static final int MOD_CTRL_ALT_SHIFT = 7;
}
