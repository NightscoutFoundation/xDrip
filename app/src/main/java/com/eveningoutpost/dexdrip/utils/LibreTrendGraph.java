package com.eveningoutpost.dexdrip.utils;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.integration.android.IntentIntegrator;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LibreTrendGraph extends AppCompatActivity {

    private LineChartView chart;
    private LineChartData data;
    //private NavigationDrawerFragment mNavigationDrawerFragment;
    private final static String plugin_color = "#88CCFF00";
    private final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
    private final boolean show_days_since = true; // could make this switchable if desired
    private final double start_x = 50; // raw range
    private double end_x = 300; //  raw range
    
    private static final String TAG = "LibreTrendGraph";
    private static LibreTrendGraph mInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.activity_libre_trend);
        JoH.fixActionBar(this);
    }

    @Override
    protected void onDestroy() {
        mInstance = null; // GC?
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupCharts();
    }

    public void setupCharts() {
        chart = (LineChartView) findViewById(R.id.libre_chart);
        List<Line> lines = new ArrayList<Line>();


    List<PointValue> lineValues = new ArrayList<PointValue>();
    final float conversion_factor = (float) (doMgdl ? 1 : Constants.MGDL_TO_MMOLL);

    lineValues.add(new PointValue(200, 70));
    lineValues.add(new PointValue(220, 85));
    lineValues.add(new PointValue(240, 95));
    lineValues.add(new PointValue(260, 100));
    

    Line calibrationLine = new Line(lineValues);
    calibrationLine.setColor(ChartUtils.COLOR_RED);
    calibrationLine.setHasLines(false);
    calibrationLine.setHasPoints(true);
    lines.add(calibrationLine);
    
        Axis axisX = new Axis();
        Axis axisY = new Axis().setHasLines(true);
        axisX.setName("Raw Value");
        axisY.setName("Glucose " + (doMgdl ? "mg/dl" : "mmol/l"));


        data = new LineChartData(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        chart.setLineChartData(data);

    }
    public void closeNow(View view) {
        try {
            mInstance = null;
            finish();
        } catch (Exception e) {
            Log.d(TAG, "Error finishing " + e.toString());
        }
    }

}


