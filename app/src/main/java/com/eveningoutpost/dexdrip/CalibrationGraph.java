package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.eveningoutpost.dexdrip.Models.Calibration;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.Utils;
import lecho.lib.hellocharts.view.LineChartView;


public class CalibrationGraph extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Calibration Graph";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LineChartView chart;
    private LineChartData data;
    public double  start_x = 50;
    public double  end_x = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_graph);
    }
    @Override
    protected void onResume(){
        super.onResume();

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);

        setupCharts();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }


    public void setupCharts() {
        chart = (LineChartView) findViewById(R.id.chart);
        List<Calibration> calibrations = Calibration.allForSensor();
        List<PointValue> values = new ArrayList<PointValue>();
        for (Calibration calibration : calibrations) {
            values.add(new PointValue((float)calibration.estimate_raw_at_time_of_calibration, (float)calibration.bg));
        }

        Line line = new Line(values);
        line.setColor(Utils.COLOR_BLUE);
        line.setHasLines(false);
        line.setPointRadius(2);
        line.setHasPoints(true);

        Calibration calibration = Calibration.last();
        List<PointValue> lineValues = new ArrayList<PointValue>();
        if(calibration != null) {
            lineValues.add(new PointValue((float) start_x, (float) (start_x * calibration.slope + calibration.intercept)));
            lineValues.add(new PointValue((float) end_x, (float) (end_x * calibration.slope + calibration.intercept)));
        }
        Line calibrationLine = new Line(lineValues);
        calibrationLine.setColor(Utils.COLOR_RED);
        calibrationLine.setHasLines(true);
        calibrationLine.setHasPoints(false);
        Axis axisX = new Axis();
        Axis axisY = new Axis().setHasLines(true);
        axisX.setName("Raw Value");
        axisY.setName("BG");

         List<Line> lines = new ArrayList<Line>();
        lines.add(line);
        lines.add(calibrationLine);

        data = new LineChartData(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        chart.setLineChartData(data);

    }
}
