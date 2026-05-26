package com.zfile.manager.service;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.core.TrashIndex;
import com.zfile.manager.model.TransferProgress;
import com.zfile.manager.model.TrashEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous soft-delete operations: move into {@code <root>/.ZFileTrash/}, restore,
 * permanent delete, and time-based purge.
 *
 * <p>All public methods must be invoked from a background thread (see
 * {@link ThreadPoolManager#execute}). Progress is published through the same
 * {@link FileTransferService.ProgressCallback} contract so the UI layer can share
 * its transfer-progress bindings.</p>
 */
public final class RecycleBinService {

    private static final String TAG = "RecycleBinService";
    private static final String TRASH_DIR_NAME = ".ZFileTrash";
    private static final String NOMEDIA = ".nomedia";
    public static final long DEFAULT_MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long PROGRESS_INTERVAL_MS = 100L;

    public void moveToTrash(@NonNull List<String> sourcePaths,
                            @Nullable FileTransferService.ProgressCallback callback,
                            @NonNull AtomicBoolean cancelled) {
        File trashDir = ensureTrashDir();
        if (trashDir == null) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.TRASH, 0L, 0L, "No trash directory available"));
            return;
        }

        int totalFiles = sourcePaths.size();
        long totalBytes = 0L;
        for (String s : sourcePaths) totalBytes += sizeOf(new File(s));
        publish(callback, TransferProgress.pending(
                TransferProgress.Operation.TRASH, totalBytes, totalFiles));

        int processed = 0;
        long transferred = 0L;
        long lastPublish = 0L;
        TrashIndex index = TrashIndex.getInstance();

        try {
            for (String path : sourcePaths) {
                if (cancelled.get()) {
                    index.save();
                    publish(callback, TransferProgress.cancelled(TransferProgress.Operation.TRASH));
                    return;
                }
                File src = new File(path);
                if (!src.exists()) {
                    processed++;
                    continue;
                }
                String trashedName = uniqueTrashName(trashDir, src.getName());
                File dest = new File(trashDir, trashedName);
                long size = sizeOf(src);
                boolean isDir = src.isDirectory();

                if (!src.renameTo(dest)) {
                    // Cross-volume — fall back to copy + delete.
                    copyRecursive(src, dest, cancelled);
                    if (cancelled.get()) {
                        // Cleanup partial copy.
                        deleteRecursive(dest);
                        index.save();
                        publish(callback, TransferProgress.cancelled(TransferProgress.Operation.TRASH));
                        return;
                    }
                    deleteRecursive(src);
                }

                index.add(TrashEntry.create(src.getAbsolutePath(), trashedName, size, isDir));
                processed++;
                transferred += size;

                long now = System.currentTimeMillis();
                if (now - lastPublish >= PROGRESS_INTERVAL_MS) {
                    publish(callback, TransferProgress.inProgress(
                            TransferProgress.Operation.TRASH, totalBytes, transferred,
                            totalFiles, processed, src.getName()));
                    lastPublish = now;
                }
            }
            index.save();
            publish(callback, TransferProgress.completed(
                    TransferProgress.Operation.TRASH, totalBytes, totalFiles));
        } catch (IOException e) {
            index.save();
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.TRASH, totalBytes, transferred, safeMessage(e)));
        }
    }

    public void restore(@NonNull List<String> ids,
                        @Nullable FileTransferService.ProgressCallback callback,
                        @NonNull AtomicBoolean cancelled) {
        File trashDir = ensureTrashDir();
        if (trashDir == null) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.RESTORE, 0L, 0L, "No trash directory available"));
            return;
        }
        TrashIndex index = TrashIndex.getInstance();
        int totalFiles = ids.size();
        long totalBytes = 0L;
        for (String id : ids) {
            TrashEntry e = index.getById(id);
            if (e != null) totalBytes += e.getOriginalSize();
        }
        publish(callback, TransferProgress.pending(
                TransferProgress.Operation.RESTORE, totalBytes, totalFiles));

        int processed = 0;
        long transferred = 0L;
        long lastPublish = 0L;

        try {
            for (String id : ids) {
                if (cancelled.get()) {
                    index.save();
                    publish(callback, TransferProgress.cancelled(TransferProgress.Operation.RESTORE));
                    return;
                }
                TrashEntry entry = index.getById(id);
                if (entry == null) {
                    processed++;
                    continue;
                }
                File trashed = new File(trashDir, entry.getTrashedFileName());
                if (!trashed.exists()) {
                    // Out-of-sync — drop the orphan record.
                    index.remove(id);
                    processed++;
                    continue;
                }
                File originalParent = new File(entry.getOriginalPath()).getParentFile();
                if (originalParent != null && !originalParent.exists()) {
                    originalParent.mkdirs();
                }
                if (originalParent == null) {
                    Log.w(TAG, "Cannot restore — no parent for " + entry.getOriginalPath());
                    processed++;
                    continue;
                }
                File restoreDest = uniqueDestination(originalParent, basename(entry.getOriginalPath()));

                if (!trashed.renameTo(restoreDest)) {
                    copyRecursive(trashed, restoreDest, cancelled);
                    if (cancelled.get()) {
                        deleteRecursive(restoreDest);
                        index.save();
                        publish(callback, TransferProgress.cancelled(TransferProgress.Operation.RESTORE));
                        return;
                    }
                    deleteRecursive(trashed);
                }
                index.remove(id);
                processed++;
                transferred += entry.getOriginalSize();

                long now = System.currentTimeMillis();
                if (now - lastPublish >= PROGRESS_INTERVAL_MS) {
                    publish(callback, TransferProgress.inProgress(
                            TransferProgress.Operation.RESTORE, totalBytes, transferred,
                            totalFiles, processed, restoreDest.getName()));
                    lastPublish = now;
                }
            }
            index.save();
            publish(callback, TransferProgress.completed(
                    TransferProgress.Operation.RESTORE, totalBytes, totalFiles));
        } catch (IOException e) {
            index.save();
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.RESTORE, totalBytes, transferred, safeMessage(e)));
        }
    }

    public void permanentlyDelete(@NonNull List<String> ids) {
        File trashDir = ensureTrashDir();
        if (trashDir == null) return;
        TrashIndex index = TrashIndex.getInstance();
        for (String id : ids) {
            TrashEntry entry = index.getById(id);
            if (entry == null) continue;
            deleteRecursive(new File(trashDir, entry.getTrashedFileName()));
            index.remove(id);
        }
        index.save();
    }

    public void emptyAll() {
        File trashDir = ensureTrashDir();
        if (trashDir == null) return;
        TrashIndex index = TrashIndex.getInstance();
        for (TrashEntry e : index.getAll()) {
            deleteRecursive(new File(trashDir, e.getTrashedFileName()));
        }
        index.clear();
        index.save();
    }

    public void purgeExpired(long maxAgeMs) {
        File trashDir = ensureTrashDir();
        if (trashDir == null) return;
        TrashIndex index = TrashIndex.getInstance();
        long now = System.currentTimeMillis();
        boolean any = false;
        for (TrashEntry e : index.getAll()) {
            if (now - e.getDeletedAt() > maxAgeMs) {
                deleteRecursive(new File(trashDir, e.getTrashedFileName()));
                index.remove(e.getId());
                any = true;
            }
        }
        if (any) index.save();
    }

    /**
     * Schedule daily purge starting at the next 02:00 local. Repeats every 24h.
     * Safe to call once from {@code Application.onCreate}.
     */
    public void scheduleCleanup() {
        long initialDelay = millisUntilNext0200();
        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                () -> {
                    try {
                        purgeExpired(DEFAULT_MAX_AGE_MS);
                    } catch (Exception ex) {
                        Log.w(TAG, "purgeExpired threw", ex);
                    }
                },
                initialDelay,
                TimeUnit.DAYS.toMillis(1),
                TimeUnit.MILLISECONDS);
    }

    @Nullable
    public static File getTrashDir() {
        String root = FileSystemManager.getInstance().getCurrentRootPath();
        if (root == null) return null;
        return new File(root, TRASH_DIR_NAME);
    }

    @Nullable
    private File ensureTrashDir() {
        File dir = getTrashDir();
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Failed to create trash dir: " + dir);
            return null;
        }
        File nomedia = new File(dir, NOMEDIA);
        if (!nomedia.exists()) {
            try {
                if (!nomedia.createNewFile()) {
                    Log.w(TAG, "Failed to create .nomedia stub");
                }
            } catch (IOException e) {
                Log.w(TAG, "createNewFile .nomedia failed", e);
            }
        }
        return dir;
    }

    @NonNull
    private static String uniqueTrashName(@NonNull File trashDir, @NonNull String originalName) {
        String ext = extOf(originalName);
        for (int i = 0; i < 5; i++) {
            String candidate = UUID.randomUUID().toString() + ext;
            if (!new File(trashDir, candidate).exists()) return candidate;
        }
        // Almost-impossible fallback.
        return UUID.randomUUID().toString() + "_" + System.nanoTime() + ext;
    }

    @NonNull
    private static String extOf(@NonNull String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot) : "";
    }

    @NonNull
    private static String basename(@NonNull String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    @NonNull
    private static File uniqueDestination(@NonNull File destDir, @NonNull String name) {
        File candidate = new File(destDir, name);
        if (!candidate.exists()) return candidate;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; i < 1000; i++) {
            File c = new File(destDir, base + " (" + i + ")" + ext);
            if (!c.exists()) return c;
        }
        return candidate;
    }

    private static long sizeOf(@NonNull File f) {
        if (!f.exists()) return 0L;
        if (!f.isDirectory()) return f.length();
        long total = 0L;
        File[] children = f.listFiles();
        if (children == null) return 0L;
        for (File c : children) total += sizeOf(c);
        return total;
    }

    private static void copyRecursive(@NonNull File src, @NonNull File dest,
                                      @NonNull AtomicBoolean cancelled) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists() && !dest.mkdirs()) {
                throw new IOException("Cannot create directory: " + dest);
            }
            File[] children = src.listFiles();
            if (children == null) return;
            for (File c : children) {
                if (cancelled.get()) return;
                copyRecursive(c, new File(dest, c.getName()), cancelled);
            }
            return;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        byte[] buf = new byte[BUFFER_SIZE];
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                if (cancelled.get()) return;
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    private static boolean deleteRecursive(@NonNull File f) {
        if (!f.exists()) return true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        return f.delete();
    }

    private static long millisUntilNext0200() {
        Calendar now = Calendar.getInstance();
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, 2);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }
        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    private static void publish(@Nullable FileTransferService.ProgressCallback cb,
                                @NonNull TransferProgress p) {
        if (cb != null) cb.onProgress(p);
    }

    @NonNull
    private static String safeMessage(@Nullable Throwable t) {
        if (t == null) return "Unknown error";
        String m = t.getMessage();
        return m != null ? m : t.getClass().getSimpleName();
    }
}
