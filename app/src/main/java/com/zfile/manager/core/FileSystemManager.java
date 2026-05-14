package com.zfile.manager.core;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton (double-checked locking) that owns global, process-wide filesystem state:
 * detected storage volumes, current root, hidden-file visibility, sort preference,
 * and the copy/cut clipboard.
 *
 * <p>Initialise once from {@code ZFileApplication} via {@link #initialize(Context)}.
 * All public mutators are synchronised; readers see consistent snapshots.
 */
public final class FileSystemManager {

    public enum ClipboardOperation { NONE, COPY, CUT }

    private static volatile FileSystemManager instance;

    @Nullable private Context appContext;
    private final List<StorageVolume> volumes = new CopyOnWriteArrayList<>();

    @Nullable private String currentRootPath;
    private volatile boolean showHiddenFiles = false;
    @NonNull private volatile SortCriteria sortCriteria = SortCriteria.NAME_ASC;
    private volatile boolean foldersFirst = true;

    private final List<String> clipboardPaths = new ArrayList<>();
    @NonNull private ClipboardOperation clipboardOperation = ClipboardOperation.NONE;

    private volatile boolean legacyPermissionGranted = false;

    private FileSystemManager() { }

    @NonNull
    public static FileSystemManager getInstance() {
        FileSystemManager local = instance;
        if (local == null) {
            synchronized (FileSystemManager.class) {
                local = instance;
                if (local == null) {
                    local = new FileSystemManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void initialize(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        refreshVolumes();
        if (currentRootPath == null && !volumes.isEmpty()) {
            currentRootPath = volumes.get(0).getPath();
        }
    }

    @NonNull
    public Context requireContext() {
        Context c = appContext;
        if (c == null) {
            throw new IllegalStateException(
                    "FileSystemManager.initialize(Context) must be called from Application.onCreate()");
        }
        return c;
    }

    public synchronized void refreshVolumes() {
        volumes.clear();

        File primary = Environment.getExternalStorageDirectory();
        if (primary != null && primary.exists()) {
            volumes.add(new StorageVolume(
                    "Internal Storage",
                    primary.getAbsolutePath(),
                    primary.getTotalSpace(),
                    primary.getFreeSpace(),
                    false,
                    true
            ));
        }

        if (appContext != null) {
            File[] externalDirs = appContext.getExternalFilesDirs(null);
            if (externalDirs != null && externalDirs.length > 1) {
                for (int i = 1; i < externalDirs.length; i++) {
                    File dir = externalDirs[i];
                    if (dir == null) continue;
                    String path = dir.getAbsolutePath();
                    int idx = path.indexOf("/Android/");
                    if (idx > 0) {
                        path = path.substring(0, idx);
                    }
                    File root = new File(path);
                    if (!root.exists()) continue;
                    volumes.add(new StorageVolume(
                            "SD Card",
                            path,
                            root.getTotalSpace(),
                            root.getFreeSpace(),
                            true,
                            false
                    ));
                }
            }
        }
    }

    @NonNull
    public List<StorageVolume> getAvailableVolumes() {
        return Collections.unmodifiableList(new ArrayList<>(volumes));
    }

    @Nullable
    public String getCurrentRootPath() { return currentRootPath; }

    public synchronized void setCurrentRootPath(@Nullable String path) {
        this.currentRootPath = path;
    }

    public boolean isShowHiddenFiles() { return showHiddenFiles; }
    public void setShowHiddenFiles(boolean show) { this.showHiddenFiles = show; }

    @NonNull public SortCriteria getSortCriteria() { return sortCriteria; }
    public void setSortCriteria(@NonNull SortCriteria criteria) { this.sortCriteria = criteria; }

    public boolean isFoldersFirst() { return foldersFirst; }
    public void setFoldersFirst(boolean v) { this.foldersFirst = v; }

    /**
     * True when the app can read/write arbitrary files on external storage —
     * checks {@code MANAGE_EXTERNAL_STORAGE} on API 30+, and falls back to
     * the runtime grant flag set by {@code PermissionHelper} below that.
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return legacyPermissionGranted;
    }

    public void setLegacyPermissionGranted(boolean granted) {
        this.legacyPermissionGranted = granted;
    }

    public synchronized void setClipboard(@NonNull List<String> paths,
                                          @NonNull ClipboardOperation operation) {
        clipboardPaths.clear();
        clipboardPaths.addAll(paths);
        clipboardOperation = paths.isEmpty() ? ClipboardOperation.NONE : operation;
    }

    public synchronized void clearClipboard() {
        clipboardPaths.clear();
        clipboardOperation = ClipboardOperation.NONE;
    }

    @NonNull
    public synchronized List<String> getClipboardPaths() {
        return new ArrayList<>(clipboardPaths);
    }

    @NonNull
    public synchronized ClipboardOperation getClipboardOperation() {
        return clipboardOperation;
    }

    public synchronized boolean hasClipboardContent() {
        return !clipboardPaths.isEmpty() && clipboardOperation != ClipboardOperation.NONE;
    }
}
