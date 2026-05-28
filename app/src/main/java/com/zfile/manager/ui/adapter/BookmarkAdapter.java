package com.zfile.manager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.R;
import com.zfile.manager.model.Bookmark;

/**
 * ListAdapter for the Bookmarks screen. Tapping a row opens the folder; the
 * trailing delete button removes the bookmark.
 */
public class BookmarkAdapter extends ListAdapter<Bookmark, BookmarkAdapter.VH> {

    public interface OnBookmarkListener {
        void onOpen(@NonNull Bookmark bookmark);
        void onDelete(@NonNull Bookmark bookmark);
    }

    @Nullable private OnBookmarkListener listener;

    public BookmarkAdapter() {
        super(DIFF);
    }

    public void setListener(@Nullable OnBookmarkListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bookmark, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Bookmark item = getItem(position);
        holder.name.setText(item.getName());
        holder.path.setText(item.getPath());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView path;
        final ImageButton delete;

        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.bookmarkName);
            path = itemView.findViewById(R.id.bookmarkPath);
            delete = itemView.findViewById(R.id.bookmarkDelete);
        }
    }

    private static final DiffUtil.ItemCallback<Bookmark> DIFF = new DiffUtil.ItemCallback<Bookmark>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bookmark a, @NonNull Bookmark b) {
            return a.getPath().equals(b.getPath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bookmark a, @NonNull Bookmark b) {
            return a.getName().equals(b.getName());
        }
    };
}
