package com.zfile.manager.service;

import androidx.annotation.NonNull;

import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.model.SortCriteria;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Plain-Java service (NOT {@link android.app.Service}) that lists files inside a directory.
 * Must run off the main thread; callers schedule via
 * {@link com.zfile.manager.core.ThreadPoolManager}.
 */
public final class DirectoryScannerService {

    @NonNull
    public List<FileItem> scanDirectory(@NonNull String path,
                                        boolean showHidden,
                                        @NonNull SortCriteria sort,
                                        boolean foldersFirst) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            return Collections.emptyList();
        }

        File[] children = dir.listFiles();
        if (children == null) {
            return Collections.emptyList();
        }

        List<FileItem> items = new ArrayList<>(children.length);
        for (File child : children) {
            // Android (Linux) hides dot-prefixed files; check both the JVM hidden flag
            // (Windows ATTRIB.HIDDEN) and the dot convention so behaviour stays the same
            // when tests run on a Windows host.
            if (!showHidden && (child.isHidden() || child.getName().startsWith("."))) continue;
            FileType type = MimeTypeHelper.getFileType(child);
            String mime = MimeTypeHelper.getMimeType(child);
            items.add(FileItem.fromFile(child, type, mime));
        }

        sortInPlace(items, sort, foldersFirst);
        return items;
    }

    public boolean exists(@NonNull String path) {
        return new File(path).exists();
    }

    public boolean isDirectory(@NonNull String path) {
        return new File(path).isDirectory();
    }

    private static void sortInPlace(@NonNull List<FileItem> items,
                                    @NonNull SortCriteria sort,
                                    boolean foldersFirst) {
        Comparator<FileItem> comparator = comparatorFor(sort);
        if (foldersFirst) {
            comparator = ((Comparator<FileItem>) (a, b) -> {
                if (a.isDirectory() == b.isDirectory()) return 0;
                return a.isDirectory() ? -1 : 1;
            }).thenComparing(comparator);
        }
        Collections.sort(items, comparator);
    }

    @NonNull
    private static Comparator<FileItem> comparatorFor(@NonNull SortCriteria sort) {
        switch (sort) {
            case NAME_DESC:
                return (a, b) -> b.getName().compareToIgnoreCase(a.getName());
            case SIZE_ASC:
                return (a, b) -> Long.compare(a.getSize(), b.getSize());
            case SIZE_DESC:
                return (a, b) -> Long.compare(b.getSize(), a.getSize());
            case DATE_ASC:
                return (a, b) -> Long.compare(a.getLastModified(), b.getLastModified());
            case DATE_DESC:
                return (a, b) -> Long.compare(b.getLastModified(), a.getLastModified());
            case TYPE:
                return Comparator.comparing(FileItem::getFileType)
                        .thenComparing((FileItem a, FileItem b) ->
                                a.getName().compareToIgnoreCase(b.getName()));
            case NAME_ASC:
            default:
                return (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        }
    }
}
