package com.zfile.manager.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.ArchiveEntry;
import com.zfile.manager.model.TransferProgress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Synchronous .zip compression / extraction. Caller must dispatch on a background
 * thread (see {@link com.zfile.manager.core.ThreadPoolManager}).
 *
 * <p>Progress is reported through the shared {@link FileTransferService.ProgressCallback}
 * contract so the existing top progress-indicator can be reused.</p>
 *
 * <p>Security: extraction validates every entry's canonical path against the
 * destination root to prevent <em>zip slip</em> attacks. Compression detects
 * symlink loops via a canonical-path visited set.</p>
 */
public final class ArchiveService {

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long PROGRESS_INTERVAL_MS = 100L;

    public void zip(@NonNull List<String> sourcePaths,
                    @NonNull File dest,
                    @Nullable FileTransferService.ProgressCallback callback,
                    @NonNull AtomicBoolean cancelled) {
        long[] tally = tallyAll(sourcePaths);
        long totalBytes = tally[0];
        int totalFiles = (int) tally[1];

        publish(callback, TransferProgress.pending(
                TransferProgress.Operation.ZIP, totalBytes, totalFiles));

        long transferredBytes = 0L;
        int processedFiles = 0;
        long lastPublish = 0L;
        Set<String> visited = new HashSet<>();

        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            byte[] buf = new byte[BUFFER_SIZE];
            for (String src : sourcePaths) {
                if (cancelled.get()) {
                    publish(callback, TransferProgress.cancelled(TransferProgress.Operation.ZIP));
                    cleanupPartial(dest);
                    return;
                }
                File s = new File(src);
                long[] result = zipRecursive(
                        s, zos, "", buf,
                        totalBytes, transferredBytes,
                        totalFiles, processedFiles,
                        lastPublish, callback, cancelled, visited);
                transferredBytes = result[0];
                processedFiles = (int) result[1];
                lastPublish = result[2];
            }
            zos.finish();
            publish(callback, TransferProgress.completed(
                    TransferProgress.Operation.ZIP, totalBytes, totalFiles));
        } catch (IOException e) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.ZIP, totalBytes, transferredBytes, safeMessage(e)));
            cleanupPartial(dest);
        }
    }

    public void unzip(@NonNull File archive,
                      @NonNull File destDir,
                      @Nullable FileTransferService.ProgressCallback callback,
                      @NonNull AtomicBoolean cancelled) {
        long totalBytes = archive.length();
        publish(callback, TransferProgress.pending(
                TransferProgress.Operation.UNZIP, totalBytes, 0));

        if (!destDir.exists() && !destDir.mkdirs()) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.UNZIP, totalBytes, 0L,
                    "Cannot create destination directory"));
            return;
        }

        String destCanonical;
        try {
            destCanonical = destDir.getCanonicalPath() + File.separator;
        } catch (IOException e) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.UNZIP, totalBytes, 0L, safeMessage(e)));
            return;
        }

        long transferredBytes = 0L;
        int processedFiles = 0;
        long lastPublish = 0L;

        try (FileInputStream fis = new FileInputStream(archive);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {
            byte[] buf = new byte[BUFFER_SIZE];
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (cancelled.get()) {
                    publish(callback, TransferProgress.cancelled(TransferProgress.Operation.UNZIP));
                    return;
                }
                File outFile = new File(destDir, entry.getName());

                // Zip-slip guard — reject entries that escape the destination root.
                String outCanonical = outFile.getCanonicalPath();
                if (!outCanonical.startsWith(destCanonical)
                        && !outCanonical.equals(destDir.getCanonicalPath())) {
                    throw new SecurityException("Zip slip: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new IOException("Cannot create directory: " + outFile);
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Cannot create parent: " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(outFile);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            if (cancelled.get()) {
                                publish(callback, TransferProgress.cancelled(TransferProgress.Operation.UNZIP));
                                return;
                            }
                            bos.write(buf, 0, n);
                            transferredBytes += n;
                            long now = System.currentTimeMillis();
                            if (now - lastPublish >= PROGRESS_INTERVAL_MS) {
                                publish(callback, TransferProgress.inProgress(
                                        TransferProgress.Operation.UNZIP,
                                        totalBytes, transferredBytes,
                                        0, processedFiles, entry.getName()));
                                lastPublish = now;
                            }
                        }
                    }
                    processedFiles++;
                }
                zis.closeEntry();
            }
            publish(callback, TransferProgress.completed(
                    TransferProgress.Operation.UNZIP, totalBytes, processedFiles));
        } catch (SecurityException e) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.UNZIP, totalBytes, transferredBytes,
                    "Unsafe archive: " + e.getMessage()));
        } catch (IOException e) {
            publish(callback, TransferProgress.failed(
                    TransferProgress.Operation.UNZIP, totalBytes, transferredBytes, safeMessage(e)));
        }
    }

    /** Inspect (without extracting) — used by the extract preview dialog. */
    @NonNull
    public List<ArchiveEntry> listEntries(@NonNull File archive, int limit) {
        List<ArchiveEntry> out = new ArrayList<>();
        try (ZipFile zf = new ZipFile(archive)) {
            java.util.Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements() && (limit <= 0 || out.size() < limit)) {
                ZipEntry entry = e.nextElement();
                out.add(new ArchiveEntry(
                        entry.getName(),
                        entry.getSize(),
                        entry.getCompressedSize(),
                        entry.isDirectory(),
                        entry.getTime()));
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    public int countEntries(@NonNull File archive) {
        try (ZipFile zf = new ZipFile(archive)) {
            return zf.size();
        } catch (IOException e) {
            return 0;
        }
    }

    private long[] zipRecursive(@NonNull File src,
                                @NonNull ZipOutputStream zos,
                                @NonNull String parentPath,
                                @NonNull byte[] buf,
                                long totalBytes,
                                long transferredBytes,
                                int totalFiles,
                                int processedFiles,
                                long lastPublish,
                                @Nullable FileTransferService.ProgressCallback callback,
                                @NonNull AtomicBoolean cancelled,
                                @NonNull Set<String> visitedCanonical) throws IOException {
        if (cancelled.get()) {
            return new long[] { transferredBytes, processedFiles, lastPublish };
        }
        if (src.isDirectory()) {
            String canonical = src.getCanonicalPath();
            if (!visitedCanonical.add(canonical)) {
                return new long[] { transferredBytes, processedFiles, lastPublish };
            }
            String entryName = parentPath + src.getName() + "/";
            ZipEntry dirEntry = new ZipEntry(entryName);
            zos.putNextEntry(dirEntry);
            zos.closeEntry();
            File[] children = src.listFiles();
            if (children == null) {
                return new long[] { transferredBytes, processedFiles, lastPublish };
            }
            for (File child : children) {
                long[] r = zipRecursive(
                        child, zos, entryName, buf,
                        totalBytes, transferredBytes,
                        totalFiles, processedFiles,
                        lastPublish, callback, cancelled, visitedCanonical);
                transferredBytes = r[0];
                processedFiles = (int) r[1];
                lastPublish = r[2];
            }
            return new long[] { transferredBytes, processedFiles, lastPublish };
        }

        String entryName = parentPath + src.getName();
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(src.lastModified());
        zos.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(src);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            int n;
            while ((n = bis.read(buf)) > 0) {
                if (cancelled.get()) {
                    zos.closeEntry();
                    return new long[] { transferredBytes, processedFiles, lastPublish };
                }
                zos.write(buf, 0, n);
                transferredBytes += n;
                long now = System.currentTimeMillis();
                if (now - lastPublish >= PROGRESS_INTERVAL_MS) {
                    publish(callback, TransferProgress.inProgress(
                            TransferProgress.Operation.ZIP,
                            totalBytes, transferredBytes,
                            totalFiles, processedFiles, src.getName()));
                    lastPublish = now;
                }
            }
        }
        zos.closeEntry();
        processedFiles++;
        return new long[] { transferredBytes, processedFiles, lastPublish };
    }

    @NonNull
    private static long[] tallyAll(@NonNull List<String> sourcePaths) {
        long totalBytes = 0L;
        long totalFiles = 0L;
        Set<String> visited = new HashSet<>();
        Deque<File> stack = new ArrayDeque<>();
        for (String p : sourcePaths) stack.push(new File(p));
        while (!stack.isEmpty()) {
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
                if (children != null) {
                    for (File c : children) stack.push(c);
                }
            } else {
                totalBytes += f.length();
                totalFiles++;
            }
        }
        return new long[] { totalBytes, totalFiles };
    }

    private static void cleanupPartial(@NonNull File dest) {
        if (dest.exists() && !dest.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
        }
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
