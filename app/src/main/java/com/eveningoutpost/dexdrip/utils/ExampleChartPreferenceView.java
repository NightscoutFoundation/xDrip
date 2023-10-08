package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;

import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;


/**
 * Created by jamorham on 12/05/2016.
 */

public class ExampleChartPreferenceView extends Preference {
    private static final String TAG = "examplechart";

    public ExampleChartPreferenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.prefs_example_chart_layout);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.prefs_example_chart_layout, parent, false);
    }

    @Override
    protected void onBindView(View view) {
        //Log.d(TAG, "onBindExampleChart: bindview");
        refreshView(view);
        super.onBindView(view);
    }

    protected void refreshView(View view) {
        LineChartView chart = (LineChartView) view.findViewById(R.id.example_linechart);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(getContext());
        chart.setLineChartData(bgGraphBuilder.lineData());
        Viewport viewport = chart.getMaximumViewport();
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        Log.d(TAG, "onBindExampleChart: refreshview " + chart.getHeight());
    }

    @Override
    protected void onClick() {
        notifyChanged();
    }
}