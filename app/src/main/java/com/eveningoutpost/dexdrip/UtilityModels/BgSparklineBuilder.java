package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by matthiasgranberry on 5/4/15.
 */
public class BgSparklineBuilder {
    private Context mContext;

    private static final String TAG = "BgSparklineBuilder";
    private static final int NOTIFICATION_WIDTH_DP = 230; // 476 width minus 8 padding on each side is the native
                                                          // resolution, but use less for lower memory requirements
    private static final int NOTIFICATION_HEIGHT_DP = 128;

    private int width;
    private int height;
    private BgGraphBuilder bgGraphBuilder;
    private LineChartView chart;
    private long end = new Date().getTime() / BgGraphBuilder.FUZZER;
    private long start = end - (60000*180 / BgGraphBuilder.FUZZER); // 3h
    private boolean showLowLine = false;
    private boolean showHighLine = false;
    private boolean showAxes = false;
    private boolean useSmallDots = true;
    private boolean useTinyDots = false;

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

    public BgSparklineBuilder showHighLine() {
        return showHighLine(true);
    }

    public BgSparklineBuilder showLowLine(boolean show) {
        this.showLowLine = show;
        return this;
    }

    public BgSparklineBuilder showLowLine() {
        return showLowLine(true);
    }

    public BgSparklineBuilder showAxes(boolean show) {
        this.showAxes = show;
        return this;
    }

    public BgSparklineBuilder showAxes() {
        return showAxes(true);
    }

    public BgSparklineBuilder setWidth(float width) {
        this.width = convertDpToPixel(width);
        return this;
    }

    public BgSparklineBuilder setHeight(float height) {
        this.height = convertDpToPixel(height);
        return this;
    }

    public BgSparklineBuilder setWidthPx(int width) {
        this.width = width;
        return this;
    }

    public BgSparklineBuilder setHeightPx(int height) {
        this.height = height;
        return this;
    }

    public BgSparklineBuilder setSmallDots(boolean useSmallDots) {
        this.useSmallDots = useSmallDots;
        return this;
    }
    public BgSparklineBuilder setTinyDots(boolean useTinyDots) {
        this.useTinyDots = useTinyDots;
        return this;
    }

    public BgSparklineBuilder setSmallDots() {
        return this.setSmallDots(true);
    }

    public BgSparklineBuilder setTinyDots() { return this.setTinyDots(true); }

    public BgSparklineBuilder setBgGraphBuilder(BgGraphBuilder bgGraphBuilder) {
        this.bgGraphBuilder = bgGraphBuilder;
        return this;
    }

    public BgSparklineBuilder(Context context) {
        mContext = context;
        chart = new LineChartView(mContext);
        width = convertDpToPixel(NOTIFICATION_WIDTH_DP);
        height = convertDpToPixel(NOTIFICATION_HEIGHT_DP);
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
        v.setDrawingCacheBackgroundColor(Color.TRANSPARENT);

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

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines();
        lines.add(bgGraphBuilder.inRangeValuesLine());
        lines.add(bgGraphBuilder.lowValuesLine());
        lines.add(bgGraphBuilder.highValuesLine());
        if (showLowLine)
            lines.add(bgGraphBuilder.lowLine());
        if (showHighLine)
            lines.add(bgGraphBuilder.highLine());
        if (useSmallDots) {
            for(Line line: lines)
                line.setPointRadius(2);
        }
        if (useTinyDots) {
            for(Line line: lines)
                line.setPointRadius(1);
        }
        LineChartData lineData = new LineChartData(lines);
        if (showAxes) {
            lineData.setAxisYLeft(bgGraphBuilder.yAxis());
            lineData.setAxisXBottom(bgGraphBuilder.chartXAxis());
        }
        //lines.add(bgGraphBuilder.rawInterpretedLine());
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        chart.setRight(width);
        chart.setBottom(height);
        return getViewBitmap(chart);
    }
}
