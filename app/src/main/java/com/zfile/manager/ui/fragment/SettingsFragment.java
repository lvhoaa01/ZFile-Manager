package com.zfile.manager.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.zfile.manager.R;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.viewmodel.SettingsViewModel;

import java.util.Arrays;

/**
 * Custom settings screen (no androidx.preference). Reads current values from
 * {@link SettingsViewModel} on bind and writes each change straight through —
 * theme changes apply immediately, the rest take effect on the next browser refresh.
 */
public class SettingsFragment extends Fragment {

    /** Sort options shown in the dropdown, parallel to {@link #SORT_LABEL_RES}. */
    private static final SortCriteria[] SORT_VALUES = {
            SortCriteria.NAME_ASC, SortCriteria.NAME_DESC,
            SortCriteria.SIZE_ASC, SortCriteria.SIZE_DESC,
            SortCriteria.DATE_ASC, SortCriteria.DATE_DESC,
            SortCriteria.TYPE
    };
    private static final int[] SORT_LABEL_RES = {
            R.string.sort_name_asc, R.string.sort_name_desc,
            R.string.sort_size_asc, R.string.sort_size_desc,
            R.string.sort_date_asc, R.string.sort_date_desc,
            R.string.sort_type
    };

    /** Trash purge options in days; 0 means "Never". Parallel to {@link #PURGE_LABEL_RES}. */
    private static final int[] PURGE_DAYS = {7, 30, 90, 0};
    private static final int[] PURGE_LABEL_RES = {
            R.string.settings_purge_7, R.string.settings_purge_30,
            R.string.settings_purge_90, R.string.settings_purge_never
    };

    private SettingsViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindTheme(view);
        bindSort(view);
        bindSwitches(view);
        bindPurge(view);
    }

    private void bindTheme(@NonNull View view) {
        MaterialRadioButton system = view.findViewById(R.id.themeSystem);
        MaterialRadioButton light = view.findViewById(R.id.themeLight);
        MaterialRadioButton dark = view.findViewById(R.id.themeDark);

        switch (viewModel.getThemeMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:  light.setChecked(true); break;
            case AppCompatDelegate.MODE_NIGHT_YES: dark.setChecked(true); break;
            default:                               system.setChecked(true); break;
        }

        ((android.widget.RadioGroup) view.findViewById(R.id.themeRadioGroup))
                .setOnCheckedChangeListener((group, checkedId) -> {
                    int mode;
                    if (checkedId == R.id.themeLight) {
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                    } else if (checkedId == R.id.themeDark) {
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                    } else {
                        mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                    if (mode != viewModel.getThemeMode()) {
                        viewModel.setThemeMode(mode);
                    }
                });
    }

    private void bindSort(@NonNull View view) {
        MaterialAutoCompleteTextView dropdown = view.findViewById(R.id.sortDropdown);
        String[] labels = new String[SORT_VALUES.length];
        for (int i = 0; i < labels.length; i++) labels[i] = getString(SORT_LABEL_RES[i]);
        dropdown.setAdapter(nonFilteringAdapter(requireContext(), labels));

        int current = indexOf(SORT_VALUES, viewModel.getSortCriteria());
        dropdown.setText(labels[current], false);
        dropdown.setOnItemClickListener((parent, v, position, id) ->
                viewModel.setSortCriteria(SORT_VALUES[position]));
    }

    private void bindSwitches(@NonNull View view) {
        MaterialSwitch foldersFirst = view.findViewById(R.id.foldersFirstSwitch);
        foldersFirst.setChecked(viewModel.isFoldersFirst());
        foldersFirst.setOnCheckedChangeListener((b, checked) -> viewModel.setFoldersFirst(checked));

        MaterialSwitch showHidden = view.findViewById(R.id.showHiddenSwitch);
        showHidden.setChecked(viewModel.isShowHidden());
        showHidden.setOnCheckedChangeListener((b, checked) -> viewModel.setShowHidden(checked));
    }

    private void bindPurge(@NonNull View view) {
        MaterialAutoCompleteTextView dropdown = view.findViewById(R.id.purgeDropdown);
        String[] labels = new String[PURGE_DAYS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = getString(PURGE_LABEL_RES[i]);
        dropdown.setAdapter(nonFilteringAdapter(requireContext(), labels));

        int current = indexOfInt(PURGE_DAYS, viewModel.getTrashPurgeDays());
        if (current < 0) current = 1; // default to 30 days if stored value isn't an offered option
        dropdown.setText(labels[current], false);
        dropdown.setOnItemClickListener((parent, v, position, id) ->
                viewModel.setTrashPurgeDays(PURGE_DAYS[position]));
    }

    /**
     * Adapter for the exposed-dropdown fields whose {@link Filter} never narrows the
     * list. The default {@code ArrayAdapter}/{@code setSimpleItems} filter treats the
     * pre-set selection text as a constraint, so re-opening the menu would otherwise
     * show only the current item.
     */
    @NonNull
    private static ArrayAdapter<String> nonFilteringAdapter(@NonNull Context ctx, @NonNull String[] items) {
        return new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, items) {
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults r = new FilterResults();
                        r.values = Arrays.asList(items);
                        r.count = items.length;
                        return r;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };
    }

    private static int indexOf(@NonNull SortCriteria[] arr, @NonNull SortCriteria value) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == value) return i;
        return 0;
    }

    private static int indexOfInt(@NonNull int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == value) return i;
        return -1;
    }
}
