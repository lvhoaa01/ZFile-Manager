package com.zfile.manager.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zfile.manager.R;
import com.zfile.manager.service.FileTransferService;

/**
 * Builder for the "Compress to .zip" dialog. Accepts a default archive name
 * (typically the first source's name + .zip) and validates the user-edited
 * value through {@link FileTransferService#isValidFilename}. Auto-appends
 * {@code .zip} if missing.
 */
public class ZipDialogBuilder {

    public interface OnCompress {
        void run(@NonNull String archiveName);
    }

    @NonNull private final Context context;
    @NonNull private String defaultName = "archive.zip";
    @Nullable private OnCompress onCompress;

    public ZipDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public ZipDialogBuilder setDefaultName(@NonNull String name) {
        this.defaultName = name.endsWith(".zip") ? name : name + ".zip";
        return this;
    }

    @NonNull
    public ZipDialogBuilder setOnCompress(@NonNull OnCompress onCompress) {
        this.onCompress = onCompress;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_zip, null, false);
        TextInputLayout layout = view.findViewById(R.id.zipNameInputLayout);
        TextInputEditText input = view.findViewById(R.id.zipNameInput);
        input.setText(defaultName);
        input.setSelection(0, defaultName.length() - 4);  // select base name (no .zip)

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.compress_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_create, null)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CharSequence text = input.getText();
            String name = text == null ? "" : text.toString().trim();
            if (!name.toLowerCase().endsWith(".zip")) name = name + ".zip";
            if (!FileTransferService.isValidFilename(name)) {
                layout.setError(context.getString(R.string.error_invalid_name));
                return;
            }
            layout.setError(null);
            if (onCompress != null) onCompress.run(name);
            dialog.dismiss();
        }));
        return dialog;
    }
}
