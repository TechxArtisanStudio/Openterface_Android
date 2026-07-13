package com.openterface.AOS.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openterface.AOS.model.ShortcutProfile;
import com.openterface.AOS.model.ShortcutProfile.Shortcut;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages shortcut profiles for the customizable keyboard shortcut strip.
 * Supports CRUD operations, JSON import/export, and active profile switching.
 */
public class ShortcutProfileManager {
    private static final String TAG = "OP-SHORTCUT";
    private static final String PREFS_NAME = "ShortcutProfiles";
    private static final String KEY_PROFILES = "profiles_list";
    private static final String KEY_ACTIVE_PROFILE = "active_profile_id";
    private static final String MY_SHORTCUTS_KEY_PREFIX = "my_shortcuts_";

    // HID Keyboard Usage IDs (USB HID Specification)
    private static final int KEY_A=4, KEY_B=5, KEY_C=6, KEY_D=7, KEY_E=8, KEY_F=9;
    private static final int KEY_G=10, KEY_H=11, KEY_I=12, KEY_J=13, KEY_K=14, KEY_L=15;
    private static final int KEY_M=16, KEY_N=17, KEY_O=18, KEY_P=19, KEY_Q=20, KEY_R=21;
    private static final int KEY_S=22, KEY_T=23, KEY_U=24, KEY_V=25, KEY_W=26, KEY_X=27;
    private static final int KEY_Y=28, KEY_Z=29;
    private static final int KEY_1=30, KEY_2=31, KEY_3=32, KEY_4=33, KEY_5=34;
    private static final int KEY_6=35, KEY_7=36, KEY_8=37, KEY_9=38, KEY_0=39;
    private static final int KEY_ENTER=40, KEY_ESC=41, KEY_BACKSPACE=42, KEY_TAB=43;
    private static final int KEY_SPACE=44, KEY_MINUS=45, KEY_EQUALS=46;
    private static final int KEY_LBRACKET=47, KEY_RBRACKET=48, KEY_BACKSLASH=49;
    private static final int KEY_SEMICOLON=51, KEY_QUOTE=52, KEY_GRAVE=53;
    private static final int KEY_COMMA=54, KEY_PERIOD=55, KEY_SLASH=56;
    private static final int KEY_F1=58, KEY_F2=59, KEY_F3=60, KEY_F4=61, KEY_F5=62, KEY_F6=63;
    private static final int KEY_F7=64, KEY_F8=65, KEY_F9=66, KEY_F10=67, KEY_F11=68, KEY_F12=69;
    private static final int KEY_HOME=74, KEY_PAGEUP=75, KEY_DELETE=76, KEY_END=77, KEY_PAGEDOWN=78;
    private static final int KEY_RIGHT=79, KEY_LEFT=80, KEY_DOWN=81, KEY_UP=82;
    private static final int NUMPAD_SLASH=84, NUMPAD_ASTERISK=85, NUMPAD_MINUS=86, NUMPAD_PLUS=87;
    private static final int NUMPAD_ENTER=88, NUMPAD_1=89, NUMPAD_2=90, NUMPAD_3=91, NUMPAD_4=92;
    private static final int NUMPAD_5=93, NUMPAD_6=94, NUMPAD_7=95, NUMPAD_8=96, NUMPAD_9=97;
    private static final int NUMPAD_0=98, NUMPAD_DOT=99;

    private static ShortcutProfileManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<ShortcutProfile> profiles;
    private String activeProfileId;
    private ProfileChangeListener listener;

    private ShortcutProfileManager(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        profiles = loadProfiles();
        activeProfileId = prefs.getString(KEY_ACTIVE_PROFILE, "default");

        if (profiles.isEmpty()) {
            createDefaultProfiles();
        } else {
            migrateIfNeeded();
        }
    }

    public static synchronized ShortcutProfileManager getInstance(Context context) {
        if (instance == null) {
            instance = new ShortcutProfileManager(context);
        }
        return instance;
    }

