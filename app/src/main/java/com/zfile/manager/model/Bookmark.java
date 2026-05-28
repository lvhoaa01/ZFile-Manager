package com.zfile.manager.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * A user-saved shortcut to a folder. Persisted as JSON in SharedPreferences via
 * {@code BookmarkStore}. Identity is the {@link #path} — a folder can only be
 * bookmarked once.
 */
public final class Bookmark {

    @NonNull private final String path;
    @NonNull private final String name;
    private final long createdAt;

    public Bookmark(@NonNull String path, @NonNull String name, long createdAt) {
        this.path = path;
        this.name = name;
        this.createdAt = createdAt;
    }

    @NonNull public String getPath() { return path; }
    @NonNull public String getName() { return name; }
    public long getCreatedAt() { return createdAt; }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("path", path);
        o.put("name", name);
        o.put("createdAt", createdAt);
        return o;
    }

    @NonNull
    public static Bookmark fromJson(@NonNull JSONObject o) throws JSONException {
        return new Bookmark(
                o.getString("path"),
                o.getString("name"),
                o.getLong("createdAt")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bookmark)) return false;
        return path.equals(((Bookmark) o).path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
