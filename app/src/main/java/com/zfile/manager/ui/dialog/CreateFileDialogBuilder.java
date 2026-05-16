package com.zfile.manager.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.zfile.manager.R;

/**
 * Builder for the "Create new file or folder" dialog. Lets the user pick a kind
 * (folder/file) and a name, with empty-name validation.
 */
public class CreateFileDialogBuilder {

    public interface OnCreate {
        void run(@NonNull String name, boolean isFolder);
    }

    @NonNull private final Context context;
    @Nullable private OnCreate onCreate;
    private boolean defaultFolder = true;

    public CreateFileDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public CreateFileDialogBuilder setDefaultFolder(boolean folder) {
        this.defaultFolder = folder;
        return this;
    }

    @NonNull
    public CreateFileDialogBuilder setOnCreate(@NonNull OnCreate onCreate) {
        this.onCreate = onCreate;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_create_file, null, false);
        RadioGroup radio = view.findViewById(R.id.radioType);
        TextInputLayout layout = view.findViewById(R.id.nameInputLayout);
        TextInputEditText input = view.findViewById(R.id.nameInput);

        radio.check(defaultFolder ? R.id.radioFolder : R.id.radioFile);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_create_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_create, null)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CharSequence text = input.getText();
            String name = text == null ? "" : text.toString().trim();
            if (name.isEmpty()) {
                layout.setError(context.getString(R.string.error_invalid_name));
                return;
            }
            layout.setError(null);
            boolean isFolder = radio.getCheckedRadioButtonId() == R.id.radioFolder;
            if (onCreate != null) onCreate.run(name, isFolder);
            dialog.dismiss();
        }));
        return dialog;
    }
}
