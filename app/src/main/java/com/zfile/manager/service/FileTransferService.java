package com.zfile.manager.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.TransferProgress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous file mutation operations: copy, move, delete, create, rename.
 * All methods run on the caller's thread — schedule via {@code ThreadPoolManager}.
 *
 * <p>Long-running operations publish {@link TransferProgress} snapshots to a
 * {@link ProgressCallback}. Throttled to {@value #PROGRESS_INTERVAL_MS} ms so we
 * don't flood the main thread with LiveData updates.</p>
 */
public final class FileTransferService {

    public interface ProgressCallback {
        void onProgress(@NonNull TransferProgress progress);
    }

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long PROGRESS_INTERVAL_MS = 100L;

    public void copyFiles(@NonNull List<String> sourcePaths,
                          @NonNull String destDir,
                          @Nullable ProgressCallback callback,
                          @NonNull AtomicBoolean cancelled) {
        run(TransferProgress.Operation.COPY, sourcePaths, destDir, callback, cancelled, false);
    }

    public void moveFiles(@NonNull List<String> sourcePaths,
                          @NonNull String destDir,
                          @Nullable ProgressCallback callback,
                          @NonNull AtomicBoolean cancelled) {
        run(TransferProgress.Operation.MOVE, sourcePaths, destDir, callback, cancelled, true);
    }

    public boolean deleteRecursive(@NonNull String path) {
        return deleteRecursive(new File(path));
    }

    public boolean createFolder(@NonNull String parentDir, @NonNull String name) {
        File parent = new File(parentDir);
        if (!parent.isDirectory()) return false;
        File target = new File(parent, name);
        if (target.exists()) return false;
        return target.mkdirs();
    }

    public boolean createFile(@NonNull String parentDir, @NonNull String name) throws IOException {
        File parent = new File(parentDir);
        if (!parent.isDirectory()) return false;
        File target = new File(parent, name);
        if (target.exists()) return false;
        return target.createNewFile();
    }

    public boolean rename(@NonNull String path, @NonNull String newName) {
        File src = new File(path);
        if (!src.exists()) return false;
        File parent = src.getParentFile();
        if (parent == null) return false;
        File dest = new File(parent, newName);
        if (dest.exists()) return false;
        return src.renameTo(dest);
    }

    private void run(@NonNull TransferProgress.Operation op,
                     @NonNull List<String> sourcePaths,
                     @NonNull String destDir,
                     @Nullable ProgressCallback callback,
                     @NonNull AtomicBoolean cancelled,
                     boolean deleteSourceAfterCopy) {
        long totalBytes = 0L;
        int totalFiles = 0;
        for (String s : sourcePaths) {
            long[] tally = tally(new File(s));
            totalBytes += tally[0];
            totalFiles += (int) tally[1];
        }

        publish(callback, TransferProgress.pending(op, totalBytes, totalFiles));

        long transferredBytes = 0L;
        int processedFiles = 0;
        long lastPublish = 0L;

        try {
            for (String src : sourcePaths) {
                if (cancelled.get()) {
                    publish(callback, TransferProgress.cancelled(op));
                    return;
                }
                File srcFile = new File(src);
                File destFile = uniqueDestination(new File(destDir), srcFile.getName());

                long[] result = copyRecursive(
                        srcFile, destFile, op,
                        totalBytes, transferredBytes,
                        totalFiles, processedFiles,
                        lastPublish, callback, cancelled);
                transferredBytes = result[0];
                processedFiles = (int) result[1];
                lastPublish = result[2];

                if (deleteSourceAfterCopy && !cancelled.get()) {
                    deleteRecursive(srcFile);
                }
            }
            publish(callback, TransferProgress.completed(op, totalBytes, totalFiles));
        } catch (IOException e) {
            publish(callback, TransferProgress.failed(op, totalBytes, transferredBytes,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private long[] copyRecursive(@NonNull File src,
                                 @NonNull File dest,
                                 @NonNull TransferProgress.Operation op,
                                 long totalBytes,
                                 long transferredBytes,
                                 int totalFiles,
                                 int processedFiles,
                                 long lastPublish,
                                 @Nullable ProgressCallback callback,
                                 @NonNull AtomicBoolean cancelled) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists() && !dest.mkdirs()) {
                throw new IOException("Cannot create directory: " + dest.getAbsolutePath());
            }
            File[] children = src.listFiles();
            if (children == null) return new long[] { transferredBytes, processedFiles, lastPublish };
            for (File child : children) {
                if (cancelled.get()) return new long[] { transferredBytes, processedFiles, lastPublish };
                long[] r = copyRecursive(child, new File(dest, child.getName()),
                        op, totalBytes, transferredBytes, totalFiles, processedFiles,
                        lastPublish, callback, cancelled);
                transferredBytes = r[0];
                processedFiles = (int) r[1];
                lastPublish = r[2];
            }
            return new long[] { transferredBytes, processedFiles, lastPublish };
        }

        File destParent = dest.getParentFile();
        if (destParent != null && !destParent.exists() && !destParent.mkdirs()) {
            throw new IOException("Cannot create parent: " + destParent.getAbsolutePath());
        }

        byte[] buf = new byte[BUFFER_SIZE];
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                if (cancelled.get()) {
                    return new long[] { transferredBytes, processedFiles, lastPublish };
                }
                out.write(buf, 0, n);
                transferredBytes += n;

                long now = System.currentTimeMillis();
                if (now - lastPublish >= PROGRESS_INTERVAL_MS) {
                    publish(callback, TransferProgress.inProgress(
                            op, totalBytes, transferredBytes,
                            totalFiles, processedFiles, src.getName()));
                    lastPublish = now;
                }
            }
            out.flush();
        }
        processedFiles++;
        return new long[] { transferredBytes, processedFiles, lastPublish };
    }

    private static long[] tally(@NonNull File f) {
        long bytes = 0L;
        long count = 0L;
        Deque<File> stack = new ArrayDeque<>();
        stack.push(f);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) {
                    for (File c : children) stack.push(c);
                }
            } else {
                bytes += current.length();
                count++;
            }
        }
        return new long[] { bytes, count };
    }

    private static boolean deleteRecursive(@NonNull File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    if (!deleteRecursive(c)) return false;
                }
            }
        }
        return f.delete();
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

    private static void publish(@Nullable ProgressCallback callback, @NonNull TransferProgress p) {
        if (callback != null) callback.onProgress(p);
    }
}
