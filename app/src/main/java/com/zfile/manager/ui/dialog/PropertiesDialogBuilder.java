package com.zfile.manager.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zfile.manager.R;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.util.FileUtils;

/**
 * Builder for the read-only file-properties dialog. Pulls name/path/size/etc.
 * from a {@link FileItem}, formatted via {@link FileUtils}.
 */
public class PropertiesDialogBuilder {

    @NonNull private final Context context;
    @Nullable private FileItem item;

    public PropertiesDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public PropertiesDialogBuilder setFileItem(@NonNull FileItem item) {
        this.item = item;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_properties, null, false);
        if (item != null) {
            ((TextView) view.findViewById(R.id.propName)).setText(item.getName());
            ((TextView) view.findViewById(R.id.propPath)).setText(item.getPath());
            ((TextView) view.findViewById(R.id.propType)).setText(item.getFileType().name());
            ((TextView) view.findViewById(R.id.propSize)).setText(
                    item.isDirectory() ? "—" : FileUtils.formatSize(item.getSize()));
            ((TextView) view.findViewById(R.id.propModified)).setText(
                    FileUtils.formatDate(item.getLastModified()));
            ((TextView) view.findViewById(R.id.propPermissions)).setText(
                    FileUtils.formatPermissions(item.isReadable(), item.isWritable()));
        }
        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_properties_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_ok, null)
                .create();
    }
}
