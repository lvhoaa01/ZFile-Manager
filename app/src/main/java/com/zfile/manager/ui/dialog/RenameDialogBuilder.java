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

/**
 * Builder for the "Rename" dialog. Pre-fills the current name and pre-selects
 * the basename (everything before the extension dot) so typing replaces only the
 * editable portion — Android Files-style.
 */
public class RenameDialogBuilder {

    public interface OnRename {
        void run(@NonNull String newName);
    }

    @NonNull private final Context context;
    @NonNull private String currentName = "";
    @Nullable private OnRename onRename;

    public RenameDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public RenameDialogBuilder setCurrentName(@NonNull String name) {
        this.currentName = name;
        return this;
    }

    @NonNull
    public RenameDialogBuilder setOnRename(@NonNull OnRename onRename) {
        this.onRename = onRename;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null, false);
        TextInputLayout layout = view.findViewById(R.id.renameInputLayout);
        TextInputEditText input = view.findViewById(R.id.renameInput);

        input.setText(currentName);
        int dot = currentName.lastIndexOf('.');
        if (dot > 0) {
            input.setSelection(0, dot);
        } else {
            input.setSelection(currentName.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_rename_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_rename, null)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CharSequence text = input.getText();
            String name = text == null ? "" : text.toString().trim();
            if (name.isEmpty()) {
                layout.setError(context.getString(R.string.error_invalid_name));
                return;
            }
            if (name.equals(currentName)) {
                dialog.dismiss();
                return;
            }
            if (onRename != null) onRename.run(name);
            dialog.dismiss();
        }));
        return dialog;
    }
}
