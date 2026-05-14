package com.zfile.manager.model.decorator;

import androidx.annotation.NonNull;

/**
 * Appends a bracketed marker (e.g. {@code [SYSTEM]}, {@code [HIDDEN]}) to the wrapped
 * tag. Stacks multiple times — each application adds one more bracket group.
 */
public class SystemTagDecorator extends FileItemDecorator {

    @NonNull private final String marker;

    public SystemTagDecorator(@NonNull FileItemComponent wrapped, @NonNull String marker) {
        super(wrapped);
        this.marker = marker;
    }

    @NonNull
    @Override
    public String getTag() {
        String existing = wrapped.getTag();
        String formatted = "[" + marker + "]";
        if (existing.isEmpty()) return formatted;
        return existing + " " + formatted;
    }
}
