package com.fabri.ministerium;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImageView extends ImageView {
    private final Matrix imageMatrix = new Matrix();
    private final ScaleGestureDetector scaleDetector;
    private float currentScale = 1f;
    private float baseScale = 1f;
    private float lastX;
    private float lastY;
    private boolean dragging;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float proposed = currentScale * detector.getScaleFactor();
                float bounded = Math.max(baseScale, Math.min(baseScale * 5f, proposed));
                float factor = bounded / currentScale;
                imageMatrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                currentScale = bounded;
                setImageMatrix(imageMatrix);
                return true;
            }
        });
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::resetZoom);
    }

    public void resetZoom() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) return;
        float scaleX = (float) getWidth() / drawable.getIntrinsicWidth();
        float scaleY = (float) getHeight() / drawable.getIntrinsicHeight();
        baseScale = Math.min(scaleX, scaleY);
        currentScale = baseScale;
        float dx = (getWidth() - drawable.getIntrinsicWidth() * baseScale) / 2f;
        float dy = (getHeight() - drawable.getIntrinsicHeight() * baseScale) / 2f;
        imageMatrix.reset();
        imageMatrix.postScale(baseScale, baseScale);
        imageMatrix.postTranslate(dx, dy);
        setImageMatrix(imageMatrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress() && currentScale > baseScale) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    imageMatrix.postTranslate(dx, dy);
                    setImageMatrix(imageMatrix);
                }
                lastX = event.getX();
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return true;
        }
    }
}
