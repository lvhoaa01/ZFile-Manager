package com.zfile.manager.model.decorator;

import androidx.annotation.NonNull;

import com.zfile.manager.model.FileItem;

/**
 * Abstract Decorator — forwards every call to the wrapped component by default.
 * Subclasses override only the methods whose presentation they want to change.
 */
public abstract class FileItemDecorator implements FileItemComponent {

    @NonNull protected final FileItemComponent wrapped;

    protected FileItemDecorator(@NonNull FileItemComponent wrapped) {
        this.wrapped = wrapped;
    }

    @NonNull
    @Override
    public String getName() {
        return wrapped.getName();
    }

    @NonNull
    @Override
    public String getPath() {
        return wrapped.getPath();
    }

    @Override
    public int getIconResource() {
        return wrapped.getIconResource();
    }

    @Override
    public int getTintColor() {
        return wrapped.getTintColor();
    }

    @NonNull
    @Override
    public String getTag() {
        return wrapped.getTag();
    }

    @NonNull
    @Override
    public FileItem getFileItem() {
        return wrapped.getFileItem();
    }
}
