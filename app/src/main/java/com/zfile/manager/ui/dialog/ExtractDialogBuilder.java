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
import com.zfile.manager.model.ArchiveEntry;

import java.util.List;

/**
 * Builder for the "Extract archive" confirmation dialog. Shows the destination
 * directory plus a small preview of the archive's first entries so the user
 * knows what's about to land on disk.
 */
public class ExtractDialogBuilder {

    public interface OnExtract {
        void run();
    }

    private static final int PREVIEW_LIMIT = 10;

    @NonNull private final Context context;
    @NonNull private String destPath = "";
    @NonNull private List<ArchiveEntry> previewEntries = java.util.Collections.emptyList();
    private int totalEntries = 0;
    @Nullable private OnExtract onExtract;

    public ExtractDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public ExtractDialogBuilder setDestPath(@NonNull String path) {
        this.destPath = path;
        return this;
    }

    @NonNull
    public ExtractDialogBuilder setPreview(@NonNull List<ArchiveEntry> entries, int total) {
        this.previewEntries = entries;
        this.totalEntries = total;
        return this;
    }

    @NonNull
    public ExtractDialogBuilder setOnExtract(@NonNull OnExtract onExtract) {
        this.onExtract = onExtract;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_extract, null, false);
        TextView destView = view.findViewById(R.id.extractDest);
        TextView previewView = view.findViewById(R.id.extractPreview);

        destView.setText(context.getString(R.string.extract_to, destPath));

        StringBuilder sb = new StringBuilder();
        int shown = Math.min(previewEntries.size(), PREVIEW_LIMIT);
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append('\n');
            ArchiveEntry e = previewEntries.get(i);
            sb.append(e.getName());
        }
        if (totalEntries > shown) {
            sb.append('\n').append(context.getString(R.string.extract_preview_x_more,
                    totalEntries - shown));
        }
        previewView.setText(sb.toString());

        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.extract_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_button_extract, (d, w) -> {
                    if (onExtract != null) onExtract.run();
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();
    }
}
