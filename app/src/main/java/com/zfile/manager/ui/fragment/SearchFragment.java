package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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
import com.zfile.manager.viewmodel.FileBrowserViewModel;
import com.zfile.manager.viewmodel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Search tab — recursive name search rooted at the current browse path (or
 * storage root if the browser hasn't navigated yet).
 *
 * <p>Keystrokes are debounced ({@value #DEBOUNCE_MS} ms) so a fast typer doesn't
 * spin up a tree walk per character. The viewmodel is fragment-scoped — search
 * state is intentionally not shared across tab switches.</p>
 */
public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 300L;

    private SearchViewModel viewModel;
    private FileListAdapter adapter;
    private EditText queryInput;
    private LinearProgressIndicator progress;
    private TextView emptyView;

    @NonNull private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    @Nullable private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Fragment-scoped: search state shouldn't outlive the tab.
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        // Sync the search root to whatever folder the browser is currently on so
        // "Search" works in the user's mental context, not always from root.
        FileBrowserViewModel browserVm =
                new ViewModelProvider(requireActivity()).get(FileBrowserViewModel.class);
        viewModel.setSearchRoot(browserVm.getCurrentPath().getValue());

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
                schedule(s.toString());
            }
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(new ArrayList<>(list));
            updateEmpty(list);
        });
        viewModel.getIsSearching().observe(getViewLifecycleOwner(), searching -> {
            progress.setVisibility(Boolean.TRUE.equals(searching) ? View.VISIBLE : View.GONE);
            // Re-evaluate empty state when the search transitions out of "searching" but
            // no new results arrived (no-match case).
            updateEmpty(viewModel.getResults().getValue());
        });
    }

    @Override
    public void onDestroyView() {
        if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
        viewModel.cancel();
        super.onDestroyView();
    }

    private void schedule(@NonNull String query) {
        if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
        pendingSearch = () -> viewModel.submitQuery(query);
        debounceHandler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void updateEmpty(@Nullable List<FileItemComponent> list) {
        String q = viewModel.getCurrentQuery().getValue();
        boolean noQuery = q == null || q.isEmpty();
        boolean noResults = list == null || list.isEmpty();
        boolean searching = Boolean.TRUE.equals(viewModel.getIsSearching().getValue());

        if (noQuery) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.search_prompt);
        } else if (noResults && !searching) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.search_empty);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
}
