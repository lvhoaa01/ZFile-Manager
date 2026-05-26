package com.zfile.manager.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.R;
import com.zfile.manager.core.MimeTypeHelper;
import com.zfile.manager.model.FileItem;
import com.zfile.manager.model.FileType;
import com.zfile.manager.util.FileUtils;

/**
 * Compact ListAdapter used by both "Largest files" and "Largest folders" sections
 * of the Storage Analyzer. Rows are size-emphasised — file/folder size is the
 * primary visual; name and path are secondary.
 */
public class TopFilesAdapter extends ListAdapter<FileItem, TopFilesAdapter.VH> {

    public interface OnTopFileClickListener {
        void onClick(@NonNull FileItem item);
    }

    @Nullable private OnTopFileClickListener listener;

    public TopFilesAdapter() {
        super(DIFF);
    }

    public void setOnClickListener(@Nullable OnTopFileClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_file, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem item = getItem(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView path;
        final TextView size;

        VH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.topFileIcon);
            name = itemView.findViewById(R.id.topFileName);
            path = itemView.findViewById(R.id.topFilePath);
            size = itemView.findViewById(R.id.topFileSize);
        }

        void bind(@NonNull FileItem item) {
            int iconRes = MimeTypeHelper.getIconResource(itemView.getContext(), item.getFileType());
            if (iconRes == 0) {
                iconRes = item.isDirectory()
                        ? R.drawable.ic_folder
                        : R.drawable.ic_file_unknown;
            }
            icon.setImageResource(iconRes);
            int colorRes = colorFor(item.getFileType(), item.isDirectory());
            icon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), colorRes)));

            name.setText(item.getName());
            path.setText(item.getPath());
            size.setText(FileUtils.formatSize(item.getSize()));
        }

        private static int colorFor(@NonNull FileType type, boolean isDirectory) {
            if (isDirectory) return R.color.category_folder;
            switch (type) {
                case IMAGE:    return R.color.category_image;
                case VIDEO:    return R.color.category_video;
                case AUDIO:    return R.color.category_audio;
                case DOCUMENT: return R.color.category_document;
                case ARCHIVE:  return R.color.category_archive;
                case APK:      return R.color.category_apk;
                case TEXT:     return R.color.category_text;
                case FOLDER:   return R.color.category_folder;
                case UNKNOWN:
                default:       return R.color.category_unknown;
            }
        }
    }

    private static final DiffUtil.ItemCallback<FileItem> DIFF = new DiffUtil.ItemCallback<FileItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull FileItem a, @NonNull FileItem b) {
            return a.getPath().equals(b.getPath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FileItem a, @NonNull FileItem b) {
            return a.getSize() == b.getSize()
                    && a.getLastModified() == b.getLastModified()
                    && a.getName().equals(b.getName());
        }
    };
}
