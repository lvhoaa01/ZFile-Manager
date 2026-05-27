package com.zfile.manager;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zfile.manager.ui.adapter.BreadcrumbAdapter;
import com.zfile.manager.viewmodel.FileBrowserViewModel;

import java.io.File;
import java.util.List;

/**
 * Single hosting activity. Owns the {@link Toolbar}, the horizontal breadcrumb,
 * and the {@link BottomNavigationView} that switches between Browse / Categories /
 * Search destinations of {@code nav_main.xml}.
 *
 * <p>The breadcrumb is only meaningful on the file-browser destination, so it is
 * hidden whenever the {@link NavController} navigates elsewhere. The activity
 * also keeps the toolbar subtitle in sync with the current browse path via
 * {@link FileBrowserViewModel#getPathSegments()}.</p>
 */
public class MainActivity extends AppCompatActivity {

    private FileBrowserViewModel browserViewModel;
    private BreadcrumbAdapter breadcrumbAdapter;
    private RecyclerView breadcrumbRecycler;
    private Toolbar toolbar;
    @Nullable private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Must run before super.onCreate(); transitions from Theme.ZFileManager.Splash
        // to postSplashScreenTheme (Theme.ZFileManager) once the splash is dismissed.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        browserViewModel = new ViewModelProvider(this).get(FileBrowserViewModel.class);

        breadcrumbAdapter = new BreadcrumbAdapter();
        breadcrumbRecycler = findViewById(R.id.breadcrumbRecycler);
        breadcrumbRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        breadcrumbRecycler.setAdapter(breadcrumbAdapter);
        breadcrumbAdapter.setOnSegmentClickListener(this::navigateToSegment);
        browserViewModel.getPathSegments().observe(this, breadcrumbAdapter::setSegments);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost == null) {
            throw new IllegalStateException("NavHostFragment not found in activity_main.xml");
        }
        navController = navHost.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Breadcrumb is browser-only; reset toolbar title/subtitle on every nav change
        // so titles set by inner fragments (e.g. CategoryDetail) don't leak across screens.
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            int id = destination.getId();
            boolean onBrowser = id == R.id.fileBrowserFragment;
            breadcrumbRecycler.setVisibility(onBrowser ? View.VISIBLE : View.GONE);
            if (!onBrowser) {
                toolbar.setSubtitle(null);
            }
            // CategoryDetailFragment sets its own title (the category name) — skip override.
            if (id != R.id.categoryDetailFragment) {
                CharSequence label = destination.getLabel();
                toolbar.setTitle(onBrowser || label == null
                        ? getString(R.string.app_name)
                        : label);
            }
            // Re-evaluate overflow menu visibility for the new destination.
            invalidateOptionsMenu();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        // Hide each item when we're already on that destination — no point
        // navigating to a screen you're looking at.
        int currentId = navController != null && navController.getCurrentDestination() != null
                ? navController.getCurrentDestination().getId()
                : 0;
        MenuItem trash = menu.findItem(R.id.global_menu_trash);
        if (trash != null) trash.setVisible(currentId != R.id.trashFragment);
        MenuItem analyzer = menu.findItem(R.id.global_menu_analyzer);
        if (analyzer != null) analyzer.setVisible(currentId != R.id.storageAnalyzerFragment);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (navController == null) return super.onOptionsItemSelected(item);
        if (id == R.id.global_menu_trash) {
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.trashFragment) {
                navController.navigate(R.id.trashFragment);
            }
            return true;
        }
        if (id == R.id.global_menu_analyzer) {
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.storageAnalyzerFragment) {
                navController.navigate(R.id.storageAnalyzerFragment);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToSegment(int index) {
        List<String> segments = browserViewModel.getPathSegments().getValue();
        if (segments == null || index < 0 || index >= segments.size()) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            String s = segments.get(i);
            if (s.isEmpty() || "/".equals(s)) continue;
            sb.append(File.separator).append(s);
        }
        String target = sb.length() == 0 ? File.separator : sb.toString();
        browserViewModel.loadDirectory(target);
    }
}
