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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.zfile.manager.R;
import com.zfile.manager.model.CategoryType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Grid adapter for the Categories tab. Each row is a {@link MaterialCardView}
 * with a tinted icon, label, and item count from MediaStore.
 *
 * <p>Resources are resolved via compile-time {@link EnumMap}s rather than runtime
 * {@code getIdentifier} lookups, so adding a new category requires a code change
 * — which is correct, because the corresponding MediaStore branch and resource
 * names need to be defined in lockstep anyway.</p>
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryVH> {

    public interface OnCategoryClickListener {
        void onCategoryClick(@NonNull CategoryType type);
    }

    @NonNull private static final Map<CategoryType, Integer> ICONS = buildIcons();
    @NonNull private static final Map<CategoryType, Integer> COLORS = buildColors();
    @NonNull private static final Map<CategoryType, Integer> LABELS = buildLabels();

    @NonNull private final List<CategoryType> categories =
            Arrays.asList(CategoryType.values());
    @NonNull private Map<CategoryType, Integer> counts = new EnumMap<>(CategoryType.class);
    @Nullable private OnCategoryClickListener listener;

    public void setCounts(@NonNull Map<CategoryType, Integer> counts) {
        this.counts = counts;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setOnCategoryClickListener(@Nullable OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new CategoryVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryVH holder, int position) {
        CategoryType type = categories.get(position);
        Integer count = counts.get(type);
        holder.bind(type, count == null ? 0 : count);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(type);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryVH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView countText;

        CategoryVH(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.categoryIcon);
            label = itemView.findViewById(R.id.categoryLabel);
            countText = itemView.findViewById(R.id.categoryCount);
        }

        void bind(@NonNull CategoryType type, int count) {
            //noinspection ConstantConditions
            icon.setImageResource(ICONS.get(type));
            //noinspection ConstantConditions
            int color = ContextCompat.getColor(itemView.getContext(), COLORS.get(type));
            icon.setImageTintList(ColorStateList.valueOf(color));

            //noinspection ConstantConditions
            label.setText(LABELS.get(type));
            countText.setText(itemView.getResources()
                    .getQuantityString(R.plurals.category_item_count, count, count));
        }
    }

    @NonNull
    private static Map<CategoryType, Integer> buildIcons() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        m.put(CategoryType.IMAGES,    R.drawable.ic_file_image);
        m.put(CategoryType.VIDEOS,    R.drawable.ic_file_video);
        m.put(CategoryType.AUDIO,     R.drawable.ic_file_audio);
        m.put(CategoryType.DOCUMENTS, R.drawable.ic_file_document);
        m.put(CategoryType.DOWNLOADS, R.drawable.ic_folder);
        m.put(CategoryType.APKS,      R.drawable.ic_file_apk);
        return m;
    }

    @NonNull
    private static Map<CategoryType, Integer> buildColors() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        m.put(CategoryType.IMAGES,    R.color.category_image);
        m.put(CategoryType.VIDEOS,    R.color.category_video);
        m.put(CategoryType.AUDIO,     R.color.category_audio);
        m.put(CategoryType.DOCUMENTS, R.color.category_document);
        m.put(CategoryType.DOWNLOADS, R.color.category_folder);
        m.put(CategoryType.APKS,      R.color.category_apk);
        return m;
    }

    @NonNull
    private static Map<CategoryType, Integer> buildLabels() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        m.put(CategoryType.IMAGES,    R.string.category_images);
        m.put(CategoryType.VIDEOS,    R.string.category_videos);
        m.put(CategoryType.AUDIO,     R.string.category_audio);
        m.put(CategoryType.DOCUMENTS, R.string.category_documents);
        m.put(CategoryType.DOWNLOADS, R.string.category_downloads);
        m.put(CategoryType.APKS,      R.string.category_apks);
        return m;
    }
}
