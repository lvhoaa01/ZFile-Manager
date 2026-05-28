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
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.R;
import com.zfile.manager.model.Bookmark;
import com.zfile.manager.ui.adapter.BookmarkAdapter;
import com.zfile.manager.viewmodel.BookmarkViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists saved folder bookmarks. Tapping a row deep-links into the file browser
 * at that path (reusing the {@code "path"} nav argument); the trailing delete
 * button removes the bookmark.
 */
public class BookmarksFragment extends Fragment {

    private BookmarkViewModel viewModel;
    private BookmarkAdapter adapter;
    private RecyclerView recycler;
    private View emptyGroup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookmarkViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmarks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recycler = view.findViewById(R.id.bookmarkRecycler);
        emptyGroup = view.findViewById(R.id.emptyGroup);

        adapter = new BookmarkAdapter();
        recycler.setAdapter(adapter);
        adapter.setListener(new BookmarkAdapter.OnBookmarkListener() {
            @Override
            public void onOpen(@NonNull Bookmark bookmark) {
                NavController nav = NavHostFragment.findNavController(BookmarksFragment.this);
                Bundle args = new Bundle();
                args.putString("path", bookmark.getPath());
                nav.navigate(R.id.fileBrowserFragment, args);
            }

            @Override
            public void onDelete(@NonNull Bookmark bookmark) {
                viewModel.remove(bookmark.getPath());
            }
        });

        observeViewModel();
        viewModel.load();
    }

    private void observeViewModel() {
        viewModel.getBookmarks().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list == null ? new ArrayList<>() : new ArrayList<>(list));
            updateEmptyState(list);
        });
    }

    private void updateEmptyState(@Nullable List<Bookmark> list) {
        emptyGroup.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
