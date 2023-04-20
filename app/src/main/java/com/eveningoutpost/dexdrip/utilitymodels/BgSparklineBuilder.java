package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.view.View;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import static com.eveningoutpost.dexdrip.ui.helpers.UiHelper.convertDpToPixel;

/**
 * Created by matthiasgranberry on 5/4/15.
 */
public class BgSparklineBuilder {
    private Context mContext;

    protected static final String TAG = "BgSparklineBuilder";
    private static final int NOTIFICATION_WIDTH_DP = 230; // 476 width minus 8 padding on each side is the native
                                                          // resolution, but use less for lower memory requirements
    private static final int NOTIFICATION_HEIGHT_DP = 128;

    protected int width;
    protected int height;
    protected BgGraphBuilder bgGraphBuilder;
    protected LineChartView chart;
    protected long end = new Date().getTime() / BgGraphBuilder.FUZZER;
    protected long start = end - (60000*180 / BgGraphBuilder.FUZZER); // 3h
    protected boolean showLowLine = false;
    protected boolean showHighLine = false;
    protected boolean showAxes = false;
    protected boolean useSmallDots = true;
    protected boolean useTinyDots = false;
    protected boolean showFiltered = false;
    protected int backgroundColor = Color.TRANSPARENT;
    protected final static int SCALE_TRIGGER = 84;

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
    public BgSparklineBuilder setShowFiltered(boolean showFiltered) {
        this.showFiltered = showFiltered;
        return this;
    }

    public BgSparklineBuilder setSmallDots() {
        return this.setSmallDots(true);
    }

    public BgSparklineBuilder setTinyDots() { return this.setTinyDots(true); }

    public BgSparklineBuilder setBackgroundColor(int color)
    {
        this.backgroundColor = color;
        return this;
    }

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
    protected Bitmap getViewBitmap(View v) {
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
            android.util.Log.e(TAG, "failed getViewBitmap(" + JoH.backTrace() + ")", new RuntimeException());

            v.destroyDrawingCache(); // duplicate of below, flow could be reordered better
            v.setWillNotCacheDrawing(willNotCache);
            v.setDrawingCacheBackgroundColor(color);
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines(true); // simple mode
        lines.add(bgGraphBuilder.inRangeValuesLine());
        lines.add(bgGraphBuilder.lowValuesLine());
        lines.add(bgGraphBuilder.highValuesLine());

        if (showFiltered) {
            for (Line line : bgGraphBuilder.filteredLines()) {
                line.setHasPoints(false);
                lines.add(line);
            }
        }
        if (showLowLine) {
            if (height <= SCALE_TRIGGER) {
                Line line = bgGraphBuilder.lowLine();
                line.setFilled(false);
                lines.add(line);
            } else {
                lines.add(bgGraphBuilder.lowLine());
            }
        }

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
            if (height<=SCALE_TRIGGER) {
                Axis xaxis = bgGraphBuilder.chartXAxis();
                xaxis.setTextSize(4);
            } else {
                Axis yaxis = bgGraphBuilder.yAxis();
                yaxis.setTextSize(6);
                lineData.setAxisYLeft(yaxis);
                Axis xaxis = bgGraphBuilder.chartXAxis();
                xaxis.setTextSize(6);
                lineData.setAxisXBottom(xaxis);
            }
        }
        //lines.add(bgGraphBuilder.rawInterpretedLine());
        chart.setBackgroundColor(backgroundColor);
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        if (height<=SCALE_TRIGGER)
        {
            // for pebble classic we always want the lowest mark to be in the same place on the image
            viewport.bottom= (float)(bgGraphBuilder.doMgdl ? 2 * Constants.MMOLL_TO_MGDL : 2);
            viewport.top= (float)(bgGraphBuilder.doMgdl ? 16 * Constants.MMOLL_TO_MGDL : 16);
        }
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        if (height>SCALE_TRIGGER) {
            chart.setRight(width);
            chart.setBottom(height);
        } else {
            chart.setRight(width*2);
            chart.setBottom(height*2);
        }

            Log.d(TAG,"pebble debug: w:"+width+" h:"+height+" start:"+start+" end:"+end+" ");

        if (height>SCALE_TRIGGER) {
            return getViewBitmap(chart);
        } else {
            return getResizedBitmap(getViewBitmap(chart),width,height);
        }
    }

    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        if (bm==null) return null;
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }
}
