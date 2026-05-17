package com.zfile.manager.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.zfile.manager.R;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.model.decorator.FileItemComponent;
import com.zfile.manager.util.FileUtils;

import java.util.Collections;
import java.util.Set;

/**
 * RecyclerView adapter that binds {@link FileItemComponent}s (from the decorator
 * chain) to row views in {@link R.layout#item_file_list}. Uses {@link DiffUtil}
 * keyed by path so updates animate cleanly.
 *
 * <p>Selection visibility is controlled by {@link #setSelectionMode(boolean)};
 * checked state mirrors a {@link Set} of paths kept in sync by the Fragment from
 * {@code FileBrowserViewModel.getSelectedPaths()}.</p>
 */
public class FileListAdapter extends ListAdapter<FileItemComponent, FileListAdapter.FileVH> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull FileItemComponent item, int position);

        void onItemLongClick(@NonNull FileItemComponent item, int position);
    }

    @Nullable private OnItemClickListener listener;
    @NonNull private Set<String> selectedPaths = Collections.emptySet();
    private boolean selectionMode = false;

    public FileListAdapter() {
        super(DIFF);
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        if (this.selectionMode != enabled) {
            this.selectionMode = enabled;
            notifyItemRangeChanged(0, getItemCount(), Payload.SELECTION);
        }
    }

    public void setSelectedPaths(@NonNull Set<String> paths) {
        this.selectedPaths = paths;
        notifyItemRangeChanged(0, getItemCount(), Payload.SELECTION);
    }

    @NonNull
    @Override
    public FileVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_list, parent, false);
        return new FileVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileVH holder, int position) {
        FileItemComponent item = getItem(position);
        holder.bind(item);
        holder.bindSelection(selectionMode, selectedPaths.contains(item.getPath()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, holder.getBindingAdapterPosition());
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener == null) return false;
            listener.onItemLongClick(item, holder.getBindingAdapterPosition());
            return true;
        });
    }

    @Override
    public void onBindViewHolder(@NonNull FileVH holder, int position, @NonNull java.util.List<Object> payloads) {
        if (payloads.contains(Payload.SELECTION)) {
            FileItemComponent item = getItem(position);
            holder.bindSelection(selectionMode, selectedPaths.contains(item.getPath()));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    enum Payload { SELECTION }

    static class FileVH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView subtitle;
        final MaterialCheckBox check;

        FileVH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            subtitle = itemView.findViewById(R.id.subtitle);
            check = itemView.findViewById(R.id.check);
        }

        void bind(@NonNull FileItemComponent component) {
            FileItem fi = component.getFileItem();
            int iconRes = component.getIconResource();
            int tint = component.getTintColor();

            if (fi.getFileType() == FileType.IMAGE && !fi.isDirectory()) {
                // Glide loads the thumbnail; tint must be cleared so the bitmap isn't recolored.
                icon.setImageTintList(null);
                Glide.with(icon.getContext())
                        .load(fi.asFile())
                        .apply(IMAGE_THUMB_OPTIONS)
                        .placeholder(iconRes != 0 ? iconRes : R.drawable.ic_file_image)
                        .error(R.drawable.ic_file_image)
                        .into(icon);
            } else {
                Glide.with(icon.getContext()).clear(icon);
                icon.setImageResource(iconRes != 0 ? iconRes : R.drawable.ic_file_unknown);
                icon.setImageTintList(tint != 0 ? ColorStateList.valueOf(tint) : null);
            }

            String display = component.getName();
            String tag = component.getTag();
            if (tag != null && !tag.isEmpty()) {
                display = display + "  " + tag;
            }
            name.setText(display);

            if (fi.isDirectory()) {
                int count = fi.getChildCount();
                String countText;
                if (count < 0) {
                    countText = "";
                } else if (count == 0) {
                    countText = itemView.getContext().getString(R.string.folder_empty);
                } else {
                    countText = itemView.getResources().getQuantityString(
                            R.plurals.folder_item_count, count, count);
                }
                String date = FileUtils.formatDate(fi.getLastModified());
                subtitle.setText(countText.isEmpty() ? date : countText + " • " + date);
            } else {
                subtitle.setText(FileUtils.formatSize(fi.getSize())
                        + " • " + FileUtils.formatDate(fi.getLastModified()));
            }
        }

        void bindSelection(boolean selectionMode, boolean selected) {
            check.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            check.setChecked(selected);
            itemView.setActivated(selected);
        }
    }

    private static final RequestOptions IMAGE_THUMB_OPTIONS = new RequestOptions()
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .override(160, 160);

    private static final DiffUtil.ItemCallback<FileItemComponent> DIFF = new DiffUtil.ItemCallback<FileItemComponent>() {
        @Override
        public boolean areItemsTheSame(@NonNull FileItemComponent a, @NonNull FileItemComponent b) {
            return a.getPath().equals(b.getPath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FileItemComponent a, @NonNull FileItemComponent b) {
            FileItem fa = a.getFileItem();
            FileItem fb = b.getFileItem();
            return fa.getName().equals(fb.getName())
                    && fa.getSize() == fb.getSize()
                    && fa.getLastModified() == fb.getLastModified()
                    && a.getIconResource() == b.getIconResource()
                    && a.getTintColor() == b.getTintColor();
        }
    };
}
