package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by Emma Black on 11/15/14.
 */

class PointValueExtended extends PointValue {
	
	public PointValueExtended(float x, float y, float filtered) {
		super(x, y);
		calculatedFilteredValue = filtered;
	}
	   public PointValueExtended(float x, float y) {
		   super(x, y);
	        calculatedFilteredValue = -1;
	    }
	
	float calculatedFilteredValue;
}


public class BgGraphBuilder {
	private static final String TAG = BgGraphBuilder.class.getSimpleName();
    public static final int FUZZER = (1000 * 30 * 5);
    public final static double NOISE_TRIGGER = 10;
    public final static double NOISE_TRIGGER_ULTRASENSITIVE = 1;
    public final static double NOISE_TOO_HIGH_FOR_PREDICT = 60;
    public final static double NOISE_HIGH = 200;
    public final static double NOISE_FORGIVE = 100;
    public static double low_occurs_at = -1;
    public static double previous_low_occurs_at = -1;
    private static double low_occurs_at_processed_till_timestamp = -1;
    private static long noise_processed_till_timestamp = -1;
    public static final int MAX_SLOPE_MINUTES = 21;
    public long  end_time;
    public long  start_time;
    public Context context;
    public SharedPreferences prefs;
    public double highMark;
    public double lowMark;
    public double defaultMinY;
    public double defaultMaxY;
    public boolean doMgdl;
    final int pointSize;
    final int axisTextSize;
    final int previewAxisTextSize;
    final int hoursPreviewStep;
    private final static double timeshift = 500000;
    private static final int MAX_VALUES =60*24;
    private final List<BgReading> bgReadings;
    private final List<Calibration> calibrations;
    private List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private List<PointValue> highValues = new ArrayList<PointValue>();
    private List<PointValue> lowValues = new ArrayList<PointValue>();
    private List<PointValue> rawInterpretedValues = new ArrayList<PointValue>();
    private List<PointValue> filteredValues = new ArrayList<PointValue>();
    private List<PointValue> calibrationValues = new ArrayList<PointValue>();
    static final boolean LINE_VISIBLE = true;
    static final boolean FILL_UNDER_LINE = false;
    public Viewport viewport;
    public final static long DEXCOM_PERIOD = 300000;//KS from app / BgGraphBuilder.java
    public static double last_noise = -99999;
    public static double best_bg_estimate = -99999;
    public static double last_bg_estimate = -99999;


    public BgGraphBuilder(Context context){
        this(context, new Date().getTime() + (60000 * 10));
    }

    public BgGraphBuilder(Context context, long end){
        this(context, end - (60000 * 60 * 24), end);
    }

    public BgGraphBuilder(Context context, long start, long end){
        this(context, start, end, MAX_VALUES);
    }

    public BgGraphBuilder(Context context, long start, long end, int numValues, boolean show_prediction) {//KS TODO implement show_prediction
        this(context, start, end, numValues);
    }

    public BgGraphBuilder(Context context, long start, long end, int numValues){
        end_time = end;
        start_time = start;
        bgReadings = BgReading.latestForGraph( numValues, start, end);
        calibrations = Calibration.latestForGraph( numValues, start, end);
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        this.lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        this.doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        defaultMinY = unitized(40);
        defaultMaxY = unitized(250);
        pointSize = isXLargeTablet(context) ? 5 : 3;
        axisTextSize = isXLargeTablet(context) ? 20 : Axis.DEFAULT_TEXT_SIZE_SP;
        previewAxisTextSize = isXLargeTablet(context) ? 12 : 5;
        hoursPreviewStep = isXLargeTablet(context) ? 2 : 1;
    }

    public LineChartData lineData() {
        LineChartData lineData = new LineChartData(defaultLines());
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(chartXAxis());
        return lineData;
    }

    public LineChartData previewLineData() {
        LineChartData previewLineData = new LineChartData(lineData());
        previewLineData.setAxisYLeft(yAxis());
        previewLineData.setAxisXBottom(previewXAxis());
        // because the lines array can now be a varying size we
        // offset from the end instead of hardcoded values. This
        // is still brittle but better than absolute offsets.
        final int array_offset = previewLineData.getLines().size()-5;
        previewLineData.getLines().get(array_offset).setPointRadius(2);
        previewLineData.getLines().get(array_offset+1).setPointRadius(2);
        previewLineData.getLines().get(array_offset+2).setPointRadius(2);
        return previewLineData;
    }

