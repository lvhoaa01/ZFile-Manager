package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.repository.FileRepository;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Observer-pattern bridge for the Categories tab.
 *
 * <p>Holds two LiveData:
 * <ul>
 *   <li>{@link #getCounts} — map of category → row count for the grid cards.</li>
 *   <li>{@link #getItems} — items in the currently opened category.</li>
 * </ul>
 * MediaStore queries hit the I/O pool via {@link ThreadPoolManager}.</p>
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
                List<FileItemComponent> items = repository.loadCategory(type);
                _items.postValue(items);
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    public void clear() {
        _items.postValue(Collections.emptyList());
        _currentCategory.postValue(null);
    }

    @NonNull
    private static Map<CategoryType, Integer> emptyCounts() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        for (CategoryType t : CategoryType.values()) m.put(t, 0);
        return m;
    }
}
