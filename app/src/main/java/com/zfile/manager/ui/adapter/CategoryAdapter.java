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
 * with a tinted icon, localised label, and item count from MediaStore.
 *
 * <p>Resources are resolved by name ({@code @drawable/ic_*}, {@code @string/category_*},
 * {@code @color/category_*}) keyed by {@link CategoryType#getResourceSuffix} so adding
 * a new category only needs an enum value plus matching resource names.</p>
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryVH> {

    public interface OnCategoryClickListener {
        void onCategoryClick(@NonNull CategoryType type);
    }

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
            String suffix = type.getResourceSuffix();
            android.content.res.Resources res = itemView.getResources();
            String pkg = itemView.getContext().getPackageName();

            int iconId = res.getIdentifier("ic_category_" + suffix, "drawable", pkg);
            if (iconId == 0) iconId = res.getIdentifier("ic_file_" + suffix, "drawable", pkg);
            if (iconId == 0) iconId = fallbackIcon(type);
            icon.setImageResource(iconId);

            int colorId = res.getIdentifier("category_" + suffix, "color", pkg);
            if (colorId == 0) colorId = fallbackColorId(type);
            int color = ContextCompat.getColor(itemView.getContext(), colorId);
            icon.setImageTintList(ColorStateList.valueOf(color));

            int labelId = res.getIdentifier("category_" + suffix, "string", pkg);
            label.setText(labelId != 0 ? res.getString(labelId) : suffix);

            countText.setText(res.getQuantityString(R.plurals.category_item_count, count, count));
        }

        private static int fallbackIcon(@NonNull CategoryType type) {
            switch (type) {
                case IMAGES:    return R.drawable.ic_file_image;
                case VIDEOS:    return R.drawable.ic_file_video;
                case AUDIO:     return R.drawable.ic_file_audio;
                case DOCUMENTS: return R.drawable.ic_file_document;
                case APKS:      return R.drawable.ic_file_apk;
                case DOWNLOADS:
                default:        return R.drawable.ic_folder;
            }
        }

        private static int fallbackColorId(@NonNull CategoryType type) {
            switch (type) {
                case IMAGES:    return R.color.category_image;
                case VIDEOS:    return R.color.category_video;
                case AUDIO:     return R.color.category_audio;
                case DOCUMENTS: return R.color.category_document;
                case APKS:      return R.color.category_apk;
                case DOWNLOADS:
                default:        return R.color.category_folder;
            }
        }
    }
}
