package com.zfile.manager.ui.viewer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.zfile.manager.R;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.FileType;
import com.zfile.manager.util.IntentHelper;

import java.io.File;
import java.util.Locale;

/**
 * Host activity for the in-app viewers. Picks the right viewer fragment from the
 * file's type and shows it. Unsupported types fall back to an external app via
 * {@link IntentHelper#openWith}. The launching screen should only start this
 * activity when {@link #isSupported(File)} is true.
 *
 * <p>Handles IMAGE, TEXT/code, VIDEO and AUDIO.</p>
 */
public class ViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_FILE_LIST = "extra_file_list";
    public static final String EXTRA_CURRENT_INDEX = "extra_current_index";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        String path = getIntent().getStringExtra(EXTRA_FILE_PATH);
        if (path == null) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            File file = new File(path);
            Fragment fragment = fragmentFor(file);
            if (fragment == null) {
                // Type not viewable in-app — hand off to an external app.
                IntentHelper.openWith(this, file);
                finish();
                return;
            }
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.viewerContainer, fragment)
                    .commit();
        }
    }

    /** True if a file can be previewed in-app (used by the file browser to decide routing). */
    public static boolean isSupported(@NonNull File file) {
        return isImage(file) || isText(file) || isVideo(file) || isAudio(file);
    }

    public static boolean isImage(@NonNull File file) {
        return MimeTypeHelper.getFileType(file) == FileType.IMAGE && !"svg".equals(extension(file));
    }

    public static boolean isText(@NonNull File file) {
        return MimeTypeHelper.getFileType(file) == FileType.TEXT;
    }

    public static boolean isVideo(@NonNull File file) {
        return MimeTypeHelper.getFileType(file) == FileType.VIDEO;
    }

    public static boolean isAudio(@NonNull File file) {
        return MimeTypeHelper.getFileType(file) == FileType.AUDIO;
    }

    @Nullable
    private static Fragment fragmentFor(@NonNull File file) {
        if (isImage(file)) return new ImageViewerFragment();
        if (isText(file)) return new TextViewerFragment();
        if (isVideo(file)) return new VideoViewerFragment();
        if (isAudio(file)) return new AudioPlayerFragment();
        return null; // unsupported → external fallback
    }

    @Nullable
    private static String extension(@NonNull File file) {
        String ext = MimeTypeHelper.getExtension(file.getName());
        return ext == null ? null : ext.toLowerCase(Locale.ROOT);
    }
}
