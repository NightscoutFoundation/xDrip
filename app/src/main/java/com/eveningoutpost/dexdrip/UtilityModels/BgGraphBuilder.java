package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.Forecast.PolyTrendLine;
import com.eveningoutpost.dexdrip.Models.Forecast.TrendLine;
import com.eveningoutpost.dexdrip.Models.Iob;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.location.DetectedActivity;
import com.rits.cloning.Cloner;

import java.lang.reflect.Field;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lecho.lib.hellocharts.formatter.LineChartValueFormatter;
import lecho.lib.hellocharts.formatter.SimpleLineChartValueFormatter;
import lecho.lib.hellocharts.listener.LineChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;

import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.X;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;

/**
 * Created by stephenblack on 11/15/14.
 */

class PointValueExtended extends PointValue {

    public PointValueExtended(float x, float y, String note_param) {
        super(x, y);
        note = note_param;
    }

    public PointValueExtended(float x, float y, float filtered) {
        super(x, y);
        calculatedFilteredValue = filtered;
    }
    public PointValueExtended(float x, float y) {
        super(x, y);
        calculatedFilteredValue = -1;
    }

    float calculatedFilteredValue;
    String note;
}

public class BgGraphBuilder {
    public static final int FUZZER = (1000 * 30 * 5); // 2.5 mins?
    public final static long DEXCOM_PERIOD = 300000;
    public final static double NOISE_TRIGGER = 10;
    public final static double NOISE_TOO_HIGH_FOR_PREDICT = 60;
    public final static double NOISE_HIGH = 200;
    public final static double NOISE_FORGIVE = 100;
    public static double low_occurs_at = -1;
    public static double previous_low_occurs_at = -1;
    private static double low_occurs_at_processed_till_timestamp = -1;
    private static long noise_processed_till_timestamp = -1;
    private final static String TAG = "jamorham graph";
    //private final static int pluginColor = Color.parseColor("#AA00FFFF"); // temporary

    private final static int pluginSize = 2;
    final int pointSize;
    final int axisTextSize;
    final int previewAxisTextSize;
    final int hoursPreviewStep;
    //private final int numValues = (60 / 5) * 24;
    public double end_time = (new Date().getTime() + (60000 * 10)) / FUZZER;
    public double predictive_end_time;
    public double start_time = end_time - ((60000 * 60 * 24)) / FUZZER;
    private final static double timeshift = 500000;
    private static final int NUM_VALUES = (60 / 5) * 24;

    // flag to indicate if readings data has been adjusted
    private static boolean plugin_adjusted = false;
    // used to prevent concurrency problems with calibration plugins
    private static final ReentrantLock readings_lock = new ReentrantLock();

    private final List<Treatments> treatments;
    private final static boolean d = false; // debug flag, could be read from preferences

    public Context context;
    public SharedPreferences prefs;
    public double highMark;
    public double lowMark;
    public double defaultMinY;
    public double defaultMaxY;
    public boolean doMgdl;
    public Viewport viewport;
    public static double capturePercentage = -1;
    private int predictivehours = 0;
    private boolean prediction_enabled = false;
    private boolean simulation_enabled = false;
    private static double avg1value = 0;
    private static double avg2value = 0;
    private static int avg1counter = 0;
    private static double avg1startfuzzed = 0;
    private static int avg2counter = 0;
    private final int loaded_numValues;
    private final long loaded_start, loaded_end;
    private final List<BgReading> bgReadings;
    private final List<Calibration> calibrations;
    private final List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private final List<PointValue> highValues = new ArrayList<PointValue>();
    private final List<PointValue> lowValues = new ArrayList<PointValue>();
    private final List<PointValue> pluginValues = new ArrayList<PointValue>();
    private final List<PointValue> rawInterpretedValues = new ArrayList<PointValue>();
    private final List<PointValue> filteredValues = new ArrayList<PointValue>();
    private final List<PointValue> calibrationValues = new ArrayList<PointValue>();
    private final List<PointValue> treatmentValues = new ArrayList<PointValue>();
    private final List<PointValue> iobValues = new ArrayList<PointValue>();
    private final List<PointValue> cobValues = new ArrayList<PointValue>();
    private final List<PointValue> predictedBgValues = new ArrayList<PointValue>();
    private final List<PointValue> polyBgValues = new ArrayList<PointValue>();
    private final List<PointValue> noisePolyBgValues = new ArrayList<PointValue>();
    private final List<PointValue> activityValues = new ArrayList<PointValue>();
    private final List<PointValue> annotationValues = new ArrayList<>();
    private static TrendLine noisePoly;
    public static double last_noise = -99999;
    public static double original_value = -99999;
    public static double best_bg_estimate = -99999;
    public static double last_bg_estimate = -99999;


    public BgGraphBuilder(Context context) {
        this(context, new Date().getTime() + (60000 * 10));
    }

    public BgGraphBuilder(Context context, long end) {
        this(context, end - (60000 * 60 * 24), end);
    }

    public BgGraphBuilder(Context context, long start, long end) {
        this(context, start, end, NUM_VALUES, true);
    }

    public BgGraphBuilder(Context context, long start, long end, int numValues, boolean show_prediction) {
        // swap argument order if needed
        if (start > end) {
            long temp = end;
            end = start;
            start = temp;
            if (d) Log.d(TAG, "Swapping timestamps");
        }
        if (d)
            Log.d(TAG, "Called timestamps: " + JoH.dateTimeText(start) + " -> " + JoH.dateTimeText(end));
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prediction_enabled = show_prediction;
        if (prediction_enabled)
            simulation_enabled = prefs.getBoolean("simulations_enabled", true);
        end_time = end / FUZZER;
        start_time = start / FUZZER;

        readings_lock.lock();
        try {
            // store the initialization values used for this instance
            loaded_numValues=numValues;
            loaded_start=start;
            loaded_end=end;
            bgReadings = BgReading.latestForGraph(numValues, start, end);
            plugin_adjusted = false;
        } finally {
            readings_lock.unlock();
        }

        if ((end - start) > 80000000) {
            try {
                capturePercentage = ((bgReadings.size() * 100) / ((end - start) / 300000));
                //Log.d(TAG, "CPTIMEPERIOD: " + Long.toString(end - start) + " percentage: " + JoH.qs(capturePercentage));
            } catch (Exception e) {
                capturePercentage = -1; // invalid reading
            }
        }
        calibrations = Calibration.latestForGraph(numValues, start, end);
        treatments = Treatments.latestForGraph(numValues, start, end + (120 * 60 * 1000));
        this.context = context;
        this.highMark = tolerantParseDouble(prefs.getString("highValue", "170"));
        this.lowMark = tolerantParseDouble(prefs.getString("lowValue", "70"));
        this.doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        defaultMinY = unitized(40);
        defaultMaxY = unitized(250);
        pointSize = isXLargeTablet(context) ? 5 : 3;
        axisTextSize = isXLargeTablet(context) ? 20 : Axis.DEFAULT_TEXT_SIZE_SP;
        previewAxisTextSize = isXLargeTablet(context) ? 12 : 5;
        hoursPreviewStep = isXLargeTablet(context) ? 2 : 1;
    }

    private static double tolerantParseDouble(String str) throws NumberFormatException {
        return Double.parseDouble(str.replace(",", "."));

    }

    private double bgScale() {
        if (doMgdl)
            return Constants.MMOLL_TO_MGDL;
        else
            return 1;
    }

