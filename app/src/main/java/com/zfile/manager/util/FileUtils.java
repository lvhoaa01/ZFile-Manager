package com.zfile.manager.util;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Stateless formatters for human-readable file size, modification date, and permissions.
 */
public final class FileUtils {

    private FileUtils() { }

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;
    private static final long TB = GB * 1024L;

    @NonNull
    public static String formatSize(long bytes) {
        if (bytes < 0) return "—";
        if (bytes < KB) return bytes + " B";
        if (bytes < MB) return String.format(Locale.ROOT, "%.1f KB", bytes / (double) KB);
        if (bytes < GB) return String.format(Locale.ROOT, "%.1f MB", bytes / (double) MB);
        if (bytes < TB) return String.format(Locale.ROOT, "%.2f GB", bytes / (double) GB);
        return String.format(Locale.ROOT, "%.2f TB", bytes / (double) TB);
    }

    @NonNull
    public static String formatDate(long millis) {
        if (millis <= 0L) return "—";
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        return df.format(new Date(millis));
    }

    @NonNull
    public static String formatPermissions(boolean readable, boolean writable) {
        StringBuilder sb = new StringBuilder(3);
        sb.append(readable ? 'r' : '-');
        sb.append(writable ? 'w' : '-');
        sb.append('-');
        return sb.toString();
    }
}
