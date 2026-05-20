package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.zfile.manager.R;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.ui.adapter.FileListAdapter;
import com.zfile.manager.util.IntentHelper;
import com.zfile.manager.viewmodel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Search tab — recursive name search rooted at the current storage volume.
 * Each keystroke {@link SearchViewModel#submitQuery} which cancels the in-flight
 * walk via the shared {@code AtomicBoolean} and queues the next on the search
 * executor (queue is drained per submission so only the latest query runs).
 */
public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private FileListAdapter adapter;
    private EditText queryInput;
    private LinearProgressIndicator progress;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        queryInput = view.findViewById(R.id.searchInput);
        progress = view.findViewById(R.id.searchProgress);
        emptyView = view.findViewById(R.id.searchEmpty);
        RecyclerView recycler = view.findViewById(R.id.searchRecycler);

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
                // No-op for search results (no multi-select in this stage).
            }
        });

        queryInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.submitQuery(s.toString());
            }
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(new ArrayList<>(list));
            updateEmpty(list);
        });
        viewModel.getIsSearching().observe(getViewLifecycleOwner(), searching -> {
            progress.setVisibility(Boolean.TRUE.equals(searching) ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.cancel();
    }

    private void updateEmpty(@Nullable List<FileItemComponent> list) {
        String q = viewModel.getCurrentQuery().getValue();
        boolean noQuery = q == null || q.isEmpty();
        boolean noResults = list == null || list.isEmpty();
        if (noQuery) {
            emptyView.setVisibility(View.VISIBLE);
            ((android.widget.TextView) emptyView).setText(R.string.search_prompt);
        } else if (noResults && Boolean.FALSE.equals(viewModel.getIsSearching().getValue())) {
            emptyView.setVisibility(View.VISIBLE);
            ((android.widget.TextView) emptyView).setText(R.string.search_empty);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
}
