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
    private static final String TAG = "ShortcutProfileManager";
    private static final String PREFS_NAME = "ShortcutProfiles";
    private static final String KEY_PROFILES = "profiles_list";
    private static final String KEY_ACTIVE_PROFILE = "active_profile_id";

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

        // Create default profile if none exist
        if (profiles.isEmpty()) {
            createDefaultProfile();
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
     * Create default profile with common shortcuts
     */
    private void createDefaultProfile() {
        ShortcutProfile p = new ShortcutProfile();
        p.id = "default";
        p.name = "Default";
        p.description = "Common shortcuts for any app";
        p.icon = "ic_default";
        p.builtIn = true;
        p.createdAt = System.currentTimeMillis();
        p.updatedAt = System.currentTimeMillis();

        p.shortcuts.add(new Shortcut("default_select_all", "Select All", "Ctrl+A", ShortcutProfile.MOD_CTRL, KEY_A, "select_all_24", 1));
        p.shortcuts.add(new Shortcut("default_copy", "Copy", "Ctrl+C", ShortcutProfile.MOD_CTRL, KEY_C, "content_copy_24", 2));
        p.shortcuts.add(new Shortcut("default_cut", "Cut", "Ctrl+X", ShortcutProfile.MOD_CTRL, KEY_X, "content_cut_24", 3));
        p.shortcuts.add(new Shortcut("default_paste", "Paste", "Ctrl+V", ShortcutProfile.MOD_CTRL, KEY_V, "content_paste_24", 4));
        p.shortcuts.add(new Shortcut("default_save", "Save", "Ctrl+S", ShortcutProfile.MOD_CTRL, KEY_S, "save_24", 5));
        p.shortcuts.add(new Shortcut("default_undo", "Undo", "Ctrl+Z", ShortcutProfile.MOD_CTRL, KEY_Z, "undo_24", 6));
        p.shortcuts.add(new Shortcut("default_redo", "Redo", "Ctrl+Y", ShortcutProfile.MOD_CTRL, KEY_Y, "redo_24", 7));
        p.shortcuts.add(new Shortcut("default_find", "Find", "Ctrl+F", ShortcutProfile.MOD_CTRL, KEY_F, "search_24", 8));
        p.shortcuts.add(new Shortcut("default_new", "New", "Ctrl+N", ShortcutProfile.MOD_CTRL, KEY_N, "new_24", 9));
        p.shortcuts.add(new Shortcut("default_open", "Open", "Ctrl+O", ShortcutProfile.MOD_CTRL, KEY_O, "open_24", 10));
        p.shortcuts.add(new Shortcut("default_print", "Print", "Ctrl+P", ShortcutProfile.MOD_CTRL, KEY_P, "print_24", 11));
        p.shortcuts.add(new Shortcut("default_close_tab", "Close Tab", "Ctrl+W", ShortcutProfile.MOD_CTRL, KEY_W, "close_24", 12));
        p.shortcuts.add(new Shortcut("default_replace", "Replace", "Ctrl+H", ShortcutProfile.MOD_CTRL, KEY_H, "replace_24", 13));

        profiles.add(p);
        saveProfiles();
        Log.d(TAG, "Created default profile");
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
