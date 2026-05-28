package com.zfile.manager.viewmodel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModel;

import com.zfile.manager.core.AppSettings;
import com.zfile.manager.model.SortCriteria;

/**
 * Thin coordinator over {@link AppSettings} for the Settings screen. Theme
 * changes additionally call {@link AppCompatDelegate#setDefaultNightMode(int)}
 * which recreates running activities so the new theme applies immediately.
 */
public class SettingsViewModel extends ViewModel {

    @NonNull
    private AppSettings settings() { return AppSettings.getInstance(); }

    public int getThemeMode() { return settings().getThemeMode(); }

    public void setThemeMode(int mode) {
        settings().setThemeMode(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    @NonNull
    public SortCriteria getSortCriteria() { return settings().getSortCriteria(); }

    public void setSortCriteria(@NonNull SortCriteria criteria) { settings().setSortCriteria(criteria); }

    public boolean isFoldersFirst() { return settings().isFoldersFirst(); }

    public void setFoldersFirst(boolean v) { settings().setFoldersFirst(v); }

    public boolean isShowHidden() { return settings().isShowHidden(); }

    public void setShowHidden(boolean v) { settings().setShowHidden(v); }

    public int getTrashPurgeDays() { return settings().getTrashPurgeDays(); }

    public void setTrashPurgeDays(int days) { settings().setTrashPurgeDays(days); }
}
