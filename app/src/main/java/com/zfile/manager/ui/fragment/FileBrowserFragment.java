package com.zfile.manager.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.zfile.manager.R;
import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.TransferProgress;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.ui.adapter.FileListAdapter;
import com.zfile.manager.ui.dialog.ConfirmDialogBuilder;
import com.zfile.manager.ui.dialog.CreateFileDialogBuilder;
import com.zfile.manager.ui.dialog.PropertiesDialogBuilder;
import com.zfile.manager.ui.dialog.RenameDialogBuilder;
import com.zfile.manager.util.IntentHelper;
import com.zfile.manager.util.PermissionHelper;
import com.zfile.manager.viewmodel.FileBrowserViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Main file-browser screen. Observes {@link FileBrowserViewModel} and binds:
 * <ul>
 *   <li>file list to {@link RecyclerView} via {@link FileListAdapter}</li>
 *   <li>selection state to ActionMode (multi-select CRUD)</li>
 *   <li>transfer progress to a top {@link LinearProgressIndicator}</li>
 *   <li>permission gate to an empty-state group with a "Grant access" button</li>
 * </ul>
 */
public class FileBrowserFragment extends Fragment {

    private FileBrowserViewModel viewModel;
    private FileListAdapter adapter;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recycler;
    private FloatingActionButton fab;
    private View emptyGroup;
    private MaterialButton grantAccessButton;
    private LinearProgressIndicator progressIndicator;

