package com.zfile.manager.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.zfile.manager.R;
import com.zfile.manager.model.CategoryType;
import com.zfile.manager.util.FileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Donut chart rendering a {@code Map<CategoryType, Long>} as colored arcs plus
 * a "Free" slice for the difference between total capacity and the used bytes.
 *
 * <p>Touches inside a slice fire {@link OnSliceClickListener} so the fragment
 * can navigate to that category's detail screen.</p>
 */
public class StoragePieChart extends View {

    public interface OnSliceClickListener {
        void onSliceClick(@NonNull CategoryType type);
    }

    private static final Map<CategoryType, Integer> COLOR_RES = buildColorRes();

    @NonNull private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull private final Paint usedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull private final Paint capTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @NonNull private final RectF arcRect = new RectF();

    @NonNull private Map<CategoryType, Long> data = new EnumMap<>(CategoryType.class);
    private long totalCapacity = 0L;
    private long usedBytes = 0L;
    private int freeColor;

    @Nullable private OnSliceClickListener listener;

    /** Slice angles parallel to {@link CategoryType#values()}; last entry is "Free". */
    @NonNull private final List<SliceMeta> slices = new ArrayList<>();

    public StoragePieChart(@NonNull Context context) {
        super(context);
        init(context);
    }

    public StoragePieChart(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StoragePieChart(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        freeColor = ContextCompat.getColor(context, R.color.category_unknown);
        // Theme-aware so dark mode renders correctly.
        holePaint.setColor(resolveThemeColor(
                context, com.google.android.material.R.attr.colorSurface, Color.WHITE));
        int textColor = resolveThemeColor(
                context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        usedTextPaint.setColor(textColor);
        usedTextPaint.setTextAlign(Paint.Align.CENTER);
        capTextPaint.setColor(textColor);
        capTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private static int resolveThemeColor(@NonNull Context context, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) {
                return ContextCompat.getColor(context, tv.resourceId);
            }
            return tv.data;
        }
        return fallback;
    }

    public void setData(@NonNull Map<CategoryType, Long> categoryBytes,
                        long usedBytes,
                        long totalCapacity) {
        this.data = new EnumMap<>(categoryBytes);
        this.usedBytes = usedBytes;
        this.totalCapacity = totalCapacity;
        invalidate();
    }

    public void setOnSliceClickListener(@Nullable OnSliceClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(w, h);
        if (size <= 0) size = w;
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float pad = w * 0.05f;
        arcRect.set(pad, pad, w - pad, h - pad);

        slices.clear();

        if (totalCapacity <= 0L) {
            slicePaint.setColor(freeColor);
            canvas.drawArc(arcRect, 0f, 360f, true, slicePaint);
            return;
        }

        float startAngle = -90f;

        // Categorized slices.
        for (CategoryType t : CategoryType.values()) {
            Long bytes = data.get(t);
            if (bytes == null || bytes <= 0L) continue;
            float sweep = 360f * bytes / (float) totalCapacity;
            slicePaint.setColor(ContextCompat.getColor(getContext(),
                    COLOR_RES.getOrDefault(t, R.color.category_unknown)));
            canvas.drawArc(arcRect, startAngle, sweep, true, slicePaint);
            slices.add(new SliceMeta(t, startAngle, sweep));
            startAngle += sweep;
        }

        // Uncategorized used (used - categorized) gets the unknown tint.
        long categorized = 0L;
        for (Long v : data.values()) if (v != null) categorized += v;
        long uncategorized = Math.max(0L, usedBytes - categorized);
        if (uncategorized > 0L) {
            float sweep = 360f * uncategorized / (float) totalCapacity;
            slicePaint.setColor(ContextCompat.getColor(getContext(), R.color.category_unknown));
            canvas.drawArc(arcRect, startAngle, sweep, true, slicePaint);
            startAngle += sweep;
        }

        // Free slice fills the remainder.
        float freeSweep = Math.max(0f, 360f - (startAngle + 90f));
        if (freeSweep > 0f) {
            slicePaint.setColor(freeColor);
            canvas.drawArc(arcRect, startAngle, freeSweep, true, slicePaint);
        }

        // Donut hole + center text.
        float cx = w / 2f;
        float cy = h / 2f;
        float holeRadius = (Math.min(w, h) - 2 * pad) * 0.55f / 2f;
        canvas.drawCircle(cx, cy, holeRadius, holePaint);

        usedTextPaint.setTextSize(holeRadius * 0.38f);
        capTextPaint.setTextSize(holeRadius * 0.22f);
        String used = FileUtils.formatSize(usedBytes);
        String cap = "/ " + FileUtils.formatSize(totalCapacity);
        canvas.drawText(used, cx, cy, usedTextPaint);
        canvas.drawText(cap, cx, cy + capTextPaint.getTextSize() + 8f, capTextPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return super.onTouchEvent(event);
        if (listener == null || slices.isEmpty()) return false;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float radius = Math.min(arcRect.width(), arcRect.height()) / 2f;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float holeRadius = radius * 0.55f;
        if (dist < holeRadius || dist > radius) return false;
        // Angle from center, normalised so 0° is 12 o'clock and grows clockwise.
        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        angleDeg = (angleDeg + 360.0 + 90.0) % 360.0;
        // Slices start at -90° (top); shift their start by +90° for comparison.
        for (SliceMeta s : slices) {
            double sliceStart = (s.startAngle + 90.0 + 360.0) % 360.0;
            double sliceEnd = sliceStart + s.sweepAngle;
            if (sliceEnd <= 360.0) {
                if (angleDeg >= sliceStart && angleDeg < sliceEnd) {
                    listener.onSliceClick(s.type);
                    return true;
                }
            } else {
                // Wraps around 360 → split into two ranges.
                if (angleDeg >= sliceStart || angleDeg < sliceEnd - 360.0) {
                    listener.onSliceClick(s.type);
                    return true;
                }
            }
        }
        return false;
    }

    @NonNull
    private static Map<CategoryType, Integer> buildColorRes() {
        Map<CategoryType, Integer> m = new EnumMap<>(CategoryType.class);
        m.put(CategoryType.IMAGES,    R.color.category_image);
        m.put(CategoryType.VIDEOS,    R.color.category_video);
        m.put(CategoryType.AUDIO,     R.color.category_audio);
        m.put(CategoryType.DOCUMENTS, R.color.category_document);
        m.put(CategoryType.DOWNLOADS, R.color.category_folder);
        m.put(CategoryType.APKS,      R.color.category_apk);
        return Collections.unmodifiableMap(m);
    }

    private static final class SliceMeta {
        @NonNull final CategoryType type;
        final float startAngle;
        final float sweepAngle;

        SliceMeta(@NonNull CategoryType type, float startAngle, float sweepAngle) {
            this.type = type;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
        }
    }
}
