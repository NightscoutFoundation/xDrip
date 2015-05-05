package com.eveningoutpost.dexdrip.UtilityModels;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by matthiasgranberry on 5/4/15.
 */
public class BgSparklineBuilder {
    private Context mContext;

    private static final String TAG = "BgSparklineBuilder";
    private static final float NOTIFICATION_WIDTH_DP = 416;
    private static final float NOTIFICATION_HEIGHT_DP = 256;

    private float width = NOTIFICATION_WIDTH_DP;
    private float height = NOTIFICATION_HEIGHT_DP;
    private BgGraphBuilder bgGraphBuilder;
    private LineChartView chart;
    private long end = new Date().getTime() / BgGraphBuilder.FUZZER;
    private long start = end - (60000*180 / BgGraphBuilder.FUZZER); // 3h
    private boolean showLowLine = false;
    private boolean showHighLine = false;

    public BgSparklineBuilder setStart(long start) {
        this.start = start / BgGraphBuilder.FUZZER;
        return this;
    }

    public BgSparklineBuilder setEnd(long end) {
        this.end = end / BgGraphBuilder.FUZZER;
        return this;
    }

    public BgSparklineBuilder showHighLine(boolean show) {
        this.showHighLine = show;
        return this;
    }

    public BgSparklineBuilder showLowLine(boolean show) {
        this.showLowLine = show;
        return this;
    }

    public BgSparklineBuilder setWidth(float width) {
        this.width = width;
        return this;
    }

    public BgSparklineBuilder setHeight(float height) {
        this.height = height;
        return this;
    }

    public BgSparklineBuilder setBgGraphBuilder(BgGraphBuilder bgGraphBuilder) {
        this.bgGraphBuilder = bgGraphBuilder;
        return this;
    }

    BgSparklineBuilder(Context context) {
        mContext = context;
        chart = new LineChartView(mContext);
    }

    /**
     * Draw the view into a bitmap.
     */
    private Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    private int convertDpToPixel(float dp){
        Resources resources = mContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * (metrics.densityDpi / 160f));
        return px;
    }

    Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines();
        lines.add(bgGraphBuilder.inRangeValuesLine());
        lines.add(bgGraphBuilder.lowValuesLine());
        lines.add(bgGraphBuilder.highValuesLine());
        chart.setLineChartData(new LineChartData(lines));

        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport, false);
        chart.setLeft(0);
        chart.setTop(0);
        chart.setRight(convertDpToPixel(width));
        chart.setBottom(convertDpToPixel(height));
        if (showLowLine)
            lines.add(bgGraphBuilder.lowLine());
        if (showHighLine)
            lines.add(bgGraphBuilder.highLine());
        return getViewBitmap(chart);
    }
}