    @Nullable private ActionMode actionMode;
    @Nullable private OnBackPressedCallback backCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        viewModel = new ViewModelProvider(requireActivity()).get(FileBrowserViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_browser, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recycler = view.findViewById(R.id.fileRecycler);
        fab = view.findViewById(R.id.createFab);
        emptyGroup = view.findViewById(R.id.emptyGroup);
        grantAccessButton = view.findViewById(R.id.grantAccessButton);
        progressIndicator = view.findViewById(R.id.transferProgress);

        adapter = new FileListAdapter();
        recycler.setAdapter(adapter);
        adapter.setOnItemClickListener(new FileListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull FileItemComponent item, int position) {
                if (actionMode != null) {
                    viewModel.toggleSelection(item.getPath());
                    return;
                }
                FileItem fi = item.getFileItem();
                if (fi.isDirectory()) {
                    viewModel.loadDirectory(fi.getPath());
                } else {
                    IntentHelper.openWith(requireContext(), fi.asFile());
                }
            }

            @Override
            public void onItemLongClick(@NonNull FileItemComponent item, int position) {
                if (actionMode == null) startActionMode();
                viewModel.toggleSelection(item.getPath());
            }
        });

        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
        fab.setOnClickListener(v -> showCreateDialog());
        grantAccessButton.setOnClickListener(v ->
                PermissionHelper.requestFullAccess(requireActivity(), REQUEST_PERMISSION));

        observeViewModel();
        installBackHandler();

        if (!PermissionHelper.hasFullAccess(requireContext())) {
            showPermissionEmptyState();
        } else {
            String root = FileSystemManager.getInstance().getCurrentRootPath();
            if (root != null && viewModel.getCurrentPath().getValue() == null) {
                viewModel.loadDirectory(root);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionHelper.hasFullAccess(requireContext())) {
            emptyGroup.setVisibility(View.GONE);
            grantAccessButton.setVisibility(View.GONE);
            if (viewModel.getCurrentPath().getValue() == null) {
                String root = FileSystemManager.getInstance().getCurrentRootPath();
                if (root != null) viewModel.loadDirectory(root);
            }
        }
    }

    private void observeViewModel() {
        viewModel.getFileList().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(new ArrayList<>(list));
            updateEmptyState(list);
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(),
                loading -> swipeRefresh.setRefreshing(loading != null && loading));
        viewModel.getSelectedPaths().observe(getViewLifecycleOwner(), selected -> {
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
        viewModel.getCurrentPath().observe(getViewLifecycleOwner(), path -> {
            if (path != null) {
                Toolbar t = requireActivity().findViewById(R.id.toolbar);
                if (t != null) t.setSubtitle(path);
            }
            requireActivity().invalidateOptionsMenu();
        });
    }

    private void updateEmptyState(@Nullable List<FileItemComponent> list) {
        boolean hasPermission = PermissionHelper.hasFullAccess(requireContext());
        if (!hasPermission) {
            showPermissionEmptyState();
            return;
        }
        if (list == null || list.isEmpty()) {
            emptyGroup.setVisibility(View.VISIBLE);
            ((android.widget.TextView) emptyGroup.findViewById(R.id.emptyTitle))
                    .setText(R.string.empty_directory);
            ((android.widget.TextView) emptyGroup.findViewById(R.id.emptyMessage)).setText("");
            grantAccessButton.setVisibility(View.GONE);
        } else {
            emptyGroup.setVisibility(View.GONE);
        }
    }

    private void showPermissionEmptyState() {
        emptyGroup.setVisibility(View.VISIBLE);
        ((android.widget.TextView) emptyGroup.findViewById(R.id.emptyTitle))
                .setText(R.string.permission_title);
        ((android.widget.TextView) emptyGroup.findViewById(R.id.emptyMessage))
                .setText(R.string.permission_explanation);
        grantAccessButton.setVisibility(View.VISIBLE);
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
                    Snackbar.make(getView(), R.string.transfer_completed, Snackbar.LENGTH_SHORT).show();
                }
                break;
            case FAILED:
                progressIndicator.setVisibility(View.GONE);
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.transfer_failed,
                                    p.getErrorMessage() != null ? p.getErrorMessage() : "?"),
                            Snackbar.LENGTH_LONG).show();
                }
                break;
            case CANCELLED:
                progressIndicator.setVisibility(View.GONE);
                if (getView() != null) {
                    Snackbar.make(getView(), R.string.transfer_cancelled, Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void installBackHandler() {
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (actionMode != null) {
                    actionMode.finish();
                    return;
                }
                if (!viewModel.navigateUp()) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void showCreateDialog() {
        new CreateFileDialogBuilder(requireContext())
                .setOnCreate((name, isFolder) -> {
                    if (isFolder) viewModel.createFolder(name);
                    else viewModel.createFile(name);
                })
                .build()
                .show();
    }

    private void showRenameDialog(@NonNull FileItem item) {
        new RenameDialogBuilder(requireContext())
                .setCurrentName(item.getName())
                .setOnRename(newName -> viewModel.rename(item.getPath(), newName))
                .build()
                .show();
    }

    private void showPropertiesDialog(@NonNull FileItem item) {
        new PropertiesDialogBuilder(requireContext())
                .setFileItem(item)
                .build()
                .show();
    }

    private void confirmDelete(int count) {
        new ConfirmDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_delete_title)
                .setMessage(R.string.dialog_confirm_delete_message, count)
                .setPositiveText(R.string.dialog_button_delete)
                .setOnConfirm(() -> {
                    viewModel.deleteSelected();
                    if (actionMode != null) actionMode.finish();
                })
                .build()
                .show();
    }

    private void startActionMode() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        actionMode = activity.startSupportActionMode(actionModeCallback);
        adapter.setSelectionMode(true);
    }

    private void updateActionModeTitle() {
        if (actionMode == null) return;
        Set<String> sel = viewModel.getSelectedPaths().getValue();
        int n = sel == null ? 0 : sel.size();
        if (n == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(getString(R.string.selected_count, n));
            actionMode.invalidate();
        }
    }

    @Nullable
    private FileItem singleSelectedItem() {
        Set<String> sel = viewModel.getSelectedPaths().getValue();
        if (sel == null || sel.size() != 1) return null;
        String path = sel.iterator().next();
        List<FileItemComponent> list = viewModel.getFileList().getValue();
        if (list == null) return null;
        for (FileItemComponent c : list) {
            if (c.getPath().equals(path)) return c.getFileItem();
        }
        return null;
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Set<String> sel = viewModel.getSelectedPaths().getValue();
            int n = sel == null ? 0 : sel.size();
            MenuItem rename = menu.findItem(R.id.action_rename);
            MenuItem props = menu.findItem(R.id.action_properties);
            if (rename != null) rename.setVisible(n == 1);
            if (props != null) props.setVisible(n == 1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            Set<String> sel = viewModel.getSelectedPaths().getValue();
            int n = sel == null ? 0 : sel.size();
            if (id == R.id.action_delete) {
                confirmDelete(n);
                return true;
            }
            if (id == R.id.action_copy) {
                viewModel.copySelected();
                Toast.makeText(requireContext(), R.string.action_copy, Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            }
            if (id == R.id.action_cut) {
                viewModel.cutSelected();
                Toast.makeText(requireContext(), R.string.action_cut, Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            }
            if (id == R.id.action_rename) {
                FileItem fi = singleSelectedItem();
                if (fi != null) showRenameDialog(fi);
                mode.finish();
                return true;
            }
            if (id == R.id.action_properties) {
                FileItem fi = singleSelectedItem();
                if (fi != null) showPropertiesDialog(fi);
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_file_browser, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem paste = menu.findItem(R.id.menu_paste);
        if (paste != null) {
            paste.setVisible(FileSystemManager.getInstance().hasClipboardContent());
        }
        MenuItem showHidden = menu.findItem(R.id.menu_show_hidden);
        if (showHidden != null) {
            showHidden.setChecked(FileSystemManager.getInstance().isShowHiddenFiles());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_refresh) { viewModel.refresh(); return true; }
        if (id == R.id.menu_paste) { viewModel.pasteHere(); return true; }
        if (id == R.id.menu_show_hidden) {
            boolean next = !item.isChecked();
            item.setChecked(next);
            viewModel.setShowHidden(next);
            return true;
        }
        if (id == R.id.sort_name_asc) { viewModel.setSortCriteria(SortCriteria.NAME_ASC); return true; }
        if (id == R.id.sort_name_desc) { viewModel.setSortCriteria(SortCriteria.NAME_DESC); return true; }
        if (id == R.id.sort_size_asc) { viewModel.setSortCriteria(SortCriteria.SIZE_ASC); return true; }
        if (id == R.id.sort_size_desc) { viewModel.setSortCriteria(SortCriteria.SIZE_DESC); return true; }
        if (id == R.id.sort_date_asc) { viewModel.setSortCriteria(SortCriteria.DATE_ASC); return true; }
        if (id == R.id.sort_date_desc) { viewModel.setSortCriteria(SortCriteria.DATE_DESC); return true; }
        if (id == R.id.sort_type) { viewModel.setSortCriteria(SortCriteria.TYPE); return true; }
        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_PERMISSION = 1001;
}
