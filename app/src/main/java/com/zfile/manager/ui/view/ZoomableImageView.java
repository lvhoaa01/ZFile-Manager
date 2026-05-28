package com.zfile.manager.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView with pinch-zoom, pan and double-tap-to-zoom built on a {@link Matrix}.
 *
 * <p>The image is fit to the view at "base" scale; zoom is clamped to
 * {@code [base, base * MAX_ZOOM]} and panning is bounded so the bitmap edges
 * never leave a gap. When at base scale the view lets a parent (e.g. ViewPager2)
 * intercept horizontal drags so paging keeps working; once zoomed it keeps the
 * gesture to pan.</p>
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MAX_ZOOM = 4f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];

    private float baseScale = 1f;
    private boolean ready = false;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        super.setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        ready = false;
        post(this::fitToView);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitToView();
    }

    private void fitToView() {
        Drawable d = getDrawable();
        if (d == null) return;
        int dw = d.getIntrinsicWidth();
        int dh = d.getIntrinsicHeight();
        int vw = getWidth();
        int vh = getHeight();
        if (dw <= 0 || dh <= 0 || vw == 0 || vh == 0) return;

        baseScale = Math.min((float) vw / dw, (float) vh / dh);
        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate((vw - dw * baseScale) / 2f, (vh - dh * baseScale) / 2f);
        setImageMatrix(matrix);
        ready = true;
    }

    private float currentScale() {
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private boolean isZoomed() {
        return currentScale() > baseScale * 1.01f;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!ready) return true;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Only hold the gesture from the parent pager when already zoomed in.
            requestDisallowIntercept(isZoomed());
        }
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void requestDisallowIntercept(boolean disallow) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(disallow);
    }

    /** Keep the bitmap pinned to the view edges; centre it on any axis where it is smaller. */
    private void fixTranslation() {
        matrix.getValues(values);
        float scale = values[Matrix.MSCALE_X];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        Drawable d = getDrawable();
        if (d == null) return;
        float imgW = d.getIntrinsicWidth() * scale;
        float imgH = d.getIntrinsicHeight() * scale;

        float fixX = clampTrans(transX, getWidth(), imgW);
        float fixY = clampTrans(transY, getHeight(), imgH);
        matrix.postTranslate(fixX - transX, fixY - transY);
    }

    private static float clampTrans(float trans, int viewSize, float imgSize) {
        if (imgSize <= viewSize) {
            // Smaller than the view → centre.
            return (viewSize - imgSize) / 2f;
        }
        float min = viewSize - imgSize; // most negative (right/bottom edge flush)
        float max = 0f;                 // left/top edge flush
        if (trans < min) return min;
        if (trans > max) return max;
        return trans;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            requestDisallowIntercept(true);
            return true;
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float current = currentScale();
            float target = current * factor;
            if (target < baseScale) factor = baseScale / current;
            else if (target > baseScale * MAX_ZOOM) factor = baseScale * MAX_ZOOM / current;
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (!isZoomed()) {
                requestDisallowIntercept(false); // let the pager swipe pages
                return false;
            }
            requestDisallowIntercept(true);
            matrix.postTranslate(-distanceX, -distanceY);
            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            float current = currentScale();
            float target = isZoomed() ? baseScale : baseScale * DOUBLE_TAP_ZOOM;
            float factor = target / current;
            matrix.postScale(factor, factor, e.getX(), e.getY());
            fixTranslation();
            setImageMatrix(matrix);
            requestDisallowIntercept(target > baseScale * 1.01f);
            return true;
        }
    }
}
