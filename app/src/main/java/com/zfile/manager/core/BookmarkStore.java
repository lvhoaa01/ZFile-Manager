package com.zfile.manager.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.Bookmark;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton (DCL) holding the in-memory list of {@link Bookmark}s, persisted to
 * SharedPreferences {@code "zfile_bookmarks"} as a JSON array. Mirrors
 * {@link TrashIndex}; mutators write through immediately so disk stays in sync.
 */
public final class BookmarkStore {

    private static final String TAG = "BookmarkStore";
    private static final String PREFS_NAME = "zfile_bookmarks";
    private static final String KEY_BOOKMARKS = "bookmarks";

    private static volatile BookmarkStore instance;

    @NonNull private final List<Bookmark> bookmarks = new ArrayList<>();
    @Nullable private Context appContext;

    private BookmarkStore() { }

    @NonNull
    public static BookmarkStore getInstance() {
        BookmarkStore local = instance;
        if (local == null) {
            synchronized (BookmarkStore.class) {
                local = instance;
                if (local == null) {
                    local = new BookmarkStore();
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void load(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        bookmarks.clear();
        String raw = prefs().getString(KEY_BOOKMARKS, null);
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                bookmarks.add(Bookmark.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Corrupted bookmarks JSON, resetting", e);
            prefs().edit().remove(KEY_BOOKMARKS).apply();
            bookmarks.clear();
        }
    }

    private synchronized void save() {
        if (appContext == null) return;
        JSONArray arr = new JSONArray();
        for (Bookmark b : bookmarks) {
            try {
                arr.put(b.toJson());
            } catch (JSONException ex) {
                Log.w(TAG, "Skipping bad bookmark on save", ex);
            }
        }
        prefs().edit().putString(KEY_BOOKMARKS, arr.toString()).apply();
    }

    /** Adds the bookmark unless one with the same path already exists. */
    public synchronized boolean add(@NonNull Bookmark bookmark) {
        if (contains(bookmark.getPath())) return false;
        bookmarks.add(bookmark);
        save();
        return true;
    }

    public synchronized boolean remove(@NonNull String path) {
        for (int i = 0; i < bookmarks.size(); i++) {
            if (bookmarks.get(i).getPath().equals(path)) {
                bookmarks.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean contains(@NonNull String path) {
        for (Bookmark b : bookmarks) {
            if (b.getPath().equals(path)) return true;
        }
        return false;
    }

    @NonNull
    public synchronized List<Bookmark> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(bookmarks));
    }

    @NonNull
    private SharedPreferences prefs() {
        if (appContext == null) {
            throw new IllegalStateException("BookmarkStore.load(Context) must be called first");
        }
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
