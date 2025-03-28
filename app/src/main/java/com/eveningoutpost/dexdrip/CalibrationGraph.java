package com.eveningoutpost.dexdrip;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class CalibrationGraph extends ActivityWithMenu {
    //public static String menu_name = "Calibration Graph";
    private LineChartView chart;
    private LineChartData data;
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private final static String plugin_color = "#88CCFF00";
    private final boolean doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
    private final boolean show_days_since = true; // could make this switchable if desired
    private final double start_x = 50; // raw range
    private double end_x = 300; //  raw range

    TextView GraphHeader;
    TextView PluginHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_graph);
        JoH.fixActionBar(this);
        GraphHeader = (TextView) findViewById(R.id.CalibrationGraphHeader);
        PluginHeader = (TextView) findViewById(R.id.calibrationPluginHeader);
        PluginHeader.setText("");
    }

    @Override
    public String getMenuName() {
        return getString(R.string.calibration_graph);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupCharts();
    }

    public void setupCharts() {
        chart = (LineChartView) findViewById(R.id.chart);
        List<Line> lines = new ArrayList<Line>();

        //calibration values
        List<Calibration> calibrations = Calibration.allForSensor();
        List<Line> greyLines = getCalibrationsLine(calibrations, Color.parseColor("#66FFFFFF"));
        calibrations = Calibration.allForSensorInLastFourDays();
        List<Line> blueLines = getCalibrationsLine(calibrations, ChartUtils.COLOR_BLUE);

        Calibration calibration = Calibration.lastValid();
        if (calibration != null) {
            //set header
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);
            String Header = getString(R.string.calibration_slope_intercept, df.format(calibration.slope), df.format(calibration.intercept));
            GraphHeader.setText(Header);

            //red line
            List<PointValue> lineValues = new ArrayList<PointValue>();
            final float conversion_factor = (float) (doMgdl ? 1 : Constants.MGDL_TO_MMOLL);

            lineValues.add(new PointValue((float) start_x, (conversion_factor * (float) (start_x * calibration.slope + calibration.intercept))));
            lineValues.add(new PointValue((float) end_x, (conversion_factor * (float) (end_x * calibration.slope + calibration.intercept))));
            Line calibrationLine = new Line(lineValues);
            calibrationLine.setColor(ChartUtils.COLOR_RED);
            calibrationLine.setHasLines(true);
            calibrationLine.setHasPoints(false);
            lines.add(calibrationLine);

            // calibration plugin
            final CalibrationAbstract plugin = getCalibrationPluginFromPreferences();
            if (plugin != null) {
                final CalibrationAbstract.CalibrationData pcalibration = plugin.getCalibrationData();

                final List<PointValue> plineValues = new ArrayList<PointValue>();

                plineValues.add(new PointValue((float) start_x, (conversion_factor * (float) (plugin.getGlucoseFromSensorValue(start_x, pcalibration)))));
                plineValues.add(new PointValue((float) end_x, (conversion_factor * (float) (plugin.getGlucoseFromSensorValue(end_x, pcalibration)))));

                final Line pcalibrationLine = new Line(plineValues);
                pcalibrationLine.setColor(Color.parseColor(plugin_color));
                pcalibrationLine.setHasLines(true);
                pcalibrationLine.setHasPoints(false);
                lines.add(pcalibrationLine);
                PluginHeader.setText("(" + plugin.getAlgorithmName() + ")  " + "s = " + df.format(pcalibration.slope) + "  i = " + df.format(pcalibration.intercept));
                PluginHeader.setTextColor(Color.parseColor(plugin_color));
            }

            //add lines in order
            for (Line greyLine : greyLines) {
                lines.add(greyLine);
            }
            for (Line blueLine : blueLines) {
                lines.add(blueLine);
            }

        }
        Axis axisX = new Axis();
        Axis axisY = new Axis().setHasLines(true);
        axisX.setName(getString(R.string.axis_x_raw_value));
        axisY.setName(getString(R.string.axis_y_glucose_mgdl_mmol, (doMgdl ? "mg/dl" : "mmol/l")));


        data = new LineChartData(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        chart.setLineChartData(data);

    }

    private static int daysAgo(long when) {
        return (int) (JoH.tsl() - when) / 86400000;
    }

    @NonNull
    public List<Line> getCalibrationsLine(List<Calibration> calibrations, int color) {
        if (calibrations == null) return new ArrayList<>();
        List<PointValue> values = new ArrayList<PointValue>();
        List<PointValue> valuesb = new ArrayList<PointValue>();
        List<PointValue> valuesc = new ArrayList<PointValue>();
        for (Calibration calibration : calibrations) {
            if (calibration.estimate_raw_at_time_of_calibration > end_x) {
                end_x = calibration.estimate_raw_at_time_of_calibration;
            }
            PointValue point = new PointValue((float) calibration.estimate_raw_at_time_of_calibration,
                    doMgdl ? (float) calibration.bg : ((float) calibration.bg) * (float) Constants.MGDL_TO_MMOLL);
            PointValue pointb = new PointValue((float) calibration.raw_value,
                    doMgdl ? (float) calibration.bg : ((float) calibration.bg) * (float) Constants.MGDL_TO_MMOLL);
            PointValue pointc = new PointValue((float) calibration.adjusted_raw_value,
                    doMgdl ? (float) calibration.bg : ((float) calibration.bg) * (float) Constants.MGDL_TO_MMOLL);
            String time;
            if (show_days_since) {
                final int days_ago = daysAgo(calibration.raw_timestamp);
                time = (days_ago > 0) ? Integer.toString(days_ago) + "d  " : "";
                time = time + (JoH.hourMinuteString(calibration.raw_timestamp));
            } else {
                time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date((long) calibration.raw_timestamp));
            }
            point.setLabel(time);
            values.add(point);

            // extra points showing real raw and age_adjusted raw for each calbration point

            valuesb.add(pointb);
            valuesc.add(pointc);
        }


        Line line = new Line(values);
        line.setColor(color);
        line.setHasLines(false);
        line.setPointRadius(4);
        line.setHasPoints(true);
        line.setHasLabels(true);

        List<Line> lines = new ArrayList<>();
        lines.add(line);

        if (Pref.getBooleanDefaultFalse("engineering_mode")) {

            // actual raw
            Line lineb = new Line(valuesb);
            lineb.setColor(Color.RED);
            lineb.setHasLines(false);
            lineb.setPointRadius(1);
            lineb.setHasPoints(true);
            lineb.setHasLabels(false);

            // age adjusted raw
            Line linec = new Line(valuesc);
            linec.setColor(Color.YELLOW);
            linec.setHasLines(false);
            linec.setPointRadius(1);
            linec.setHasPoints(true);
            linec.setHasLabels(false);

            lines.add(lineb);
            lines.add(linec);
        }
        return lines;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Just generate the menu in engineering mode
        if (!Pref.getBooleanDefaultFalse("engineering_mode")) {
            return false;
        }

        getMenuInflater().inflate(R.menu.menu_calibrationgraph, menu);

        // Only show elements if there is a calibration to overwrite
        if (Calibration.lastValid() != null) {
            menu.findItem(R.id.action_overwrite_intercept).setVisible(true);
            menu.findItem(R.id.action_overwrite_slope).setVisible(true);
            menu.findItem(R.id.action_overwrite_calibration_impossible).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_overwrite_intercept:
                overWriteIntercept();
                return true;
            //break;
            case R.id.action_overwrite_slope:
                overWriteSlope();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void overWriteIntercept() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Ovewrite Intercept")
                .setMessage("Overwrite Intercept")
                .setView(editText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            double doubleValue = JoH.tolerantParseDouble(text);
                            Calibration calibration = Calibration.lastValid();
                            calibration.intercept = doubleValue;
                            calibration.save();
                            CalibrationSendQueue.addToQueue(calibration, getApplicationContext());
                            recreate();
                        } else {
                            JoH.static_toast_long(gs(R.string.input_not_found_cancelled));
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JoH.static_toast_long(gs(R.string.cancelled));
                    }
                })
                .show();
    }

    private void overWriteSlope() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Ovewrite Slope")
                .setMessage("Overwrite Slope")
                .setView(editText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            double doubleValue = JoH.tolerantParseDouble(text);
                            Calibration calibration = Calibration.lastValid();
                            calibration.slope = doubleValue;
                            calibration.save();
                            CalibrationSendQueue.addToQueue(calibration, getApplicationContext());
                            recreate();
                        } else {
                            JoH.static_toast_long(gs(R.string.input_not_found_cancelled));
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JoH.static_toast_long(gs(R.string.cancelled));
                    }
                })
                .show();
    }


}