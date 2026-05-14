package com.zfile.manager.model.decorator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Decorator that overrides only the tint color applied to the icon.
 * Commonly stacked on top of an {@link IconDecorator}.
 */
public class ColorDecorator extends FileItemDecorator {

    @ColorInt private final int tintColor;

    public ColorDecorator(@NonNull FileItemComponent wrapped, @ColorInt int tintColor) {
        super(wrapped);
        this.tintColor = tintColor;
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }
}
