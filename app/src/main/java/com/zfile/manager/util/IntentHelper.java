package com.zfile.manager.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.zfile.manager.R;
import com.zfile.manager.core.MimeTypeHelper;

import java.io.File;

/**
 * Builds {@link Intent}s for opening / sharing files through other apps,
 * routing access through the manifest-registered {@code FileProvider}
 * so we never expose raw {@code file://} URIs.
 */
public final class IntentHelper {

    private IntentHelper() { }

    public static void openWith(@NonNull Context context, @NonNull File file) {
        Uri uri = getUriForFile(context, file);
        String mime = MimeTypeHelper.getMimeType(file);
        if (mime == null) mime = "*/*";

        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(Intent.createChooser(intent, file.getName()));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_no_app_for_file, Toast.LENGTH_SHORT).show();
        }
    }

    public static void share(@NonNull Context context, @NonNull File file) {
        Uri uri = getUriForFile(context, file);
        String mime = MimeTypeHelper.getMimeType(file);
        if (mime == null) mime = "*/*";

        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent,
                context.getString(R.string.share_chooser_title, file.getName())));
    }

    @NonNull
    private static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
        String authority = context.getPackageName() + ".fileprovider";
        return FileProvider.getUriForFile(context, authority, file);
    }
}
