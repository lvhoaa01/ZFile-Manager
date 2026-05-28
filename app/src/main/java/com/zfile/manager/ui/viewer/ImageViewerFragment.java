package com.zfile.manager.ui.viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.zfile.manager.R;
import com.zfile.manager.ui.viewer.adapter.ImagePageAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen image viewer. Builds a {@link ViewPager2} over the image siblings
 * in the same folder (from {@link ViewerActivity#EXTRA_FILE_LIST}) so the user can
 * swipe between photos; each page is a zoomable image. Updates the toolbar title
 * via {@link #setCurrentFile(File)} as pages change.
 */
public class ImageViewerFragment extends BaseViewerFragment {

    @NonNull private final List<File> images = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        String currentPath = args != null ? args.getString(ViewerActivity.EXTRA_FILE_PATH) : null;
        List<String> all = args != null ? args.getStringArrayList(ViewerActivity.EXTRA_FILE_LIST) : null;

        int startIndex = 0;
        if (all != null) {
            for (String p : all) {
                File f = new File(p);
                if (ViewerActivity.isImage(f)) {
                    if (currentPath != null && currentPath.equals(p)) startIndex = images.size();
                    images.add(f);
                }
            }
        }
        if (images.isEmpty() && currentPath != null) {
            images.add(new File(currentPath));
        }

        ViewPager2 pager = view.findViewById(R.id.imagePager);
        pager.setAdapter(new ImagePageAdapter(images));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position >= 0 && position < images.size()) {
                    setCurrentFile(images.get(position));
                }
            }
        });
        pager.setCurrentItem(startIndex, false);
        if (startIndex >= 0 && startIndex < images.size()) {
            setCurrentFile(images.get(startIndex));
        }
    }
}