    public synchronized List<Line> defaultLines(boolean simple) {//KS TODO support simple
        return defaultLines();
    }

    public List<Line> defaultLines() {
        addBgReadingValues();
        List<Line> lines = new ArrayList<Line>();
        Line[] calib = calibrationValuesLine();
        lines.add(calib[0]); // white circle of calib in background
        lines.add(minShowLine());
        lines.add(maxShowLine());
        lines.add(highLine());
        lines.add(lowLine());

        if (prefs.getBoolean("show_filtered_curve", false)) {
            final ArrayList<Line> filtered_lines = filteredLines();
            for (Line thisline : filtered_lines) {
                lines.add(thisline);
            }
        }
        // these last entries cannot be moved if
        // the point size change in previewLineData is to work
        lines.add(inRangeValuesLine());
        lines.add(lowValuesLine());
        lines.add(highValuesLine());
        lines.add(rawInterpretedLine());
        lines.add(calib[1]); // red dot of calib in foreground
        return lines;
    }

    public Line highValuesLine() {
        Line highValuesLine = new Line(highValues);
        highValuesLine.setColor(Color.parseColor("#FFFF00"));
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(pointSize);
        highValuesLine.setHasPoints(true);
        return highValuesLine;
    }

    public Line lowValuesLine() {
        Line lowValuesLine = new Line(lowValues);
        lowValuesLine.setColor(Color.parseColor("#FF0000"));
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(pointSize);
        lowValuesLine.setHasPoints(true);
        return lowValuesLine;
    }

    public Line inRangeValuesLine() {
        Line inRangeValuesLine = new Line(inRangeValues);
        inRangeValuesLine.setColor(Color.parseColor("#00FF00"));
        inRangeValuesLine.setHasLines(false);
        inRangeValuesLine.setPointRadius(pointSize);
        inRangeValuesLine.setHasPoints(true);
        return inRangeValuesLine;
    }

    public Line rawInterpretedLine() {
        Line line = new Line(rawInterpretedValues);
        line.setHasLines(false);
        line.setPointRadius(1);
        line.setHasPoints(true);
        return line;
    }

    // Produce an array of cubic lines, split as needed
    public ArrayList<Line> filteredLines() {
        ArrayList<Line> line_array = new ArrayList<Line>();
        float last_x_pos = -999999; // bogus mark value
        final float jump_threshold = 15; // in minutes
        List<PointValue> local_points = new ArrayList<PointValue>();

        if (filteredValues.size() > 0) {
            final float end_marker = filteredValues.get(filteredValues.size() - 1).getX();

            for (PointValue current_point : filteredValues) {
                // a jump too far for a line? make it a new one
                if (((last_x_pos != -999999) && (Math.abs(current_point.getX() - last_x_pos) > jump_threshold))
                        || current_point.getX() == end_marker) {
                    Line line = new Line(local_points);
                    line.setHasPoints(true);
                    line.setPointRadius(2);
                    line.setStrokeWidth(1);
                    line.setColor(Color.parseColor("#a0a0a0"));
                    line.setCubic(true);
                    line.setHasLines(true);
                    line_array.add(line);
                    local_points = new ArrayList<PointValue>();
                }
                last_x_pos = current_point.getX();
                local_points.add(current_point); // grow current line list
            }
        }
        return line_array;
    }

    public Line[] calibrationValuesLine() {
        Line[] lines = new Line[2];
        lines[0] = new Line(calibrationValues);
        lines[0].setColor(Color.parseColor("#FFFFFF"));
        lines[0].setHasLines(false);
        lines[0].setPointRadius(pointSize * 3 / 2);
        lines[0].setHasPoints(true);
        lines[1] = new Line(calibrationValues);
        lines[1].setColor(ChartUtils.COLOR_RED);
        lines[1].setHasLines(false);
        lines[1].setPointRadius(pointSize * 3 / 4);
        lines[1].setHasPoints(true);
        return lines;
    }


    private void addBgReadingValues(final boolean simple) {//KS TODO Add Noise, Momentum Trend implmentation
        addBgReadingValues();
    }

