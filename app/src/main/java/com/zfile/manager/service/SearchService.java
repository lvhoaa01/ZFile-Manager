package com.zfile.manager.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous recursive file-name search. Run from a background thread —
 * callers should use {@link com.zfile.manager.core.ThreadPoolManager#submitSearch}
 * so a newer query supersedes an in-flight older one.
 *
 * <p>Matching is case-insensitive substring on the file name. Symlink cycles
 * are blocked via a visited-canonical-paths set; results are emitted in
 * batches to {@link ResultCallback#onPartialResults} so the UI can paint
 * before the entire tree is walked.</p>
 */
public final class SearchService {

    public interface ResultCallback {
        void onPartialResults(@NonNull List<FileItem> batch);
        void onFinished(int totalMatches);
    }

    private static final int BATCH_SIZE = 50;

    /** Walks {@code rootPath} breadth-first; aborts as soon as {@code cancelled} flips true. */
    @NonNull
    public List<FileItem> search(@NonNull String rootPath,
                                 @NonNull String query,
                                 @NonNull AtomicBoolean cancelled,
                                 @Nullable ResultCallback callback) {
        if (query.trim().isEmpty()) {
            if (callback != null) callback.onFinished(0);
            return Collections.emptyList();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        File root = new File(rootPath);
        if (!root.exists() || !root.canRead()) {
            if (callback != null) callback.onFinished(0);
            return Collections.emptyList();
        }

        List<FileItem> matches = new ArrayList<>();
        List<FileItem> pendingBatch = new ArrayList<>(BATCH_SIZE);
        Set<String> visited = new HashSet<>();
        Deque<File> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            if (cancelled.get()) break;
            File current = queue.poll();
            if (current == null) continue;

            if (current.isDirectory()) {
                String canonical;
                try {
                    canonical = current.getCanonicalPath();
                } catch (IOException e) {
                    continue;
                }
                if (!visited.add(canonical)) continue;
                File[] children = current.listFiles();
                if (children == null) continue;
                for (File child : children) {
                    if (cancelled.get()) break;
                    queue.add(child);
                }
            }

            if (current.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                FileType type = MimeTypeHelper.getFileType(current);
                String mime = MimeTypeHelper.getMimeType(current);
                FileItem item = FileItem.fromFile(current, type, mime);
                if (current.isDirectory()) {
                    String[] grand = current.list();
                    item.setChildCount(grand == null ? 0 : grand.length);
                }
                matches.add(item);
                pendingBatch.add(item);

                if (pendingBatch.size() >= BATCH_SIZE && callback != null) {
                    callback.onPartialResults(new ArrayList<>(pendingBatch));
                    pendingBatch.clear();
                }
            }
        }

        if (callback != null) {
            if (!pendingBatch.isEmpty()) callback.onPartialResults(pendingBatch);
            callback.onFinished(matches.size());
        }
        return matches;
    }
}
