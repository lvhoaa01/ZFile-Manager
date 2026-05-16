package com.zfile.manager.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Storage permission gate. Two distinct flows because Android storage permissions
 * fundamentally changed at API 30:
 *
 * <ul>
 *   <li><b>API 30+ (Android 11+)</b>: requires {@code MANAGE_EXTERNAL_STORAGE} granted via
 *       the system Settings page; cannot be a runtime prompt. We open
 *       {@code ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION}.</li>
 *   <li><b>API 24-29</b>: runtime request for {@code READ_EXTERNAL_STORAGE}; combined with
 *       {@code requestLegacyExternalStorage="true"} in the manifest this also enables writes.</li>
 * </ul>
 */
public final class PermissionHelper {

    private PermissionHelper() { }

    public static boolean hasFullAccess(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestFullAccess(@NonNull Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent;
            try {
                intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
            } catch (Exception e) {
                intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivity(intent);
            }
        } else {
            activity.requestPermissions(
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    requestCode);
        }
    }
}
