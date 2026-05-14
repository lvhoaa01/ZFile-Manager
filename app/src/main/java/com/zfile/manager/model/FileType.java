package com.zfile.manager.model;

/**
 * High-level classification used across the app for icon selection,
 * category browsing, and storage breakdown. Maps from MIME / extension
 * via {@link com.zfile.manager.core.MimeTypeHelper}.
 */
public enum FileType {
    FOLDER,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    APK,
    TEXT,
    UNKNOWN
}