    private static Object cloneObject(Object obj) {
        try {
            Object clone = obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(clone, field.get(obj));
            }
            return clone;
        } catch (Exception e) {
            return null;
        }
    }

    static public boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
    
    static public boolean isLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public static String noiseString(double thisnoise) {
        if (thisnoise > NOISE_HIGH) return "Extreme";
        if (thisnoise > NOISE_TOO_HIGH_FOR_PREDICT) return "Very High";
        if (thisnoise > NOISE_TRIGGER) return "High";
        return "Low";
    }

    private void extend_line(List<PointValue> points, float x, float y) {
        if (points.size() > 1) {
            points.remove(1); // replace last
        }
        points.add(new PointValue(x, y));
        Log.d(TAG,"Extend line size: "+points.size());
    }

    // line illustrating result from step counter
    private List<Line> stepsLines() {
        final List<Line> stepsLines = new ArrayList<>();
        if ((prefs.getBoolean("use_pebble_health", true)
                && prefs.getBoolean("show_pebble_movement_line", true))) {
            final List<PebbleMovement> pmlist = PebbleMovement.deltaListFromMovementList(PebbleMovement.latestForGraph(2000, loaded_start, loaded_end));
            PointValue last_point = null;
            final boolean d = false;
            if (d) Log.d(TAG, "Delta: pmlist size: " + pmlist.size());
            final float yscale = doMgdl ? (float) Constants.MMOLL_TO_MGDL : 1f;
            final float ypos = 6 * yscale; // TODO Configurable
            //final long last_timestamp = pmlist.get(pmlist.size() - 1).timestamp;
            final float MAX_SIZE = 40;
            int flipper = 0;
            int accumulator = 0;

            for (PebbleMovement pm : pmlist) {
                if (last_point == null) {
                    last_point = new PointValue((float) pm.timestamp / FUZZER, ypos);
                } else {
                    final PointValue this_point = new PointValue((float) pm.timestamp / FUZZER, ypos);
                    final float time_delta = this_point.getX() - last_point.getX();
                    if (time_delta > 1) {

                        final List<PointValue> new_points = new ArrayList<>();
                        new_points.add(last_point);
                        new_points.add(this_point);

                        last_point = this_point; // update pointer
                        final Line this_line = new Line(new_points);
                        flipper ^= 1;
                        this_line.setColor((flipper == 0) ? getCol(X.color_step_counter1) : getCol(X.color_step_counter2));

                        float stroke_size = Math.min(MAX_SIZE, (float) Math.log1p(((double) (pm.metric + accumulator)) / time_delta) * 5);
                        if (d) Log.d(TAG, "Delta stroke: " + stroke_size);
                        this_line.setStrokeWidth((int) stroke_size);

                        if (d)
                            Log.d(TAG, "Delta-Line: " + JoH.dateTimeText(pm.timestamp) + " time delta: " + time_delta + "  total: " + (pm.metric + accumulator) + " lsize: " + stroke_size + " / " + (int) stroke_size);
                        accumulator = 0;

                        if (this_line.getStrokeWidth() > 0) {
                            stepsLines.add(this_line);
                            this_line.setHasPoints(false);
                            this_line.setHasLines(true);
                        } else {
                            if (d) Log.d(TAG, "Delta skip: " + JoH.dateTimeText(pm.timestamp));
                        }
                        if (d)
                            Log.d(TAG, "Delta-List: " + JoH.dateTimeText(pm.timestamp) + " time delta: " + time_delta + "  val: " + pm.metric);
                    } else {
                        accumulator += pm.metric;
                        if (d)
                            Log.d(TAG, "Delta: added: " + JoH.dateTimeText(pm.timestamp) + " metric: " + pm.metric + " to accumulator: " + accumulator);
                    }
                }
            }
            if (d)
                Log.d(TAG, "Delta returning stepsLines: " + stepsLines.size() + " final accumulator remaining: " + accumulator);
        }
        return stepsLines;
    }

    private List<Line> motionLine() {

        final ArrayList<ActivityRecognizedService.motionData> motion_datas = ActivityRecognizedService.getForGraph((long) start_time * FUZZER, (long) end_time * FUZZER);
        List<PointValue> linePoints = new ArrayList<>();

        final float ypos = (float)highMark;
        int last_type = -9999;


        final ArrayList<Line> line_array = new ArrayList<>();

        Log.d(TAG,"Motion datas size: "+motion_datas.size());
        if (motion_datas.size() > 0) {
            motion_datas.add(new ActivityRecognizedService.motionData((long) end_time * FUZZER, DetectedActivity.UNKNOWN)); // terminator

            for (ActivityRecognizedService.motionData item : motion_datas) {

                Log.d(TAG, "Motion detail: " + JoH.dateTimeText(item.timestamp) + " activity: " + item.activity);
                if ((last_type != -9999) && (last_type != item.activity)) {
                    extend_line(linePoints, item.timestamp / FUZZER, ypos);
                    Line new_line = new Line(linePoints);
                    new_line.setHasLines(true);
                    new_line.setPointRadius(0);
                    new_line.setStrokeWidth(1);
                    new_line.setAreaTransparency(40);
                    new_line.setHasPoints(false);
                    new_line.setFilled(true);

                    switch (last_type) {
                        case DetectedActivity.IN_VEHICLE:
                            new_line.setColor(Color.parseColor("#70445599"));
                            break;
                        case DetectedActivity.ON_FOOT:
                            new_line.setColor(Color.parseColor("#70995599"));
                            break;
                    }
                    line_array.add(new_line);
                    linePoints = new ArrayList<>();
                }
                //current
                switch (item.activity) {
                    case DetectedActivity.ON_FOOT:
                    case DetectedActivity.IN_VEHICLE:
                        extend_line(linePoints, item.timestamp / FUZZER, ypos);
                        last_type = item.activity;
                        break;

                    default:
                        // do nothing?
                        break;
                }
            }

        }
        Log.d(TAG,"Motion array size: "+line_array.size());
            return line_array;
    }


    public LineChartData lineData() {
       // if (d) Log.d(TAG, "START lineData from: " + JoH.backTrace());
       JoH.benchmark(null);
        LineChartData lineData = new LineChartData(defaultLines(false));
        JoH.benchmark("Default lines create - bggraph builder");
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(chartXAxis());
        return lineData;
    }

    public LineChartData previewLineData(LineChartData hint) {

        LineChartData previewLineData;
        if (hint == null) {
            previewLineData = new LineChartData(lineData());
        } else {
            JoH.benchmark(null);
            Cloner cloner = new Cloner();
           // cloner.setDumpClonedClasses(true);
            cloner.dontClone(
                    lecho.lib.hellocharts.model.PointValue.class,
                    lecho.lib.hellocharts.formatter.SimpleLineChartValueFormatter.class,
                    lecho.lib.hellocharts.model.Axis.class,
                    android.graphics.DashPathEffect.class);
            previewLineData = cloner.deepClone(hint);
            JoH.benchmark("Clone preview data");
            if (d) Log.d(TAG,"Cloned preview chart data");
        }

        previewLineData.setAxisYLeft(yAxis());
        previewLineData.setAxisXBottom(previewXAxis());

        final List<Line> removeItems = new ArrayList<>();
        int unlabledLinesSize = 1;
        if (isXLargeTablet(context)) {
            unlabledLinesSize = 2;
        }
        for (Line lline : previewLineData.getLines()) {
            if (((lline.getPointRadius() == pluginSize) && (lline.getPointColor() == getCol(X.color_secondary_glucose_value)))
                    || ((lline.getColor() == getCol(X.color_step_counter1) || (lline.getColor() == getCol(X.color_step_counter2))))) {
                removeItems.add(lline); // remove plugin or step counter plot from preview graph
            }

            if ((lline.hasLabels() && (lline.getPointRadius() > 0))) {

                lline.setPointRadius(3); // preserve size for treatments
                lline.setPointColor(Color.parseColor("#FFFFFF"));
            } else if (lline.getPointRadius() > 0) {
                lline.setPointRadius(unlabledLinesSize);
            }
            lline.setHasLabels(false);
        }

        for (Line item : removeItems) {
            previewLineData.getLines().remove(item);
        }

        // needs more adjustments - foreach
        return previewLineData;
    }

    public synchronized List<Line> defaultLines(boolean simple) {
        List<Line> lines = new ArrayList<Line>();
        try {

            addBgReadingValues(simple);

            if (!simple) {
                // motion lines
                if (Home.getPreferencesBoolean("motion_tracking_enabled", false) && Home.getPreferencesBoolean("plot_motion", false)) {
                    lines.addAll(motionLine());
                }
                lines.addAll(stepsLines());
            }

            Line[] calib = calibrationValuesLine();
            Line[] treatments = treatmentValuesLine();

            for (Line subLine : autoSplitLine(treatments[2], 10)) {
                lines.add(subLine); // iob line
            }

            predictive_end_time = simple ? end_time : ((end_time * FUZZER) + (60000 * 10) + (1000 * 60 * 60 * predictivehours)) / FUZZER; // used first in ideal/highline
//            predictive_end_time = (new Date().getTime() + (60000 * 10) + (1000 * 60 * 60 * predictivehours)) / FUZZER; // used first in ideal/highline


            if (prefs.getBoolean("show_full_average_line", false)) {
                if (avg2value > 0) lines.add(avg2Line());
            }
            if (prefs.getBoolean("show_recent_average_line", true)) {
                if (avg1value > 0) lines.add(avg1Line());
            }
            if (prefs.getBoolean("show_target_line", false)) {
                lines.add(idealLine());
            }

            lines.add(treatments[3]); // activity
            lines.add(treatments[5]); // predictive
            lines.add(treatments[6]); // cob
            lines.add(treatments[7]); // poly predict


            lines.add(minShowLine());
            lines.add(maxShowLine());
            lines.add(highLine());
            lines.add(predictiveHighLine());
            lines.add(lowLine());
            lines.add(predictiveLowLine());

            if (prefs.getBoolean("show_filtered_curve", true)) {
                // use autosplit here too
               final ArrayList<Line> filtered_lines = filteredLines();

                for (Line thisline : filtered_lines) {
                    lines.add(thisline);
                }
            }
            lines.add(rawInterpretedLine());

            lines.add(inRangeValuesLine());
            lines.add(lowValuesLine());
            lines.add(highValuesLine());

            List<Line> extra_lines = extraLines();
            for (Line eline : extra_lines) {
                lines.add(eline);
            }

            // check show debug option here - drawn on top of others
            lines.add(treatments[8]); // noise poly predict

            lines.add(calib[0]); // white circle of calib in background
            lines.add(treatments[0]); // white circle of treatment in background

            lines.add(calib[1]); // red dot of calib in foreground
            lines.add(treatments[1]); // blue dot in centre // has annotation
            lines.add(treatments[4]); // annotations






        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error in bgbuilder defaultlines: " + e.toString());
        }
        return lines;
    }

    public Line highValuesLine() {
        Line highValuesLine = new Line(highValues);
        //highValuesLine.setColor(ChartUtils.COLOR_ORANGE);
        highValuesLine.setColor(getCol(X.color_high_values));
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(pointSize);
        highValuesLine.setHasPoints(true);
        return highValuesLine;
    }

    public Line lowValuesLine() {
        Line lowValuesLine = new Line(lowValues);
        //lowValuesLine.setColor(Color.parseColor("#C30909"));
        lowValuesLine.setColor(getCol(X.color_low_values));
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(pointSize);
        lowValuesLine.setHasPoints(true);
        return lowValuesLine;
    }

    public Line inRangeValuesLine() {
        Line inRangeValuesLine = new Line(inRangeValues);
        //inRangeValuesLine.setColor(ChartUtils.COLOR_BLUE);
        inRangeValuesLine.setColor(getCol(X.color_inrange_values));
        inRangeValuesLine.setHasLines(false);
        inRangeValuesLine.setPointRadius(pointSize);
        inRangeValuesLine.setHasPoints(true);
        return inRangeValuesLine;
    }

    public void debugPrintPoints(List<PointValue> mypoints) {
        for (PointValue thispoint : mypoints) {
            UserError.Log.i(TAG, "Debug Points: " + thispoint.toString());
        }
    }

    // auto split a line - jump thresh in minutes
    public ArrayList<Line> autoSplitLine(Line macroline, final float jumpthresh) {
       // if (d) Log.d(TAG, "Enter autoSplit Line");
        ArrayList<Line> linearray = new ArrayList<Line>();
        float lastx = -999999;

        List<PointValue> macropoints = macroline.getValues();
        List<PointValue> thesepoints = new ArrayList<PointValue>();

        if (macropoints.size() > 0) {

            final float endmarker = macropoints.get(macropoints.size() - 1).getX();
            for (PointValue thispoint : macropoints) {

                // a jump too far for a line? make it a new one
                if (((lastx != -999999) && (Math.abs(thispoint.getX() - lastx) > jumpthresh))
                        || thispoint.getX() == endmarker) {

                    if (thispoint.getX() == endmarker) {
                        thesepoints.add(thispoint);
                    }
                    Line line = (Line) cloneObject(macroline); // aieeee
                    line.setValues(thesepoints);
                    linearray.add(line);
                    thesepoints = new ArrayList<PointValue>();
                }

                lastx = thispoint.getX();
                thesepoints.add(thispoint); // grow current line list
            }
        }
     //   if (d) Log.d(TAG, "Exit autoSplit Line");
        return linearray;
    }
    // Produce an array of cubic lines, split as needed
    public ArrayList<Line> filteredLines() {
        ArrayList<Line> linearray = new ArrayList<Line>();
        float lastx = -999999; // bogus mark value
        final float jumpthresh = 15; // in minutes
        List<PointValue> thesepoints = new ArrayList<PointValue>();

        if (filteredValues.size() > 0) {

            final float endmarker = filteredValues.get(filteredValues.size() - 1).getX();

            for (PointValue thispoint : filteredValues) {
                // a jump too far for a line? make it a new one
                if (((lastx != -999999) && (Math.abs(thispoint.getX() - lastx) > jumpthresh))
                        || thispoint.getX() == endmarker) {
                    Line line = new Line(thesepoints);
                    line.setHasPoints(true);
                    line.setPointRadius(2);
                    line.setStrokeWidth(1);
                    line.setColor(getCol(X.color_filtered));
                    line.setCubic(true);
                    line.setHasLines(true);
                    linearray.add(line);
                    thesepoints = new ArrayList<PointValue>();
                }

                lastx = thispoint.getX();
                thesepoints.add(thispoint); // grow current line list
            }
        } else {
            UserError.Log.i(TAG, "Raw points size is zero");
        }


        //UserError.Log.i(TAG, "Returning linearray: " + Integer.toString(linearray.size()));
        return linearray;
    }

    public Line rawInterpretedLine() {
        Line line = new Line(rawInterpretedValues);
        line.setHasLines(false);
        line.setPointRadius(1);
        line.setHasPoints(true);
        return line;
    }

    public List<Line> extraLines()
    {
        final List<Line> lines = new ArrayList<>();
        Line line = new Line(pluginValues);
        line.setHasLines(false);
        line.setPointRadius(pluginSize);
        line.setHasPoints(true);
        line.setColor(getCol(X.color_secondary_glucose_value));
        lines.add(line);
        return lines;
    }

    public Line[] calibrationValuesLine() {
        Line[] lines = new Line[2];
        lines[0] = new Line(calibrationValues);
        lines[0].setColor(getCol(X.color_calibration_dot_background));
        lines[0].setHasLines(false);
        lines[0].setPointRadius(pointSize * 3 / 2);
        lines[0].setHasPoints(true);
        lines[1] = new Line(calibrationValues);
        lines[1].setColor(getCol(X.color_calibration_dot_foreground));
        lines[1].setHasLines(false);
        lines[1].setPointRadius(pointSize * 3 / 4);
        lines[1].setHasPoints(true);
        return lines;
    }

    public Line[] treatmentValuesLine() {
        Line[] lines = new Line[9];
        try {

            lines[0] = new Line(treatmentValues);
            lines[0].setColor(getCol(X.color_treatment_dot_background));
            lines[0].setHasLines(false);
            lines[0].setPointRadius(pointSize * 5 / 2);
            lines[0].setHasPoints(true);

            lines[1] = new Line(treatmentValues);
            lines[1].setColor(getCol(X.color_treatment_dot_foreground));
            lines[1].setHasLines(false);
            lines[1].setPointRadius(pointSize * 5 / 4);
            lines[1].setHasPoints(true);
            lines[1].setShape(ValueShape.DIAMOND);
            lines[1].setHasLabels(true);

            LineChartValueFormatter formatter = new SimpleLineChartValueFormatter(1);
            lines[1].setFormatter(formatter);

            // insulin on board
            lines[2] = new Line(iobValues);
            lines[2].setColor(getCol(X.color_treatment));
            // need splitter for cubics
            lines[2].setHasLines(true);
            lines[2].setCubic(false);
            lines[2].setFilled(true);
            lines[2].setAreaTransparency(35);
            lines[2].setFilled(true);
            lines[2].setPointRadius(1);
            lines[2].setHasPoints(true);
            // lines[2].setShape(ValueShape.DIAMOND);
            // lines[2].setHasLabels(true);

            // iactivity on board
            lines[3] = new Line(activityValues);
            lines[3].setColor(getCol(X.color_treatment_dark));
            lines[3].setHasLines(false);
            lines[3].setCubic(false);
            lines[3].setFilled(false);

            lines[3].setFilled(false);
            lines[3].setPointRadius(1);
            lines[3].setHasPoints(true);

            // annotations
            lines[4] = new Line(annotationValues);
            lines[4].setColor(getCol(X.color_treatment_dot_foreground));
            lines[4].setHasLines(false);
            lines[4].setCubic(false);
            lines[4].setFilled(false);
            lines[4].setPointRadius(0);
            lines[4].setHasPoints(true);
            lines[4].setHasLabels(true);

            lines[5] = new Line(predictedBgValues);
            lines[5].setColor(getCol(X.color_predictive));
            lines[5].setHasLines(false);
            lines[5].setCubic(false);
            lines[5].setStrokeWidth(1);
            lines[5].setFilled(false);
            lines[5].setPointRadius(2);
            lines[5].setHasPoints(true);
            lines[5].setHasLabels(false);

            lines[6] = new Line(cobValues);
            lines[6].setColor(getCol(X.color_predictive_dark));
            lines[6].setHasLines(false);
            lines[6].setCubic(false);
            lines[6].setFilled(false);
            lines[6].setPointRadius(1);
            lines[6].setHasPoints(true);
            lines[6].setHasLabels(false);

            lines[7] = new Line(polyBgValues);
            lines[7].setColor(ChartUtils.COLOR_RED);
            lines[7].setHasLines(false);
            lines[7].setCubic(false);
            lines[7].setStrokeWidth(1);
            lines[7].setFilled(false);
            lines[7].setPointRadius(1);
            lines[7].setHasPoints(true);
            lines[7].setHasLabels(false);

            lines[8] = new Line(noisePolyBgValues);
            lines[8].setColor(ChartUtils.COLOR_ORANGE);
            lines[8].setHasLines(true);
            lines[8].setCubic(false);
            lines[8].setStrokeWidth(1);
            lines[8].setFilled(false);
            lines[8].setPointRadius(1);
            lines[8].setHasPoints(true);
            lines[8].setHasLabels(false);


        } catch (Exception e) {
            if (d) Log.i(TAG, "Exception making treatment lines: " + e.toString());
        }
        return lines;
    }

    private void addBgReadingValues() {
        addBgReadingValues(true);
    }

    private synchronized void addBgReadingValues(final boolean simple) {
        if (readings_lock.isLocked()) {
            Log.d(TAG, "BgReadings lock is currently held");
        }
        readings_lock.lock();

        try {

            if (plugin_adjusted) {
                Log.i(TAG, "Reloading as Plugin modified data: " + JoH.backTrace(1) + " size:" + bgReadings.size());
                bgReadings.clear();
                bgReadings.addAll(BgReading.latestForGraph(loaded_numValues, loaded_start, loaded_end));
            } else {
                //Log.d(TAG, "not adjusted");
            }

            filteredValues.clear();
            rawInterpretedValues.clear();
            iobValues.clear();
            activityValues.clear();
            cobValues.clear();
            predictedBgValues.clear();
            polyBgValues.clear();
            noisePolyBgValues.clear();
            annotationValues.clear();
            treatmentValues.clear();
            highValues.clear();
            lowValues.clear();
            inRangeValues.clear();
            calibrationValues.clear();
            pluginValues.clear();

            final double bgScale = bgScale();
            final double now = JoH.ts();
            long highest_bgreading_timestamp = -1; // most recent bgreading timestamp we have
            double trend_start_working = now - (1000 * 60 * 12); // 10 minutes // TODO MAKE PREFERENCE?
            if (bgReadings.size() > 0) {
                highest_bgreading_timestamp = bgReadings.get(0).timestamp;
                final double ms_since_last_reading = now - highest_bgreading_timestamp;
                if (ms_since_last_reading < 500000) {
                    trend_start_working -= ms_since_last_reading; // push back start of trend calc window
                    Log.d(TAG, "Pushed back trend start by: " + JoH.qs(ms_since_last_reading / 1000) + " secs - last reading: " + JoH.dateTimeText(highest_bgreading_timestamp));
                }
            }

            final double trendstart = trend_start_working;
            final double noise_trendstart = now - (1000 * 60 * 20); // 20 minutes // TODO MAKE PREFERENCE
            double oldest_noise_timestamp = now;
            double newest_noise_timestamp = 0;
            TrendLine[] polys = new TrendLine[5];

            polys[0] = new PolyTrendLine(1);
            // polys[1] = new PolyTrendLine(2);
            polys[1] = new Forecast.LogTrendLine();
            polys[2] = new Forecast.ExpTrendLine();
            polys[3] = new Forecast.PowerTrendLine();
            TrendLine poly = null;

            final List<Double> polyxList = new ArrayList<>();
            final List<Double> polyyList = new ArrayList<>();
            final List<Double> noise_polyxList = new ArrayList<>();
            final List<Double> noise_polyyList = new ArrayList<>();

            final double avg1start = now - (1000 * 60 * 60 * 8); // 8 hours
            final double momentum_illustration_start = now - (1000 * 60 * 60 * 2); // 8 hours
            avg1startfuzzed = avg1start / FUZZER;
            avg1value = 0;
            avg1counter = 0;
            avg2value = 0;
            avg2counter = 0;

            double last_calibration = 0;

            if (doMgdl) {
                Profile.scale_factor = Constants.MMOLL_TO_MGDL;
            } else {
                Profile.scale_factor = 1;
            }

            // enumerate calibrations
            try {
                for (Calibration calibration : calibrations) {
                    if (calibration.slope_confidence != 0) {
                        calibrationValues.add(new PointValue((float) (calibration.timestamp / FUZZER), (float) unitized(calibration.bg)));
                        if (calibration.timestamp > last_calibration) {
                            last_calibration = calibration.timestamp;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception doing calibration values in bggraphbuilder: " + e.toString());
            }
            final boolean has_filtered = DexCollectionType.hasFiltered();
            final boolean predict_use_momentum = prefs.getBoolean("predict_use_momentum", true);
            final boolean show_moment_working_line = prefs.getBoolean("show_momentum_working_line", false);
            final boolean interpret_raw = prefs.getBoolean("interpret_raw", false);
            final boolean show_filtered = prefs.getBoolean("show_filtered_curve", false) && has_filtered;
            final boolean predict_lows = prefs.getBoolean("predict_lows", true);
            final boolean show_plugin = prefs.getBoolean("plugin_plot_on_graph", false);
            final boolean glucose_from_plugin = prefs.getBoolean("display_glucose_from_plugin", false);

            if ((Home.get_follower()) && (bgReadings.size() < 3)) {
                GcmActivity.requestBGsync();
            }

            final CalibrationAbstract plugin = (show_plugin) ? PluggableCalibration.getCalibrationPluginFromPreferences() : null;
            final CalibrationAbstract.CalibrationData cd = (plugin != null) ? plugin.getCalibrationData() : null;

            if ((glucose_from_plugin) && (cd != null)) {
                plugin_adjusted = true; // plugin will be adjusting data
            }

            for (final BgReading bgReading : bgReadings) {
                // jamorham special

                // swap main and plugin plot if display glucose is from plugin
                if ((glucose_from_plugin) && (cd != null)) {
                    pluginValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                    // recalculate from plugin - beware floating / cached references!
                    bgReading.calculated_value = plugin.getGlucoseFromBgReading(bgReading, cd);
                    bgReading.filtered_calculated_value = plugin.getGlucoseFromFilteredBgReading(bgReading, cd);
                }

                if ((show_filtered) && (bgReading.filtered_calculated_value > 0) && (bgReading.filtered_calculated_value != bgReading.calculated_value)) {
                    filteredValues.add(new PointValue((float) ((bgReading.timestamp - timeshift) / FUZZER), (float) unitized(bgReading.filtered_calculated_value)));
                }
                if ((interpret_raw && (bgReading.raw_calculated > 0))) {
                    rawInterpretedValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.raw_calculated)));
                }
                if ((!glucose_from_plugin) && (plugin != null) && (cd != null)) {
                    pluginValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(plugin.getGlucoseFromBgReading(bgReading, cd))));
                }
                if (bgReading.calculated_value >= 400) {
                    highValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(400)));
                } else if (unitized(bgReading.calculated_value) >= highMark) {
                    highValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                } else if (unitized(bgReading.calculated_value) >= lowMark) {
                    inRangeValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                } else if (bgReading.calculated_value >= 40) {
                    lowValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                } else if (bgReading.calculated_value > 13) {
                    lowValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(40)));
                }

                avg2counter++;
                avg2value += bgReading.calculated_value;
                if (bgReading.timestamp > avg1start) {
                    avg1counter++;
                    avg1value += bgReading.calculated_value;
                }

                // noise calculator
                if ((!simple || (noise_processed_till_timestamp < highest_bgreading_timestamp)) && (bgReading.timestamp > noise_trendstart) && (bgReading.timestamp > last_calibration)) {
                    if (has_filtered && (bgReading.filtered_calculated_value > 0) && (bgReading.filtered_calculated_value != bgReading.calculated_value)) {
                        final double shifted_timestamp = bgReading.timestamp - timeshift;

                        if (shifted_timestamp > last_calibration) {
                            if (shifted_timestamp < oldest_noise_timestamp)
                                oldest_noise_timestamp = shifted_timestamp;
                            noise_polyxList.add(shifted_timestamp);
                            noise_polyyList.add((bgReading.filtered_calculated_value));
                            if (d)
                                Log.d(TAG, "flt noise poly Added: " + noise_polyxList.size() + " " + JoH.qs(noise_polyxList.get(noise_polyxList.size() - 1)) + " / " + JoH.qs(noise_polyyList.get(noise_polyyList.size() - 1), 2));
                        }

                    }
                    if (bgReading.calculated_value > 0) {
                        if (bgReading.timestamp < oldest_noise_timestamp)
                            oldest_noise_timestamp = bgReading.timestamp;
                        if (bgReading.timestamp > newest_noise_timestamp) {
                            newest_noise_timestamp = bgReading.timestamp;
                            original_value = bgReading.calculated_value;
                        }
                        noise_polyxList.add((double) bgReading.timestamp);
                        noise_polyyList.add((bgReading.calculated_value));
                        if (d)
                            Log.d(TAG, "raw noise poly Added: " + noise_polyxList.size() + " " + JoH.qs(noise_polyxList.get(noise_polyxList.size() - 1)) + " / " + JoH.qs(noise_polyyList.get(noise_polyyList.size() - 1), 2));
                    }
                }

                // momentum trend
                if (!simple && (bgReading.timestamp > trendstart) && (bgReading.timestamp > last_calibration)) {
                    if (has_filtered && (bgReading.filtered_calculated_value > 0) && (bgReading.filtered_calculated_value != bgReading.calculated_value)) {
                        polyxList.add((double) bgReading.timestamp - timeshift);
                        polyyList.add(unitized(bgReading.filtered_calculated_value));
                    }
                    if (bgReading.calculated_value > 0) {
                        polyxList.add((double) bgReading.timestamp);
                        polyyList.add(unitized(bgReading.calculated_value));
                    }
                    if (d)
                        Log.d(TAG, "poly Added: " + JoH.qs(polyxList.get(polyxList.size() - 1)) + " / " + JoH.qs(polyyList.get(polyyList.size() - 1), 2));
                }

            }
            if (avg1counter > 0) {
                avg1value = avg1value / avg1counter;
            }

            if (avg2counter > 0) {
                avg2value = avg2value / avg2counter;
            }


            // always calculate noise if needed
            if (noise_processed_till_timestamp < highest_bgreading_timestamp) {
                // noise evaluate
                Log.d(TAG, "Noise: Processing new data for noise: " + JoH.dateTimeText(noise_processed_till_timestamp) + " vs now: " + JoH.dateTimeText(highest_bgreading_timestamp));

                try {
                    if (d) Log.d(TAG, "noise Poly list size: " + noise_polyxList.size());
                    // TODO Impossible to satisfy noise evaluation size with only raw data do we want it with raw only??
                    if (noise_polyxList.size() > 5) {
                        noisePoly = new PolyTrendLine(2);
                        final double[] noise_polyys = PolyTrendLine.toPrimitiveFromList(noise_polyyList);
                        final double[] noise_polyxs = PolyTrendLine.toPrimitiveFromList(noise_polyxList);
                        noisePoly.setValues(noise_polyys, noise_polyxs);
                        last_noise = noisePoly.errorVarience();
                        if (newest_noise_timestamp > oldest_noise_timestamp) {
                            best_bg_estimate = noisePoly.predict(newest_noise_timestamp);
                            last_bg_estimate = noisePoly.predict(newest_noise_timestamp - DEXCOM_PERIOD);
                        } else {
                            best_bg_estimate = -99;
                            last_bg_estimate = -99;
                        }
                        Log.i(TAG, "Noise: Poly Error Varience: " + JoH.qs(last_noise, 5));
                    } else {
                        Log.i(TAG, "Noise: Not enough data to get sensible noise value");
                        noisePoly = null;
                        last_noise = -9999;
                        best_bg_estimate = -9999;
                        last_bg_estimate = -9999;
                    }
                    noise_processed_till_timestamp = highest_bgreading_timestamp; // store that we have processed up to this timestamp
                } catch (Exception e) {
                    Log.e(TAG, " Error with noise poly trend: " + e.toString());
                }
            } else {
                Log.d(TAG, "Noise Cached noise timestamp: " + JoH.dateTimeText(noise_processed_till_timestamp));
            }

            if (!simple) {
                // momentum
                try {
                    if (d) Log.d(TAG, "moment Poly list size: " + polyxList.size());
                    if (polyxList.size() > 1) {
                        final double[] polyys = PolyTrendLine.toPrimitiveFromList(polyyList);
                        final double[] polyxs = PolyTrendLine.toPrimitiveFromList(polyxList);

                        // set and evaluate poly curve models and select first best
                        double min_errors = 9999999;
                        for (TrendLine this_poly : polys) {
                            if (this_poly != null) {
                                if (poly == null) poly = this_poly;
                                this_poly.setValues(polyys, polyxs);
                                if (this_poly.errorVarience() < min_errors) {
                                    min_errors = this_poly.errorVarience();
                                    poly = this_poly;
                                    //if (d) Log.d(TAG, "set forecast best model to: " + poly.getClass().getSimpleName() + " with varience of: " + JoH.qs(poly.errorVarience(),14));
                                }

                            }
                        }
                        if (d)
                            Log.i(TAG, "set forecast best model to: " + poly.getClass().getSimpleName() + " with varience of: " + JoH.qs(poly.errorVarience(), 4));
                    } else {
                        if (d) Log.i(TAG, "Not enough data for forecast model");
                    }

                } catch (Exception e) {
                    Log.e(TAG, " Error with poly trend: " + e.toString());
                }

                try {
                    // show trend for whole bg reading area
                    if ((show_moment_working_line) && (poly != null)) {
                        for (BgReading bgReading : bgReadings) {
                            // only show working curve for last x hours to a
                            if (bgReading.timestamp > momentum_illustration_start) {
                                double polyPredicty = poly.predict(bgReading.timestamp);
                                //if (d) Log.d(TAG, "Poly predict: "+JoH.qs(polyPredict)+" @ "+JoH.qs(iob.timestamp));
                                if ((polyPredicty < highMark) && (polyPredicty > 0)) {
                                    PointValue zv = new PointValue((float) (bgReading.timestamp / FUZZER), (float) polyPredicty);
                                    polyBgValues.add(zv);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating back trend: " + e.toString());
                }

                // low estimator
                // work backwards to see whether we think a low is estimated
                low_occurs_at = -1;
                try {
                    if ((predict_lows) && (prediction_enabled) && (poly != null)) {
                        final double offset = ActivityRecognizedService.raise_limit_due_to_vehicle_mode() ? unitized(ActivityRecognizedService.getVehicle_mode_adjust_mgdl()) : 0;
                        final double plow_now = JoH.ts();
                        double plow_timestamp = plow_now + (1000 * 60 * 99); // max look-ahead
                        double polyPredicty = poly.predict(plow_timestamp);
                        Log.d(TAG, "Low predictor at max lookahead is: " + JoH.qs(polyPredicty));
                        low_occurs_at_processed_till_timestamp = highest_bgreading_timestamp; // store that we have processed up to this timestamp
                        if (polyPredicty <= (lowMark + offset)) {
                            low_occurs_at = plow_timestamp;
                            final double lowMarkIndicator = (lowMark - (lowMark / 4));
                            //if (d) Log.d(TAG, "Poly predict: "+JoH.qs(polyPredict)+" @ "+JoH.qsz(iob.timestamp));
                            while (plow_timestamp > plow_now) {
                                plow_timestamp = plow_timestamp - FUZZER;
                                polyPredicty = poly.predict(plow_timestamp);
                                if (polyPredicty > (lowMark + offset)) {
                                    PointValue zv = new PointValue((float) (plow_timestamp / FUZZER), (float) polyPredicty);
                                    polyBgValues.add(zv);
                                } else {
                                    low_occurs_at = plow_timestamp;
                                    if (polyPredicty > lowMarkIndicator) {
                                        polyBgValues.add(new PointValue((float) (plow_timestamp / FUZZER), (float) polyPredicty));
                                    }
                                }
                            }
                            Log.i(TAG, "LOW PREDICTED AT: " + JoH.dateTimeText((long) low_occurs_at));
                            predictivehours = Math.max(predictivehours, (int) ((low_occurs_at - plow_now) / (60 * 60 * 1000)) + 1);
                        }
                    }

                } catch (NullPointerException e) {
                    //Log.d(TAG,"Error with low prediction trend: "+e.toString());
                }

                final boolean show_noise_working_line;
                if ((last_noise > NOISE_TRIGGER) && prefs.getBoolean("bg_compensate_noise", false)) {
                    show_noise_working_line = true;
                } else {
                    show_noise_working_line = prefs.getBoolean("show_noise_workings", false);
                }
                // noise debug
                try {
                    // overlay noise curve
                    if ((show_noise_working_line) && (prediction_enabled) && (noisePoly != null)) {
                        for (BgReading bgReading : bgReadings) {
                            // only show working curve for last x hours to a
                            if ((bgReading.timestamp > oldest_noise_timestamp) && (bgReading.timestamp > last_calibration)) {
                                double polyPredicty = unitized(noisePoly.predict(bgReading.timestamp));
                                if (d)
                                    Log.d(TAG, "noise Poly predict: " + JoH.qs(polyPredicty) + " @ " + JoH.qs(bgReading.timestamp));
                                if ((polyPredicty < highMark) && (polyPredicty > 0)) {
                                    PointValue zv = new PointValue((float) (bgReading.timestamp / FUZZER), (float) polyPredicty);
                                    noisePolyBgValues.add(zv);
                                }
                            }
                        }
                    }


                } catch (Exception e) {
                    Log.e(TAG, "Error creating noise working trend: " + e.toString());
                }

                //Log.i(TAG,"Average1 value: "+unitized(avg1value));
                //Log.i(TAG,"Average2 value: "+unitized(avg2value));


                try {
                    // display treatment blobs and annotations
                    for (Treatments treatment : treatments) {
                        double height = 6 * bgScale;
                        if (treatment.insulin > 0)
                            height = treatment.insulin; // some scaling needed I think
                        if (height > highMark) height = highMark;
                        if (height < lowMark) height = lowMark;

                        PointValueExtended pv = new PointValueExtended((float) (treatment.timestamp / FUZZER), (float) height);
                        String mylabel = "";
                        if (treatment.insulin > 0) {
                            if (mylabel.length() > 0)
                                mylabel = mylabel + System.getProperty("line.separator");
                            mylabel = mylabel + (Double.toString(treatment.insulin) + "u").replace(".0u", "u");
                        }
                        if (treatment.carbs > 0) {
                            if (mylabel.length() > 0)
                                mylabel = mylabel + System.getProperty("line.separator");
                            mylabel = mylabel + (Double.toString(treatment.carbs) + "g").replace(".0g", "g");
                        }
                        pv.setLabel(mylabel); // standard label
                        if ((treatment.notes != null) && (treatment.notes.length() > 0)) {
                            pv.note = treatment.notes;
                            try {
                                final Pattern p = Pattern.compile(".*?pos:([0-9.]+).*");
                                final Matcher m = p.matcher(treatment.enteredBy);
                                if (m.matches()) {
                                    pv.set(pv.getX(), (float) JoH.tolerantParseDouble(m.group(1)));
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "Exception matching position: " + e);
                            }
                        } else {
                            pv.note = "Treatment";
                        }
                        if (treatmentValues.size() > 0) { // not sure if this >1 is right really - needs a review
                            PointValue lastpv = treatmentValues.get(treatmentValues.size() - 1);
                            if (Math.abs(lastpv.getX() - pv.getX()) < ((10 * 60 * 1000) / FUZZER)) {
                                // merge label with previous - Intelligent parsing and additions go here
                                if (d)
                                    Log.d(TAG, "Merge treatment difference: " + Float.toString(lastpv.getX() - pv.getX()));
                                String lastlabel = String.valueOf(lastpv.getLabelAsChars());
                                if (lastlabel.length() > 0) {
                                    lastpv.setLabel(lastlabel + "+" + mylabel);
                                    pv.setLabel("");
                                }
                            }
                        }
                        treatmentValues.add(pv); // hover
                        if (d)
                            Log.d(TAG, "Treatment total record: " + Double.toString(height) + " " + " timestamp: " + Long.toString(treatment.timestamp));
                    }

                } catch (Exception e) {

                    Log.e(TAG, "Exception doing treatment values in bggraphbuilder: " + e.toString());
                }
                try {


                    // we need to check we actually have sufficient data for this
                    double predictedbg = -1000;
                    BgReading mylastbg = bgReadings.get(0);
                    double lasttimestamp = 0;

                    // this can be optimised to oncreate and onchange
                    Profile.reloadPreferencesIfNeeded(prefs); // TODO handle this better now we use profile time blocks


                    try {
                        if (mylastbg != null) {
                            if (doMgdl) {
                                predictedbg = mylastbg.calculated_value;
                            } else {
                                predictedbg = mylastbg.calculated_value_mmol();
                            }
                            //if (d) Log.d(TAG, "Starting prediction with bg of: " + JoH.qs(predictedbg));
                            lasttimestamp = mylastbg.timestamp / FUZZER;

                            if (d)
                                Log.d(TAG, "Starting prediction with bg of: " + JoH.qs(predictedbg) + " secs ago: " + (JoH.ts() - mylastbg.timestamp) / 1000);
                        } else {
                            Log.i(TAG, "COULD NOT GET LAST BG READING FOR PREDICTION!!!");
                        }
                    } catch (Exception e) {
                        // could not get a bg reading
                    }

                    final double iobscale = 1 * bgScale;
                    final double cobscale = 0.2 * bgScale;
                    final double initial_predicted_bg = predictedbg;
                    final double relaxed_predicted_bg_limit = initial_predicted_bg * 1.20;
                    final double cob_insulin_max_draw_value = highMark * 1.20;
                    // final List<Iob> iobinfo_old = Treatments.ioBForGraph(numValues, (start_time * FUZZER));
                    final List<Iob> iobinfo = (simulation_enabled) ? Treatments.ioBForGraph_new(NUM_VALUES, (start_time * FUZZER)) : null; // for test

                    long fuzzed_timestamp = (long) end_time; // initial value in case there are no iob records
                    if (d)
                        Log.d(TAG, "Internal date timestamp: " + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", new java.util.Date()));


                    if (d)
                        Log.d(TAG, "initial Fuzzed end timestamp: " + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", fuzzed_timestamp * FUZZER));
                    if (d)
                        Log.d(TAG, "initial Fuzzed start timestamp: " + android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", (long) start_time * FUZZER));
                    if ((iobinfo != null) && (prediction_enabled) && (simulation_enabled)) {

                        double predict_weight = 0.1;

                        for (Iob iob : iobinfo) {

                            double activity = iob.activity;
                            if ((iob.iob > 0) || (iob.cob > 0) || (iob.jActivity > 0) || (iob.jCarbImpact > 0)) {
                                fuzzed_timestamp = iob.timestamp / FUZZER;
                                if (d) Log.d(TAG, "iob timestamp: " + iob.timestamp);
                                if (iob.iob > Profile.minimum_shown_iob) {
                                    double height = iob.iob * iobscale;
                                    if (height > cob_insulin_max_draw_value)
                                        height = cob_insulin_max_draw_value;
                                    PointValue pv = new PointValue((float) fuzzed_timestamp, (float) height);
                                    iobValues.add(pv);
                                    double activityheight = iob.jActivity * 3; // currently scaled by profile
                                    if (activityheight > cob_insulin_max_draw_value)
                                        activityheight = cob_insulin_max_draw_value;
                                    PointValue av = new PointValue((float) fuzzed_timestamp, (float) activityheight);
                                    activityValues.add(av);
                                }

                                if (iob.cob > 0) {
                                    double height = iob.cob * cobscale;
                                    if (height > cob_insulin_max_draw_value)
                                        height = cob_insulin_max_draw_value;
                                    PointValue pv = new PointValue((float) fuzzed_timestamp, (float) height);
                                    if (d)
                                        Log.d(TAG, "Cob total record: " + JoH.qs(height) + " " + JoH.qs(iob.cob) + " " + Float.toString(pv.getY()) + " @ timestamp: " + Long.toString(iob.timestamp));
                                    cobValues.add(pv); // warning should not be hardcoded
                                }

                                // momentum curve
                                // do we actually need to calculate this within the loop - can we use only the last datum?
                                if (fuzzed_timestamp > (lasttimestamp)) {
                                    double polyPredict = 0;
                                    if (poly != null) {
                                        try {
                                            polyPredict = poly.predict(iob.timestamp);
                                            if (d)
                                                Log.d(TAG, "Poly predict: " + JoH.qs(polyPredict) + " @ " + JoH.dateTimeText(iob.timestamp));
                                            if (show_moment_working_line) {
                                                if (((polyPredict < highMark) || (polyPredict < initial_predicted_bg)) && (polyPredict > 0)) {
                                                    PointValue zv = new PointValue((float) fuzzed_timestamp, (float) polyPredict);
                                                    polyBgValues.add(zv);
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Got exception with poly predict: " + e.toString());
                                        }
                                    }
                                    if (d)
                                        Log.d(TAG, "Processing prediction: before: " + JoH.qs(predictedbg) + " activity: " + JoH.qs(iob.jActivity) + " jcarbimpact: " + JoH.qs(iob.jCarbImpact));
                                    predictedbg -= iob.jActivity; // lower bg by current insulin activity
                                    predictedbg += iob.jCarbImpact;

                                    double predictedbg_final = predictedbg;
                                    // add momentum characteristics if we have them
                                    final boolean momentum_smoothing = true;
                                    if ((predict_use_momentum) && (polyPredict > 0)) {
                                        predictedbg_final = ((predictedbg * predict_weight) + polyPredict) / (predict_weight + 1);
                                        if (momentum_smoothing) predictedbg = predictedbg_final;

                                        if (d)
                                            Log.d(TAG, "forecast predict_weight: " + JoH.qs(predict_weight));
                                    }
                                    predict_weight = predict_weight * 2.5; // from 0-infinity - // TODO account for step!!!
                                    // we should pull in actual graph upper and lower limits here
                                    if (((predictedbg_final < cob_insulin_max_draw_value) || (predictedbg_final < relaxed_predicted_bg_limit)) && (predictedbg_final > 0)) {
                                        PointValue zv = new PointValue((float) fuzzed_timestamp, (float) predictedbg_final);
                                        predictedBgValues.add(zv);
                                    }
                                }
                                if (fuzzed_timestamp > end_time) {
                                    predictivehours = (int) (((fuzzed_timestamp - end_time) * FUZZER) / (1000 * 60 * 60)) + 1; // round up to nearest future hour - timestamps in minutes here
                                    if (d)
                                        Log.d(TAG, "Predictive hours updated to: " + predictivehours);
                                } else {
                                    if ((fuzzed_timestamp == end_time - 4) && (iob.iob > 0)) {
                                        // show current iob
                                        double position = 12.4 * bgScale; // this is for mmol - needs generic for mg/dl
                                        if (Math.abs(predictedbg - position) < (2 * bgScale)) {
                                            position = 7.0 * bgScale;
                                        }

                                        PointValue iv = new PointValue((float) fuzzed_timestamp, (float) position);
                                        DecimalFormat df = new DecimalFormat("#");
                                        df.setMaximumFractionDigits(2);
                                        df.setMinimumIntegerDigits(1);
                                        iv.setLabel("IoB: " + df.format(iob.iob));
                                        annotationValues.add(iv); // needs to be different value list so we can make annotation nicer
                                    }
                                }

                            }
                        }
                        if (d)
                            Log.i(TAG, "Size of iob: " + Integer.toString(iobinfo.size()) + " Predictive hours: " + Integer.toString(predictivehours)
                                    + " Predicted end game change: " + JoH.qs(predictedbg - mylastbg.calculated_value_mmol())
                                    + " Start bg: " + JoH.qs(mylastbg.calculated_value_mmol()) + " Predicted: " + JoH.qs(predictedbg));
                        // calculate bolus or carb adjustment - these should have granularity for injection / pump and thresholds
                    } else {
                        if (d) Log.i(TAG, "iobinfo was null");
                    }

                    double[] evaluation;
                    if (prediction_enabled && simulation_enabled) {
                        if (doMgdl) {
                            // These routines need to understand how the profile is defined to use native instead of scaled
                            evaluation = Profile.evaluateEndGameMmol(predictedbg, lasttimestamp * FUZZER, end_time * FUZZER);
                        } else {
                            evaluation = Profile.evaluateEndGameMmol(predictedbg, lasttimestamp * FUZZER, end_time * FUZZER);

                        }

                        if (d)
                            Log.i(TAG, "Predictive BWP: Current prediction: " + JoH.qs(predictedbg) + " / carbs: " + JoH.qs(evaluation[0]) + " insulin: " + JoH.qs(evaluation[1]));
                        if ((low_occurs_at < 1) && (Home.getPreferencesBooleanDefaultFalse("show_bwp"))) {
                            if (evaluation[0] > Profile.minimum_carb_recommendation) {
                                PointValue iv = new PointValue((float) fuzzed_timestamp, (float) (10 * bgScale));
                                iv.setLabel("+Carbs: " + JoH.qs(evaluation[0], 0));
                                annotationValues.add(iv); // needs to be different value list so we can make annotation nicer
                            }
                            if (evaluation[1] > Profile.minimum_insulin_recommendation) {
                                PointValue iv = new PointValue((float) fuzzed_timestamp, (float) (11 * bgScale));
                                iv.setLabel("+Insulin: " + JoH.qs(evaluation[1], 1));
                                annotationValues.add(iv); // needs to be different value list so we can make annotation nicer
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Exception doing iob values in bggraphbuilder: " + e.toString());
                }
            } // if !simple
        } finally {
            readings_lock.unlock();
        }
    }

    public static synchronized double getCurrentLowOccursAt() {
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

    public static synchronized void refreshNoiseIfOlderThan(long timestamp) {
        if (noise_processed_till_timestamp < timestamp) {
            Log.d(TAG, "Refreshing Noise as Older: " + JoH.dateTimeText((long) noise_processed_till_timestamp) + " vs " + JoH.dateTimeText(timestamp));
            // new only the last hour worth of data for this, simple mode should work for this calculation
            (new BgGraphBuilder(xdrip.getAppContext(), System.currentTimeMillis() - 60 * 60 * 1000, System.currentTimeMillis() + 5 * 60 * 1000, 24, true)).addBgReadingValues(true);
        }
    }

    public Line avg1Line() {
        List<PointValue> myLineValues = new ArrayList<PointValue>();
        myLineValues.add(new PointValue((float) avg1startfuzzed, (float) unitized(avg1value)));
        myLineValues.add(new PointValue((float) end_time, (float) unitized(avg1value)));
        Line myLine = new Line(myLineValues);
        myLine.setHasPoints(false);
        myLine.setStrokeWidth(1);
        myLine.setColor(getCol(X.color_average1_line));
        myLine.setPathEffect(new DashPathEffect(new float[]{10.0f, 10.0f}, 0));
        myLine.setAreaTransparency(50);
        return myLine;
    }
    public Line avg2Line() {
        List<PointValue> myLineValues = new ArrayList<PointValue>();
        myLineValues.add(new PointValue((float) start_time, (float) unitized(avg2value)));
        myLineValues.add(new PointValue((float) end_time, (float) unitized(avg2value)));
        Line myLine = new Line(myLineValues);
        myLine.setHasPoints(false);
        myLine.setStrokeWidth(1);
        myLine.setColor(getCol(X.color_average2_line));
        myLine.setPathEffect(new DashPathEffect(new float[]{30.0f, 10.0f}, 0));
        myLine.setAreaTransparency(50);
        return myLine;
    }

    public Line idealLine() {
        // if profile has more than 1 target bg value then we need to iterate those and plot them for completeness
        List<PointValue> myLineValues = new ArrayList<PointValue>();
        myLineValues.add(new PointValue((float) start_time, (float)  Profile.getTargetRangeInUnits(start_time)));
        myLineValues.add(new PointValue((float) predictive_end_time, (float) Profile.getTargetRangeInUnits(predictive_end_time)));
        Line myLine = new Line(myLineValues);
        myLine.setHasPoints(false);
        myLine.setStrokeWidth(1);
        myLine.setColor(getCol(X.color_target_line));
        myLine.setPathEffect(new DashPathEffect(new float[]{5f, 5f}, 0));
        myLine.setAreaTransparency(50);
        return myLine;
    }

    public Line highLine() {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue((float) start_time, (float) highMark));
        highLineValues.add(new PointValue((float) end_time, (float) highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(getCol(X.color_high_values));
        return highLine;
    }

    public Line predictiveHighLine() {
        List<PointValue> predictiveHighLineValues = new ArrayList<PointValue>();
        predictiveHighLineValues.add(new PointValue((float) end_time, (float) highMark));
        predictiveHighLineValues.add(new PointValue((float) predictive_end_time, (float) highMark));
        Line highLine = new Line(predictiveHighLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(ChartUtils.darkenColor(ChartUtils.darkenColor(ChartUtils.darkenColor(getCol(X.color_high_values)))));
        return highLine;
    }

    public Line lowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float) start_time, (float) lowMark));
        lowLineValues.add(new PointValue((float) end_time, (float) lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setAreaTransparency(50);
        lowLine.setColor(getCol(X.color_low_values));
        lowLine.setStrokeWidth(1);
        lowLine.setFilled(true);
        return lowLine;
    }

    public Line predictiveLowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue((float) end_time, (float) lowMark));
        lowLineValues.add(new PointValue((float) predictive_end_time, (float) lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setAreaTransparency(40);
        lowLine.setColor(ChartUtils.darkenColor(ChartUtils.darkenColor(ChartUtils.darkenColor(getCol(X.color_low_values)))));
        lowLine.setStrokeWidth(1);
        lowLine.setFilled(true);
        return lowLine;
    }

    public Line maxShowLine() {
        List<PointValue> maxShowValues = new ArrayList<PointValue>();
        maxShowValues.add(new PointValue((float) start_time, (float) defaultMaxY));
        maxShowValues.add(new PointValue((float) end_time, (float) defaultMaxY));
        Line maxShowLine = new Line(maxShowValues);
        maxShowLine.setHasLines(false);
        maxShowLine.setHasPoints(false);
        return maxShowLine;
    }

    public Line minShowLine() {
        List<PointValue> minShowValues = new ArrayList<PointValue>();
        minShowValues.add(new PointValue((float) start_time, (float) defaultMinY));
        minShowValues.add(new PointValue((float) end_time, (float) defaultMinY));
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

        for (int j = 1; j <= 12; j += 1) {
            if (doMgdl) {
                axisValues.add(new AxisValue(j * 50));
            } else {
                axisValues.add(new AxisValue(j * 2));
            }
        }
        yAxis.setValues(axisValues);
       // yAxis.setHasLines(true);
        yAxis.setMaxLabelChars(5);
        yAxis.setInside(true);
        yAxis.setTextSize(axisTextSize);
        yAxis.setHasLines(prefs.getBoolean("show_graph_grid_glucose",true));
        return yAxis;
    }

    @NonNull
    public Axis xAxis() {
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        //GregorianCalendar now = new GregorianCalendar();
        //GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
       // double start_hour_block = today.getTime().getTime();
        //double timeNow = new Date().getTime();
        //for (int l = 0; l <= 24; l++) {
        //    if ((start_hour_block + (60000 * 60 * (l))) < timeNow) {
        //        if ((start_hour_block + (60000 * 60 * (l + 1))) >= timeNow) {
        //            endHour = start_hour_block + (60000 * 60 * (l));
        //            l = 25;
       //         }
        //    }
       // }


        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis((long)(start_time * FUZZER));
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis()<(start_time * FUZZER)){
            calendar.add(Calendar.HOUR, 1);
        }
        while (calendar.getTimeInMillis()< ( (end_time * FUZZER) + (predictivehours * 60 * 60 * 1000))) {
            xAxisValues.add(new AxisValue((calendar.getTimeInMillis() / FUZZER), (timeFormat.format(calendar.getTimeInMillis())).toCharArray()));
            calendar.add(Calendar.HOUR, 1);
        }

        //for (int l = 0; l <= (24 + predictivehours); l++) {
        //    double timestamp = (endHour + ((predictivehours) * 60 * 1000 * 60) - (60000 * 60 * l));
        //    xAxisValues.add(new AxisValue((long) (timestamp / FUZZER), (timeFormat.format(timestamp)).toCharArray()));
       // }
        xAxis.setValues(xAxisValues);
        return xAxis;
    }

    public Axis chartXAxis() {
        Axis xAxis = xAxis();
        xAxis.setHasLines(prefs.getBoolean("show_graph_grid_time", true));
        xAxis.setTextSize(axisTextSize);
        return xAxis;
    }

    public Axis previewXAxis(){
        Axis previewXaxis = xAxis();
        previewXaxis.setTextSize(previewAxisTextSize);
        previewXaxis.setHasLines(true);
        return previewXaxis;
    }

    private SimpleDateFormat hourFormat() {
        return new SimpleDateFormat(DateFormat.is24HourFormat(context) ? "HH" : "h a");
    }

  /*  public Axis previewXAxis() {
        List<AxisValue> previewXaxisValues = new ArrayList<AxisValue>();
        final java.text.DateFormat timeFormat = hourFormat();
        timeFormat.setTimeZone(TimeZone.getDefault());
        for (int l = 0; l <= (24 + predictivehours); l += hoursPreviewStep) {
            double timestamp = (endHour + (predictivehours * 60 * 1000 * 60) - (60000 * 60 * l));
            previewXaxisValues.add(new AxisValue((long) (timestamp / FUZZER), (timeFormat.format(timestamp)).toCharArray()));
        }
        Axis previewXaxis = new Axis();
        previewXaxis.setValues(previewXaxisValues);
        previewXaxis.setHasLines(true);
        previewXaxis.setTextSize(previewAxisTextSize);
        return previewXaxis;
    }
*/
    /////////VIEWPORT RELATED//////////////
    public Viewport advanceViewport(Chart chart, Chart previewChart) {
        viewport = new Viewport(previewChart.getMaximumViewport());
        viewport.inset((float) ((86400000 / 2.5) / FUZZER), 0);
        double distance_to_move = ((new Date().getTime()) / FUZZER) - viewport.left - (((viewport.right - viewport.left) / 2));
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

    public OnValueSelectTooltipListener getOnValueSelectTooltipListener(Activity callerActivity) {
        return new OnValueSelectTooltipListener(callerActivity);
    }

    public class OnValueSelectTooltipListener implements LineChartOnValueSelectListener {

        private Toast tooltip;
        private Activity callerActivity;

        public OnValueSelectTooltipListener(Activity callerActivity) {
            this.callerActivity = callerActivity;
        }

        @Override
        public synchronized void onValueSelected(int i, int i1, PointValue pointValue) {

            String filtered = "";
            String alternate = "";
            try {
                PointValueExtended pve = (PointValueExtended) pointValue;
                if(pve.calculatedFilteredValue != -1) {
                    filtered = " (" + Math.round(pve.calculatedFilteredValue*10) / 10d +")";
                }
                if (pve.note!=null)
                {
                    alternate=pve.note;
                }
            } catch (ClassCastException e) {
               // Log.e(TAG, "Error casting a point from pointValue to PointValueExtended", e);
            }

            final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
            //Won't give the exact time of the reading but the time on the grid: close enough.
            final Long time = ((long) pointValue.getX()) * FUZZER;
            final double ypos = pointValue.getY();

            final String message;
            if (alternate.length()>0) {
                message = timeFormat.format(time) + "    "+alternate;
            } else {
                message = timeFormat.format(time) + "      " + (Math.round(pointValue.getY() * 10) / 10d) + " "+unit() +  filtered;
            }

            final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Home.startHomeWithExtra(xdrip.getAppContext(), Home.CREATE_TREATMENT_NOTE, time.toString(), Double.toString(ypos));
                }
            };
            Home.snackBar(message, mOnClickListener, callerActivity);

        }

        @Override
        public void onValueDeselected() {
            // do nothing
        }
    }
}
