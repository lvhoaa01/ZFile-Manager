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

import com.google.android.material.button.MaterialButton;
import com.zfile.manager.R;
import com.zfile.manager.ui.adapter.CategoryAdapter;
import com.zfile.manager.util.PermissionHelper;
import com.zfile.manager.viewmodel.CategoryViewModel;

/**
 * Categories tab — 2-column grid of {@link com.zfile.manager.model.CategoryType}
 * cards. Tapping a card navigates to {@link CategoryDetailFragment} with the
 * category name as a nav-graph argument.
 *
 * <p>Falls back to a permission-empty-state when the app lacks storage access,
 * since MediaStore queries silently return 0 rows in that case which would look
 * like "every category is empty".</p>
 */
public class CategoryFragment extends Fragment {

    private static final int REQUEST_PERMISSION = 2001;

    private CategoryViewModel viewModel;
    private CategoryAdapter adapter;
    private RecyclerView grid;
    private View permissionGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        grid = view.findViewById(R.id.categoryGrid);
        permissionGroup = view.findViewById(R.id.categoryPermissionGroup);
        MaterialButton grantButton = view.findViewById(R.id.categoryGrantAccessButton);

        grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new CategoryAdapter();
        grid.setAdapter(adapter);
        adapter.setOnCategoryClickListener(type -> {
            NavController nav = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putString(CategoryDetailFragment.ARG_CATEGORY, type.name());
            nav.navigate(R.id.action_categories_to_categoryDetail, args);
        });

        grantButton.setOnClickListener(v ->
                PermissionHelper.requestFullAccess(requireActivity(), REQUEST_PERMISSION));

        viewModel.getCounts().observe(getViewLifecycleOwner(), counts -> {
            if (counts != null) adapter.setCounts(counts);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean hasAccess = PermissionHelper.hasFullAccess(requireContext());
        permissionGroup.setVisibility(hasAccess ? View.GONE : View.VISIBLE);
        grid.setVisibility(hasAccess ? View.VISIBLE : View.GONE);
        if (hasAccess) {
            viewModel.refreshCounts();
        }
    }
}