    private void addBgReadingValues() {
        final boolean show_filtered = prefs.getBoolean("show_filtered_curve", false);

        for (BgReading bgReading : bgReadings) {
            if (bgReading.raw_calculated != 0 && prefs.getBoolean("interpret_raw", false)) {
                rawInterpretedValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.raw_calculated),(float) unitized(bgReading.filtered_calculated_value)));
            } else if (bgReading.calculated_value >= 400) {
                highValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(400),(float) unitized(bgReading.filtered_calculated_value)));
            } else if (unitized(bgReading.calculated_value) >= highMark) {
                highValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value), (float) unitized(bgReading.filtered_calculated_value)));
            } else if (unitized(bgReading.calculated_value) >= lowMark) {
                inRangeValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value), (float) unitized(bgReading.filtered_calculated_value)));
            } else if (bgReading.calculated_value >= 40) {
                lowValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value),(float) unitized(bgReading.filtered_calculated_value)));
            } else if (bgReading.calculated_value > 13) {
                lowValues.add(new PointValueExtended((float) (bgReading.timestamp / FUZZER), (float) unitized(40), (float) unitized(bgReading.filtered_calculated_value)));
            }

            if ((show_filtered) && (bgReading.filtered_calculated_value > 0) && (bgReading.filtered_calculated_value != bgReading.calculated_value)) {
                filteredValues.add(new PointValueExtended((float) ((bgReading.timestamp - timeshift) / FUZZER), (float) unitized(bgReading.filtered_calculated_value)));
            }
        }
        for (Calibration calibration : calibrations) {
            calibrationValues.add(new PointValueExtended((float) (calibration.timestamp / FUZZER), (float) unitized(calibration.bg)));
        }
    }

    public static synchronized double getCurrentLowOccursAt() {//KS TODO implement low predictions
        try {
            final long last_bg_reading_timestamp = BgReading.last().timestamp;
            if (low_occurs_at_processed_till_timestamp < last_bg_reading_timestamp) {
                Log.d(TAG, "Recalculating lowOccursAt: " + JoH.dateTimeText((long) low_occurs_at_processed_till_timestamp) + " vs " + JoH.dateTimeText(last_bg_reading_timestamp));
                // new only the last hour worth of data for this
                (new BgGraphBuilder(xdrip.getAppContext(), System.currentTimeMillis() - 60 * 60 * 1000, System.currentTimeMillis() + 5 * 60 * 1000, 24, true)).addBgReadingValues(false);
            } else {
                Log.d(TAG, "Cached current low timestamp ok: " +  JoH.dateTimeText((long) low_occurs_at_processed_till_timestamp) + " vs " + JoH.dateTimeText(last_bg_reading_timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception in getCurrentLowOccursAt() " + e);
        }
        return low_occurs_at;
    }

    public Line highLine(){ return highLine(LINE_VISIBLE);}

    public Line highLine(boolean show) {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue((float) start_time / FUZZER, (float) highMark));
        highLineValues.add(new PointValue((float) end_time / FUZZER, (float) highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        if(show) {
            highLine.setColor(Color.parseColor("#FFFF00"));
        } else {
            highLine.setColor(Color.TRANSPARENT);
        }
        return highLine;
    }

    public Line lowLine(){ return lowLine(LINE_VISIBLE, FILL_UNDER_LINE);}

    public Line lowLine(boolean show, boolean line_only) {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float)start_time / FUZZER, (float)lowMark));
        lowLineValues.add(new PointValue((float) end_time / FUZZER, (float) lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        if(!line_only) {
            lowLine.setAreaTransparency(20);
            lowLine.setFilled(true);
        }
        lowLine.setStrokeWidth(1);
        if(show){
            lowLine.setColor(Color.parseColor("#C30909"));
        } else {
            lowLine.setColor(Color.TRANSPARENT);
        }
        return lowLine;
    }

    public Line maxShowLine() {
        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float) start_time / FUZZER, (float) defaultMaxY));
        maxShowValues.add(new PointValue((float) end_time / FUZZER, (float) defaultMaxY));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }

    public Line minShowLine() {
        List<PointValue> minShowValues = new ArrayList<PointValue>();
        minShowValues.add(new PointValue((float) start_time / FUZZER, (float) defaultMinY));
        minShowValues.add(new PointValue((float) end_time / FUZZER, (float) defaultMinY));
        Line minShowLine = new Line(minShowValues);
        minShowLine.setHasPoints(false);
        minShowLine.setHasLines(false);
        return minShowLine;
    }

    /////////AXIS RELATED//////////////
    public Axis yAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(false);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        for(int j = 1; j <= 12; j += 1) {
            if (doMgdl) {
                axisValues.add(new AxisValue(j * 50));
            } else {
                axisValues.add(new AxisValue(j*2));
            }
        }
        yAxis.setValues(axisValues);
        yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        yAxis.setTextSize(axisTextSize);
        return yAxis;
    }

    public Axis chartXAxis() {
        Axis xAxis = xAxis();
        xAxis.setTextSize(axisTextSize);
        return xAxis;
    }

    private SimpleDateFormat hourFormat() {
        return new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH" : "h a");
    }

    // Please note, an xLarge table is also large, but a small one is only small.
    static public boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
    static public boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static String noiseString(double thisnoise) {
        if (thisnoise > NOISE_HIGH) return "Extreme";
        if (thisnoise > NOISE_TOO_HIGH_FOR_PREDICT) return "Very High";
        if (thisnoise > NOISE_TRIGGER) return "High";
        return "Low";
    }

    public Axis previewXAxis(){
        Axis previewXaxis = xAxis();
        previewXaxis.setTextSize(previewAxisTextSize);
        return previewXaxis;
    }

    @NonNull
    private Axis xAxis() {
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(start_time);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis()<start_time){
            calendar.add(Calendar.HOUR, 1);
        }
        while (calendar.getTimeInMillis()<end_time){
            axisValues.add(new AxisValue((calendar.getTimeInMillis() / FUZZER), (timeFormat.format(calendar.getTimeInMillis())).toCharArray()));
            calendar.add(Calendar.HOUR, 1);
        }
        Axis axis = new Axis();
        axis.setValues(axisValues);
        axis.setHasLines(true);
        return axis;
    }

    /////////VIEWPORT RELATED//////////////
    public Viewport advanceViewport(Chart chart, Chart previewChart) {
        viewport = new Viewport(previewChart.getMaximumViewport());
        viewport.inset((float) ((86400000 / 2.5) / FUZZER), 0);
        double distance_to_move = ((new Date().getTime())/ FUZZER) - viewport.left - (((viewport.right - viewport.left) /2));
        viewport.offset((float) distance_to_move, 0);
        return viewport;
    }

    public double unitized(double value) {
        if (doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public static double unitized(double value, boolean doMgdl) {
        if (doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public String unitized_string(double value) {
        return unitized_string(value, doMgdl);
    }

    public static String unitized_string_static(double value) {
        return unitized_string(value, Pref.getString("units", "mgdl").equals("mgdl"));
    }
    public static String unitized_string_with_units_static(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl)+" "+(domgdl ? "mg/dl" : "mmol/l");
    }
    public static String unitized_string_with_units_static_short(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl)+" "+(domgdl ? "mgdl" : "mmol");
    }


    public static String unitized_string(double value, boolean doMgdl) {
        DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            return "HIGH";
        } else if (value >= 40) {
            if (doMgdl) {
                df.setMaximumFractionDigits(0);
                return df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                //next line ensures mmol/l value is XX.x always.  Required by PebbleWatchSync, and probably not a bad idea.
                df.setMinimumFractionDigits(1);
                return df.format(mmolConvert(value));
            }
        } else if (value > 12) {
            return "LOW";
        } else {
            switch ((int) value) {
                case 0:
                    return "??0";
                case 1:
                    return "?SN";
                case 2:
                    return "??2";
                case 3:
                    return "?NA";
                case 5:
                    return "?NC";
                case 6:
                    return "?CD";
                case 9:
                    return "?AD";
                case 12:
                    return "?RF";
                default:
                    return "???";
            }
        }
    }


    public String unitizedDeltaString(boolean showUnit, boolean highGranularity) {
        return unitizedDeltaString( showUnit, highGranularity,Home.get_follower());
    }

    public String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean is_follower) {
        return unitizedDeltaString(showUnit, highGranularity, is_follower, doMgdl);
    }

    public static String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean is_follower, boolean doMgdl) {

        List<BgReading> last2 = BgReading.latest(2,is_follower);
        if (last2.size() < 2 || last2.get(0).timestamp - last2.get(1).timestamp > 20 * 60 * 1000) {
            // don't show delta if there are not enough values or the values are more than 20 mintes apart
            return "???";
        }

        double value = BgReading.currentSlope(is_follower) * 5 * 60 * 1000;

        return unitizedDeltaStringRaw(showUnit, highGranularity, value, doMgdl);
    }

    public String unitizedDeltaStringRaw(boolean showUnit, boolean highGranularity, double value) {
        return unitizedDeltaStringRaw(showUnit, highGranularity, value, doMgdl);
    }

    public static String unitizedDeltaStringRaw(boolean showUnit, boolean highGranularity,double value, boolean doMgdl) {


        if (Math.abs(value) > 100) {
            // a delta > 100 will not happen with real BG values -> problematic sensor data
            return "ERR";
        }

        // TODO: allow localization from os settings once pebble doesn't require english locale
        DecimalFormat df = new DecimalFormat("#", new DecimalFormatSymbols(Locale.ENGLISH));
        String delta_sign = "";
        if (value > 0) {
            delta_sign = "+";
        }
        if (doMgdl) {

            if (highGranularity) {
                df.setMaximumFractionDigits(1);
            } else {
                df.setMaximumFractionDigits(0);
            }

            return delta_sign + df.format(unitized(value,doMgdl)) + (showUnit ? " mg/dl" : "");
        } else {
            // only show 2 decimal places on mmol/l delta when less than 0.1 mmol/l
            if (highGranularity && (Math.abs(value) < (Constants.MMOLL_TO_MGDL * 0.1))) {
                df.setMaximumFractionDigits(2);
            } else {
                df.setMaximumFractionDigits(1);
            }

            df.setMinimumFractionDigits(1);
            df.setMinimumIntegerDigits(1);
            return delta_sign + df.format(unitized(value,doMgdl)) + (showUnit ? " mmol/l" : "");
        }
    }

    public String unit() {
        return unit(doMgdl);
    }

    public static String unit(boolean doMgdl) {
        if (doMgdl) {
            return "mg/dl";
        } else {
            return "mmol";
        }
    }


    public String oldunitizedDeltaString(boolean showUnit, boolean highGranularity) {

        List<BgReading> last2 = BgReading.latest(2);
        if(last2.size() < 2 || last2.get(0).timestamp - last2.get(1).timestamp > MAX_SLOPE_MINUTES * 60 * 1000){
            // don't show delta if there are not enough values or the values are more than 20 mintes apart
            return "???";
        }

        double value = BgReading.currentSlope() * 5*60*1000;

        if(Math.abs(value) > 100){
            // a delta > 100 will not happen with real BG values -> problematic sensor data
            return "ERR";
        }

        // TODO: allow localization from os settings once pebble doesn't require english locale
        DecimalFormat df = new DecimalFormat("#", new DecimalFormatSymbols(Locale.ENGLISH));
        String delta_sign = "";
        if (value > 0) { delta_sign = "+"; }
        if(doMgdl) {

            if(highGranularity){
                df.setMaximumFractionDigits(1);
            } else {
                df.setMaximumFractionDigits(0);
            }

            return delta_sign + df.format(unitized(value)) +  (showUnit?" mg/dl":"");
        } else {

            if(highGranularity){
                df.setMaximumFractionDigits(2);
            } else {
                df.setMaximumFractionDigits(1);
            }

            df.setMinimumFractionDigits(1);
            df.setMinimumIntegerDigits(1);
            return delta_sign + df.format(unitized(value)) + (showUnit?" mmol/l":"");
        }
    }


    public static double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }


    public OnValueSelectTooltipListener getOnValueSelectTooltipListener(){
        return new OnValueSelectTooltipListener();
    }

    public class OnValueSelectTooltipListener implements LineChartOnValueSelectListener{

        private Toast tooltip;

        @Override
        public synchronized void onValueSelected(int i, int i1, PointValue pointValue) {
        	
        	String filtered = "";
        	try {
        		PointValueExtended pve = (PointValueExtended) pointValue;
        		if(pve.calculatedFilteredValue != -1) {
        			filtered = " (" + Math.round(pve.calculatedFilteredValue*10) / 10d +")";
        		}
        	} catch (ClassCastException e) {
        		Log.e(TAG, "Error casting a point from pointValue to PointValueExtended", e);
        	}
            final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
            //Won't give the exact time of the reading but the time on the grid: close enough.
            Long time = ((long)pointValue.getX())*FUZZER;
            if(tooltip!= null){
                tooltip.cancel();
            }
            tooltip = Toast.makeText(context, timeFormat.format(time)+ ": " + Math.round(pointValue.getY()*10)/ 10d + filtered, Toast.LENGTH_LONG);
            tooltip.show();
        }

        @Override
        public void onValueDeselected() {
            // do nothing
        }
    }

    public static void refreshNoiseIfOlderThan(long timestamp) {
        // stub method for wear compatibility - update when noise calculations supported
    }
}
