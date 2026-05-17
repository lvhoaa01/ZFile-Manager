package com.zfile.manager;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.ui.adapter.BreadcrumbAdapter;
import com.zfile.manager.viewmodel.FileBrowserViewModel;

import java.io.File;
import java.util.List;

/**
 * Single hosting activity. Owns the {@link Toolbar} and the horizontal breadcrumb
 * {@link RecyclerView}; the file list, FAB, permission UI, ActionMode and dialogs
 * all live inside the embedded {@code FileBrowserFragment}.
 *
 * <p>The breadcrumb is wired here (not in the fragment) because the toolbar +
 * breadcrumb collectively form the app bar — both belong to the activity-level chrome.</p>
 */
public class MainActivity extends AppCompatActivity {

    private FileBrowserViewModel viewModel;
    private BreadcrumbAdapter breadcrumbAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Must run before super.onCreate(); transitions from Theme.ZFileManager.Splash
        // to postSplashScreenTheme (Theme.ZFileManager) once the splash is dismissed.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(FileBrowserViewModel.class);

        breadcrumbAdapter = new BreadcrumbAdapter();
        RecyclerView breadcrumb = findViewById(R.id.breadcrumbRecycler);
        breadcrumb.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        breadcrumb.setAdapter(breadcrumbAdapter);
        breadcrumbAdapter.setOnSegmentClickListener(this::navigateToSegment);

        viewModel.getPathSegments().observe(this, breadcrumbAdapter::setSegments);
    }

    private void navigateToSegment(int index) {
        List<String> segments = viewModel.getPathSegments().getValue();
        if (segments == null || index < 0 || index >= segments.size()) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            String s = segments.get(i);
            if (s.isEmpty() || "/".equals(s)) continue;
            sb.append(File.separator).append(s);
        }
        String target = sb.length() == 0 ? File.separator : sb.toString();
        viewModel.loadDirectory(target);
    }
}
