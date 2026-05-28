package com.zfile.manager.ui.viewer;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.zfile.manager.R;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.service.RecycleBinService;
import com.zfile.manager.ui.dialog.ConfirmDialogBuilder;
import com.zfile.manager.ui.dialog.PropertiesDialogBuilder;
import com.zfile.manager.util.IntentHelper;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared chrome for every viewer fragment: the top toolbar and its Share /
 * Open with / File info / Delete actions, plus back navigation. Subclasses
 * inflate their own content layout (which must {@code <include>} the
 * {@code view_viewer_toolbar} so {@code R.id.viewerToolbar} is present) and
 * call {@link #setCurrentFile(File)} as the displayed file changes.
 */
public abstract class BaseViewerFragment extends Fragment {

    @Nullable private MaterialToolbar toolbar;
    @Nullable protected File currentFile;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar = view.findViewById(R.id.viewerToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> requireActivity().finish());
            toolbar.inflateMenu(R.menu.menu_viewer);
            onInflateExtraMenu(toolbar);
            toolbar.setOnMenuItemClickListener(this::onMenuItemSelected);
        }
    }

    /** Subclasses may add their own toolbar items (e.g. the text viewer's search / font). */
    protected void onInflateExtraMenu(@NonNull MaterialToolbar toolbar) { }

    /** Subclasses handle their own toolbar items here; return true if consumed. */
    protected boolean onExtraMenuItemSelected(@NonNull MenuItem item) { return false; }

    protected void setCurrentFile(@NonNull File file) {
        currentFile = file;
        if (toolbar != null) toolbar.setTitle(file.getName());
    }

    /** Called after the current file was sent to the recycle bin. Default closes the viewer. */
    protected void onFileDeleted() {
        if (isAdded()) requireActivity().finish();
    }

    private boolean onMenuItemSelected(@NonNull MenuItem item) {
        File f = currentFile;
        if (f == null) return false;
        int id = item.getItemId();
        if (id == R.id.viewer_action_share) {
            IntentHelper.share(requireContext(), f);
            return true;
        }
        if (id == R.id.viewer_action_open_with) {
            IntentHelper.openWith(requireContext(), f);
            return true;
        }
        if (id == R.id.viewer_action_info) {
            new PropertiesDialogBuilder(requireContext())
                    .setFileItem(FileItem.fromFile(f, MimeTypeHelper.getFileType(f), MimeTypeHelper.getMimeType(f)))
                    .build()
                    .show();
            return true;
        }
        if (id == R.id.viewer_action_delete) {
            confirmDelete(f);
            return true;
        }
        return onExtraMenuItemSelected(item);
    }

    private void confirmDelete(@NonNull File file) {
        new ConfirmDialogBuilder(requireContext())
                .setTitle(R.string.dialog_confirm_delete_title)
                .setMessage(R.string.dialog_confirm_delete_message, 1)
                .setPositiveText(R.string.dialog_button_delete)
                .setOnConfirm(() -> moveToTrash(file))
                .build()
                .show();
    }

    private void moveToTrash(@NonNull File file) {
        String path = file.getAbsolutePath();
        ThreadPoolManager.getInstance().execute(() -> {
            new RecycleBinService().moveToTrash(
                    Collections.singletonList(path), null, new AtomicBoolean(false));
            if (isAdded()) {
                requireActivity().runOnUiThread(this::onFileDeleted);
            }
        });
    }
}
