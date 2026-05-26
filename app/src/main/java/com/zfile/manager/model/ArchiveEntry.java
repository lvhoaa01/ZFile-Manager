package com.zfile.manager.model;

import androidx.annotation.NonNull;

/**
 * One row inside a {@code .zip} archive — used by the extract preview dialog
 * so users see what's in the archive before committing to disk.
 */
public final class ArchiveEntry {

    @NonNull private final String name;
    private final long size;
    private final long compressedSize;
    private final boolean directory;
    private final long modifiedTime;

    public ArchiveEntry(@NonNull String name,
                        long size,
                        long compressedSize,
                        boolean directory,
                        long modifiedTime) {
        this.name = name;
        this.size = size;
        this.compressedSize = compressedSize;
        this.directory = directory;
        this.modifiedTime = modifiedTime;
    }

    @NonNull public String getName() { return name; }
    public long getSize() { return size; }
    public long getCompressedSize() { return compressedSize; }
    public boolean isDirectory() { return directory; }
    public long getModifiedTime() { return modifiedTime; }
}
