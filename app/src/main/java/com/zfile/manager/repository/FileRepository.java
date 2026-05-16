package com.zfile.manager.repository;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.model.decorator.BaseFileItem;
import com.zfile.manager.model.decorator.ColorDecorator;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.model.decorator.IconDecorator;
import com.zfile.manager.model.decorator.SystemTagDecorator;
import com.zfile.manager.service.DirectoryScannerService;
import com.zfile.manager.service.FileTransferService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton facade that hides the service layer from ViewModels.
 *
 * <p>Reads delegate to {@link DirectoryScannerService} and wrap each {@link FileItem}
 * in a decorator chain ({@link IconDecorator} → {@link ColorDecorator} → optional
 * {@link SystemTagDecorator}) so the UI receives view-ready {@link FileItemComponent}s.</p>
 *
 * <p>Writes (copy/move/delete/create/rename) delegate to {@link FileTransferService}.</p>
 */
public final class FileRepository {

    private static volatile FileRepository instance;

    @NonNull private final DirectoryScannerService scanner = new DirectoryScannerService();
    @NonNull private final FileTransferService transfer = new FileTransferService();

    private FileRepository() { }

    @NonNull
    public static FileRepository getInstance() {
        FileRepository local = instance;
        if (local == null) {
            synchronized (FileRepository.class) {
                local = instance;
                if (local == null) {
                    local = new FileRepository();
                    instance = local;
                }
            }
        }
        return local;
    }

    @NonNull
    public List<FileItemComponent> loadDirectory(@NonNull String path) {
        FileSystemManager fsm = FileSystemManager.getInstance();
        List<FileItem> raw = scanner.scanDirectory(
                path,
                fsm.isShowHiddenFiles(),
                fsm.getSortCriteria(),
                fsm.isFoldersFirst());
        return decorate(raw);
    }

    @NonNull
    public List<FileItem> loadDirectoryRaw(@NonNull String path) {
        FileSystemManager fsm = FileSystemManager.getInstance();
        return scanner.scanDirectory(
                path,
                fsm.isShowHiddenFiles(),
                fsm.getSortCriteria(),
                fsm.isFoldersFirst());
    }

    public boolean exists(@NonNull String path) {
        return scanner.exists(path);
    }

    public boolean isDirectory(@NonNull String path) {
        return scanner.isDirectory(path);
    }

    public void copy(@NonNull List<String> sources,
                     @NonNull String destDir,
                     @Nullable FileTransferService.ProgressCallback cb,
                     @NonNull AtomicBoolean cancelled) {
        transfer.copyFiles(sources, destDir, cb, cancelled);
    }

    public void move(@NonNull List<String> sources,
                     @NonNull String destDir,
                     @Nullable FileTransferService.ProgressCallback cb,
                     @NonNull AtomicBoolean cancelled) {
        transfer.moveFiles(sources, destDir, cb, cancelled);
    }

    public boolean delete(@NonNull String path) {
        return transfer.deleteRecursive(path);
    }

    public boolean createFolder(@NonNull String parentDir, @NonNull String name) {
        return transfer.createFolder(parentDir, name);
    }

    public boolean createFile(@NonNull String parentDir, @NonNull String name) throws IOException {
        return transfer.createFile(parentDir, name);
    }

    public boolean rename(@NonNull String path, @NonNull String newName) {
        return transfer.rename(path, newName);
    }

    @NonNull
    private List<FileItemComponent> decorate(@NonNull List<FileItem> items) {
        Context ctx = FileSystemManager.getInstance().requireContext();
        List<FileItemComponent> out = new ArrayList<>(items.size());
        for (FileItem item : items) {
            FileItemComponent comp = new BaseFileItem(item);
            int iconRes = MimeTypeHelper.getIconResource(ctx, item.getFileType());
            if (iconRes != 0) {
                comp = new IconDecorator(comp, iconRes);
            }
            comp = new ColorDecorator(comp, tintFor(item.getFileType()));
            if (item.isHidden()) {
                comp = new SystemTagDecorator(comp, "HIDDEN");
            }
            out.add(comp);
        }
        return out;
    }

    private static int tintFor(@NonNull FileType type) {
        switch (type) {
            case FOLDER:   return Color.parseColor("#FFC107");
            case IMAGE:    return Color.parseColor("#4CAF50");
            case VIDEO:    return Color.parseColor("#F44336");
            case AUDIO:    return Color.parseColor("#9C27B0");
            case DOCUMENT: return Color.parseColor("#2196F3");
            case ARCHIVE:  return Color.parseColor("#795548");
            case APK:      return Color.parseColor("#3DDC84");
            case TEXT:     return Color.parseColor("#607D8B");
            case UNKNOWN:
            default:       return Color.parseColor("#9E9E9E");
        }
    }
}
