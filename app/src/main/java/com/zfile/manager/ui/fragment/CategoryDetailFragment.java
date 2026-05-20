package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.zfile.manager.R;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.ui.adapter.FileListAdapter;
import com.zfile.manager.util.IntentHelper;
import com.zfile.manager.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Detail screen for one category — reuses {@link FileListAdapter} to render the
 * MediaStore-sourced list and forwards taps to {@link IntentHelper#openWith} so
 * files open in the user's preferred app via the FileProvider authority.
 */
public class CategoryDetailFragment extends Fragment {

    public static final String ARG_CATEGORY = "category";

    private CategoryViewModel viewModel;
    private FileListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;

    @Nullable
    private CategoryType category;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        Bundle args = getArguments();
        String name = args == null ? null : args.getString(ARG_CATEGORY);
        category = parseCategory(name);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        RecyclerView recycler = view.findViewById(R.id.categoryRecycler);
        emptyView = view.findViewById(R.id.emptyText);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FileListAdapter();
        recycler.setAdapter(adapter);

        adapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull FileItemComponent item, int position) {
                FileItem fi = item.getFileItem();
                if (!fi.isDirectory()) {
                    IntentHelper.openWith(requireContext(), fi.asFile());
                }
            }

            @Override
            public void onItemLongClick(@NonNull FileItemComponent item, int position) {
                IntentHelper.share(requireContext(), item.getFileItem().asFile());
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            if (category != null) viewModel.loadCategory(category);
        });

        viewModel.getItems().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(new ArrayList<>(list));
            updateEmpty(list);
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(),
                loading -> swipeRefresh.setRefreshing(loading != null && loading));

        if (category != null) {
            requireActivity().setTitle(localizedTitle(category));
            viewModel.loadCategory(category);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.clear();
    }

    private void updateEmpty(@Nullable List<FileItemComponent> list) {
        emptyView.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private CharSequence localizedTitle(@NonNull CategoryType type) {
        int id = getResources().getIdentifier(
                "category_" + type.getResourceSuffix(),
                "string",
                requireContext().getPackageName());
        return id != 0 ? getString(id) : type.name();
    }

    @Nullable
    private static CategoryType parseCategory(@Nullable String name) {
        if (name == null) return null;
        try {
            return CategoryType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
