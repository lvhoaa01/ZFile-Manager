package com.zfile.manager.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zfile.manager.R;

/**
 * Builder pattern wrapper around {@link MaterialAlertDialogBuilder} for the
 * common "Title / Message / Positive button → action" confirmation dialog.
 *
 * <p>Each setter returns {@code this} for chaining; {@link #build()} returns
 * a configured but un-shown {@link AlertDialog}.</p>
 */
public class ConfirmDialogBuilder {

    public interface OnConfirm {
        void run();
    }

    @NonNull private final Context context;
    @Nullable private CharSequence title;
    @Nullable private CharSequence message;
    @Nullable private CharSequence positiveText;
    @Nullable private CharSequence negativeText;
    @Nullable private OnConfirm onConfirm;
    private boolean cancelable = true;

    public ConfirmDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public ConfirmDialogBuilder setTitle(@Nullable CharSequence title) {
        this.title = title;
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setTitle(@StringRes int resId) {
        this.title = context.getString(resId);
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setMessage(@Nullable CharSequence message) {
        this.message = message;
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setMessage(@StringRes int resId, Object... formatArgs) {
        this.message = context.getString(resId, formatArgs);
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setPositiveText(@Nullable CharSequence text) {
        this.positiveText = text;
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setPositiveText(@StringRes int resId) {
        this.positiveText = context.getString(resId);
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setNegativeText(@StringRes int resId) {
        this.negativeText = context.getString(resId);
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setOnConfirm(@NonNull OnConfirm onConfirm) {
        this.onConfirm = onConfirm;
        return this;
    }

    @NonNull
    public ConfirmDialogBuilder setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(context)
                .setCancelable(cancelable);
        if (title != null) b.setTitle(title);
        if (message != null) b.setMessage(message);

        CharSequence pos = positiveText != null ? positiveText : context.getString(R.string.dialog_button_ok);
        CharSequence neg = negativeText != null ? negativeText : context.getString(R.string.dialog_button_cancel);

        b.setPositiveButton(pos, (d, w) -> {
            if (onConfirm != null) onConfirm.run();
        });
        b.setNegativeButton(neg, null);
        return b.create();
    }
}
