package com.eveningoutpost.dexdrip.ui.graphic;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.R;

public class CircularProgressBar extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;

    private int progressColor;
    private int backgroundColor;

    private float strokeWidth = 20f;

    private int max = 600;
    private int progress = 0;

    private RectF rectF;

    public CircularProgressBar(Context context) {
        super(context);
        init(null);
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CircularProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        rectF = new RectF();

        // Значения по умолчанию
        progressColor = ContextCompat.getColor(getContext(), android.R.color.holo_green_light);
        backgroundColor = ContextCompat.getColor(getContext(), android.R.color.darker_gray);

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CustomCircularProgressBar,
                    0, 0);

            try {
                progressColor = a.getColor(R.styleable.CustomCircularProgressBar_progressColor, progressColor);
                backgroundColor = a.getColor(R.styleable.CustomCircularProgressBar_backgroundColor, backgroundColor);
                strokeWidth = a.getDimension(R.styleable.CustomCircularProgressBar_strokeWidth, strokeWidth);
                max = a.getInt(R.styleable.CustomCircularProgressBar_max, max);
                progress = a.getInt(R.styleable.CustomCircularProgressBar_progress, progress);
            } finally {
                a.recycle();
            }
        }

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);

        float autoStrokeWidth = size * 0.15f; // 15% от размера
        float usedStrokeWidth = Math.min(strokeWidth, autoStrokeWidth);

        float backgroundStrokeWidth = usedStrokeWidth * 0.4f;
        float progressStrokeWidth = usedStrokeWidth;

        backgroundPaint.setStrokeWidth(backgroundStrokeWidth);
        progressPaint.setStrokeWidth(progressStrokeWidth);

        float padding = progressStrokeWidth / 2f;

        rectF.set(padding, padding, size - padding, size - padding);

        canvas.drawOval(rectF, backgroundPaint);

        float angle = 360f * progress / max;
        canvas.drawArc(rectF, -90, angle, false, progressPaint);
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
        progressPaint.setColor(progressColor);
        invalidate();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        backgroundPaint.setColor(backgroundColor);
        invalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        backgroundPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);
        invalidate();
        requestLayout();
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        if (max > 0) {
            this.max = max;
            if (progress > max) {
                progress = max;
            }
            invalidate();
        }
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > max) progress = max;
        this.progress = progress;
        invalidate();
    }
}
