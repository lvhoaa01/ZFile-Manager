package com.zfile.manager.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot returned by {@code StorageAnalyzerService.analyze} — describes
 * how a single volume's bytes are distributed.
 *
 * <ul>
 *   <li>{@link #categoryBytes}: bytes per {@link CategoryType} from the MediaStore.</li>
 *   <li>{@link #topFiles}: largest individual files (newest-first walk; top-N by size).</li>
 *   <li>{@link #topFolders}: largest <em>root-level</em> folders to avoid double-counting.</li>
 *   <li>{@link #totalUsed} + {@link #totalFree} = {@link #totalCapacity} (modulo FS overhead).</li>
 * </ul>
 */
public final class StorageAnalysis {

    @NonNull private final Map<CategoryType, Long> categoryBytes;
    @NonNull private final List<FileItem> topFiles;
    @NonNull private final List<FileItem> topFolders;
    private final long totalUsed;
    private final long totalFree;
    private final long totalCapacity;

    public StorageAnalysis(@NonNull Map<CategoryType, Long> categoryBytes,
                           @NonNull List<FileItem> topFiles,
                           @NonNull List<FileItem> topFolders,
                           long totalUsed,
                           long totalFree,
                           long totalCapacity) {
        this.categoryBytes = Collections.unmodifiableMap(new EnumMap<>(categoryBytes));
        this.topFiles = Collections.unmodifiableList(topFiles);
        this.topFolders = Collections.unmodifiableList(topFolders);
        this.totalUsed = totalUsed;
        this.totalFree = totalFree;
        this.totalCapacity = totalCapacity;
    }

    @NonNull public Map<CategoryType, Long> getCategoryBytes() { return categoryBytes; }
    @NonNull public List<FileItem> getTopFiles() { return topFiles; }
    @NonNull public List<FileItem> getTopFolders() { return topFolders; }
    public long getTotalUsed() { return totalUsed; }
    public long getTotalFree() { return totalFree; }
    public long getTotalCapacity() { return totalCapacity; }

    public long getCategorizedBytes() {
        long sum = 0L;
        for (Long v : categoryBytes.values()) sum += v;
        return sum;
    }
}
