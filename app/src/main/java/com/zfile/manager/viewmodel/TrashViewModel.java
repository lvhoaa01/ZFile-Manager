package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.TransferProgress;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.repository.TrashRepository;
import com.zfile.manager.service.RecycleBinService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Trash-tab counterpart of {@link FileBrowserViewModel}. Exposes the same
 * {@code entries / selection / transferProgress / errorMessage} LiveData shape
 * so the existing {@code FileListAdapter} can be reused.
 *
 * <p>Selection keys are trash entry ids (see {@link TrashRepository}'s synthetic
 * {@code FileItem.path}).</p>
 */
public class TrashViewModel extends ViewModel {

    @NonNull private final TrashRepository repository = TrashRepository.getInstance();
    @NonNull private final RecycleBinService service = new RecycleBinService();

    private final MutableLiveData<List<FileItemComponent>> _entries =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Set<String>> _selectedIds =
            new MutableLiveData<>(Collections.emptySet());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<TransferProgress> _transferProgress = new MutableLiveData<>();
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();

    @NonNull private SortCriteria sortCriteria = SortCriteria.DATE_DESC;
    @NonNull private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    @NonNull public LiveData<List<FileItemComponent>> getEntries() { return _entries; }
    @NonNull public LiveData<Set<String>> getSelectedIds() { return _selectedIds; }
    @NonNull public LiveData<Boolean> getIsLoading() { return _isLoading; }
    @NonNull public LiveData<TransferProgress> getTransferProgress() { return _transferProgress; }
    @NonNull public LiveData<String> getErrorMessage() { return _errorMessage; }
    @NonNull public SortCriteria getSortCriteria() { return sortCriteria; }

    public void loadTrash() {
        _isLoading.postValue(true);
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                List<FileItemComponent> list = repository.getEntries(sortCriteria);
                _entries.postValue(list);
                // Drop selections referencing deleted entries.
                Set<String> sel = _selectedIds.getValue();
                if (sel != null && !sel.isEmpty()) {
                    Set<String> stillExisting = new HashSet<>();
                    for (FileItemComponent c : list) {
                        if (sel.contains(c.getPath())) stillExisting.add(c.getPath());
                    }
                    _selectedIds.postValue(Collections.unmodifiableSet(stillExisting));
                }
            } catch (Exception e) {
                _errorMessage.postValue(safeMessage(e));
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    public void toggleSelection(@NonNull String id) {
        Set<String> next = new HashSet<>(currentSelection());
        if (!next.remove(id)) next.add(id);
        _selectedIds.setValue(Collections.unmodifiableSet(next));
    }

    public void clearSelection() {
        _selectedIds.setValue(Collections.emptySet());
    }

    public void selectAll() {
        List<FileItemComponent> items = _entries.getValue();
        if (items == null || items.isEmpty()) return;
        Set<String> all = new HashSet<>(items.size());
        for (FileItemComponent c : items) all.add(c.getPath());
        _selectedIds.setValue(Collections.unmodifiableSet(all));
    }

    public void restoreSelected() {
        Set<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        List<String> ids = new ArrayList<>(sel);
        cancelFlag.set(false);
        ThreadPoolManager.getInstance().execute(() -> {
            service.restore(ids, _transferProgress::postValue, cancelFlag);
            _selectedIds.postValue(Collections.emptySet());
            loadTrash();
        });
    }

    public void deleteForeverSelected() {
        Set<String> sel = currentSelection();
        if (sel.isEmpty()) return;
        List<String> ids = new ArrayList<>(sel);
        ThreadPoolManager.getInstance().execute(() -> {
            service.permanentlyDelete(ids);
            _selectedIds.postValue(Collections.emptySet());
            loadTrash();
        });
    }

    public void emptyAll() {
        ThreadPoolManager.getInstance().execute(() -> {
            service.emptyAll();
            _selectedIds.postValue(Collections.emptySet());
            loadTrash();
        });
    }

    public void setSortCriteria(@NonNull SortCriteria criteria) {
        this.sortCriteria = criteria;
        loadTrash();
    }

    public void cancelTransfer() {
        cancelFlag.set(true);
    }

    public void clearErrorMessage() {
        _errorMessage.setValue(null);
    }

    public void clearTransferProgress() {
        _transferProgress.setValue(null);
    }

    @NonNull
    private Set<String> currentSelection() {
        Set<String> s = _selectedIds.getValue();
        return s == null ? Collections.emptySet() : s;
    }

    @NonNull
    private static String safeMessage(@NonNull Throwable t) {
        String m = t.getMessage();
        return m != null ? m : t.getClass().getSimpleName();
    }
}
