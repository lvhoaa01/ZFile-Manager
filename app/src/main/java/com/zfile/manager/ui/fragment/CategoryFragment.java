package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.R;
import com.zfile.manager.ui.adapter.CategoryAdapter;
import com.zfile.manager.util.PermissionHelper;
import com.zfile.manager.viewmodel.CategoryViewModel;

/**
 * Categories tab — shows a 2-column grid of {@link com.zfile.manager.model.CategoryType}
 * cards. Tapping a card navigates to {@link CategoryDetailFragment} with the
 * category name as a nav-graph argument.
 */
public class CategoryFragment extends Fragment {

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        RecyclerView grid = view.findViewById(R.id.categoryGrid);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new CategoryAdapter();
        grid.setAdapter(adapter);
        adapter.setOnCategoryClickListener(type -> {
            NavController nav = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putString(CategoryDetailFragment.ARG_CATEGORY, type.name());
            nav.navigate(R.id.action_categories_to_categoryDetail, args);
        });

        viewModel.getCounts().observe(getViewLifecycleOwner(), counts -> {
            if (counts != null) adapter.setCounts(counts);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionHelper.hasFullAccess(requireContext())) {
            viewModel.refreshCounts();
        }
    }
}
