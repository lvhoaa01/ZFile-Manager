package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.repository.FileRepository;
import com.zfile.manager.service.SearchService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Observer-pattern bridge for the Search tab.
 *
 * <p>Each {@link #submitQuery} flips the previous task's {@code AtomicBoolean}
 * flag to {@code true} (so its file-tree walk aborts ASAP), creates a fresh
 * flag for the new task, and hands it to {@link SearchService}. The search
 * executor drains its queue per submission so only the latest query proceeds
 * once the in-flight one finishes draining.</p>
 *
 * <p>Per-task flags (not a single shared one) are necessary because resetting a
 * shared flag would also un-cancel a previously cancelled in-flight task.</p>
 *
 * <p>The root path defaults to {@link FileSystemManager#getCurrentRootPath()} but
 * can be overridden via {@link #setSearchRoot} so the Search tab respects the
 * folder the user is currently browsing.</p>
 */
public class SearchViewModel extends ViewModel {

    @NonNull private final FileRepository repository = FileRepository.getInstance();
    @NonNull private final SearchService service = new SearchService();

    private final MutableLiveData<List<FileItemComponent>> _results =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> _isSearching = new MutableLiveData<>(false);
    private final MutableLiveData<String> _currentQuery = new MutableLiveData<>("");

    @NonNull private volatile AtomicBoolean currentCancel = new AtomicBoolean(false);
    @Nullable private volatile String searchRoot;

    @NonNull public LiveData<List<FileItemComponent>> getResults() { return _results; }
    @NonNull public LiveData<Boolean> getIsSearching() { return _isSearching; }
    @NonNull public LiveData<String> getCurrentQuery() { return _currentQuery; }

    public void setSearchRoot(@Nullable String path) {
        this.searchRoot = path;
    }

    public void submitQuery(@Nullable String query) {
        String q = query == null ? "" : query.trim();
        _currentQuery.postValue(q);

        currentCancel.set(true);

        if (q.isEmpty()) {
            _results.postValue(Collections.emptyList());
            _isSearching.postValue(false);
            return;
        }

        final AtomicBoolean myCancel = new AtomicBoolean(false);
        currentCancel = myCancel;

        _isSearching.postValue(true);
        _results.postValue(Collections.emptyList());

        String root = searchRoot != null ? searchRoot
                : FileSystemManager.getInstance().getCurrentRootPath();
        if (root == null) {
            _isSearching.postValue(false);
            return;
        }
        final String rootPath = root;
        // Cumulative decorated cache so each batch only decorates new items (O(n)
        // total instead of O(n²) across all partial-result callbacks).
        final List<FileItemComponent> decorated = Collections.synchronizedList(new ArrayList<>());

        ThreadPoolManager.getInstance().submitSearch(() -> {
            service.search(rootPath, q, myCancel, new SearchService.ResultCallback() {
                @Override
                public void onPartialResults(@NonNull List<FileItem> batch) {
                    if (myCancel.get()) return;
                    decorated.addAll(repository.decorateAll(batch));
                    _results.postValue(new ArrayList<>(decorated));
                }

                @Override
                public void onFinished(int totalMatches) {
                    if (myCancel.get()) return;
                    _isSearching.postValue(false);
                }
            });
        });
    }

    public void cancel() {
        currentCancel.set(true);
        _isSearching.postValue(false);
    }
}
