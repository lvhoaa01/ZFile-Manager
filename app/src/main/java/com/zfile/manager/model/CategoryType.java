package com.zfile.manager.model;

/**
 * Top-level media categories surfaced on the Categories tab. Each value maps to a
 * MediaStore query handled by {@code MediaStoreRepository}; the UI labels and
 * icons are resolved by name in the Fragment / Adapter (string and drawable
 * lookup via {@code getIdentifier} keyed by {@link #resourceSuffix}).
 */
public enum CategoryType {
    IMAGES("images"),
    VIDEOS("videos"),
    AUDIO("audio"),
    DOCUMENTS("documents"),
    DOWNLOADS("downloads"),
    APKS("apks");

    private final String resourceSuffix;

    CategoryType(String resourceSuffix) {
        this.resourceSuffix = resourceSuffix;
    }

    public String getResourceSuffix() {
        return resourceSuffix;
    }
}
