package com.zfile.manager.model.decorator;

import androidx.annotation.NonNull;

import com.zfile.manager.model.FileItem;

/**
 * Concrete Component — the unwrapped, undecorated baseline.
 * Returns {@code 0} for icon / tint so callers can detect "no decoration applied".
 */
public class BaseFileItem implements FileItemComponent {

    @NonNull private final FileItem fileItem;

    public BaseFileItem(@NonNull FileItem fileItem) {
        this.fileItem = fileItem;
    }

    @NonNull
    @Override
    public String getName() {
        return fileItem.getName();
    }

    @NonNull
    @Override
    public String getPath() {
        return fileItem.getPath();
    }

    @Override
    public int getIconResource() {
        return 0;
    }

    @Override
    public int getTintColor() {
        return 0;
    }

    @NonNull
    @Override
    public String getTag() {
        return "";
    }

    @NonNull
    @Override
    public FileItem getFileItem() {
        return fileItem;
    }
}
