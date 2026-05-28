package com.zfile.manager.ui.viewer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zfile.manager.R;
import com.zfile.manager.ui.view.ZoomableImageView;

import java.io.File;
import java.util.List;

/**
 * One page per image for the {@code ViewPager2} in {@link com.zfile.manager.ui.viewer.ImageViewerFragment}.
 * Glide decodes each file (capped at 2048px so large photos don't OOM while still
 * leaving detail to zoom into); the {@link ZoomableImageView} handles fit + gestures.
 */
public class ImagePageAdapter extends RecyclerView.Adapter<ImagePageAdapter.VH> {

    private static final int MAX_DECODE = 2048;

    @NonNull private final List<File> images;

    public ImagePageAdapter(@NonNull List<File> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_page, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Glide.with(holder.image)
                .load(images.get(position))
                .fitCenter()
                .override(MAX_DECODE, MAX_DECODE)
                .error(R.drawable.ic_file_image)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ZoomableImageView image;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imagePageView);
        }
    }
}
