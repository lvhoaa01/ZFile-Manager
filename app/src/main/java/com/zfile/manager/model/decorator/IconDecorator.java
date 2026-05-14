package com.zfile.manager.model.decorator;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * Decorator that overrides only the icon drawable resource. Use to attach
 * a type-specific icon (folder, image, video, etc.) without touching name/path.
 */
public class IconDecorator extends FileItemDecorator {

    @DrawableRes private final int iconRes;

    public IconDecorator(@NonNull FileItemComponent wrapped, @DrawableRes int iconRes) {
        super(wrapped);
        this.iconRes = iconRes;
    }

    @Override
    public int getIconResource() {
        return iconRes;
    }
}
