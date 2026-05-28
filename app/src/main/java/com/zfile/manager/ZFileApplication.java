package com.zfile.manager;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.zfile.manager.core.AppSettings;
import com.zfile.manager.core.BookmarkStore;
import com.zfile.manager.core.FileSystemManager;
import com.zfile.manager.core.ThreadPoolManager;
import com.zfile.manager.core.TrashIndex;
import com.zfile.manager.service.RecycleBinService;

/**
 * Application entry point. Initialises the process-wide singletons:
 * {@link FileSystemManager} (storage volumes, clipboard, prefs),
 * {@link ThreadPoolManager} (warm pools for I/O and search), and
 * {@link TrashIndex} (recycle-bin metadata).
 *
 * <p>Dynamic Color is applied automatically because {@code Theme.ZFileManager}
 * inherits from {@code Theme.Material3.DynamicColors.DayNight.NoActionBar} —
 * no explicit {@code DynamicColors.applyToActivitiesIfAvailable(this)} needed.</p>
 */
public class ZFileApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Settings first — FileSystemManager reads sort/hidden/folders-first through it,
        // and the saved night mode must be applied before any Activity is themed.
        AppSettings.getInstance().load(this);
        AppCompatDelegate.setDefaultNightMode(AppSettings.getInstance().getThemeMode());
        FileSystemManager.getInstance().initialize(this);
        // Touch the singleton so its thread pools spin up eagerly.
        ThreadPoolManager.getInstance();
        TrashIndex.getInstance().load(this);
        BookmarkStore.getInstance().load(this);
        new RecycleBinService().scheduleCleanup();
    }
}
