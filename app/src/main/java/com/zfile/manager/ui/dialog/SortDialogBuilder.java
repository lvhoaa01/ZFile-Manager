package com.zfile.manager.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.zfile.manager.R;
import com.zfile.manager.model.SortCriteria;

/**
 * Builder wrapper that produces a single-choice sort picker. The dialog reads
 * the seven entries from {@code @array/sort_options} (kept in sync with
 * {@link SortCriteria#values()}) and invokes the listener once a row is chosen.
 */
public class SortDialogBuilder {

    public interface OnSortPicked {
        void onPick(@NonNull SortCriteria criteria);
    }

    private static final SortCriteria[] OPTIONS = {
            SortCriteria.NAME_ASC,
            SortCriteria.NAME_DESC,
            SortCriteria.SIZE_ASC,
            SortCriteria.SIZE_DESC,
            SortCriteria.DATE_ASC,
            SortCriteria.DATE_DESC,
            SortCriteria.TYPE,
    };

    @NonNull private final Context context;
    @NonNull private SortCriteria current = SortCriteria.NAME_ASC;
    @Nullable private OnSortPicked listener;

    public SortDialogBuilder(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    public SortDialogBuilder setCurrent(@NonNull SortCriteria criteria) {
        this.current = criteria;
        return this;
    }

    @NonNull
    public SortDialogBuilder setOnSortPicked(@NonNull OnSortPicked listener) {
        this.listener = listener;
        return this;
    }

    @NonNull
    public AlertDialog build() {
        String[] labels = new String[OPTIONS.length];
        int selectedIndex = 0;
        for (int i = 0; i < OPTIONS.length; i++) {
            labels[i] = context.getString(labelResFor(OPTIONS[i]));
            if (OPTIONS[i] == current) selectedIndex = i;
        }

        return new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.action_sort)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> {
                    if (listener != null && which >= 0 && which < OPTIONS.length) {
                        listener.onPick(OPTIONS[which]);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();
    }

    private static int labelResFor(@NonNull SortCriteria c) {
        switch (c) {
            case NAME_DESC: return R.string.sort_name_desc;
            case SIZE_ASC:  return R.string.sort_size_asc;
            case SIZE_DESC: return R.string.sort_size_desc;
            case DATE_ASC:  return R.string.sort_date_asc;
            case DATE_DESC: return R.string.sort_date_desc;
            case TYPE:      return R.string.sort_type;
            case NAME_ASC:
            default:        return R.string.sort_name_asc;
        }
    }
}
