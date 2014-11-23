package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import lecho.lib.hellocharts.ViewportChangeListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.Utils;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class HomeDecay extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Decay";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    private LineChartData data;
    private LineChartData previewData;
    public double  end_time = new Date().getTime() + (60000 * 20);
    public double  start_time = end_time - (60000 * 60 * 24);
    public int highMark = 190;
    public int lowMark = 70;
    Viewport tempViewport = new Viewport();
    public float left;
    public float right;
    public float top;
    public float bottom;
    public boolean updateStuff;
    BroadcastReceiver _broadcastReceiver;
//    final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        startService(new Intent(this, DexCollectionService.class));
        setContentView(R.layout.activity_home);

    }

    @Override
    protected void onResume(){
        super.onResume();

        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo();
                }
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

        setupCharts();
        updateCurrentBgInfo();
    }

    public void setupCharts() {

        end_time = new Date().getTime() + (60000 * 20);
        start_time = end_time - (60000 * 60 * 24);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        generateDefaultData();

        previewChart.setViewportChangeListener(new ViewportListener());

        chart.setLineChartData(data);
        previewChart.setLineChartData(previewData);
        updateStuff = true;
        previewX(false);

    }

    private void generateDefaultData() {
        int numValues =(60/5)*24;
        List<BgReadingDecay> bgReadings = BgReadingDecay.latestForGraph( numValues, start_time);
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        List<PointValue> values = new ArrayList<PointValue>();
        for (BgReadingDecay bgReading : bgReadings) {
            if (bgReading.calculated_value < highMark) {
                if (bgReading.calculated_value > lowMark) {
                    values.add(new PointValue((float)bgReading.timestamp, (float)bgReading.calculated_value));
                }
            }
        }
        Line line = new Line(values);
        line.setColor(Utils.COLOR_BLUE);
        line.setHasLines(false);
        line.setPointRadius(2);
        line.setStrokeWidth(1);
        line.setHasPoints(true);


        List<PointValue> highValues = new ArrayList<PointValue>();
        for (BgReadingDecay bgReading : bgReadings) {
            if (bgReading.calculated_value >= highMark) {
                highValues.add(new PointValue((float)bgReading.timestamp, (float)bgReading.calculated_value));
            }
        }
        Line highValuesLine = new Line(highValues);
        highValuesLine.setColor(Utils.COLOR_ORANGE);
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(2);
        highValuesLine.setStrokeWidth(1);
        highValuesLine.setHasPoints(true);

        List<PointValue> lowValues = new ArrayList<PointValue>();
        for (BgReadingDecay bgReading : bgReadings) {
            if (bgReading.calculated_value <= lowMark) {
                lowValues.add(new PointValue((float)bgReading.timestamp, (float)bgReading.calculated_value));
            }
        }
        Line lowValuesLine = new Line(lowValues);
        lowValuesLine.setColor(Utils.COLOR_RED);
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(2);
        lowValuesLine.setStrokeWidth(1);
        lowValuesLine.setHasPoints(true);


        List<PointValue> minShowValues = new ArrayList<PointValue>();
        minShowValues.add(new PointValue((float)start_time, 30));
        minShowValues.add(new PointValue((float)end_time, 30));
        Line minShow = new Line(minShowValues);
        minShow.setStrokeWidth(1);
        minShow.setHasPoints(false);
        minShow.setHasLines(false);

        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float)start_time, 250));
        maxShowValues.add(new PointValue((float)end_time, 250));
        Line maxShow = new Line(maxShowValues);
        maxShow.setHasLines(false);
        maxShow.setStrokeWidth(1);
        maxShow.setHasPoints(false);

        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float)start_time, lowMark));
        lowLineValues.add(new PointValue((float)end_time, lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setAreaTransparency(30);
        lowLine.setColor(Utils.COLOR_RED);
        lowLine.setStrokeWidth(1);
        lowLine.setFilled(true);

        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue((float)start_time, highMark));
        highLineValues.add(new PointValue((float)end_time, highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(Utils.COLOR_ORANGE);

        List<Line> lines = new ArrayList<Line>();
        lines.add(line);
        lines.add(minShow);
        lines.add(maxShow);
        lines.add(highLine);
        lines.add(lowLine);
        lines.add(lowValuesLine);
        lines.add(highValuesLine);

        data = new LineChartData(lines);
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        axisValues.add(new AxisValue(50));
        axisValues.add(new AxisValue(100));
        axisValues.add(new AxisValue(150));
        axisValues.add(new AxisValue(200));
        axisValues.add(new AxisValue(250));
        axisValues.add(new AxisValue(300));
        axisValues.add(new AxisValue(350));
        axisValues.add(new AxisValue(400));
        Log.w("AXIS:", axisValues.toString());
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);

        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);

        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();

        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        SimpleDateFormat timeFormat = new SimpleDateFormat("h a");

        timeFormat.setTimeZone(TimeZone.getDefault());

        double start_hour = today.getTime().getTime();
        double endHour = 0;
        double timeNow = new Date().getTime();
        for(int l=0; l<=24; l++) {
            if ((start_hour + (60000 * 60 * (l))) <  timeNow) {
                if((start_hour + (60000 * 60 * (l + 1))) >=  timeNow) {
                    endHour = start_hour + (60000 * 60 * (l));
                    l=25;
                }
            }
        }
        for(int l=0; l<=24; l++) {
            double timestamp = endHour - (60000 * 60 * l);
            xAxisValues.add(new AxisValue((long)(timestamp), (timeFormat.format(timestamp)).toCharArray()));
        }
        xAxis.setValues(xAxisValues);
        xAxis.setHasLines(true);
        data.setAxisXBottom(xAxis);
        data.setAxisYLeft(yAxis);



        previewData = new LineChartData(data);

        List<AxisValue> previewXaxisValues = new ArrayList<AxisValue>();

        for(int l=0; l<=24; l++) {
            double timestamp = endHour - (60000 * 60 * l);
            previewXaxisValues.add(new AxisValue((long)(timestamp), (timeFormat.format(timestamp)).toCharArray()));
        }
        Axis previewXaxis = new Axis();
        previewXaxis.setValues(previewXaxisValues);
        previewXaxis.setHasLines(true);
        previewXaxis.setTextSize(5);
        previewData.setAxisXBottom(previewXaxis);
        previewData.getLines().get(1).setColor(Utils.DEFAULT_DARKEN_COLOR);
        previewData.getLines().get(2).setColor(Utils.DEFAULT_DARKEN_COLOR);
        previewData.getLines().get(3).setColor(Utils.DEFAULT_DARKEN_COLOR);
        previewData.getLines().get(4).setColor(Utils.DEFAULT_DARKEN_COLOR);
    }

    private class ViewportListener implements ViewportChangeListener {

        @Override
        public void onViewportChanged(Viewport newViewport) {
            // don't use animation, it is unnecessary when using preview chart.
            chart.setZoomEnabled(false);
            chart.setZoomLevel(1, 1, 1, true);
            chart.setCurrentViewport(newViewport, false);
            if(updateStuff == true) {
                tempViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    private void previewX(boolean animate) {
          setViewport();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    public void setViewport() {
        if (tempViewport.left == 0.0) {
            Log.w("SETTING UP PREVIEWX:", "" + tempViewport.left);
            Log.w("SETTING UP PREVIEWX:", "A");
            tempViewport = new Viewport(chart.getMaximumViewport());
            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            tempViewport.inset((float)(86400000 / 2.5), 0);
            double width = tempViewport.right - tempViewport.left;
            double center = (width / 2) + tempViewport.left;
            double distance_to_move = (new Date().getTime()) - tempViewport.left - (((tempViewport.right - tempViewport.left) /2));
            tempViewport.offset((float)distance_to_move, 0);

            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            previewChart.setCurrentViewport(tempViewport, false);
            previewChart.setZoomType(ZoomType.HORIZONTAL);
            Log.w("SETTING UP PREVIEWX:", "" + tempViewport.left);
            Log.w("SETTING UP PREVIEWX:", "" + tempViewport.right);

        } else if (tempViewport.right >= (new Date().getTime())) {
            Log.w("SETTING UP PREVIEWX:", "" + tempViewport.left);
            Log.w("SETTING UP PREVIEWX:", "" + tempViewport.right);
            Log.w("SETTING UP PREVIEWX:", "" + new Date().getTime());
            tempViewport = new Viewport(chart.getMaximumViewport());
            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            Log.w("SETTING UP PREVIEWX:", "B");
            tempViewport.inset((float)(86400000 / 2.5), 0);
            double width = tempViewport.right - tempViewport.left;
            double center = (width / 2) + tempViewport.left;
            double distance_to_move = (new Date().getTime()) - tempViewport.left - (((tempViewport.right - tempViewport.left) /2));
            tempViewport.offset((float)distance_to_move, 0);
            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            previewChart.setCurrentViewport(tempViewport, false);
            previewChart.setZoomType(ZoomType.HORIZONTAL);
        } else {
            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            Log.w("SETTING UP PREVIEWX:", "C");
            Log.w("SETTING UP PREVIEWX:", tempViewport.toString());
            previewChart.setCurrentViewport(tempViewport, false);
            previewChart.setZoomType(ZoomType.HORIZONTAL);
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (_broadcastReceiver != null)
            unregisterReceiver(_broadcastReceiver);
    }

    public void updateCurrentBgInfo() {
        final TextView currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        if (Sensor.isActive()) {
            if (BgReadingDecay.latest(2).size() > 1) {
                if (Calibration.latest(2).size() >= 1) {
                    displayCurrentInfo();
                } else {
                    currentBgValueText.setText("Please enter two calibrations to get started!");
                }
            } else {
                currentBgValueText.setText("Please wait, need 2 readings from transmitter first.");
            }
        } else {
            currentBgValueText.setText("You must first start your sensor");
        }

        NavigationDrawerFragment mNavigationDrawerFragment;
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    public void displayCurrentInfo() {
        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        BgReadingDecay lastBgreading = BgReadingDecay.lastNoSenssor();

        if (lastBgreading != null) {
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.timestamp > 0) {
                currentBgValueText.setText("Signal Missed");
            } else {
                if (lastBgreading != null) {
                    double slope = (float) (BgReadingDecay.activeSlope() * 60000);
                    Log.w("SLOPE", "" + slope);
                    String arrow = new String();
                    if (slope <= (-4)) {
                        Log.w("SLOPE", " double down");
                        arrow = "\u21ca";
                    } else if (slope <= (-3)) {
                        Log.w("SLOPE", " down");
                        arrow = "\u2193";
                    } else if (slope <= (-1.5)) {
                        Log.w("SLOPE", " diag down");
                        arrow = "\u2198";
                    } else if (slope <= (1.5)) {
                        Log.w("SLOPE", " level");
                        arrow = "\u2192";
                    } else if (slope <= (3)) {
                        Log.w("SLOPE", " diagonal up");
                        arrow = "\u2197";
                    } else if (slope <= (4)) {
                        Log.w("SLOPE", " up");
                        arrow = "\u2191";
                    } else {
                        arrow = "\u21c8";
                        Log.w("SLOPE", " double up");
                    }

                    Log.w("Current active predition ", BgReadingDecay.activePrediction());

                    Log.w("Current slope ", "" + slope);
                    currentBgValueText.setText("" + BgReadingDecay.activePrediction() + " " + arrow);

                }

            }

        }
    setupCharts();
    }
}
