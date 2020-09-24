package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.Utils;

import android.content.Context;
import android.graphics.Bitmap;

import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;

public class BgMibandSparklineBuilder extends BgSparklineBuilder {
    protected boolean showTreatment = false;
    protected int pointSize = 1;
    protected final static int GRAPH_LIMIT = 16;

    public BgMibandSparklineBuilder(Context context) {
        super(context);
    }

    public BgSparklineBuilder showTreatmentLine(boolean show) {
        this.showTreatment = show;
        return this;
    }

    public BgSparklineBuilder setPointSize( int pointSize) {
        this.pointSize = pointSize;
        return this;
    }

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines(false); // simple mode

        if (showLowLine) {
            Line line = bgGraphBuilder.lowLine();
            line.setFilled(false);
            lines.add(line);
        }

        if (showHighLine) {
            lines.add(bgGraphBuilder.highLine());
        }

        lines.add(bgGraphBuilder.inRangeValuesLine().setPointRadius(pointSize));
        lines.add(bgGraphBuilder.lowValuesLine().setPointRadius(pointSize));
        lines.add(bgGraphBuilder.highValuesLine().setPointRadius(pointSize));

        Line[] treatments = bgGraphBuilder.treatmentValuesLine();

        lines.add(treatments[5]); // predictive
        treatments[7].setHasLines(true);
        treatments[7].setHasPoints(true);
        lines.add(treatments[7]); // poly predict

        if (showFiltered) {
            for (Line line : bgGraphBuilder.filteredLines()) {
                line.setHasPoints(false);
                lines.add(line);
            }
        }

        if (useSmallDots) {
            for (Line line : lines)
                line.setPointRadius(2);
        }
        if (useTinyDots) {
            for (Line line : lines)
                line.setPointRadius(1);
        }

        if (showTreatment) {
            treatments[2].setStrokeWidth(1); //bolus line
            treatments[2].setHasPoints(false);
            lines.add(treatments[2]);

            treatments[1].setFilled(false); //bolus dots
            treatments[1].setHasLabels(false);
            treatments[1].setPointRadius(2);
            lines.add(treatments[1]);
            //lines.add(treatments[3]); // activity
            //  lines.add(treatments[6]); // cob
        }

        LineChartData lineData = new LineChartData(lines);
        if (showAxes) {
            Axis xaxis = bgGraphBuilder.chartXAxis();
            xaxis.setTextSize(4);
        }
        chart.setBackgroundColor(backgroundColor);
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        viewport.bottom = 0;
        viewport.top = (float) (bgGraphBuilder.doMgdl ? GRAPH_LIMIT * Constants.MMOLL_TO_MGDL : GRAPH_LIMIT);
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        chart.setRight(width * 2);
        chart.setBottom(height * 2);
        return getResizedBitmap(getViewBitmap(chart), width, height);
    }
}
