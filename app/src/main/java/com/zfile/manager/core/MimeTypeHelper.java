package com.zfile.manager.core;

import android.content.Context;
import android.webkit.MimeTypeMap;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.FileType;

import java.io.File;
import java.util.Locale;

/**
 * Stateless utility. Detects MIME types via {@link MimeTypeMap}, classifies
 * extensions into {@link FileType}, and resolves icon drawable resources by name.
 *
 * <p>Drawables are resolved via {@code getIdentifier} so this class compiles
 * independently of the resource generation order — the UI layer creates the
 * matching {@code ic_file_*} vectors in a later stage.</p>
 */
public final class MimeTypeHelper {

    private MimeTypeHelper() {
        // no instances
    }

    @Nullable
    public static String getExtension(@NonNull String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1);
    }

    @Nullable
    public static String getMimeType(@NonNull File file) {
        if (file.isDirectory()) return null;
        return getMimeTypeFromName(file.getName());
    }

    @Nullable
    public static String getMimeTypeFromName(@NonNull String filename) {
        String ext = getExtension(filename);
        if (ext == null) return null;
        try {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            // JVM unit tests stub Android framework classes and throw — degrade gracefully.
            return null;
        }
    }

    @NonNull
    public static FileType getFileType(@NonNull File file) {
        if (file.isDirectory()) return FileType.FOLDER;
        return getFileTypeFromName(file.getName());
    }

    @NonNull
    public static FileType getFileTypeFromName(@NonNull String filename) {
        String ext = getExtension(filename);
        if (ext == null) return FileType.UNKNOWN;
        switch (ext.toLowerCase(Locale.ROOT)) {
            case "jpg": case "jpeg": case "png": case "gif": case "bmp":
            case "webp": case "svg": case "heic": case "heif": case "ico":
                return FileType.IMAGE;

            case "mp4": case "mkv": case "avi": case "mov": case "wmv":
            case "flv": case "webm": case "3gp": case "m4v": case "mpg": case "mpeg":
                return FileType.VIDEO;

            case "mp3": case "wav": case "ogg": case "flac": case "aac":
            case "m4a": case "opus": case "wma": case "amr":
                return FileType.AUDIO;

            case "pdf": case "doc": case "docx": case "xls": case "xlsx":
            case "ppt": case "pptx": case "odt": case "ods": case "odp": case "rtf":
                return FileType.DOCUMENT;

            case "zip": case "rar": case "7z": case "tar": case "gz":
            case "bz2": case "xz": case "tgz":
                return FileType.ARCHIVE;

            case "apk": case "xapk": case "apks":
                return FileType.APK;

            case "txt": case "md": case "json": case "xml": case "html":
            case "htm": case "css": case "js": case "java": case "kt":
            case "log": case "csv": case "yaml": case "yml": case "ini":
            case "conf": case "properties":
                return FileType.TEXT;

            default:
                return FileType.UNKNOWN;
        }
    }

    /**
     * Resource name (not ID) for the icon associated with a {@link FileType}.
     * UI layer creates matching drawables under {@code res/drawable/}.
     */
    @NonNull
    public static String getIconResourceName(@NonNull FileType type) {
        switch (type) {
            case FOLDER:   return "ic_folder";
            case IMAGE:    return "ic_file_image";
            case VIDEO:    return "ic_file_video";
            case AUDIO:    return "ic_file_audio";
            case DOCUMENT: return "ic_file_document";
            case ARCHIVE:  return "ic_file_archive";
            case APK:      return "ic_file_apk";
            case TEXT:     return "ic_file_text";
            case UNKNOWN:
            default:       return "ic_file_unknown";
        }
    }

    /**
     * Resolves the drawable resource ID for a file type at runtime.
     * Returns {@code 0} if no matching drawable exists yet — callers should
     * fall back to {@code ic_file_unknown} or render a placeholder.
     */
    @DrawableRes
    public static int getIconResource(@NonNull Context context, @NonNull FileType type) {
        String name = getIconResourceName(type);
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }
}
