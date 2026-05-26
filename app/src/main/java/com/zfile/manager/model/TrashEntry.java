package com.zfile.manager.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata for one item currently in the recycle bin. Stored as JSON in
 * SharedPreferences via {@code TrashIndex}. The actual file lives in the
 * trash directory under {@link #trashedFileName} so collisions across deletes
 * of same-named files are impossible.
 */
public final class TrashEntry {

    @NonNull private final String id;
    @NonNull private final String originalPath;
    @NonNull private final String trashedFileName;
    private final long deletedAt;
    private final long originalSize;
    private final boolean directory;

    public TrashEntry(@NonNull String id,
                      @NonNull String originalPath,
                      @NonNull String trashedFileName,
                      long deletedAt,
                      long originalSize,
                      boolean directory) {
        this.id = id;
        this.originalPath = originalPath;
        this.trashedFileName = trashedFileName;
        this.deletedAt = deletedAt;
        this.originalSize = originalSize;
        this.directory = directory;
    }

    @NonNull
    public static TrashEntry create(@NonNull String originalPath,
                                    @NonNull String trashedFileName,
                                    long originalSize,
                                    boolean directory) {
        return new TrashEntry(
                UUID.randomUUID().toString(),
                originalPath,
                trashedFileName,
                System.currentTimeMillis(),
                originalSize,
                directory
        );
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getOriginalPath() { return originalPath; }
    @NonNull public String getTrashedFileName() { return trashedFileName; }
    public long getDeletedAt() { return deletedAt; }
    public long getOriginalSize() { return originalSize; }
    public boolean isDirectory() { return directory; }

    @NonNull
    public String getDisplayName() {
        int slash = originalPath.lastIndexOf('/');
        return slash < 0 ? originalPath : originalPath.substring(slash + 1);
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("originalPath", originalPath);
        o.put("trashedFileName", trashedFileName);
        o.put("deletedAt", deletedAt);
        o.put("originalSize", originalSize);
        o.put("directory", directory);
        return o;
    }

    @NonNull
    public static TrashEntry fromJson(@NonNull JSONObject o) throws JSONException {
        return new TrashEntry(
                o.getString("id"),
                o.getString("originalPath"),
                o.getString("trashedFileName"),
                o.getLong("deletedAt"),
                o.getLong("originalSize"),
                o.getBoolean("directory")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrashEntry)) return false;
        return id.equals(((TrashEntry) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
