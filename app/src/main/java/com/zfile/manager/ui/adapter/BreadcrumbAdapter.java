package com.zfile.manager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.zfile.manager.R;

import java.util.Collections;
import java.util.List;

/**
 * Horizontal RecyclerView adapter showing the path as tappable segments.
 * The Fragment passes a {@code List<String>} of segment names matching
 * {@code FileBrowserViewModel.getPathSegments()}.
 *
 * <p>Tapping a segment fires {@link OnSegmentClickListener} with the index;
 * Fragment resolves the absolute path by joining segments {@code [0..index]}.</p>
 */
public class BreadcrumbAdapter extends RecyclerView.Adapter<BreadcrumbAdapter.SegmentVH> {

    public interface OnSegmentClickListener {
        void onSegmentClick(int index);
    }

    @NonNull private List<String> segments = Collections.emptyList();
    @Nullable private OnSegmentClickListener listener;

    public void setSegments(@NonNull List<String> segments) {
        this.segments = segments;
        notifyDataSetChanged();
    }

    public void setOnSegmentClickListener(@Nullable OnSegmentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SegmentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_breadcrumb, parent, false);
        return new SegmentVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SegmentVH holder, int position) {
        String label = segments.get(position);
        if (label.isEmpty()) label = "/";
        holder.text.setText(label);
        holder.separator.setVisibility(position == segments.size() - 1 ? View.GONE : View.VISIBLE);
        holder.text.setOnClickListener(v -> {
            if (listener != null) listener.onSegmentClick(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return segments.size();
    }

    static class SegmentVH extends RecyclerView.ViewHolder {
        final TextView text;
        final TextView separator;

        SegmentVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.segmentText);
            separator = itemView.findViewById(R.id.separator);
        }
    }
}
