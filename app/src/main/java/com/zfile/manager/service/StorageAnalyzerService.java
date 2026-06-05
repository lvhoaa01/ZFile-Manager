package com.zfile.manager.service;

import android.os.StatFs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.model.StorageAnalysis;
import com.zfile.manager.repository.MediaStoreRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous storage-analysis service. Walks a single volume to find the
 * largest files and largest root-level folders, queries the MediaStore for
 * per-category byte totals, then snapshots free/used/capacity via {@link StatFs}.
 *
 * <p>Caller must dispatch on a background thread (use
 * {@link com.zfile.manager.core.ThreadPoolManager}) — {@link #analyze} is
 * straight-line blocking I/O.</p>
 */
public final class StorageAnalyzerService {

    public interface ProgressCallback {
        /**
         * Called when the analyzer advances to a new stage.
         *
         * @param category non-null when scanning a specific MediaStore category;
         *                 null while walking the filesystem for top files.
         */
        // hàm này được gọi khi phân tích tiến đến một giai đoạn mới.
        void onProgress(@Nullable CategoryType category);
    }

    private static final int TOP_N = 10;

    @NonNull
    public StorageAnalysis analyze(@NonNull String volumePath,
                                   @Nullable ProgressCallback callback,
                                   @NonNull AtomicBoolean cancelled) {
        MediaStoreRepository mediaStore = MediaStoreRepository.getInstance();

        //1 ) Byte theo từng loại (rẻ, hỗ trợ bởi MediaStore).
        EnumMap<CategoryType, Long> categoryBytes = new EnumMap<>(CategoryType.class);
        for (CategoryType t : CategoryType.values()) {
            if (cancelled.get()) return emptyAnalysis(volumePath);
            if (callback != null) callback.onProgress(t);
            categoryBytes.put(t, mediaStore.sumSize(t));
        } 

        // 2) Top largest files (BFS walk, min-heap of size TOP_N).
        // Top largest root-level folders to avoid double counting with nested folders.
        PriorityQueue<FileItem> topFilesPQ = new PriorityQueue<>(
                TOP_N + 1, Comparator.comparingLong(FileItem::getSize));
        PriorityQueue<FileItem> topFoldersPQ = new PriorityQueue<>(
                TOP_N + 1, Comparator.comparingLong(FileItem::getSize));

        File root = new File(volumePath);
        if (root.exists() && root.isDirectory()) {
            // Top-level children only for folder ranking → no nested double counting.
            File[] rootChildren = root.listFiles();
            if (rootChildren != null) {
                for (File child : rootChildren) {
                    if (cancelled.get()) break;
                    if (child.isDirectory()) {
                        long folderSize = tallyFolder(child, cancelled);
                        if (cancelled.get()) break;
                        FileItem folderItem = new FileItem(
                                child.getName(), child.getAbsolutePath(),
                                folderSize, child.lastModified(),
                                true, child.isHidden(),
                                child.canRead(), child.canWrite(),
                                FileType.FOLDER, null);
                        pushBounded(topFoldersPQ, folderItem);
                    }
                }
            }

            if (callback != null) callback.onProgress(null);  // null → "Scanning files" stage
            walkFiles(root, topFilesPQ, cancelled);
        }

        List<FileItem> topFiles = drainSortedDesc(topFilesPQ);
        List<FileItem> topFolders = drainSortedDesc(topFoldersPQ);

        // 3) Total capacity via StatFs (fallback to File.getTotalSpace).
        long totalCapacity;
        long totalFree;
        try {
            StatFs stat = new StatFs(volumePath);
            totalCapacity = stat.getTotalBytes();
            totalFree = stat.getAvailableBytes();
        } catch (IllegalArgumentException e) {
            totalCapacity = root.getTotalSpace();
            totalFree = root.getFreeSpace();
        }
        long totalUsed = Math.max(0L, totalCapacity - totalFree);

        return new StorageAnalysis(categoryBytes, topFiles, topFolders,
                totalUsed, totalFree, totalCapacity);
    }

    private static void walkFiles(@NonNull File root,
                                  @NonNull PriorityQueue<FileItem> topFilesPQ,
                                  @NonNull AtomicBoolean cancelled) {
        Deque<File> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            if (cancelled.get()) return;
            File f = stack.pop();
            if (f.isDirectory()) {
                String canonical;
                try {
                    canonical = f.getCanonicalPath();
                } catch (IOException e) {
                    continue;
                }
                if (!visited.add(canonical)) continue;
                File[] children = f.listFiles();
                if (children == null) continue;
                for (File c : children) stack.push(c);
            } else {
                long size = f.length();
                if (size <= 0L) continue;
                String name = f.getName();
                FileItem item = new FileItem(
                        name, f.getAbsolutePath(),
                        size, f.lastModified(),
                        false, f.isHidden(),
                        f.canRead(), f.canWrite(),
                        MimeTypeHelper.getFileTypeFromName(name), null);
                pushBounded(topFilesPQ, item);
            }
        }
    }

    private static long tallyFolder(@NonNull File folder, @NonNull AtomicBoolean cancelled) {
        long bytes = 0L;
        Set<String> visited = new HashSet<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(folder);
        while (!stack.isEmpty()) {
            if (cancelled.get()) return bytes;
            File current = stack.pop();
            if (current.isDirectory()) {
                String canonical;
                try {
                    canonical = current.getCanonicalPath();
                } catch (IOException e) {
                    continue;
                }
                if (!visited.add(canonical)) continue;
                File[] children = current.listFiles();
                if (children != null) {
                    for (File c : children) stack.push(c);
                }
            } else {
                bytes += current.length();
            }
        }
        return bytes;
    }

    private static void pushBounded(@NonNull PriorityQueue<FileItem> pq, @NonNull FileItem item) {
        pq.offer(item); // Nếu item nhỏ hơn thì nó được thêm vao
        while (pq.size() > TOP_N) pq.poll();
    }

    @NonNull
    private static List<FileItem> drainSortedDesc(@NonNull PriorityQueue<FileItem> pq) {
        List<FileItem> out = new ArrayList<>(pq);
        out.sort(Comparator.comparingLong(FileItem::getSize).reversed());
        return out;
    }

    @NonNull
    private static StorageAnalysis emptyAnalysis(@NonNull String volumePath) {
        File root = new File(volumePath);
        long cap = root.exists() ? root.getTotalSpace() : 0L;
        long free = root.exists() ? root.getFreeSpace() : 0L;
        return new StorageAnalysis(
                new EnumMap<>(CategoryType.class),
                Collections.emptyList(),
                Collections.emptyList(),
                Math.max(0L, cap - free),
                free,
                cap);
    }
}
