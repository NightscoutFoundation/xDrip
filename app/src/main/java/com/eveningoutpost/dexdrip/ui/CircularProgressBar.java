package com.eveningoutpost.dexdrip.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressBar extends View {
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF rectF;

    private long startTime = 0;
    private boolean isRunning = false;
    private final Handler handler = new Handler();
    private Runnable updateRunnable;

    private static final int FULL_CIRCLE_DEGREES = 360;
    private static final int TEN_MINUTES_MS = 10 * 60 * 1000;
    private static final int FIVE_MINUTES_MS = 5 * 60 * 1000;
    private static final int UPDATE_INTERVAL_MS = 1000;

    private static final int COLOR_GREEN = Color.GREEN;
    private static final int COLOR_YELLOW = Color.YELLOW;
    private static final int COLOR_RED = Color.RED;
    private static final int COLOR_BACKGROUND = Color.LTGRAY;
    private static final int COLOR_TEXT = Color.WHITE;

    public CircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(COLOR_BACKGROUND);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(10f);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(COLOR_GREEN);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(10f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    updateProgress();
                    invalidate();
                    handler.postDelayed(this, UPDATE_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float strokeWidth = 10f;

        rectF.set(strokeWidth, strokeWidth, width - strokeWidth, height - strokeWidth);

        canvas.drawArc(rectF, 0, FULL_CIRCLE_DEGREES, false, backgroundPaint);

        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(elapsed, TEN_MINUTES_MS);
        float angle = (progress / TEN_MINUTES_MS) * FULL_CIRCLE_DEGREES;
        canvas.drawArc(rectF, -90, angle, false, progressPaint);

        String timeText = formatTime(elapsed);
        float textY = rectF.centerY() - (textPaint.descent() + textPaint.ascent()) / 2;
        canvas.drawText(timeText, rectF.centerX(), textY, textPaint);
    }

    public void updateProgress(long timeSinceLastUpdMs) {
        stopTimer();
        startTime = System.currentTimeMillis() - timeSinceLastUpdMs;
        isRunning = true;
        updateProgress();
        invalidate();
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }

    private void updateProgress() {
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed <= FIVE_MINUTES_MS) {
            progressPaint.setColor(COLOR_GREEN);
        } else if (elapsed <= TEN_MINUTES_MS) {
            progressPaint.setColor(COLOR_YELLOW);
        } else {
            progressPaint.setColor(COLOR_RED);
        }
    }

    public void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(updateRunnable);
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopTimer();
    }
}