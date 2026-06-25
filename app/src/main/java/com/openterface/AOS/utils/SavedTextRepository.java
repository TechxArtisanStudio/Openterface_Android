package com.openterface.AOS.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openterface.AOS.model.SavedTextItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Persists saved compose snippets in a dedicated prefs file.
 * Mirrors KeyCMD's SavedTextRepository.
 */
public final class SavedTextRepository {

    private static final String PREFS_NAME = "SavedTextPrefs";
    private static final String KEY_ITEMS_JSON = "saved_text_items_v1";

    /** Keeps SharedPreferences size reasonable. */
    public static final int MAX_ITEMS = 60;
    /** Per-snippet content cap (UTF-16 length). */
    public static final int MAX_CONTENT_CHARS = 24_000;
    public static final int MAX_TITLE_CHARS = 80;

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<SavedTextItem>>() {}.getType();

    private final SharedPreferences prefs;

    public SavedTextRepository(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public synchronized List<SavedTextItem> loadSorted() {
        List<SavedTextItem> raw = readAll();
        sortInPlace(raw);
        return raw;
    }

    @Nullable
    public synchronized SavedTextItem findById(long id) {
        for (SavedTextItem it : readAll()) {
            if (it.id == id) {
                return it;
            }
        }
        return null;
    }

    /**
     * Saves current editor text as a new item (trimmed). Returns null if empty after trim.
     * Truncates content to MAX_CONTENT_CHARS if needed.
     */
    @Nullable
    public synchronized SavedTextItem addFromPlainText(@Nullable String plain) {
        if (plain == null) {
            return null;
        }
        String content = plain;
        if (content.length() > MAX_CONTENT_CHARS) {
            content = content.substring(0, MAX_CONTENT_CHARS);
        }
        if (content.trim().isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        SavedTextItem item = new SavedTextItem();
        item.id = now;
        item.title = deriveTitle(content);
        item.content = content;
        item.pinned = false;
        item.pinnedAt = 0L;
        item.createdAt = now;
        item.updatedAt = now;

        List<SavedTextItem> list = readAll();
        list.add(0, item);
        evictDownToMax(list);
        writeAll(list);
        return item;
    }

    public synchronized void updateTitle(long id, @NonNull String newTitle) {
        String t = newTitle.trim();
        if (t.isEmpty()) {
            t = "…";
        }
        if (t.length() > MAX_TITLE_CHARS) {
            t = t.substring(0, MAX_TITLE_CHARS);
        }
        List<SavedTextItem> list = readAll();
        for (SavedTextItem it : list) {
            if (it.id == id) {
                it.title = t;
                it.updatedAt = System.currentTimeMillis();
                writeAll(list);
                return;
            }
        }
    }

    public synchronized void delete(long id) {
        List<SavedTextItem> list = readAll();
        Iterator<SavedTextItem> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().id == id) {
                it.remove();
                break;
            }
        }
        writeAll(list);
    }

    public synchronized void setPinned(long id, boolean pinned) {
        long now = System.currentTimeMillis();
        List<SavedTextItem> list = readAll();
        for (SavedTextItem it : list) {
            if (it.id == id) {
                it.pinned = pinned;
                it.pinnedAt = pinned ? now : 0L;
                it.updatedAt = now;
                break;
            }
        }
        writeAll(list);
    }

    @NonNull
    public static String deriveTitle(@NonNull String content) {
        String[] lines = content.split("\\R", -1);
        for (String line : lines) {
            String s = line.trim();
            if (!s.isEmpty()) {
                if (s.length() > MAX_TITLE_CHARS) {
                    return s.substring(0, MAX_TITLE_CHARS);
                }
                return s;
            }
        }
        return "…";
    }

    private static void sortInPlace(@NonNull List<SavedTextItem> list) {
        Collections.sort(list, new Comparator<SavedTextItem>() {
            @Override
            public int compare(SavedTextItem a, SavedTextItem b) {
                if (a.pinned != b.pinned) {
                    return a.pinned ? -1 : 1;
                }
                if (a.pinned && b.pinned) {
                    int c = Long.compare(b.pinnedAt, a.pinnedAt);
                    if (c != 0) return c;
                }
                return Long.compare(b.updatedAt, a.updatedAt);
            }
        });
    }

    private void evictDownToMax(@NonNull List<SavedTextItem> list) {
        while (list.size() > MAX_ITEMS) {
            int victim = -1;
            long oldest = Long.MAX_VALUE;
            for (int i = 0; i < list.size(); i++) {
                SavedTextItem it = list.get(i);
                if (it.pinned) continue;
                if (it.updatedAt < oldest) {
                    oldest = it.updatedAt;
                    victim = i;
                }
            }
            if (victim >= 0) {
                list.remove(victim);
            } else {
                // All pinned: drop oldest by updatedAt regardless (rare).
                int drop = 0;
                for (int i = 1; i < list.size(); i++) {
                    if (list.get(i).updatedAt < list.get(drop).updatedAt) {
                        drop = i;
                    }
                }
                list.remove(drop);
            }
        }
    }

    @NonNull
    private List<SavedTextItem> readAll() {
        String json = prefs.getString(KEY_ITEMS_JSON, null);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        List<SavedTextItem> parsed = GSON.fromJson(json, LIST_TYPE);
        return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
    }

    private void writeAll(@NonNull List<SavedTextItem> list) {
        prefs.edit().putString(KEY_ITEMS_JSON, GSON.toJson(list)).apply();
    }
}