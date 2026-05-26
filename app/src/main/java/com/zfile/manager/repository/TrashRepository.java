package com.zfile.manager.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.zfile.manager.R;
import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.core.TrashIndex;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.model.SortCriteria;
import com.zfile.manager.model.TrashEntry;
import com.zfile.manager.model.decorator.BaseFileItem;
import com.zfile.manager.model.decorator.ColorDecorator;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.model.decorator.IconDecorator;
import com.zfile.manager.model.decorator.SystemTagDecorator;
import com.zfile.manager.service.DirectoryScannerService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

/**
 * Facade over {@link TrashIndex}: turns {@link TrashEntry} records into the
 * {@link FileItemComponent} list the UI consumes.
 *
 * <p>The synthetic {@link FileItem#getPath()} returned for each row is the
 * <em>trash entry id</em>, not a filesystem path — the Trash tab uses paths
 * as selection keys and never tries to open them as files.</p>
 */
public final class TrashRepository {

    private static volatile TrashRepository instance;

    private TrashRepository() { }

    @NonNull
    public static TrashRepository getInstance() {
        TrashRepository local = instance;
        if (local == null) {
            synchronized (TrashRepository.class) {
                local = instance;
                if (local == null) {
                    local = new TrashRepository();
                    instance = local;
                }
            }
        }
        return local;
    }

    @NonNull
    public List<FileItemComponent> getEntries(@NonNull SortCriteria sort) {
        List<TrashEntry> raw = TrashIndex.getInstance().getAll();
        List<FileItem> items = new ArrayList<>(raw.size());
        for (TrashEntry e : raw) {
            items.add(toFileItem(e));
        }
        sortInPlace(items, sort);
        Context ctx = FileSystemManager.getInstance().requireContext();
        List<FileItemComponent> out = new ArrayList<>(items.size());
        for (FileItem item : items) {
            FileItemComponent comp = new BaseFileItem(item);
            int iconRes = MimeTypeHelper.getIconResource(ctx, item.getFileType());
            if (iconRes != 0) {
                comp = new IconDecorator(comp, iconRes);
            }
            comp = new ColorDecorator(comp, tintFor(ctx, item.getFileType()));
            comp = new SystemTagDecorator(comp, "TRASH");
            out.add(comp);
        }
        return out;
    }

    @NonNull
    private static FileItem toFileItem(@NonNull TrashEntry entry) {
        String name = entry.getDisplayName();
        FileType type = entry.isDirectory()
                ? FileType.FOLDER
                : MimeTypeHelper.getFileTypeFromName(name);
        return new FileItem(
                name,
                entry.getId(),                    // synthetic "path" == id
                entry.getOriginalSize(),
                entry.getDeletedAt(),
                entry.isDirectory(),
                false,                            // not hidden
                true,                             // readable
                false,                            // writable
                type,
                null);
    }

    private static void sortInPlace(@NonNull List<FileItem> items, @NonNull SortCriteria sort) {
        switch (sort) {
            case DATE_ASC:
                Collections.sort(items, Comparator.comparingLong(FileItem::getLastModified));
                break;
            case DATE_DESC:
                Collections.sort(items, (a, b) -> Long.compare(b.getLastModified(), a.getLastModified()));
                break;
            default:
                DirectoryScannerService.sortInPlace(items, sort, false);
                break;
        }
    }

    private static int tintFor(@NonNull Context ctx, @NonNull FileType type) {
        int colorRes;
        switch (type) {
            case FOLDER:   colorRes = R.color.category_folder; break;
            case IMAGE:    colorRes = R.color.category_image; break;
            case VIDEO:    colorRes = R.color.category_video; break;
            case AUDIO:    colorRes = R.color.category_audio; break;
            case DOCUMENT: colorRes = R.color.category_document; break;
            case ARCHIVE:  colorRes = R.color.category_archive; break;
            case APK:      colorRes = R.color.category_apk; break;
            case TEXT:     colorRes = R.color.category_text; break;
            case UNKNOWN:
            default:       colorRes = R.color.category_unknown; break;
        }
        return ContextCompat.getColor(ctx, colorRes);
    }
}
