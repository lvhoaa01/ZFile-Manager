package com.zfile.manager;

import android.app.Application;

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
        FileSystemManager.getInstance().initialize(this);
        // Touch the singleton so its thread pools spin up eagerly.
        ThreadPoolManager.getInstance();
        TrashIndex.getInstance().load(this);
        new RecycleBinService().scheduleCleanup();
    }
}
