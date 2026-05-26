package com.zfile.manager.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.model.TrashEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton (DCL) holding the in-memory list of {@link TrashEntry} records.
 * Persisted to SharedPreferences as a JSON array under {@link #KEY_ENTRIES}.
 *
 * <p>All mutators are synchronised. Callers must invoke {@link #save()} after
 * mutations so the on-disk JSON stays in step with memory.</p>
 */
public final class TrashIndex {

    private static final String TAG = "TrashIndex";
    private static final String PREFS_NAME = "zfile_trash";
    private static final String KEY_ENTRIES = "trash_index";

    private static volatile TrashIndex instance;

    @NonNull private final List<TrashEntry> entries = new ArrayList<>();
    @Nullable private Context appContext;

    private TrashIndex() { }

    @NonNull
    public static TrashIndex getInstance() {
        TrashIndex local = instance;
        if (local == null) {
            synchronized (TrashIndex.class) {
                local = instance;
                if (local == null) {
                    local = new TrashIndex();
                    instance = local;
                }
            }
        }
        return local;
    }

    public synchronized void load(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        entries.clear();
        SharedPreferences prefs = prefs();
        String raw = prefs.getString(KEY_ENTRIES, null);
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                entries.add(TrashEntry.fromJson(o));
            }
        } catch (JSONException e) {
            Log.w(TAG, "Corrupted trash_index JSON, resetting", e);
            prefs.edit().putString(KEY_ENTRIES + ".bak", raw).remove(KEY_ENTRIES).apply();
            entries.clear();
        }
    }

    public synchronized void save() {
        if (appContext == null) return;
        JSONArray arr = new JSONArray();
        for (TrashEntry e : entries) {
            try {
                arr.put(e.toJson());
            } catch (JSONException ex) {
                Log.w(TAG, "Skipping bad entry on save", ex);
            }
        }
        prefs().edit().putString(KEY_ENTRIES, arr.toString()).apply();
    }

    public synchronized void add(@NonNull TrashEntry entry) {
        entries.add(entry);
    }

    public synchronized boolean remove(@NonNull String id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(id)) {
                entries.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized void clear() {
        entries.clear();
    }

    @NonNull
    public synchronized List<TrashEntry> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    @Nullable
    public synchronized TrashEntry getById(@NonNull String id) {
        for (TrashEntry e : entries) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    public synchronized int size() {
        return entries.size();
    }

    @NonNull
    private SharedPreferences prefs() {
        if (appContext == null) {
            throw new IllegalStateException("TrashIndex.load(Context) must be called first");
        }
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
