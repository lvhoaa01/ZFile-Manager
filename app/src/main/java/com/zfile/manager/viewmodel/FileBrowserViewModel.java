package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.R;
import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.TransferProgress;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.repository.FileRepository;
import com.zfile.manager.service.FileTransferService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Observer-pattern bridge between the {@link FileRepository} and the file-browser UI.
 *
 * <p>UI observes {@link LiveData} fields for file list, path, selection, loading,
 * and transfer progress. All filesystem work is dispatched to the I/O pool via
 * {@link ThreadPoolManager}; results are returned through {@code postValue}.</p>
 */
public class FileBrowserViewModel extends ViewModel {

    @NonNull private final FileRepository repository = FileRepository.getInstance();

    private final MutableLiveData<List<FileItemComponent>> _fileList = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> _currentPath = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<TransferProgress> _transferProgress = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> _selectedPaths = new MutableLiveData<>(Collections.emptySet());

    /** Derived from {@link #_currentPath} so the two never drift out of sync. */
    @NonNull private final LiveData<List<String>> pathSegments =
            Transformations.map(_currentPath, FileBrowserViewModel::toSegments);

    @NonNull private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @NonNull public LiveData<List<FileItemComponent>> getFileList() { return _fileList; }
    @NonNull public LiveData<String> getCurrentPath() { return _currentPath; }
    @NonNull public LiveData<List<String>> getPathSegments() { return pathSegments; }
    @NonNull public LiveData<Boolean> getIsLoading() { return _isLoading; }
    @NonNull public LiveData<TransferProgress> getTransferProgress() { return _transferProgress; }
    @NonNull public LiveData<String> getErrorMessage() { return _errorMessage; }
    @NonNull public LiveData<Set<String>> getSelectedPaths() { return _selectedPaths; }

    public void loadDirectory(@NonNull String path) {
        _isLoading.postValue(true);
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<FileItemComponent> items = repository.loadDirectory(path);
                _fileList.postValue(items);
                _currentPath.postValue(path);
                _selectedPaths.postValue(Collections.emptySet());
            } catch (Exception e) {
                _errorMessage.postValue(safeMessage(e));
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    public void refresh() {
        String path = _currentPath.getValue();
        if (path != null) loadDirectory(path);
    }

    public boolean navigateUp() {
        String path = _currentPath.getValue();
        if (path == null) return false;
        File parent = new File(path).getParentFile();
        if (parent == null || !parent.canRead()) return false;
        loadDirectory(parent.getAbsolutePath());
        return true;
    }

    public void toggleSelection(@NonNull String path) {
        Set<String> next = new HashSet<>(currentSelection());
        if (!next.remove(path)) next.add(path);
        _selectedPaths.setValue(Collections.unmodifiableSet(next));
    }

    public void clearSelection() {
        _selectedPaths.setValue(Collections.emptySet());
    }

    public void selectAll() {
        List<FileItemComponent> items = _fileList.getValue();
        if (items == null || items.isEmpty()) return;
        Set<String> all = new HashSet<>(items.size());
        for (FileItemComponent c : items) all.add(c.getPath());
        _selectedPaths.setValue(Collections.unmodifiableSet(all));
    }

    public void copySelected() {
        Set<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        FileSystemManager.getInstance().setClipboard(
                new ArrayList<>(sel), FileSystemManager.ClipboardOperation.COPY);
    }

    public void cutSelected() {
        Set<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        FileSystemManager.getInstance().setClipboard(
                new ArrayList<>(sel), FileSystemManager.ClipboardOperation.CUT);
    }

    public void pasteHere() {
        FileSystemManager fsm = FileSystemManager.getInstance();
        if (!fsm.hasClipboardContent()) return;
        String destDir = _currentPath.getValue();
        if (destDir == null) return;

        List<String> sources = fsm.getClipboardPaths();
        FileSystemManager.ClipboardOperation op = fsm.getClipboardOperation();
        cancelFlag.set(false);

        ThreadPoolManager.getInstance().execute(() -> {
            FileTransferService.ProgressCallback cb = _transferProgress::postValue;
            if (op == FileSystemManager.ClipboardOperation.CUT) {
                repository.move(sources, destDir, cb, cancelFlag);
            } else {
                repository.copy(sources, destDir, cb, cancelFlag);
            }
            if (op == FileSystemManager.ClipboardOperation.CUT) {
                fsm.clearClipboard();
            }
            refresh();
        });
    }

    public void cancelTransfer() {
        cancelFlag.set(true);
    }

    public void deleteSelected() {
        Set<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        List<String> toDelete = new ArrayList<>(sel);
        ThreadPoolManager.getInstance().execute(() -> {
            boolean allOk = true;
            for (String p : toDelete) {
                if (!repository.delete(p)) allOk = false;
            }
            if (!allOk) _errorMessage.postValue(stringRes(R.string.error_delete_failed));
            refresh();
            _selectedPaths.postValue(Collections.emptySet());
        });
    }

    public void rename(@NonNull String path, @NonNull String newName) {
        ThreadPoolManager.getInstance().execute(() -> {
            if (!repository.rename(path, newName)) {
                _errorMessage.postValue(stringRes(R.string.error_rename_failed));
            }
            refresh();
        });
    }

    public void createFolder(@NonNull String name) {
        String cur = _currentPath.getValue();
        if (cur == null) return;
        ThreadPoolManager.getInstance().execute(() -> {
            if (!repository.createFolder(cur, name)) {
                _errorMessage.postValue(stringRes(R.string.error_create_failed));
            }
            refresh();
        });
    }

    public void createFile(@NonNull String name) {
        String cur = _currentPath.getValue();
        if (cur == null) return;
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                if (!repository.createFile(cur, name)) {
                    _errorMessage.postValue(stringRes(R.string.error_create_failed));
                }
            } catch (IOException e) {
                _errorMessage.postValue(safeMessage(e));
            }
            refresh();
        });
    }

    public void setSortCriteria(@NonNull SortCriteria criteria) {
        FileSystemManager.getInstance().setSortCriteria(criteria);
        refresh();
    }

    public void setShowHidden(boolean show) {
        FileSystemManager.getInstance().setShowHiddenFiles(show);
        refresh();
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public void clearTransferProgress() {
        _transferProgress.setValue(null);
    }

    @NonNull
    private static String stringRes(int resId) {
        return FileSystemManager.getInstance().requireContext().getString(resId);
    }

    @NonNull
    private Set<String> currentSelection() {
        Set<String> s = _selectedPaths.getValue();
        return s == null ? Collections.emptySet() : s;
    }

    @NonNull
    private static List<String> toSegments(@Nullable String path) {
        if (path == null || path.isEmpty()) return Collections.emptyList();
        List<String> segs = new ArrayList<>();
        File f = new File(path);
        while (f != null && f.getName().length() > 0) {
            segs.add(0, f.getName());
            f = f.getParentFile();
        }
        if (segs.isEmpty()) segs.add("/");
        return segs;
    }

    @NonNull
    private static String safeMessage(@Nullable Throwable t) {
        if (t == null) return "Unknown error";
        String m = t.getMessage();
        return m != null ? m : t.getClass().getSimpleName();
    }
}
