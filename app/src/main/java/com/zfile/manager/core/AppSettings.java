package com.zfile.manager.core;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.zfile.manager.model.SortCriteria;

/**
 * Singleton (DCL) single source of truth for all scalar user preferences,
 * persisted to SharedPreferences {@code "zfile_settings"}.
 *
 * <p>The three view preferences (hidden files, sort, folders-first) were
 * previously held only in-memory by {@link FileSystemManager}; that class now
 * delegates to this store so they survive process death. Theme mode and the
 * trash auto-purge period are new in Phase 4.</p>
 *
 * <p>Values are cached in volatile fields so hot read paths (directory scan /
 * sort) don't touch disk; {@link #load(Context)} must run once from
 * {@code Application.onCreate} before any reader.</p>
 */
public final class AppSettings {

    private static final String PREFS_NAME = "zfile_settings";

    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_TRASH_PURGE_DAYS = "trash_purge_days";
    private static final String KEY_SHOW_HIDDEN = "show_hidden";
    private static final String KEY_SORT_CRITERIA = "sort_criteria";
    private static final String KEY_FOLDERS_FIRST = "folders_first";

    public static final int DEFAULT_TRASH_PURGE_DAYS = 30;

    private static volatile AppSettings instance;

    @Nullable private Context appContext;

    private volatile int themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    private volatile int trashPurgeDays = DEFAULT_TRASH_PURGE_DAYS;
    private volatile boolean showHidden = false;
    @NonNull private volatile SortCriteria sortCriteria = SortCriteria.NAME_ASC;
    private volatile boolean foldersFirst = true;

    private AppSettings() { }

    @NonNull
    public static AppSettings getInstance() {
        AppSettings local = instance;
        if (local == null) {
            synchronized (AppSettings.class) {
                local = instance;
                if (local == null) {
                    local = new AppSettings();
                    instance = local;
                }
            }
        }
        return local;
    }
    
    public synchronized void load(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        SharedPreferences p = prefs();
        
        themeMode = p.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        trashPurgeDays = p.getInt(KEY_TRASH_PURGE_DAYS, DEFAULT_TRASH_PURGE_DAYS);
        showHidden = p.getBoolean(KEY_SHOW_HIDDEN, false);
        foldersFirst = p.getBoolean(KEY_FOLDERS_FIRST, true);
        sortCriteria = parseSort(p.getString(KEY_SORT_CRITERIA, SortCriteria.NAME_ASC.name()));
    }

    public int getThemeMode() { return themeMode; }

    public void setThemeMode(int mode) {
        themeMode = mode;
        prefs().edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getTrashPurgeDays() { return trashPurgeDays; }

    public void setTrashPurgeDays(int days) {
        trashPurgeDays = days; 
        prefs().edit().putInt(KEY_TRASH_PURGE_DAYS, days).apply();

    }

    public boolean isShowHidden() { return showHidden; }

    public void setShowHidden(boolean show) {
        showHidden = show;
        prefs().edit().putBoolean(KEY_SHOW_HIDDEN, show).apply();
    }

    @NonNull
    public SortCriteria getSortCriteria() { return sortCriteria; }

    public void setSortCriteria(@NonNull SortCriteria criteria) {
        sortCriteria = criteria;
        prefs().edit().putString(KEY_SORT_CRITERIA, criteria.name()).apply();
    }

    public boolean isFoldersFirst() { return foldersFirst; }

    public void setFoldersFirst(boolean v) {
        foldersFirst = v;
        prefs().edit().putBoolean(KEY_FOLDERS_FIRST, v).apply();
    }

    @NonNull
    private static SortCriteria parseSort(@Nullable String name) {
        if (name == null) return SortCriteria.NAME_ASC;
        try {
            return SortCriteria.valueOf(name);
        } catch (IllegalArgumentException e) {
            return SortCriteria.NAME_ASC;
        }
    }

    @NonNull
    private SharedPreferences prefs() {
        if (appContext == null) {
            throw new IllegalStateException("AppSettings.load(Context) must be called first");
        }
        
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
