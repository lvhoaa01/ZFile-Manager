package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.zfile.manager.R;
import com.zfile.manager.model.TransferProgress;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.ui.adapter.FileListAdapter;
import com.zfile.manager.ui.dialog.ConfirmDialogBuilder;
import com.zfile.manager.ui.dialog.SortDialogBuilder;
import com.zfile.manager.viewmodel.TrashViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Recycle Bin screen. Mirrors {@link FileBrowserFragment}'s observe-and-bind
 * shape but routes everything through {@link TrashViewModel} / {@link com.zfile.manager.service.RecycleBinService}.
 *
 * <p>Reuses {@link FileListAdapter} — entries are exposed as synthetic
 * {@link com.zfile.manager.model.FileItem}s whose {@code path} field carries the
 * trash entry id, used as the selection key.</p>
 */
public class TrashFragment extends Fragment {

    private TrashViewModel viewModel;
    private FileListAdapter adapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recycler;
    private View emptyGroup;
    private LinearProgressIndicator progressIndicator;

    @Nullable private ActionMode actionMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TrashViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recycler = view.findViewById(R.id.trashRecycler);
        emptyGroup = view.findViewById(R.id.emptyGroup);
        progressIndicator = view.findViewById(R.id.transferProgress);

        adapter = new FileListAdapter();
        recycler.setAdapter(adapter);
        adapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull FileItemComponent item, int position) {
                if (actionMode == null) startActionMode();
                viewModel.toggleSelection(item.getPath());
            }

            @Override
            public void onItemLongClick(@NonNull FileItemComponent item, int position) {
                if (actionMode == null) startActionMode();
                viewModel.toggleSelection(item.getPath());
            }
        });

        swipeRefresh.setOnRefreshListener(() -> viewModel.loadTrash());

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        observeViewModel();
        viewModel.loadTrash();
    }

    @Override
    public void onDestroyView() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        super.onDestroyView();
    }

    private void observeViewModel() {
        viewModel.getEntries().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(new ArrayList<>(list));
            updateEmptyState(list);
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(),
                loading -> swipeRefresh.setRefreshing(loading != null && loading));
        viewModel.getSelectedIds().observe(getViewLifecycleOwner(), selected -> {
            adapter.setSelectedPaths(selected != null ? selected : Collections.emptySet());
            updateActionModeTitle();
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty() && getView() != null) {
                Snackbar.make(getView(), msg, Snackbar.LENGTH_SHORT).show();
                viewModel.clearErrorMessage();
            }
        });
        viewModel.getTransferProgress().observe(getViewLifecycleOwner(), this::bindTransferProgress);
    }

    private void updateEmptyState(@Nullable List<FileItemComponent> list) {
        if (list == null || list.isEmpty()) {
            emptyGroup.setVisibility(View.VISIBLE);
        } else {
            emptyGroup.setVisibility(View.GONE);
        }
    }

    private void bindTransferProgress(@Nullable TransferProgress p) {
        if (p == null) {
            progressIndicator.setVisibility(View.GONE);
            return;
        }
        switch (p.getStatus()) {
            case PENDING:
                progressIndicator.setIndeterminate(true);
                progressIndicator.setVisibility(View.VISIBLE);
                break;
            case IN_PROGRESS:
                progressIndicator.setIndeterminate(false);
                progressIndicator.setProgress(p.getPercentage());
                progressIndicator.setVisibility(View.VISIBLE);
                break;
            case COMPLETED:
                progressIndicator.setVisibility(View.GONE);
                if (getView() != null) {
                    int msgRes = p.getOperation() == TransferProgress.Operation.RESTORE
                            ? R.string.trash_restore_completed
                            : R.string.transfer_completed;
                    Snackbar.make(getView(), msgRes, Snackbar.LENGTH_SHORT).show();
                }
                viewModel.clearTransferProgress();
                break;
            case FAILED:
                progressIndicator.setVisibility(View.GONE);
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.transfer_failed,
                                    p.getErrorMessage() != null ? p.getErrorMessage() : "?"),
                            Snackbar.LENGTH_LONG).show();
                }
                viewModel.clearTransferProgress();
                break;
            case CANCELLED:
                progressIndicator.setVisibility(View.GONE);
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.transfer_cancelled, Snackbar.LENGTH_SHORT).show();
                }
                viewModel.clearTransferProgress();
                break;
        }
    }

    private void startActionMode() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        actionMode = activity.startSupportActionMode(actionModeCallback);
        adapter.setSelectionMode(true);
    }

    private void updateActionModeTitle() {
        if (actionMode == null) return;
        Set<String> sel = viewModel.getSelectedIds().getValue();
        int n = sel == null ? 0 : sel.size();
        if (n == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(getString(R.string.selected_count, n));
        }
    }

    private void confirmEmptyAll() {
        new ConfirmDialogBuilder(requireContext())
                .setTitle(R.string.trash_empty_all_title)
                .setMessage(R.string.trash_empty_all_message)
                .setPositiveText(R.string.trash_empty_all)
                .setOnConfirm(viewModel::emptyAll)
                .build()
                .show();
    }

    private void confirmDeleteForever(int count) {
        new ConfirmDialogBuilder(requireContext())
                .setTitle(R.string.trash_delete_forever_title)
                .setMessage(R.string.trash_delete_forever_message, count)
                .setPositiveText(R.string.trash_delete_forever)
                .setOnConfirm(() -> {
                    viewModel.deleteForeverSelected();
                    if (actionMode != null) actionMode.finish();
                })
                .build()
                .show();
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_trash_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            Set<String> sel = viewModel.getSelectedIds().getValue();
            int n = sel == null ? 0 : sel.size();
            if (id == R.id.action_restore) {
                viewModel.restoreSelected();
                mode.finish();
                return true;
            }
            if (id == R.id.action_delete_forever) {
                confirmDeleteForever(n);
                return true;
            }
            if (id == R.id.action_select_all) {
                viewModel.selectAll();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setSelectionMode(false);
            viewModel.clearSelection();
        }
    };

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
            inflater.inflate(R.menu.menu_trash, menu);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.menu_empty_trash) {
                confirmEmptyAll();
                return true;
            }
            if (id == R.id.menu_refresh) {
                viewModel.loadTrash();
                return true;
            }
            if (id == R.id.menu_sort) {
                new SortDialogBuilder(requireContext())
                        .setCurrent(viewModel.getSortCriteria())
                        .setOnSortPicked(viewModel::setSortCriteria)
                        .build()
                        .show();
                return true;
            }
            return false;
        }
    };
}
