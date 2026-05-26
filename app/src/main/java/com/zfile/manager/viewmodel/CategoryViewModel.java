package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.repository.FileRepository;
import com.zfile.manager.repository.MediaStoreRepository;
import com.zfile.manager.service.DirectoryScannerService;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Observer-pattern bridge for the Categories tab.
 *
 * <p>Holds two LiveData streams:
 * <ul>
 *   <li>{@link #getCounts} — map of category → row count for the grid cards.</li>
 *   <li>{@link #getItems} — items in the currently opened category.</li>
 * </ul>
 * MediaStore queries hit the I/O pool via {@link ThreadPoolManager}; results
 * are decorated through {@link FileRepository} so they share rendering with the
 * file-browser list.</p>
 */
public class CategoryViewModel extends ViewModel {

    @NonNull private final FileRepository repository = FileRepository.getInstance();

    private final MutableLiveData<Map<CategoryType, Integer>> _counts =
            new MutableLiveData<>(emptyCounts());
    private final MutableLiveData<List<FileItemComponent>> _items =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<CategoryType> _currentCategory = new MutableLiveData<>();

    @NonNull public LiveData<Map<CategoryType, Integer>> getCounts() { return _counts; }
    @NonNull public LiveData<List<FileItemComponent>> getItems() { return _items; }
    @NonNull public LiveData<Boolean> getIsLoading() { return _isLoading; }
    @NonNull public LiveData<CategoryType> getCurrentCategory() { return _currentCategory; }

    public void refreshCounts() {
        ThreadPoolManager.getInstance().execute(() -> {
            Map<CategoryType, Integer> map = new EnumMap<>(CategoryType.class);
            for (CategoryType t : CategoryType.values()) {
                map.put(t, repository.countCategory(t));
            }
            _counts.postValue(map);
        });
    }

    public void loadCategory(@NonNull CategoryType type) {
        _currentCategory.postValue(type);
        _isLoading.postValue(true);
        ThreadPoolManager.getInstance().execute(() -> {
            try {
                FileSystemManager fsm = FileSystemManager.getInstance();
                List<FileItem> raw = MediaStoreRepository.getInstance().queryCategory(type);
                // Apply the user's currently-selected sort so the Category screen
                // respects the same sort dropdown as the file browser.
                DirectoryScannerService.sortInPlace(raw, fsm.getSortCriteria(), false);
                _items.postValue(repository.decorateAll(raw));
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /** Re-sort + re-emit the currently-loaded category without re-querying MediaStore. */
    public void setSortCriteria(@NonNull SortCriteria criteria) {
        FileSystemManager.getInstance().setSortCriteria(criteria);
        CategoryType cur = _currentCategory.getValue();
        if (cur != null) loadCategory(cur);
    }

    @NonNull
    private static Map<CategoryType, Integer> emptyCounts() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        for (CategoryType t : CategoryType.values()) m.put(t, 0);
        return m;
    }
}
