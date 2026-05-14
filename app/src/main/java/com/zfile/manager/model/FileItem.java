package com.zfile.manager.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * Immutable description of a file or directory on disk, plus a transient
 * {@code selected} flag used by multi-select UI. All file-system mutations
 * happen elsewhere — this is a pure value object.
 */
public final class FileItem {

    @NonNull private final String name;
    @NonNull private final String path;
    private final long size;
    private final long lastModified;
    private final boolean directory;
    private final boolean hidden;
    private final boolean readable;
    private final boolean writable;
    @NonNull private final FileType fileType;
    @Nullable private final String mimeType;

    private boolean selected;

    public FileItem(@NonNull String name,
                    @NonNull String path,
                    long size,
                    long lastModified,
                    boolean directory,
                    boolean hidden,
                    boolean readable,
                    boolean writable,
                    @NonNull FileType fileType,
                    @Nullable String mimeType) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.directory = directory;
        this.hidden = hidden;
        this.readable = readable;
        this.writable = writable;
        this.fileType = fileType;
        this.mimeType = mimeType;
    }

    @NonNull
    public static FileItem fromFile(@NonNull File file,
                                    @NonNull FileType type,
                                    @Nullable String mimeType) {
        return new FileItem(
                file.getName(),
                file.getAbsolutePath(),
                file.isDirectory() ? 0L : file.length(),
                file.lastModified(),
                file.isDirectory(),
                file.isHidden(),
                file.canRead(),
                file.canWrite(),
                type,
                mimeType
        );
    }

    @NonNull public String getName() { return name; }
    @NonNull public String getPath() { return path; }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }
    public boolean isDirectory() { return directory; }
    public boolean isHidden() { return hidden; }
    public boolean isReadable() { return readable; }
    public boolean isWritable() { return writable; }
    @NonNull public FileType getFileType() { return fileType; }
    @Nullable public String getMimeType() { return mimeType; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    @Nullable
    public String getParentPath() {
        File f = new File(path);
        File parent = f.getParentFile();
        return parent == null ? null : parent.getAbsolutePath();
    }

    @Nullable
    public String getExtension() {
        if (directory) return null;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return null;
        return name.substring(dot + 1);
    }

    @NonNull
    public File asFile() {
        return new File(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileItem)) return false;
        FileItem other = (FileItem) o;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @NonNull
    @Override
    public String toString() {
        return "FileItem{" + path + ", type=" + fileType + ", size=" + size + '}';
    }
}
