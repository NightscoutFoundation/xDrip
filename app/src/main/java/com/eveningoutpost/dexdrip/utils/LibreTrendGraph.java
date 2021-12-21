package com.eveningoutpost.dexdrip.utils;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.ReadingData;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.FUZZER;


public class LibreTrendGraph extends BaseAppCompatActivity {
    private static final String TAG = "LibreTrendGraph";
    
    private static LibreTrendGraph mInstance;
    private LineChartView chart;
    private LineChartData data;
    private final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
    private final int MINUTES_TO_DISPLAY = 45;
    
    public void closeNow(View view) {
        try {
            finish();
        } catch (Exception e) {
            Log.d(TAG, "Error finishing " + e.toString());
        }
    }

    private static ArrayList<Float> getLatestBgForXMinutes(int NumberOfMinutes) {

        Log.i(TAG, "getLatestBgForXMinutes number of minutes = " + NumberOfMinutes);
        
        List<LibreTrendPoint> LibreTrendPoints = LibreTrendUtil.getInstance().getData(JoH.tsl() - NumberOfMinutes * 60 * 1000, JoH.tsl(), true);
        if(LibreTrendPoints == null || LibreTrendPoints.size() == 0) {
            Log.e(TAG, "Error getting data from getLatestBgForXMinutes");
            return null;
        }
        
        LibreTrendLatest libreTrendLatest = LibreTrendUtil.getInstance().getLibreTrendLatest();
        if(libreTrendLatest == null) {
            Log.e(TAG, "LibreTrendPoints exists but libreTrendLatest is NULL.");
            return null;
        }
        ArrayList<Float> ret = new ArrayList<Float>();
        
        double factor = libreTrendLatest.getFactor();
        if(factor == 0) {
            Log.e(TAG, "getLatestBgForXMinutes: factor is 0 returning.");
            return null;
        }
        
        int count = 0;
        for(int i = libreTrendLatest.id ; i >= 0 && count < NumberOfMinutes; i--) {
            count ++;
            ret.add(new Float(factor * LibreTrendPoints.get(i).rawSensorValue));
        }
        return ret;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libre_trend);
        JoH.fixActionBar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String titleFormat = getResources().getString(R.string.libre_last_x_minutes_graph);
        setTitle(String.format(titleFormat, MINUTES_TO_DISPLAY));
        setupCharts();
    }

    public static List<PointValue> getTrendDataPoints(boolean doMgdl, long start_time, long end_time) {
        // TODO needs to cut off if would exceed the current graph scope
         final float conversion_factor_mmol = (float) (doMgdl ? 1 : Constants.MGDL_TO_MMOLL);
         ArrayList<Float> bg_data = getLatestBgForXMinutes((int) ((end_time - start_time) /  Constants.MINUTE_IN_MS)  );
         if (bg_data == null) {
             Log.e(TAG, "Error getting data from getLatestBgForXMinutes. Returning");
             return null;
             
         }
         
         LibreTrendLatest libreTrendLatest = LibreTrendUtil.getInstance().getLibreTrendLatest();
         if(libreTrendLatest == null || libreTrendLatest.getFactor() == 0) {
             Log.e(TAG, "libreBlock exists but libreTrendLatest.getFactor is zero ");
             return null;
         }

         final ArrayList<PointValue> points = new ArrayList<>(bg_data.size());
         long time_offset = 0;
         for (Float bg : bg_data) {
             if(bg <= 0) {
                 time_offset += Constants.MINUTE_IN_MS;
                 continue;   
             }
             long bg_time = libreTrendLatest.timestamp - time_offset;
             if (bg_time <= end_time && bg_time >= start_time) {
                 points.add(new PointValue((float) ((double)(bg_time) / FUZZER), bg * conversion_factor_mmol));
             }
             
             time_offset += Constants.MINUTE_IN_MS;
         }
         return points;
       
     }

    
    
    public void setupCharts() {
        
       final TextView trendView = (TextView) findViewById(R.id.textLibreHeader);
         
        chart = (LineChartView) findViewById(R.id.libre_chart);
        List<Line> lines = new ArrayList<Line>();

        List<PointValue> lineValues = new ArrayList<PointValue>();
        final float conversion_factor_mmol = (float) (doMgdl ? 1 : Constants.MGDL_TO_MMOLL);

        LibreBlock libreBlock= LibreBlock.getLatestForTrend();
        if(libreBlock == null) {
            trendView.setText("No libre data to display");
            setupEmptyCharts();
            return;
        }
        String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date((long) libreBlock.timestamp));

        ArrayList<Float> bg_data = getLatestBgForXMinutes(MINUTES_TO_DISPLAY);
        
        if(bg_data == null) {
            trendView.setText("Error displaying data for " + time);
            setupEmptyCharts();
            return;
        }
        
        trendView.setText("Scan from " + time);
        float min = 1000;
        float max = 0;
        int i = 0;
        for(float bg : bg_data ) {
            if(bg <= 0) {
                i++;
                continue;   
            }
            if(min > bg) {
                min = bg;
            }
            if(max < bg) {
                max = bg;
            }
            
            lineValues.add(new PointValue(-i, bg * conversion_factor_mmol));
            i++;
        }

        Line trendLine = new Line(lineValues);
        trendLine.setColor(ChartUtils.COLOR_RED);
        trendLine.setHasLines(false);
        trendLine.setHasPoints(true);
        trendLine.setPointRadius(3);
        lines.add(trendLine);
        
        final int MIN_GRAPH = 20;
        if(max - min < MIN_GRAPH)
        {
            // On relative flat trend the graph can look very noise althouth with the right resolution it is not that way.
            // I will add two dummy invisible points that will cause the graph to look with bigger Y range.
            float average = (max + min) /2;
            List<PointValue> dummyPointValues = new ArrayList<PointValue>();
            Line dummyPointLine = new Line(dummyPointValues);
            dummyPointValues.add(new PointValue(0, (average - MIN_GRAPH / 2) * conversion_factor_mmol));
            dummyPointValues.add(new PointValue(0, (average + MIN_GRAPH / 2) * conversion_factor_mmol));
            dummyPointLine.setColor(ChartUtils.COLOR_RED);
            dummyPointLine.setHasLines(false);
            dummyPointLine.setHasPoints(false);
            lines.add(dummyPointLine);
        }

        Axis axisX = new Axis();
        Axis axisY = new Axis().setHasLines(true);
        axisX.setTextSize(16);
        axisY.setTextSize(16);
        axisX.setName("Time from last scan");
        axisY.setName("Glucose " + (doMgdl ? "mg/dl" : "mmol/l"));

        data = new LineChartData(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        chart.setLineChartData(data);

    }
    
    // In a case of an error when there is no data to show in the graph hellocharts shows a default graph.
    // This can be supper confusing for users, so I'm drawing an empty graph just in case.
    void setupEmptyCharts() {
        chart = (LineChartView) findViewById(R.id.libre_chart);
        List<Line> lines = new ArrayList<Line>();
        data = new LineChartData(lines);
        chart.setLineChartData(data);
    }

}