    /**
     * Load profiles from SharedPreferences
     */
    private List<ShortcutProfile> loadProfiles() {
        String json = prefs.getString(KEY_PROFILES, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<ShortcutProfile>>(){}.getType();
        List<ShortcutProfile> loaded = gson.fromJson(json, type);
        return loaded != null ? loaded : new ArrayList<>();
    }

    /**
     * Save profiles to SharedPreferences
     */
    private void saveProfiles() {
        String json = gson.toJson(profiles);
        prefs.edit().putString(KEY_PROFILES, json).apply();
        Log.d(TAG, "Saved " + profiles.size() + " profiles");
    }

    /**
     * Migrate old profiles that don't have categories support.
     * Handles three cases:
     * 1. Built-in profile is completely empty (no shortcuts, no categories) → replace with default
     * 2. Built-in profile has shortcuts but no categories → migrate shortcuts to a "General" category
     * 3. Default profile has categories but they're all empty (corrupted state) → replace with default
     */
    private void migrateIfNeeded() {
        boolean migrated = false;

        for (int i = 0; i < profiles.size(); i++) {
            ShortcutProfile p = profiles.get(i);
            if (!p.builtIn) continue;

            boolean hasShortcuts = p.shortcuts != null && !p.shortcuts.isEmpty();
            boolean hasCategories = p.categories != null && !p.categories.isEmpty();

            // Case 1: Completely empty built-in profile → replace with default
            if (!hasShortcuts && !hasCategories) {
                ShortcutProfile replacement = getBuiltinReplacement(p.id);
                if (replacement != null) {
                    profiles.set(i, replacement);
                    migrated = true;
                    Log.d(TAG, "Case 1: Replaced empty built-in profile: " + p.id);
                }
            }
            // Case 2: Has shortcuts but no categories → migrate to a category
            else if (hasShortcuts && !hasCategories) {
                ShortcutProfile.ShortcutCategory cat =
                    new ShortcutProfile.ShortcutCategory("migrated", "General");
                cat.shortcuts.addAll(p.shortcuts);
                p.categories.add(cat);
                p.shortcuts.clear();
                migrated = true;
                Log.d(TAG, "Case 2: Migrated shortcuts to category for profile: " + p.id);
            }
            // Case 3: Default profile has categories but all are empty → replace with default
            else if ("default".equals(p.id) && hasCategories) {
                int totalShortcuts = 0;
                for (ShortcutProfile.ShortcutCategory c : p.categories) {
                    if (c.shortcuts != null) {
                        totalShortcuts += c.shortcuts.size();
                    }
                }
                if (totalShortcuts == 0) {
                    ShortcutProfile replacement = createDefaultProfile();
                    profiles.set(i, replacement);
                    migrated = true;
                    Log.d(TAG, "Case 3: Replaced corrupted default profile");
                }
            }
        }

        if (migrated) {
            saveProfiles();
        }
    }

    /**
     * Get the default replacement for a built-in profile by ID
     */
    private ShortcutProfile getBuiltinReplacement(String profileId) {
        switch (profileId) {
            case "default": return createDefaultProfile();
            case "blender": return createBlenderProfile();
            case "kicad": return createKiCADProfile();
            case "nomad": return createNomadProfile();
            case "fusion360": return createFusion360Profile();
            case "photoshop": return createPhotoshopProfile();
            case "vscode": return createVSCodeProfile();
            default: return null;
        }
    }

    /**
     * Reset a built-in profile to factory defaults
     */
    public void resetProfile(String profileId) {
        for (int i = 0; i < profiles.size(); i++) {
            ShortcutProfile p = profiles.get(i);
            if (p.builtIn && p.id.equals(profileId)) {
                ShortcutProfile replacement = getBuiltinReplacement(p.id);
                if (replacement != null) {
                    profiles.set(i, replacement);
                    saveProfiles();
                    if (listener != null) {
                        listener.onProfileUpdated(replacement);
                    }
                }
                return;
            }
        }
    }

    /**
     * Create all default profiles (matching KeyCMD)
     */
    private void createDefaultProfiles() {
        profiles.add(createDefaultProfile());
        profiles.add(createBlenderProfile());
        profiles.add(createKiCADProfile());
        profiles.add(createNomadProfile());
        profiles.add(createFusion360Profile());
        profiles.add(createPhotoshopProfile());
        profiles.add(createVSCodeProfile());
        saveProfiles();
        Log.d(TAG, "Created " + profiles.size() + " default profiles");
    }

    /**
     * Default profile — common shortcuts for any app
     */
    private ShortcutProfile createDefaultProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "default";
        p.name = "Default";
        p.description = "Common shortcuts for any app";
        p.icon = "ic_default";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        // Basic Editing
        ShortcutProfile.ShortcutCategory editing = new ShortcutProfile.ShortcutCategory("editing", "Editing");
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-1", "Undo", "Ctrl+Z", ShortcutProfile.MOD_CTRL, KEY_Z));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-2", "Redo", "Ctrl+Y", ShortcutProfile.MOD_CTRL, KEY_Y));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-3", "Cut", "Ctrl+X", ShortcutProfile.MOD_CTRL, KEY_X));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-4", "Copy", "Ctrl+C", ShortcutProfile.MOD_CTRL, KEY_C));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-5", "Paste", "Ctrl+V", ShortcutProfile.MOD_CTRL, KEY_V));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-6", "Select All", "Ctrl+A", ShortcutProfile.MOD_CTRL, KEY_A));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-7", "Find", "Ctrl+F", ShortcutProfile.MOD_CTRL, KEY_F));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("d-e-8", "Replace", "Ctrl+H", ShortcutProfile.MOD_CTRL, KEY_H));
        p.categories.add(editing);

        // File Operations
        ShortcutProfile.ShortcutCategory file = new ShortcutProfile.ShortcutCategory("file", "File");
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-1", "New", "Ctrl+N", ShortcutProfile.MOD_CTRL, KEY_N));
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-2", "Open", "Ctrl+O", ShortcutProfile.MOD_CTRL, KEY_O));
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-3", "Save", "Ctrl+S", ShortcutProfile.MOD_CTRL, KEY_S));
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-4", "Save As", "Ctrl+Shift+S", ShortcutProfile.MOD_CTRL_SHIFT, KEY_S));
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-5", "Print", "Ctrl+P", ShortcutProfile.MOD_CTRL, KEY_P));
        file.shortcuts.add(new ShortcutProfile.Shortcut("d-f-6", "Close", "Ctrl+W", ShortcutProfile.MOD_CTRL, KEY_W));
        p.categories.add(file);

        // Window Operations
        ShortcutProfile.ShortcutCategory window = new ShortcutProfile.ShortcutCategory("window", "Window");
        window.shortcuts.add(new ShortcutProfile.Shortcut("d-w-1", "Minimize", "Win+D", ShortcutProfile.MOD_WIN, KEY_D));
        window.shortcuts.add(new ShortcutProfile.Shortcut("d-w-2", "Switch Window", "Alt+Tab", ShortcutProfile.MOD_ALT, KEY_TAB));
        window.shortcuts.add(new ShortcutProfile.Shortcut("d-w-3", "Close Window", "Alt+F4", ShortcutProfile.MOD_ALT, KEY_F4));
        window.shortcuts.add(new ShortcutProfile.Shortcut("d-w-4", "Task Manager", "Ctrl+Shift+Esc", ShortcutProfile.MOD_CTRL_SHIFT, KEY_ESC));
        window.shortcuts.add(new ShortcutProfile.Shortcut("d-w-5", "Show Desktop", "Win+D", ShortcutProfile.MOD_WIN, KEY_D));
        p.categories.add(window);

        // System
        ShortcutProfile.ShortcutCategory system = new ShortcutProfile.ShortcutCategory("system", "System");
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-1", "Task Manager", "Ctrl+Alt+Del", ShortcutProfile.MOD_CTRL_ALT, KEY_DELETE));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-2", "Lock Screen", "Win+L", ShortcutProfile.MOD_WIN, KEY_L));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-3", "Screenshot", "Win+Shift+S", ShortcutProfile.MOD_WIN_SHIFT, KEY_S));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-4", "Show Desktop", "Win+D", ShortcutProfile.MOD_WIN, KEY_D));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-5", "Switch Window", "Alt+Tab", ShortcutProfile.MOD_ALT, KEY_TAB));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-6", "Close Window", "Alt+F4", ShortcutProfile.MOD_ALT, KEY_F4));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-7", "Run", "Win+R", ShortcutProfile.MOD_WIN, KEY_R));
        system.shortcuts.add(new ShortcutProfile.Shortcut("d-sys-8", "Explorer", "Win+E", ShortcutProfile.MOD_WIN, KEY_E));
        p.categories.add(system);

        // Browser
        ShortcutProfile.ShortcutCategory browser = new ShortcutProfile.ShortcutCategory("browser", "Browser");
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-1", "New Tab", "Ctrl+T", ShortcutProfile.MOD_CTRL, KEY_T));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-2", "Close Tab", "Ctrl+W", ShortcutProfile.MOD_CTRL, KEY_W));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-3", "Restore Tab", "Ctrl+Shift+T", ShortcutProfile.MOD_CTRL_SHIFT, KEY_T));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-4", "Refresh", "F5", ShortcutProfile.MOD_NONE, KEY_F5));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-5", "Hard Refresh", "Ctrl+F5", ShortcutProfile.MOD_CTRL, KEY_F5));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-6", "Address Bar", "Ctrl+L", ShortcutProfile.MOD_CTRL, KEY_L));
        browser.shortcuts.add(new ShortcutProfile.Shortcut("d-b-7", "Bookmarks", "Ctrl+D", ShortcutProfile.MOD_CTRL, KEY_D));
        p.categories.add(browser);

        // Text
        ShortcutProfile.ShortcutCategory text = new ShortcutProfile.ShortcutCategory("text", "Text");
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-1", "Bold", "Ctrl+B", ShortcutProfile.MOD_CTRL, KEY_B));
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-2", "Italic", "Ctrl+I", ShortcutProfile.MOD_CTRL, KEY_I));
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-3", "Underline", "Ctrl+U", ShortcutProfile.MOD_CTRL, KEY_U));
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-4", "Align Left", "Ctrl+L", ShortcutProfile.MOD_CTRL, KEY_L));
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-5", "Center", "Ctrl+E", ShortcutProfile.MOD_CTRL, KEY_E));
        text.shortcuts.add(new ShortcutProfile.Shortcut("d-t-6", "Align Right", "Ctrl+R", ShortcutProfile.MOD_CTRL, KEY_R));
        p.categories.add(text);

        // Media Control
        ShortcutProfile.ShortcutCategory media = new ShortcutProfile.ShortcutCategory("media", "Media");
        media.shortcuts.add(new ShortcutProfile.Shortcut("d-med-1", "Play/Pause", "F8", ShortcutProfile.MOD_NONE, KEY_F8));
        media.shortcuts.add(new ShortcutProfile.Shortcut("d-med-2", "Previous Track", "F7", ShortcutProfile.MOD_NONE, KEY_F7));
        media.shortcuts.add(new ShortcutProfile.Shortcut("d-med-3", "Next Track", "F9", ShortcutProfile.MOD_NONE, KEY_F9));
        media.shortcuts.add(new ShortcutProfile.Shortcut("d-med-4", "Mute", "F6", ShortcutProfile.MOD_NONE, KEY_F6));
        p.categories.add(media);

        return p;
    }

    /**
     * Blender 3D — shortcuts for Blender 3D modeling
     */
    private ShortcutProfile createBlenderProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "blender";
        p.name = "Blender 3D";
        p.description = "Shortcuts for Blender 3D modeling";
        p.icon = "ic_blender";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory transform = new ShortcutProfile.ShortcutCategory("transform", "Transform");
        transform.shortcuts.add(new ShortcutProfile.Shortcut("b-t-1", "Move",    "G", ShortcutProfile.MOD_NONE, KEY_G));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("b-t-2", "Rotate",  "R", ShortcutProfile.MOD_NONE, KEY_R));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("b-t-3", "Scale",   "S", ShortcutProfile.MOD_NONE, KEY_S));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("b-t-4", "Extrude", "E", ShortcutProfile.MOD_NONE, KEY_E));
        p.categories.add(transform);

        ShortcutProfile.ShortcutCategory selection = new ShortcutProfile.ShortcutCategory("selection", "Selection");
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-1", "Select All",       "A",      ShortcutProfile.MOD_NONE,       KEY_A));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-2", "Select Linked",    "Alt+L",  ShortcutProfile.MOD_ALT,        KEY_L));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-3", "Linked All",       "Ctrl+L", ShortcutProfile.MOD_CTRL,       KEY_L));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-4", "Invert Sel.",      "Ctrl+I", ShortcutProfile.MOD_CTRL,       KEY_I));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-5", "Circle Select",    "C",      ShortcutProfile.MOD_NONE,       KEY_C));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-6", "Grow Selection",   "Ctrl++", ShortcutProfile.MOD_CTRL_SHIFT, KEY_EQUALS));
        selection.shortcuts.add(new ShortcutProfile.Shortcut("b-s-7", "Shrink Selection", "Ctrl+-", ShortcutProfile.MOD_CTRL,       KEY_MINUS));
        p.categories.add(selection);

        ShortcutProfile.ShortcutCategory view = new ShortcutProfile.ShortcutCategory("view", "View");
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-1", "Top",          "Numpad 7", ShortcutProfile.MOD_NONE, NUMPAD_7));
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-2", "Front",        "Numpad 1", ShortcutProfile.MOD_NONE, NUMPAD_1));
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-3", "Right",        "Numpad 3", ShortcutProfile.MOD_NONE, NUMPAD_3));
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-4", "Opposite",     "Numpad 9", ShortcutProfile.MOD_NONE, NUMPAD_9));
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-5", "Camera",       "Numpad 0", ShortcutProfile.MOD_NONE, NUMPAD_0));
        view.shortcuts.add(new ShortcutProfile.Shortcut("b-v-6", "Toggle X-Ray", "Alt+Z",    ShortcutProfile.MOD_ALT,  KEY_Z));
        p.categories.add(view);

        ShortcutProfile.ShortcutCategory mesh = new ShortcutProfile.ShortcutCategory("mesh", "Mesh");
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-1",  "Loop Cut",       "Ctrl+R",  ShortcutProfile.MOD_CTRL,  KEY_R));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-2",  "Bevel",          "Ctrl+B",  ShortcutProfile.MOD_CTRL,  KEY_B));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-3",  "Merge",          "M",       ShortcutProfile.MOD_NONE,  KEY_M));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-4",  "Create Face",    "F",       ShortcutProfile.MOD_NONE,  KEY_F));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-5",  "Inset Faces",    "I",       ShortcutProfile.MOD_NONE,  KEY_I));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-6",  "Knife",          "K",       ShortcutProfile.MOD_NONE,  KEY_K));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-7",  "Rip",            "V",       ShortcutProfile.MOD_NONE,  KEY_V));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-8",  "Bend",           "Shift+W", ShortcutProfile.MOD_SHIFT, KEY_W));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-9",  "Recalc Normals", "Shift+N", ShortcutProfile.MOD_SHIFT, KEY_N));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("b-m-10", "Delete",         "X",       ShortcutProfile.MOD_NONE,  KEY_X));
        p.categories.add(mesh);

        ShortcutProfile.ShortcutCategory mode = new ShortcutProfile.ShortcutCategory("mode", "Mode");
        mode.shortcuts.add(new ShortcutProfile.Shortcut("b-mo-1", "Edit ↔ Object", "Tab", ShortcutProfile.MOD_NONE, KEY_TAB));
        p.categories.add(mode);

        ShortcutProfile.ShortcutCategory component = new ShortcutProfile.ShortcutCategory("component", "Component");
        component.shortcuts.add(new ShortcutProfile.Shortcut("b-c-1", "Vertex", "1", ShortcutProfile.MOD_NONE, KEY_1));
        component.shortcuts.add(new ShortcutProfile.Shortcut("b-c-2", "Edge",   "2", ShortcutProfile.MOD_NONE, KEY_2));
        component.shortcuts.add(new ShortcutProfile.Shortcut("b-c-3", "Face",   "3", ShortcutProfile.MOD_NONE, KEY_3));
        p.categories.add(component);

        ShortcutProfile.ShortcutCategory object = new ShortcutProfile.ShortcutCategory("object", "Object");
        object.shortcuts.add(new ShortcutProfile.Shortcut("b-o-1", "Duplicate", "Alt+D",  ShortcutProfile.MOD_ALT,  KEY_D));
        object.shortcuts.add(new ShortcutProfile.Shortcut("b-o-2", "Separate",  "P",      ShortcutProfile.MOD_NONE, KEY_P));
        object.shortcuts.add(new ShortcutProfile.Shortcut("b-o-3", "Join",      "Ctrl+J", ShortcutProfile.MOD_CTRL, KEY_J));
        p.categories.add(object);

        ShortcutProfile.ShortcutCategory tools = new ShortcutProfile.ShortcutCategory("tools", "Tools");
        tools.shortcuts.add(new ShortcutProfile.Shortcut("b-to-1", "Search",      "F3",      ShortcutProfile.MOD_NONE,  KEY_F3));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("b-to-2", "Repeat Last", "Shift+R", ShortcutProfile.MOD_SHIFT, KEY_R));
        p.categories.add(tools);

        ShortcutProfile.ShortcutCategory ui = new ShortcutProfile.ShortcutCategory("ui", "UI");
        ui.shortcuts.add(new ShortcutProfile.Shortcut("b-ui-1", "Toolbar", "T", ShortcutProfile.MOD_NONE, KEY_T));
        ui.shortcuts.add(new ShortcutProfile.Shortcut("b-ui-2", "Sidebar", "N", ShortcutProfile.MOD_NONE, KEY_N));
        p.categories.add(ui);

        ShortcutProfile.ShortcutCategory navigation = new ShortcutProfile.ShortcutCategory("navigation", "Navigation");
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("b-n-1", "Zoom In",      "+",       ShortcutProfile.MOD_SHIFT, KEY_EQUALS));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("b-n-2", "Zoom Out",     "-",       ShortcutProfile.MOD_NONE,  KEY_MINUS));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("b-n-3", "Reset Cursor", "Shift+C", ShortcutProfile.MOD_SHIFT, KEY_C));
        p.categories.add(navigation);

        ShortcutProfile.ShortcutCategory shading = new ShortcutProfile.ShortcutCategory("shading", "Shading");
        shading.shortcuts.add(new ShortcutProfile.Shortcut("b-sh-1", "Shading Pie",  "Z",           ShortcutProfile.MOD_NONE,       KEY_Z));
        shading.shortcuts.add(new ShortcutProfile.Shortcut("b-sh-2", "UV Mapping",   "U",           ShortcutProfile.MOD_NONE,       KEY_U));
        shading.shortcuts.add(new ShortcutProfile.Shortcut("b-sh-3", "Connect Nodes","F",           ShortcutProfile.MOD_NONE,       KEY_F));
        shading.shortcuts.add(new ShortcutProfile.Shortcut("b-sh-4", "Tex Setup",    "Ctrl+T",      ShortcutProfile.MOD_CTRL,       KEY_T));
        shading.shortcuts.add(new ShortcutProfile.Shortcut("b-sh-5", "Princ. Setup", "Ctrl+Sft+T",  ShortcutProfile.MOD_CTRL_SHIFT, KEY_T));
        p.categories.add(shading);

        ShortcutProfile.ShortcutCategory animation = new ShortcutProfile.ShortcutCategory("animation", "Animation");
        animation.shortcuts.add(new ShortcutProfile.Shortcut("b-a-1", "Add Keyframe", "I",          ShortcutProfile.MOD_NONE,     KEY_I));
        animation.shortcuts.add(new ShortcutProfile.Shortcut("b-a-2", "Set Camera",   "Ctrl+Alt+0", ShortcutProfile.MOD_CTRL_ALT, KEY_0));
        p.categories.add(animation);

        return p;
    }

    /**
     * KiCAD — shortcuts for KiCAD PCB design
     */
    private ShortcutProfile createKiCADProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "kicad";
        p.name = "KiCAD";
        p.description = "Shortcuts for KiCAD PCB design";
        p.icon = "ic_kicad";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory viewZoom = new ShortcutProfile.ShortcutCategory("view-zoom", "View & Zoom");
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-1", "Zoom In",       "F1",     ShortcutProfile.MOD_NONE, KEY_F1));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-2", "Zoom Out",      "F2",     ShortcutProfile.MOD_NONE, KEY_F2));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-3", "Redraw",        "F3",     ShortcutProfile.MOD_NONE, KEY_F3));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-4", "Center Zoom",   "F4",     ShortcutProfile.MOD_NONE, KEY_F4));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-5", "Fit Screen",    "Home",   ShortcutProfile.MOD_NONE, KEY_HOME));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-6", "Switch Units",  "Ctrl+U", ShortcutProfile.MOD_CTRL, KEY_U));
        viewZoom.shortcuts.add(new ShortcutProfile.Shortcut("k-vz-7", "Reset Coords",  "Space",  ShortcutProfile.MOD_NONE, KEY_SPACE));
        p.categories.add(viewZoom);

        ShortcutProfile.ShortcutCategory layers = new ShortcutProfile.ShortcutCategory("layers", "Layers");
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-1", "Copper Layer",    "PgDn",  ShortcutProfile.MOD_NONE,  KEY_PAGEDOWN));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-2", "Component Layer", "PgUp",  ShortcutProfile.MOD_NONE,  KEY_PAGEUP));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-3", "Inner Layer 1",   "F5",    ShortcutProfile.MOD_NONE,  KEY_F5));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-4", "Inner Layer 2",   "F6",    ShortcutProfile.MOD_NONE,  KEY_F6));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-5", "Next Layer",      "+",     ShortcutProfile.MOD_SHIFT, KEY_EQUALS));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-6", "Prev Layer",      "-",     ShortcutProfile.MOD_NONE,  KEY_MINUS));
        layers.shortcuts.add(new ShortcutProfile.Shortcut("k-l-7", "High Contrast",   "H",     ShortcutProfile.MOD_NONE,  KEY_H));
        p.categories.add(layers);

        ShortcutProfile.ShortcutCategory drawing = new ShortcutProfile.ShortcutCategory("drawing-edit", "Drawing & Edit");
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-1",  "Begin Wire",   "W",  ShortcutProfile.MOD_NONE, KEY_W));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-2",  "Begin Bus",    "B",  ShortcutProfile.MOD_NONE, KEY_B));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-3",  "Add Label",    "L",  ShortcutProfile.MOD_NONE, KEY_L));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-4",  "Add Power",    "P",  ShortcutProfile.MOD_NONE, KEY_P));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-5",  "Add Junction", "J",  ShortcutProfile.MOD_NONE, KEY_J));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-6",  "Add Sheet",    "S",  ShortcutProfile.MOD_NONE, KEY_S));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-7",  "No Connect",   "Q",  ShortcutProfile.MOD_NONE, KEY_Q));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-8",  "Hier. Label",  "H",  ShortcutProfile.MOD_NONE, KEY_H));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-9",  "Wire Entry",   "Z",  ShortcutProfile.MOD_NONE, KEY_Z));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-10", "Bus Entry",    "/",  ShortcutProfile.MOD_NONE, KEY_SLASH));
        drawing.shortcuts.add(new ShortcutProfile.Shortcut("k-de-11", "End Wire/Bus", "K",  ShortcutProfile.MOD_NONE, KEY_K));
        p.categories.add(drawing);

        ShortcutProfile.ShortcutCategory components = new ShortcutProfile.ShortcutCategory("components", "Components");
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-1", "Add Component", "A",  ShortcutProfile.MOD_NONE, KEY_A));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-2", "Move Item",     "M",  ShortcutProfile.MOD_NONE, KEY_M));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-3", "Copy Item",     "C",  ShortcutProfile.MOD_NONE, KEY_C));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-4", "Drag Item",     "G",  ShortcutProfile.MOD_NONE, KEY_G));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-5", "Rotate Item",   "R",  ShortcutProfile.MOD_NONE, KEY_R));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-6", "Mirror X",      "X",  ShortcutProfile.MOD_NONE, KEY_X));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-7", "Mirror Y",      "Y",  ShortcutProfile.MOD_NONE, KEY_Y));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-8", "Flip Item",     "F",  ShortcutProfile.MOD_NONE, KEY_F));
        components.shortcuts.add(new ShortcutProfile.Shortcut("k-c-9", "Orient Normal", "N",  ShortcutProfile.MOD_NONE, KEY_N));
        p.categories.add(components);

        ShortcutProfile.ShortcutCategory properties = new ShortcutProfile.ShortcutCategory("properties", "Properties");
        properties.shortcuts.add(new ShortcutProfile.Shortcut("k-p-1", "Edit Item",      "E",  ShortcutProfile.MOD_NONE, KEY_E));
        properties.shortcuts.add(new ShortcutProfile.Shortcut("k-p-2", "Edit Value",     "V",  ShortcutProfile.MOD_NONE, KEY_V));
        properties.shortcuts.add(new ShortcutProfile.Shortcut("k-p-3", "Edit Ref.",      "U",  ShortcutProfile.MOD_NONE, KEY_U));
        properties.shortcuts.add(new ShortcutProfile.Shortcut("k-p-4", "Edit Footprint", "F",  ShortcutProfile.MOD_NONE, KEY_F));
        properties.shortcuts.add(new ShortcutProfile.Shortcut("k-p-5", "Get Footprint",  "T",  ShortcutProfile.MOD_NONE, KEY_T));
        p.categories.add(properties);

        ShortcutProfile.ShortcutCategory pcb = new ShortcutProfile.ShortcutCategory("pcb-design", "PCB Design");
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-1", "Add Track",      "X",      ShortcutProfile.MOD_NONE, KEY_X));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-2", "Add Via",        "V",      ShortcutProfile.MOD_NONE, KEY_V));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-3", "Add Microvia",   "Ctrl+V", ShortcutProfile.MOD_CTRL, KEY_V));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-4", "Track Posture",  "/",      ShortcutProfile.MOD_NONE, KEY_SLASH));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-5", "Drag Track",     "D",      ShortcutProfile.MOD_NONE, KEY_D));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-6", "End Track",      "End",    ShortcutProfile.MOD_NONE, KEY_END));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-7", "Add Module",     "O",      ShortcutProfile.MOD_NONE, KEY_O));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-8", "Track Width +",  "W",      ShortcutProfile.MOD_NONE, KEY_W));
        pcb.shortcuts.add(new ShortcutProfile.Shortcut("k-pd-9", "Track Width -",  "Ctrl+W", ShortcutProfile.MOD_CTRL, KEY_W));
        p.categories.add(pcb);

        ShortcutProfile.ShortcutCategory fileMisc = new ShortcutProfile.ShortcutCategory("file-misc", "File & Misc");
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-1", "Save Board",  "Ctrl+S",  ShortcutProfile.MOD_CTRL, KEY_S));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-2", "Load Board",  "Ctrl+L",  ShortcutProfile.MOD_CTRL, KEY_L));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-3", "Find Item",   "Ctrl+F",  ShortcutProfile.MOD_CTRL, KEY_F));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-4", "Undo",        "Ctrl+Z",  ShortcutProfile.MOD_CTRL, KEY_Z));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-5", "Redo",        "Ctrl+Y",  ShortcutProfile.MOD_CTRL, KEY_Y));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-6", "Delete Item", "Del",     ShortcutProfile.MOD_NONE, KEY_DELETE));
        fileMisc.shortcuts.add(new ShortcutProfile.Shortcut("k-f-7", "Delete Seg.", "BkSp",    ShortcutProfile.MOD_NONE, KEY_BACKSPACE));
        p.categories.add(fileMisc);

        return p;
    }

    /**
     * Nomad Sculpt — shortcuts for Nomad Sculpt
     */
    private ShortcutProfile createNomadProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "nomad";
        p.name = "Nomad Sculpt";
        p.description = "Shortcuts for Nomad Sculpt";
        p.icon = "ic_nomad";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory sculpt = new ShortcutProfile.ShortcutCategory("tools", "Tools");
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-1",  "Clay",    "C", ShortcutProfile.MOD_NONE, KEY_C));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-2",  "Smooth",  "S", ShortcutProfile.MOD_NONE, KEY_S));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-3",  "Flatten", "F", ShortcutProfile.MOD_NONE, KEY_F));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-4",  "Inflate", "I", ShortcutProfile.MOD_NONE, KEY_I));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-5",  "Crease",  "R", ShortcutProfile.MOD_NONE, KEY_R));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-6",  "Pinch",   "P", ShortcutProfile.MOD_NONE, KEY_P));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-7",  "Nudge",   "N", ShortcutProfile.MOD_NONE, KEY_N));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-8",  "Stamp",   "T", ShortcutProfile.MOD_NONE, KEY_T));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-9",  "Tube",    "U", ShortcutProfile.MOD_NONE, KEY_U));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-10", "Drag",    "D", ShortcutProfile.MOD_NONE, KEY_D));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-11", "Trim",    "X", ShortcutProfile.MOD_NONE, KEY_X));
        sculpt.shortcuts.add(new ShortcutProfile.Shortcut("n-t-12", "Split",   "V", ShortcutProfile.MOD_NONE, KEY_V));
        p.categories.add(sculpt);

        ShortcutProfile.ShortcutCategory brush = new ShortcutProfile.ShortcutCategory("brush", "Brush");
        brush.shortcuts.add(new ShortcutProfile.Shortcut("n-b-1", "Radius +",     "]",        ShortcutProfile.MOD_NONE,  KEY_RBRACKET));
        brush.shortcuts.add(new ShortcutProfile.Shortcut("n-b-2", "Radius -",     "[",        ShortcutProfile.MOD_NONE,  KEY_LBRACKET));
        brush.shortcuts.add(new ShortcutProfile.Shortcut("n-b-3", "Intensity +",  "Shift++",  ShortcutProfile.MOD_SHIFT, KEY_EQUALS));
        brush.shortcuts.add(new ShortcutProfile.Shortcut("n-b-4", "Intensity -",  "Shift+-",  ShortcutProfile.MOD_SHIFT, KEY_MINUS));
        p.categories.add(brush);

        ShortcutProfile.ShortcutCategory transform = new ShortcutProfile.ShortcutCategory("transform", "Transform");
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-1", "Move",   "W", ShortcutProfile.MOD_NONE, KEY_W));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-2", "Rotate", "E", ShortcutProfile.MOD_NONE, KEY_E));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-3", "Scale",  "Z", ShortcutProfile.MOD_NONE, KEY_Z));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-4", "Sym X",  "1", ShortcutProfile.MOD_NONE, KEY_1));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-5", "Sym Y",  "2", ShortcutProfile.MOD_NONE, KEY_2));
        transform.shortcuts.add(new ShortcutProfile.Shortcut("n-tr-6", "Sym Z",  "3", ShortcutProfile.MOD_NONE, KEY_3));
        p.categories.add(transform);

        ShortcutProfile.ShortcutCategory view = new ShortcutProfile.ShortcutCategory("view", "View");
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-1", "Front",        "F1",      ShortcutProfile.MOD_NONE,  KEY_F1));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-2", "Back",         "F2",      ShortcutProfile.MOD_NONE,  KEY_F2));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-3", "Right",        "F3",      ShortcutProfile.MOD_NONE,  KEY_F3));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-4", "Left",         "F4",      ShortcutProfile.MOD_NONE,  KEY_F4));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-5", "Top",          "F5",      ShortcutProfile.MOD_NONE,  KEY_F5));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-6", "Bottom",       "F6",      ShortcutProfile.MOD_NONE,  KEY_F6));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-7", "Reset Camera", "Home",    ShortcutProfile.MOD_NONE,  KEY_HOME));
        view.shortcuts.add(new ShortcutProfile.Shortcut("n-v-8", "Frame Object", "Shift+F", ShortcutProfile.MOD_SHIFT, KEY_F));
        p.categories.add(view);

        ShortcutProfile.ShortcutCategory mesh = new ShortcutProfile.ShortcutCategory("mesh", "Mesh");
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("n-m-1", "Subdivide",    "Tab",    ShortcutProfile.MOD_NONE, KEY_TAB));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("n-m-2", "Voxel Remesh", "M",      ShortcutProfile.MOD_NONE, KEY_M));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("n-m-3", "Dyn. Topo",    "B",      ShortcutProfile.MOD_NONE, KEY_B));
        mesh.shortcuts.add(new ShortcutProfile.Shortcut("n-m-4", "Validate",     "Ctrl+V", ShortcutProfile.MOD_CTRL, KEY_V));
        p.categories.add(mesh);

        ShortcutProfile.ShortcutCategory masking = new ShortcutProfile.ShortcutCategory("masking", "Masking");
        masking.shortcuts.add(new ShortcutProfile.Shortcut("n-ma-1", "Mask Brush",   "K",       ShortcutProfile.MOD_NONE, KEY_K));
        masking.shortcuts.add(new ShortcutProfile.Shortcut("n-ma-2", "Invert Mask",  "Ctrl+I",  ShortcutProfile.MOD_CTRL, KEY_I));
        masking.shortcuts.add(new ShortcutProfile.Shortcut("n-ma-3", "Clear Mask",   "Alt+M",   ShortcutProfile.MOD_ALT,  KEY_M));
        masking.shortcuts.add(new ShortcutProfile.Shortcut("n-ma-4", "Fill Mask",    "Ctrl+M",  ShortcutProfile.MOD_CTRL, KEY_M));
        masking.shortcuts.add(new ShortcutProfile.Shortcut("n-ma-5", "Sharpen Mask", "Ctrl+S",  ShortcutProfile.MOD_CTRL, KEY_S));
        p.categories.add(masking);

        ShortcutProfile.ShortcutCategory edit = new ShortcutProfile.ShortcutCategory("edit", "Edit");
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-1", "Undo",       "Ctrl+Z", ShortcutProfile.MOD_CTRL, KEY_Z));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-2", "Redo",       "Ctrl+Y", ShortcutProfile.MOD_CTRL, KEY_Y));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-3", "Save",       "Ctrl+S", ShortcutProfile.MOD_CTRL, KEY_S));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-4", "Duplicate",  "Ctrl+D", ShortcutProfile.MOD_CTRL, KEY_D));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-5", "Delete",     "Del",    ShortcutProfile.MOD_NONE, KEY_DELETE));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("n-e-6", "Select All", "Ctrl+A", ShortcutProfile.MOD_CTRL, KEY_A));
        p.categories.add(edit);

        return p;
    }

    /**
     * Fusion 360 — shortcuts for Autodesk Fusion 360
     */
    private ShortcutProfile createFusion360Profile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "fusion360";
        p.name = "Fusion 360";
        p.description = "Shortcuts for Autodesk Fusion 360";
        p.icon = "ic_fusion360";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory navigation = new ShortcutProfile.ShortcutCategory("navigation", "Navigation");
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("f-nav-1", "Zoom to Fit", "F6",   ShortcutProfile.MOD_NONE, KEY_F6));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("f-nav-2", "Home View",   "Home", ShortcutProfile.MOD_NONE, KEY_HOME));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("f-nav-3", "Look At",     "L",    ShortcutProfile.MOD_NONE, KEY_L));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("f-nav-4", "Full Screen", "F11",  ShortcutProfile.MOD_NONE, KEY_F11));
        p.categories.add(navigation);

        ShortcutProfile.ShortcutCategory file = new ShortcutProfile.ShortcutCategory("file", "File");
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-1", "New Design", "Ctrl+N",       ShortcutProfile.MOD_CTRL,       KEY_N));
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-2", "Open",       "Ctrl+O",       ShortcutProfile.MOD_CTRL,       KEY_O));
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-3", "Save",       "Ctrl+S",       ShortcutProfile.MOD_CTRL,       KEY_S));
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-4", "Save As",    "Ctrl+Shift+S", ShortcutProfile.MOD_CTRL_SHIFT, KEY_S));
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-5", "Export",     "Ctrl+E",       ShortcutProfile.MOD_CTRL,       KEY_E));
        file.shortcuts.add(new ShortcutProfile.Shortcut("f-fi-6", "3D Print",   "Ctrl+P",       ShortcutProfile.MOD_CTRL,       KEY_P));
        p.categories.add(file);

        ShortcutProfile.ShortcutCategory general = new ShortcutProfile.ShortcutCategory("general", "General");
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-1", "Undo",       "Ctrl+Z", ShortcutProfile.MOD_CTRL, KEY_Z));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-2", "Redo",       "Ctrl+Y", ShortcutProfile.MOD_CTRL, KEY_Y));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-3", "Copy",       "Ctrl+C", ShortcutProfile.MOD_CTRL, KEY_C));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-4", "Paste",      "Ctrl+V", ShortcutProfile.MOD_CTRL, KEY_V));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-5", "Delete",     "Del",    ShortcutProfile.MOD_NONE, KEY_DELETE));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-6", "Select All", "Ctrl+A", ShortcutProfile.MOD_CTRL, KEY_A));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-7", "Find",       "Ctrl+F", ShortcutProfile.MOD_CTRL, KEY_F));
        general.shortcuts.add(new ShortcutProfile.Shortcut("f-ge-8", "Escape",     "Esc",    ShortcutProfile.MOD_NONE, KEY_ESC));
        p.categories.add(general);

        ShortcutProfile.ShortcutCategory sketch = new ShortcutProfile.ShortcutCategory("sketch", "Sketch");
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-1",  "Create Sketch",  "S",     ShortcutProfile.MOD_NONE, KEY_S));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-2",  "Line",           "L",     ShortcutProfile.MOD_NONE, KEY_L));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-3",  "Rectangle",      "R",     ShortcutProfile.MOD_NONE, KEY_R));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-4",  "Circle",         "C",     ShortcutProfile.MOD_NONE, KEY_C));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-5",  "Arc",            "A",     ShortcutProfile.MOD_NONE, KEY_A));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-6",  "Dimension",      "D",     ShortcutProfile.MOD_NONE, KEY_D));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-7",  "Trim",           "T",     ShortcutProfile.MOD_NONE, KEY_T));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-8",  "Offset",         "O",     ShortcutProfile.MOD_NONE, KEY_O));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-9",  "Mirror",         "M",     ShortcutProfile.MOD_NONE, KEY_M));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-10", "Fillet",         "F",     ShortcutProfile.MOD_NONE, KEY_F));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-11", "Construction",   "X",     ShortcutProfile.MOD_NONE, KEY_X));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-12", "Project",        "P",     ShortcutProfile.MOD_NONE, KEY_P));
        sketch.shortcuts.add(new ShortcutProfile.Shortcut("f-sk-13", "Finish Sketch",  "Enter", ShortcutProfile.MOD_NONE, KEY_ENTER));
        p.categories.add(sketch);

        ShortcutProfile.ShortcutCategory modeling = new ShortcutProfile.ShortcutCategory("modeling", "Modeling");
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-1", "Extrude",      "E",            ShortcutProfile.MOD_NONE,       KEY_E));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-2", "Press Pull",   "Q",            ShortcutProfile.MOD_NONE,       KEY_Q));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-3", "Fillet",       "F",            ShortcutProfile.MOD_NONE,       KEY_F));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-4", "Chamfer",      "Ctrl+Shift+F", ShortcutProfile.MOD_CTRL_SHIFT, KEY_F));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-5", "Hole",         "H",            ShortcutProfile.MOD_NONE,       KEY_H));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-6", "Combine",      "Ctrl+Shift+C", ShortcutProfile.MOD_CTRL_SHIFT, KEY_C));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-7", "Move/Copy",    "M",            ShortcutProfile.MOD_NONE,       KEY_M));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-8", "Appearance",   "A",            ShortcutProfile.MOD_NONE,       KEY_A));
        modeling.shortcuts.add(new ShortcutProfile.Shortcut("f-mo-9", "Phys. Mat.",   "P",            ShortcutProfile.MOD_NONE,       KEY_P));
        p.categories.add(modeling);

        ShortcutProfile.ShortcutCategory construction = new ShortcutProfile.ShortcutCategory("construction", "Construction");
        construction.shortcuts.add(new ShortcutProfile.Shortcut("f-co-1", "Offset Plane", "Ctrl+Shift+P", ShortcutProfile.MOD_CTRL_SHIFT, KEY_P));
        construction.shortcuts.add(new ShortcutProfile.Shortcut("f-co-2", "Midplane",     "Ctrl+Shift+M", ShortcutProfile.MOD_CTRL_SHIFT, KEY_M));
        construction.shortcuts.add(new ShortcutProfile.Shortcut("f-co-3", "Axis",         "Ctrl+Shift+A", ShortcutProfile.MOD_CTRL_SHIFT, KEY_A));
        construction.shortcuts.add(new ShortcutProfile.Shortcut("f-co-4", "Measure",      "I",            ShortcutProfile.MOD_NONE,       KEY_I));
        p.categories.add(construction);

        ShortcutProfile.ShortcutCategory assembly = new ShortcutProfile.ShortcutCategory("assembly", "Assembly");
        assembly.shortcuts.add(new ShortcutProfile.Shortcut("f-as-1", "As-Built Joint", "Ctrl+Shift+J", ShortcutProfile.MOD_CTRL_SHIFT, KEY_J));
        assembly.shortcuts.add(new ShortcutProfile.Shortcut("f-as-2", "Joint",          "J",            ShortcutProfile.MOD_NONE,       KEY_J));
        assembly.shortcuts.add(new ShortcutProfile.Shortcut("f-as-3", "Ground",         "G",            ShortcutProfile.MOD_NONE,       KEY_G));
        assembly.shortcuts.add(new ShortcutProfile.Shortcut("f-as-4", "Align",          "Ctrl+Shift+L", ShortcutProfile.MOD_CTRL_SHIFT, KEY_L));
        p.categories.add(assembly);

        ShortcutProfile.ShortcutCategory display = new ShortcutProfile.ShortcutCategory("display", "Display");
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-1", "Visibility",       "V",            ShortcutProfile.MOD_NONE,       KEY_V));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-2", "Show/Hide Bodies", "Ctrl+Shift+V", ShortcutProfile.MOD_CTRL_SHIFT, KEY_V));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-3", "Section Analysis", "Ctrl+Shift+9", ShortcutProfile.MOD_CTRL_SHIFT, KEY_9));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-4", "Wireframe",        "Ctrl+1",       ShortcutProfile.MOD_CTRL,       KEY_1));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-5", "Shaded",           "Ctrl+2",       ShortcutProfile.MOD_CTRL,       KEY_2));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-6", "Shaded+Wire",      "Ctrl+3",       ShortcutProfile.MOD_CTRL,       KEY_3));
        display.shortcuts.add(new ShortcutProfile.Shortcut("f-di-7", "Toggle Grid",      "Ctrl+Shift+G", ShortcutProfile.MOD_CTRL_SHIFT, KEY_G));
        p.categories.add(display);

        return p;
    }

    /**
     * Photoshop — shortcuts for Adobe Photoshop
     */
    private ShortcutProfile createPhotoshopProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "photoshop";
        p.name = "Photoshop";
        p.description = "Shortcuts for Adobe Photoshop";
        p.icon = "ic_photoshop";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory tools = new ShortcutProfile.ShortcutCategory("tools", "Tools");
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-1",  "Move",          "V",  ShortcutProfile.MOD_NONE, KEY_V));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-2",  "Brush",         "B",  ShortcutProfile.MOD_NONE, KEY_B));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-3",  "Eraser",        "E",  ShortcutProfile.MOD_NONE, KEY_E));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-4",  "Clone Stamp",   "S",  ShortcutProfile.MOD_NONE, KEY_S));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-5",  "Healing Brush", "J",  ShortcutProfile.MOD_NONE, KEY_J));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-6",  "Text",          "T",  ShortcutProfile.MOD_NONE, KEY_T));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-7",  "Pen",           "P",  ShortcutProfile.MOD_NONE, KEY_P));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-8",  "Rectangle",     "U",  ShortcutProfile.MOD_NONE, KEY_U));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-9",  "Hand",          "H",  ShortcutProfile.MOD_NONE, KEY_H));
        tools.shortcuts.add(new ShortcutProfile.Shortcut("ps-t-10", "Zoom",          "Z",  ShortcutProfile.MOD_NONE, KEY_Z));
        p.categories.add(tools);

        ShortcutProfile.ShortcutCategory edit = new ShortcutProfile.ShortcutCategory("edit", "Edit");
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-1",  "Undo",           "Ctrl+Z",       ShortcutProfile.MOD_CTRL,       KEY_Z));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-2",  "Redo",           "Ctrl+Y",       ShortcutProfile.MOD_CTRL,       KEY_Y));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-3",  "Copy",           "Ctrl+C",       ShortcutProfile.MOD_CTRL,       KEY_C));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-4",  "Paste",          "Ctrl+V",       ShortcutProfile.MOD_CTRL,       KEY_V));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-5",  "Save",           "Ctrl+S",       ShortcutProfile.MOD_CTRL,       KEY_S));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-6",  "Free Transform", "Ctrl+T",       ShortcutProfile.MOD_CTRL,       KEY_T));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-7",  "Deselect",       "Ctrl+D",       ShortcutProfile.MOD_CTRL,       KEY_D));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-8",  "Select All",     "Ctrl+A",       ShortcutProfile.MOD_CTRL,       KEY_A));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-9",  "Levels",         "Ctrl+L",       ShortcutProfile.MOD_CTRL,       KEY_L));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-10", "Curves",         "Ctrl+M",       ShortcutProfile.MOD_CTRL,       KEY_M));
        edit.shortcuts.add(new ShortcutProfile.Shortcut("ps-e-11", "New Layer",      "Ctrl+Shift+N", ShortcutProfile.MOD_CTRL_SHIFT, KEY_N));
        p.categories.add(edit);

        ShortcutProfile.ShortcutCategory view = new ShortcutProfile.ShortcutCategory("view", "View");
        view.shortcuts.add(new ShortcutProfile.Shortcut("ps-v-1", "Zoom In",    "Ctrl++",     ShortcutProfile.MOD_CTRL_SHIFT, KEY_EQUALS));
        view.shortcuts.add(new ShortcutProfile.Shortcut("ps-v-2", "Zoom Out",   "Ctrl+-",     ShortcutProfile.MOD_CTRL,       KEY_MINUS));
        view.shortcuts.add(new ShortcutProfile.Shortcut("ps-v-3", "Fit Screen", "Ctrl+0",     ShortcutProfile.MOD_CTRL,       KEY_0));
        view.shortcuts.add(new ShortcutProfile.Shortcut("ps-v-4", "100%",       "Ctrl+Alt+0", ShortcutProfile.MOD_CTRL_ALT,   KEY_0));
        view.shortcuts.add(new ShortcutProfile.Shortcut("ps-v-5", "Rulers",     "Ctrl+R",     ShortcutProfile.MOD_CTRL,       KEY_R));
        p.categories.add(view);

        return p;
    }

    /**
     * VS Code — shortcuts for Visual Studio Code
     */
    private ShortcutProfile createVSCodeProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "vscode";
        p.name = "VS Code";
        p.description = "Shortcuts for Visual Studio Code";
        p.icon = "ic_vscode";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        ShortcutProfile.ShortcutCategory navigation = new ShortcutProfile.ShortcutCategory("navigation", "Navigation");
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-1", "Quick Open",   "Ctrl+P",       ShortcutProfile.MOD_CTRL,       KEY_P));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-2", "Cmd Palette",  "Ctrl+Shift+P", ShortcutProfile.MOD_CTRL_SHIFT, KEY_P));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-3", "Go to Line",   "Ctrl+G",       ShortcutProfile.MOD_CTRL,       KEY_G));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-4", "Explorer",     "Ctrl+Shift+E", ShortcutProfile.MOD_CTRL_SHIFT, KEY_E));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-5", "Search",       "Ctrl+Shift+F", ShortcutProfile.MOD_CTRL_SHIFT, KEY_F));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-6", "Extensions",   "Ctrl+Shift+X", ShortcutProfile.MOD_CTRL_SHIFT, KEY_X));
        navigation.shortcuts.add(new ShortcutProfile.Shortcut("v-nav-7", "Terminal",     "Ctrl+`",       ShortcutProfile.MOD_CTRL,       KEY_GRAVE));
        p.categories.add(navigation);

        ShortcutProfile.ShortcutCategory editing = new ShortcutProfile.ShortcutCategory("edit", "Edit");
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-1",  "Find",          "Ctrl+F",       ShortcutProfile.MOD_CTRL,      KEY_F));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-2",  "Replace",       "Ctrl+H",       ShortcutProfile.MOD_CTRL,      KEY_H));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-3",  "Comment",       "Ctrl+/",       ShortcutProfile.MOD_CTRL,      KEY_SLASH));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-4",  "Format",        "Shift+Alt+F",  ShortcutProfile.MOD_SHIFT_ALT, KEY_F));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-5",  "Undo",          "Ctrl+Z",       ShortcutProfile.MOD_CTRL,      KEY_Z));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-6",  "Redo",          "Ctrl+Shift+Z", ShortcutProfile.MOD_CTRL_SHIFT,KEY_Z));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-7",  "Dup. Line",     "Shift+Alt+↓",  ShortcutProfile.MOD_SHIFT_ALT, KEY_DOWN));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-8",  "Move Line Up",  "Alt+↑",        ShortcutProfile.MOD_ALT,       KEY_UP));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-9",  "Move Line Dn",  "Alt+↓",        ShortcutProfile.MOD_ALT,       KEY_DOWN));
        editing.shortcuts.add(new ShortcutProfile.Shortcut("v-e-10", "Delete Line",   "Ctrl+Shift+K", ShortcutProfile.MOD_CTRL_SHIFT,KEY_K));
        p.categories.add(editing);

        ShortcutProfile.ShortcutCategory code = new ShortcutProfile.ShortcutCategory("code", "Code");
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-1", "Save",           "Ctrl+S",    ShortcutProfile.MOD_CTRL,  KEY_S));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-2", "Save All",       "Ctrl+K S",  ShortcutProfile.MOD_CTRL,  KEY_S));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-3", "Rename",         "F2",        ShortcutProfile.MOD_NONE,  KEY_F2));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-4", "Go to Def.",     "F12",       ShortcutProfile.MOD_NONE,  KEY_F12));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-5", "Peek Def.",      "Alt+F12",   ShortcutProfile.MOD_ALT,   KEY_F12));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-6", "Find All Refs.", "Shift+F12", ShortcutProfile.MOD_SHIFT, KEY_F12));
        code.shortcuts.add(new ShortcutProfile.Shortcut("v-c-7", "Quick Fix",      "Ctrl+.",    ShortcutProfile.MOD_CTRL,  KEY_PERIOD));
        p.categories.add(code);

        ShortcutProfile.ShortcutCategory viewCat = new ShortcutProfile.ShortcutCategory("view", "View");
        viewCat.shortcuts.add(new ShortcutProfile.Shortcut("v-v-1", "Sidebar",      "Ctrl+B",       ShortcutProfile.MOD_CTRL,       KEY_B));
        viewCat.shortcuts.add(new ShortcutProfile.Shortcut("v-v-2", "Full Screen",  "F11",          ShortcutProfile.MOD_NONE,       KEY_F11));
        viewCat.shortcuts.add(new ShortcutProfile.Shortcut("v-v-3", "Split Editor", "Ctrl+\\",      ShortcutProfile.MOD_CTRL,       KEY_BACKSLASH));
        viewCat.shortcuts.add(new ShortcutProfile.Shortcut("v-v-4", "Zoom In",      "Ctrl++",       ShortcutProfile.MOD_CTRL_SHIFT, KEY_EQUALS));
        viewCat.shortcuts.add(new ShortcutProfile.Shortcut("v-v-5", "Zoom Out",     "Ctrl+-",       ShortcutProfile.MOD_CTRL,       KEY_MINUS));
        p.categories.add(viewCat);

        return p;
    }

    /**
     * Get all profiles
     */
    public List<ShortcutProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }

    /**
     * Get profile by ID
     */
    @Nullable
    public ShortcutProfile getProfileById(String id) {
        if (id == null) return null;
        for (ShortcutProfile profile : profiles) {
            if (id.equals(profile.id)) {
                return profile;
            }
        }
        return null;
    }

    /**
     * Get active profile
     */
    public ShortcutProfile getActiveProfile() {
        activeProfileId = prefs.getString(KEY_ACTIVE_PROFILE, activeProfileId != null ? activeProfileId : "default");
        return getProfileById(activeProfileId);
    }

    /**
     * Set active profile
     */
    public void setActiveProfile(String profileId) {
        if (getProfileById(profileId) != null) {
            activeProfileId = profileId;
            prefs.edit().putString(KEY_ACTIVE_PROFILE, profileId).apply();
            Log.d(TAG, "Active profile set to: " + profileId);

            if (listener != null) {
                listener.onProfileChanged(profileId);
            }
        }
    }

    /**
     * Create new profile
     */
    public ShortcutProfile createProfile(String name, String description) {
        ShortcutProfile profile = new ShortcutProfile();
        profile.id = "custom_" + System.currentTimeMillis();
        profile.name = name;
        profile.description = description;
        profile.icon = "ic_custom";
        profile.builtIn = false;
        profile.shortcuts = new ArrayList<>();
        profile.createdAt = System.currentTimeMillis();
        profile.updatedAt = System.currentTimeMillis();

        profiles.add(profile);
        saveProfiles();

        if (listener != null) {
            listener.onProfileCreated(profile);
        }

        return profile;
    }

    /**
     * Update profile
     */
    public void updateProfile(ShortcutProfile profile) {
        if (profile == null) return;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(profile.id)) {
                profile.updatedAt = System.currentTimeMillis();
                profiles.set(i, profile);
                saveProfiles();

                if (listener != null) {
                    listener.onProfileUpdated(profile);
                }
                return;
            }
        }
    }

    /**
     * Delete profile (cannot delete default profile)
     */
    public void deleteProfile(String profileId) {
        if ("default".equals(profileId)) {
            Log.w(TAG, "Cannot delete default profile");
            return;
        }

        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id.equals(profileId)) {
                profiles.remove(i);
                saveProfiles();

                // Switch to default if active profile was deleted
                if (activeProfileId.equals(profileId)) {
                    setActiveProfile("default");
                }

                if (listener != null) {
                    listener.onProfileDeleted(profileId);
                }
                return;
            }
        }
    }

    /**
     * Duplicate profile
     */
    @Nullable
    public ShortcutProfile duplicateProfile(String profileId) {
        ShortcutProfile original = getProfileById(profileId);
        if (original == null) return null;

        ShortcutProfile duplicate = gson.fromJson(gson.toJson(original), ShortcutProfile.class);
        duplicate.id = "copy_" + System.currentTimeMillis();
        duplicate.name = original.name + " (Copy)";
        duplicate.builtIn = false;
        duplicate.createdAt = System.currentTimeMillis();
        duplicate.updatedAt = System.currentTimeMillis();

        profiles.add(duplicate);
        saveProfiles();

        if (listener != null) {
            listener.onProfileCreated(duplicate);
        }

        return duplicate;
    }

    /**
     * Export profile to JSON string
     */
    @Nullable
    public String exportProfile(String profileId) {
        ShortcutProfile profile = getProfileById(profileId);
        if (profile != null) {
            return gson.toJson(profile);
        }
        return null;
    }

    /**
     * Export all profiles to JSON string
     */
    public String exportAllProfiles() {
        return gson.toJson(profiles);
    }

    /**
     * Import profile from JSON string
     */
    @Nullable
    public ShortcutProfile importProfile(String json) {
        try {
            Type type = new TypeToken<ShortcutProfile>(){}.getType();
            ShortcutProfile profile = gson.fromJson(json, type);

            // Generate new ID to avoid conflicts
            profile.id = "imported_" + System.currentTimeMillis();
            profile.builtIn = false;
            profile.createdAt = System.currentTimeMillis();
            profile.updatedAt = System.currentTimeMillis();

            profiles.add(profile);
            saveProfiles();

            if (listener != null) {
                listener.onProfileImported(profile);
            }

            return profile;
        } catch (Exception e) {
            Log.e(TAG, "Failed to import profile: " + e.getMessage());
            return null;
        }
    }

    /**
     * Import multiple profiles from JSON string
     */
    @Nullable
    public List<ShortcutProfile> importProfiles(String json) {
        try {
            Type type = new TypeToken<List<ShortcutProfile>>(){}.getType();
            List<ShortcutProfile> imported = gson.fromJson(json, type);
            if (imported == null) return null;

            List<ShortcutProfile> added = new ArrayList<>();
            for (ShortcutProfile profile : imported) {
                profile.id = "imported_" + System.currentTimeMillis() + "_" + added.size();
                profile.builtIn = false;
                profile.createdAt = System.currentTimeMillis();
                profile.updatedAt = System.currentTimeMillis();
                profiles.add(profile);
                added.add(profile);
            }

            saveProfiles();

            if (listener != null) {
                for (ShortcutProfile profile : added) {
                    listener.onProfileImported(profile);
                }
            }

            return added;
        } catch (Exception e) {
            Log.e(TAG, "Failed to import profiles: " + e.getMessage());
            return null;
        }
    }

    /**
     * Set listener for profile changes
     */
    public void setListener(ProfileChangeListener listener) {
        this.listener = listener;
    }

    // === My Shortcuts (Favorites) ===

    /**
     * Get the "My Shortcuts" (favorites) list for a profile.
     */
    public List<Shortcut> getMyShortcuts(String profileId) {
        String key = MY_SHORTCUTS_KEY_PREFIX + profileId;
        String json = prefs.getString(key, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Shortcut>>(){}.getType();
            List<Shortcut> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Save the "My Shortcuts" list for a profile.
     */
    public void updateMyShortcuts(String profileId, List<Shortcut> shortcuts) {
        String key = MY_SHORTCUTS_KEY_PREFIX + profileId;
        String json = gson.toJson(shortcuts != null ? shortcuts : new ArrayList<>());
        prefs.edit().putString(key, json).apply();
    }

    /**
     * Append a shortcut to the favorites list if not already present (by id).
     * Returns true if added.
     */
    public boolean appendCloneIfAbsent(List<Shortcut> list, Shortcut shortcut) {
        if (shortcut == null || shortcut.id == null) return false;
        for (Shortcut s : list) {
            if (s != null && shortcut.id.equals(s.id)) return false;
        }
        // Create a copy
        Shortcut clone = new Shortcut(shortcut.id, shortcut.name, shortcut.label,
                shortcut.modifiers, shortcut.keyCode, shortcut.icon, shortcut.displayOrder);
        list.add(clone);
        return true;
    }

    /**
     * Profile change listener interface
     */
    public interface ProfileChangeListener {
        void onProfileChanged(String profileId);
        void onProfileCreated(ShortcutProfile profile);
        void onProfileUpdated(ShortcutProfile profile);
        void onProfileDeleted(String profileId);
        void onProfileImported(ShortcutProfile profile);
    }
}
