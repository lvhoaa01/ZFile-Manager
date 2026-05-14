package com.zfile.manager.model.decorator;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.zfile.manager.model.FileItem;

/**
 * Decorator-pattern root: a view-facing description of a {@link FileItem}
 * with the visual attributes the UI needs (icon, tint, tag text).
 *
 * <p>Concrete components return defaults; decorators layer on additional
 * presentation without mutating the underlying {@link FileItem}.</p>
 */
public interface FileItemComponent {

    @NonNull
    String getName();

    @NonNull
    String getPath();

    @DrawableRes
    int getIconResource();

    @ColorInt
    int getTintColor();

    @NonNull
    String getTag();

    @NonNull
    FileItem getFileItem();
}
