package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.BookmarkStore;
import com.zfile.manager.model.Bookmark;

import java.util.List;

/**
 * Exposes the {@link BookmarkStore} list as {@link LiveData} for
 * {@code BookmarksFragment}. Bookmarks live entirely in memory (a small list
 * cached by the store), so reads are synchronous and writes go straight through.
 */
public class BookmarkViewModel extends ViewModel {

    private final MutableLiveData<List<Bookmark>> _bookmarks = new MutableLiveData<>();

    @NonNull
    public LiveData<List<Bookmark>> getBookmarks() { return _bookmarks; }

    public void load() {
        _bookmarks.setValue(BookmarkStore.getInstance().getAll());
    }

    public void remove(@NonNull String path) {
        BookmarkStore.getInstance().remove(path);
        load();
    }
}
