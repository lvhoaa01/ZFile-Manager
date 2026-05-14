package com.zfile.manager.model;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Lightweight snapshot of a mounted storage location (internal storage, SD card, USB OTG).
 * Free / total space values are sampled at construction time — refresh by re-querying
 * {@link com.zfile.manager.core.FileSystemManager}.
 */
public final class StorageVolume {

    @NonNull private final String name;
    @NonNull private final String path;
    private final long totalSpace;
    private final long freeSpace;
    private final boolean removable;
    private final boolean primary;

    public StorageVolume(@NonNull String name,
                         @NonNull String path,
                         long totalSpace,
                         long freeSpace,
                         boolean removable,
                         boolean primary) {
        this.name = name;
        this.path = path;
        this.totalSpace = totalSpace;
        this.freeSpace = freeSpace;
        this.removable = removable;
        this.primary = primary;
    }

    @NonNull public String getName() { return name; }
    @NonNull public String getPath() { return path; }
    public long getTotalSpace() { return totalSpace; }
    public long getFreeSpace() { return freeSpace; }
    public long getUsedSpace() { return Math.max(0L, totalSpace - freeSpace); }
    public boolean isRemovable() { return removable; }
    public boolean isPrimary() { return primary; }

    public float getUsageRatio() {
        if (totalSpace <= 0) return 0f;
        return (float) getUsedSpace() / (float) totalSpace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StorageVolume)) return false;
        StorageVolume that = (StorageVolume) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
