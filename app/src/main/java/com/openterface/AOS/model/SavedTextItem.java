package com.openterface.AOS.model;

/**
 * User-saved snippet for IME compose module.
 *
 * <p>Fields are Gson-friendly (public, mutable).
 * Mirrors KeyCMD's SavedTextItem for compatibility.
 */
public class SavedTextItem {

    public long id;           // Timestamp ID
    /** User-visible title; may differ from first line of content. */
    public String title;
    public String content;
    public boolean pinned;    // Whether this item is pinned/favorited
    /** Millis when last pinned; meaningful when pinned is true. */
    public long pinnedAt;
    public long createdAt;
    public long updatedAt;

    public SavedTextItem() {}

    public SavedTextItem(
            long id,
            String title,
            String content,
            boolean pinned,
            long pinnedAt,
            long createdAt,
            long updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.pinnedAt = pinnedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}