package com.zfile.manager.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton facade that queries Android's MediaStore for category-based listings
 * (Images / Videos / Audio / Documents / Downloads / APKs). Returns plain
 * {@link FileItem} so the same {@code FileListAdapter} can render results.
 *
 * <p>All queries hit the {@code ContentResolver} which performs disk I/O — call
 * from a background thread via {@code ThreadPoolManager}.</p>
 */
public final class MediaStoreRepository {

    private static volatile MediaStoreRepository instance;

    private MediaStoreRepository() { }

    @NonNull
    public static MediaStoreRepository getInstance() {
        MediaStoreRepository local = instance;
        if (local == null) {
            synchronized (MediaStoreRepository.class) {
                local = instance;
                if (local == null) {
                    local = new MediaStoreRepository();
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Sum of {@code SIZE} bytes for the given category. */
    public long sumSize(@NonNull CategoryType type) {
        Context ctx = FileSystemManager.getInstance().requireContext();
        Uri uri = uriFor(type);
        String[] projection = { MediaStore.MediaColumns.SIZE };
        String selection = selectionFor(type);
        String[] selectionArgs = selectionArgsFor(type);
        long total = 0L;
        try (Cursor c = ctx.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null)) {
            if (c == null || !c.moveToFirst()) return 0L;
            int sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE);
            if (sizeIdx < 0) return 0L;
            do {
                total += c.getLong(sizeIdx);
            } while (c.moveToNext());
        } catch (Exception ignored) {
        }
        return total;
    }

    /** Total number of entries for the given category (cheap — no row payload fetched). */
    public int countCategory(@NonNull CategoryType type) {
        Context ctx = FileSystemManager.getInstance().requireContext();
        Uri uri = uriFor(type);
        String[] projection = { MediaStore.MediaColumns._ID };
        String selection = selectionFor(type);
        String[] selectionArgs = selectionArgsFor(type);
        try (Cursor c = ctx.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null)) {
            return c == null ? 0 : c.getCount();
        } catch (Exception e) {
            return 0;
        }
    }

    // Ý tưởng là sử dụng MediaStore để truy vấn
    //  các loại file khác nhau dựa trên MIME type
    //  hoặc đường dẫn, tùy thuộc vào loại file.
    //  Ví dụ, để truy vấn tài liệu, chúng ta có 
    // thể lọc theo MIME type của các định dạng tài 
    // liệu phổ biến. Để truy vấn các file trong thư 
    // mục Downloads, chúng ta có thể lọc theo đường 
    // dẫn chứa thư mục đó (đặc biệt là trên các phiên 
    // bản Android cũ hơn). Kết quả truy vấn sẽ được ánh 
    // xạ thành danh sách FileItem để dễ dàng hiển thị 
    // trong giao diện người dùng.
    @NonNull
    public List<FileItem> queryCategory(@NonNull CategoryType type) {
        Context ctx = FileSystemManager.getInstance().requireContext();
        Uri uri = uriFor(type); 
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.MIME_TYPE,
        };
        String selection = selectionFor(type);
        String[] selectionArgs = selectionArgsFor(type);
        String sortOrder = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";

        ContentResolver cr = ctx.getContentResolver();
        try (Cursor c = cr.query(uri, projection, selection, selectionArgs, sortOrder)) { 
            if (c == null || !c.moveToFirst()) return Collections.emptyList();

            int dataIdx = c.getColumnIndex(MediaStore.MediaColumns.DATA);
            int nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            int sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE);
            int dateIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);
            int mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE);

            List<FileItem> out = new ArrayList<>(c.getCount());
            do {
                String path = dataIdx >= 0 ? c.getString(dataIdx) : null;
                String name = nameIdx >= 0 ? c.getString(nameIdx) : null;
                if (path == null || name == null) continue;

                File file = new File(path);
                if (!file.exists()) continue;

                long size = sizeIdx >= 0 ? c.getLong(sizeIdx) : 0L;
                // DATE_MODIFIED is seconds, File.lastModified() is millis — normalize to ms.
                long modifiedSec = dateIdx >= 0 ? c.getLong(dateIdx) : 0L;
                long modifiedMs = modifiedSec * 1000L;
                String mime = mimeIdx >= 0 ? c.getString(mimeIdx) : null;
                FileType ft = MimeTypeHelper.getFileTypeFromName(name);

                out.add(new FileItem(
                        name, path, size, modifiedMs,
                        false, name.startsWith("."),
                        file.canRead(), file.canWrite(),
                        ft, mime));
            } while (c.moveToNext());
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @NonNull
    private static Uri uriFor(@NonNull CategoryType type) {
        switch (type) {
            case IMAGES:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            case VIDEOS:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            case AUDIO:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            case DOWNLOADS:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                return MediaStore.Files.getContentUri("external");
            case DOCUMENTS:
            case APKS:
            default:
                return MediaStore.Files.getContentUri("external");
        }
    }

    @androidx.annotation.Nullable
    private static String selectionFor(@NonNull CategoryType type) {// Dùng MediaStore để truy vấn các loại file khác nhau dựa trên MIME type hoặc đường dẫn, tùy thuộc vào loại file.
        switch (type) {
            case DOCUMENTS:
                // MediaStore exposes documents only via the Files collection; filter by MIME.
                return MediaStore.Files.FileColumns.MIME_TYPE + " IN (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            case APKS:
                return MediaStore.Files.FileColumns.MIME_TYPE + " = ?";
            case DOWNLOADS:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Pre-Q: scan rows whose DATA path starts with the Downloads dir.
                    return MediaStore.Files.FileColumns.DATA + " LIKE ?";
                }
                return null;
            case IMAGES:
            case VIDEOS:
            case AUDIO:
            default:
                return null;
        }
    }

    @androidx.annotation.Nullable
    private static String[] selectionArgsFor(@NonNull CategoryType type) { 
        switch (type) {
            case DOCUMENTS:
                return new String[] {
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.oasis.opendocument.text",
                        "application/vnd.oasis.opendocument.spreadsheet",
                        "application/vnd.oasis.opendocument.presentation",
                        "application/rtf",
                        "text/plain",
                        "text/csv",
                        "text/html",
                        "text/markdown",
                };
            case APKS:
                return new String[] { "application/vnd.android.package-archive" };
            case DOWNLOADS:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    File dl = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    return new String[] { dl.getAbsolutePath() + "/%" };
                }
                return null;
            case IMAGES:
            case VIDEOS:
            case AUDIO:
            default:
                return null;
        }
    }
}
