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

    public BgMibandSparklineBuilder(Context context) {
        super(context);
    }

    public BgSparklineBuilder showTreatmentLine(boolean show) {
        this.showTreatment = show;
        return this;
    }

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines(false); // simple mode
        lines.add(bgGraphBuilder.inRangeValuesLine().setHasLines(true).setStrokeWidth(1));
        lines.add(bgGraphBuilder.lowValuesLine().setHasLines(true).setStrokeWidth(1));
        lines.add(bgGraphBuilder.highValuesLine().setHasLines(true).setStrokeWidth(1));

        Line[] treatments = bgGraphBuilder.treatmentValuesLine();

        if (showTreatment) {
            treatments[2].setStrokeWidth(1); //bolus line
            treatments[2].setHasPoints(false);
            lines.add(treatments[2]);

            //lines.add(treatments[3]); // activity
            lines.add(treatments[5]); // predictive
            //  lines.add(treatments[6]); // cob
            treatments[7].setHasLines(true);
            treatments[7].setHasPoints(true);
        }
        lines.add(treatments[7]); // poly predict

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
            for (Line line : lines)
                line.setPointRadius(2);
        }
        if (useTinyDots) {
            for (Line line : lines)
                line.setPointRadius(1);
        }
        if (showTreatment) {
            treatments[1].setFilled(false); //bolus dots
            treatments[1].setHasLabels(false);
            treatments[1].setPointRadius(2);
            lines.add(treatments[1]);
        }

        LineChartData lineData = new LineChartData(lines);
        if (showAxes) {
            if (height <= SCALE_TRIGGER) {
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
        chart.setBackgroundColor(backgroundColor);
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        if (height <= SCALE_TRIGGER) {
            viewport.bottom = 0;
            viewport.top = (float) (bgGraphBuilder.doMgdl ? 16 * Constants.MMOLL_TO_MGDL : 16);
        }
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        if (height > SCALE_TRIGGER) {
            chart.setRight(width);
            chart.setBottom(height);
        } else {
            chart.setRight(width * 2);
            chart.setBottom(height * 2);
        }

        if (height > SCALE_TRIGGER) {
            return getViewBitmap(chart);
        } else {
            return getResizedBitmap(getViewBitmap(chart), width, height);
        }
    }
}
