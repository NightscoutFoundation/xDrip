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
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.AddCalibration;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.APStatus;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.Forecast;
import com.eveningoutpost.dexdrip.Models.Forecast.PolyTrendLine;
import com.eveningoutpost.dexdrip.Models.Forecast.TrendLine;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.Iob;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Libre2RawValue;
import com.eveningoutpost.dexdrip.Models.Prediction;
import com.eveningoutpost.dexdrip.Models.Profile;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.insulin.opennov.Options;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.store.KeyStore;
import com.eveningoutpost.dexdrip.ui.classifier.NoteClassifier;
import com.eveningoutpost.dexdrip.ui.dialog.DoseAdjustDialog;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapLoader;
import com.eveningoutpost.dexdrip.ui.helpers.ColorUtil;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LibreTrendGraph;
import com.eveningoutpost.dexdrip.utils.math.RollingAverage;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.location.DetectedActivity;
import com.rits.cloning.Cloner;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
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
import lombok.val;

import static com.eveningoutpost.dexdrip.Models.JoH.tolerantParseDouble;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.X;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;

public class BgGraphBuilder {
    public static final int FUZZER = (Pref.getBoolean("lower_fuzzer", false)) ? 500 * 15 * 5 : 1000 * 30 * 5; // 37.5 seconds : 2.5 minutes
    public final static long DEXCOM_PERIOD = 300_000; // 5 minutes
    public final static double NOISE_TRIGGER = 10;
    public final static double NOISE_TRIGGER_ULTRASENSITIVE = 1;
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
    public long end_time = (new Date().getTime() + (60000 * 10)) / FUZZER;
    public long predictive_end_time;
    public long start_time = end_time - ((60000 * 60 * 24)) / FUZZER;


    private final static double timeshift = 500_000;
    private static final int NUM_VALUES = (60 / 5) * 24;

    // flag to indicate if readings data has been adjusted
    private static boolean plugin_adjusted = false;
    // used to prevent concurrency problems with calibration plugins
    private static final ReentrantLock readings_lock = new ReentrantLock();

    private final List<Treatments> treatments;
    private final static boolean d = false; // debug flag, could be read from preferences

    private Context context;
    private SharedPreferences prefs;
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
    private List<Libre2RawValue> Libre2RawValues;
    private final List<Calibration> calibrations;
    private final List<BloodTest> bloodtests;
    private final List<PointValue> inRangeValues = new ArrayList<>();
    private final List<PointValue> backfillValues = new ArrayList<>();
    private final List<PointValue> remoteValues = new ArrayList<>();
    private final List<PointValue> highValues = new ArrayList<>();
    private final List<PointValue> lowValues = new ArrayList<>();
    private final List<PointValue> badValues = new ArrayList<>();
    private final List<PointValue> pluginValues = new ArrayList<PointValue>();
    private final List<PointValue> rawInterpretedValues = new ArrayList<PointValue>();
    private final List<PointValue> filteredValues = new ArrayList<PointValue>();
    private final List<PointValue> bloodTestValues = new ArrayList<PointValue>();
    private final List<PointValue> calibrationValues = new ArrayList<PointValue>();
    private final List<PointValue> treatmentValues = new ArrayList<PointValue>();
    private final List<PointValue> smbValues = new ArrayList<>();
    private final List<PointValue> iconValues = new ArrayList<>();
    private final List<PointValue> iobValues = new ArrayList<PointValue>();
    private final List<PointValue> cobValues = new ArrayList<PointValue>();
    private final List<PointValue> predictedBgValues = new ArrayList<PointValue>();
    private final List<PointValue> polyBgValues = new ArrayList<PointValue>();
    private final List<PointValue> noisePolyBgValues = new ArrayList<PointValue>();
    private final List<PointValue> activityValues = new ArrayList<PointValue>();
    private final List<PointValue> annotationValues = new ArrayList<>();
    private final Pattern posPattern = Pattern.compile(".*?pos:([0-9.]+).*");
    private final boolean hidePriming = Options.hidePrimingDoses();
    private static TrendLine noisePoly;
    public static double last_noise = -99999;
    public static double original_value = -99999;
    public static double best_bg_estimate = -99999;
    public static double last_bg_estimate = -99999;
    private KeyStore keyStore = FastStore.getInstance();

    private final boolean showSMB = Pref.getBoolean("show_smb_icons", true);

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
        this(context, start, end, numValues, show_prediction, false);
    }

    public BgGraphBuilder(Context context, long start, long end, int numValues, boolean show_prediction, final boolean useArchive) {
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
            loaded_numValues = numValues;
            loaded_start = start;
            loaded_end = end;
            bgReadings = BgReading.latestForGraph(numValues, start, end);
            if (DexCollectionType.getDexCollectionType() == DexCollectionType.LibreReceiver)
                Libre2RawValues = Libre2RawValue.latestForGraph(numValues * 5, start, end);
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
        bloodtests = BloodTest.latestForGraph(numValues, start, end);
        // get extra calibrations so we can use them for historical readings
        calibrations = Calibration.latestForGraph(numValues, start - (3 * Constants.DAY_IN_MS), end);
        treatments = Treatments.latestForGraph(numValues, start, end + (120 * 60 * 1000));
        this.context = context;
        this.highMark = tolerantParseDouble(prefs.getString("highValue", "170"), 170);
        this.lowMark = tolerantParseDouble(prefs.getString("lowValue", "70"), 70);
        this.doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        defaultMinY = unitized(40);
        defaultMaxY = unitized(250);
        pointSize = isXLargeTablet(context) ? 5 : 3;
        axisTextSize = isXLargeTablet(context) ? 20 : Axis.DEFAULT_TEXT_SIZE_SP;
        previewAxisTextSize = isXLargeTablet(context) ? 12 : 5;
        hoursPreviewStep = isXLargeTablet(context) ? 2 : 1;
    }


    private double bgScale() {
        if (doMgdl)
            return Constants.MMOLL_TO_MGDL;
        else
            return 1;
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
        if (thisnoise > NOISE_TRIGGER_ULTRASENSITIVE && Pref.getBooleanDefaultFalse("engineering_mode") && Pref.getBooleanDefaultFalse("bg_compensate_noise_ultrasensitive"))
            return "Some";
        return "Low";
    }

    private void extend_line(List<PointValue> points, float x, float y) {
        if (points.size() > 1) {
            points.remove(1); // replace last
        }
        points.add(new PointValue(x, y));
        Log.d(TAG, "Extend line size: " + points.size());
    }

    private List<Line> predictiveLines() {
        final List<Line> lines = new LinkedList<>();

        final boolean g_prediction = Pref.getBooleanDefaultFalse("show_g_prediction");
        final boolean medtrum = Pref.getBooleanDefaultFalse("show_medtrum_secondary");
        if (medtrum || g_prediction) {
            final List<Prediction> plist = Prediction.latestForGraph(4000, loaded_start, loaded_end);
            if (plist.size() > 0) {
                final List<PointValue> gpoints = new ArrayList<>(plist.size());
                final float yscale = !doMgdl ? (float) Constants.MGDL_TO_MMOLL : 1f;
                for (Prediction p : plist) {
                    switch (p.source) {
                        case "EGlucoseRx":
                            final PointValue point = new PointValue(((float) (p.timestamp + (Constants.MINUTE_IN_MS * 10)) / FUZZER), (float) (p.glucose * yscale));
                            gpoints.add(point);
                            break;
                        case "Medtrum2nd":
                            final PointValue mpoint = new PointValue(((float) p.timestamp / FUZZER), (float) (p.glucose * yscale));
                            gpoints.add(mpoint);
                            break;
                    }
                }

                if (gpoints.size() > 0) {
                    lines.add(new Line(gpoints)
                            .setHasLabels(false)
                            .setHasPoints(true)
                            .setHasLines(false)
                            .setPointRadius(1)
                            .setColor(ChartUtils.darkenColor(ChartUtils.darkenColor(getCol(X.color_predictive)))));
                }
            }
        }

        return lines;
    }


    private List<Line> basalLines() {
        final List<Line> basalLines = new ArrayList<>();
        if (prefs.getBoolean("show_basal_line", false)) {

            final float yscale = doMgdl ? (float) Constants.MMOLL_TO_MGDL : 1f;

            final List<APStatus> aplist = APStatus.latestForGraph(2000, loaded_start, loaded_end);

            if (aplist.size() > 0) {

                // divider line

                final Line dividerLine = new Line();
                dividerLine.setTag("tbr"); // not quite true
                dividerLine.setHasPoints(false);
                dividerLine.setHasLines(true);
                dividerLine.setStrokeWidth(1);
                dividerLine.setColor(getCol(X.color_basal_tbr));
                dividerLine.setPathEffect(new DashPathEffect(new float[]{10.0f, 10.0f}, 0));
                dividerLine.setReverseYAxis(true);
                dividerLine.setHasPoints(false);

                final float one_hundred_percent = (100 * yscale) / 100f;
                final List<PointValue> divider_points = new ArrayList<>(2);
                divider_points.add(new PointValue(loaded_start / FUZZER, one_hundred_percent));
                dividerLine.setPointRadius(0);
                divider_points.add(new PointValue(loaded_end / FUZZER, one_hundred_percent));
                dividerLine.setValues(divider_points);
                basalLines.add(dividerLine);

                final List<PointValue> points = new ArrayList<>(aplist.size());

                int last_percent = -1;

                int count = aplist.size();
                for (APStatus item : aplist) {
                    if (--count == 0 || (item.basal_percent != last_percent)) {
                        final float this_ypos = (Math.min(item.basal_percent, 500) * yscale) / 100f; // capped at 500%
                        points.add(new PointValue((float) item.timestamp / FUZZER, this_ypos));

                        last_percent = item.basal_percent;
                    }
                }

                final Line line = new Line(points);
                line.setFilled(true);
                line.setFillFlipped(true);
                line.setHasGradientToTransparent(true);
                line.setHasPoints(false);
                line.setStrokeWidth(1);
                line.setHasLines(true);
                line.setSquare(true);
                line.setPointRadius(1);
                line.setReverseYAxis(true);
                line.setBackgroundUnclipped(true);
                line.setGradientDivider(10f);
                line.setColor(getCol(X.color_basal_tbr));
                basalLines.add(line);
            }
        }

        return basalLines;
    }

    // line illustrating result from step counter
    private List<Line> stepsLines() {
        final List<Line> stepsLines = new ArrayList<>();
        if ((prefs.getBoolean("use_pebble_health", true)
                && prefs.getBoolean("show_pebble_movement_line", true))) {
            final List<StepCounter> pmlist = StepCounter.deltaListFromMovementList(StepCounter.latestForGraph(2000, loaded_start, loaded_end));
            PointValue last_point = null;
            final boolean d = false;
            if (d) Log.d(TAG, "Delta: pmlist size: " + pmlist.size());
            final float yscale = doMgdl ? (float) Constants.MMOLL_TO_MGDL : 1f;
            final float ypos = 6 * yscale; // TODO Configurable
            //final long last_timestamp = pmlist.get(pmlist.size() - 1).timestamp;
            final float MAX_SIZE = 50;
            int flipper = 0;
            int accumulator = 0;

            for (StepCounter pm : pmlist) {
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

                        float stroke_size = Math.min(MAX_SIZE, (float) Math.log1p(((double) (pm.metric + accumulator)) / time_delta) * 4);
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

    // line illustrating result from heartrate monitor
    private List<Line> heartLines() {
        final boolean d = false;
        final List<Line> heartLines = new ArrayList<>();
        if ((prefs.getBoolean("use_pebble_health", true)
                && prefs.getBoolean("show_pebble_movement_line", true))) {

            final List<HeartRate> heartRates = HeartRate.latestForGraph(2000, loaded_start, loaded_end);

//            final long condenseCutoffMs = Pref.getBooleanDefaultFalse("smooth_heartrate") ? (10 * Constants.MINUTE_IN_MS) : FUZZER;
            final long condenseCutoffMs = Pref.getBooleanDefaultFalse("smooth_heartrate") ? (10 * Constants.MINUTE_IN_MS) : 1000 * 30 * 5;
            final List<HeartRate> condensedHeartRateList = new ArrayList<>();
            for (HeartRate thisHeartRateRecord : heartRates) {
                final int condensedListSize = condensedHeartRateList.size();
                if (condensedListSize > 0) {
                    final HeartRate tailOfList = condensedHeartRateList.get(condensedListSize - 1);
                    // if its close enough to merge then average with previous
                    if ((thisHeartRateRecord.timestamp - tailOfList.timestamp) < condenseCutoffMs) {
                        tailOfList.bpm = (tailOfList.bpm += thisHeartRateRecord.bpm) / 2;
                    } else {
                        // not close enough to merge
                        condensedHeartRateList.add(thisHeartRateRecord);
                    }
                } else {
                    condensedHeartRateList.add(thisHeartRateRecord); // first record
                }
            }

            if (d) Log.d(TAG, "heartrate before size: " + heartRates.size());
            if (d) Log.d(TAG, "heartrate after c size: " + condensedHeartRateList.size());
            //final float yscale = doMgdl ? (float) Constants.MMOLL_TO_MGDL : 1f;
            final float yscale = doMgdl ? 10f : 1f;
            float ypos; //

            final List<PointValue> new_points = new ArrayList<>();
            if (d) UserError.Log.d("HEARTRATE", "Size " + condensedHeartRateList.size());

            for (HeartRate pm : condensedHeartRateList) {
                if (d)
                    UserError.Log.d("HEARTRATE: ", JoH.dateTimeText(pm.timestamp) + " \tHR: " + pm.bpm);

                ypos = (pm.bpm * yscale) / 10;
                final PointValue this_point = new PointValue((float) pm.timestamp / FUZZER, ypos);
                new_points.add(this_point);
            }
            final Line macroHeartRateLine = new Line(new_points);
            for (Line this_line : autoSplitLine(macroHeartRateLine, 30)) {
                this_line.setColor(getCol(X.color_heart_rate1));
                this_line.setStrokeWidth(6);
                this_line.setHasPoints(false);
                this_line.setHasLines(true);
                this_line.setCubic(true);
                heartLines.add(this_line);
            }
        }
        return heartLines;
    }


    private List<Line> motionLine() {

        final ArrayList<ActivityRecognizedService.motionData> motion_datas = ActivityRecognizedService.getForGraph((long) start_time * FUZZER, (long) end_time * FUZZER);
        List<PointValue> linePoints = new ArrayList<>();

        final float ypos = (float) highMark;
        int last_type = -9999;


        final ArrayList<Line> line_array = new ArrayList<>();

        Log.d(TAG, "Motion datas size: " + motion_datas.size());
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
        Log.d(TAG, "Motion array size: " + line_array.size());
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
            if (d) Log.d(TAG, "Cloned preview chart data");
        }

        previewLineData.setAxisYLeft(yAxis());
        previewLineData.setAxisXBottom(previewXAxis());

        // reduce complexity of preview chart by removing some lines
        final List<Line> removeItems = new ArrayList<>();
        int unlabledLinesSize = 1;
        if (isXLargeTablet(context)) {
            unlabledLinesSize = 2;
        }
        for (Line lline : previewLineData.getLines()) {
            if (((lline.getPointRadius() == pluginSize) && (lline.getPointColor() == getCol(X.color_secondary_glucose_value)))
                    || ((lline.getColor() == getCol(X.color_step_counter1) || (lline.getColor() == getCol(X.color_step_counter2) || (lline.getColor() == getCol(X.color_heart_rate1)))))) {
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

        for (Line item : previewLineData.getLines()) {
            switch (item.getTag()) {
                case "smb":
                case "icon":
                    item.setBitmapScale(item.getBitmapScale() / 3);
                    break;
            }
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
                if (Pref.getBoolean("motion_tracking_enabled", false) && Pref.getBoolean("plot_motion", false)) {
                    lines.addAll(motionLine());
                }
                lines.addAll(basalLines());
                lines.addAll(heartLines());
                lines.addAll(stepsLines());
                lines.addAll(predictiveLines());
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


            if (prefs.getBoolean("show_libre_trend_line", false)) {
                if (DexCollectionType.hasLibre()) {
                    lines.add(libreTrendLine());
                }
            }

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

            lines.add(remoteValuesLine()); // TODO conditional ?
            lines.add(backFillValuesLine()); // TODO conditional ?
            lines.add(badValuesLine());
            lines.add(inRangeValuesLine());
            lines.add(lowValuesLine());
            lines.add(highValuesLine());

            List<Line> extra_lines = extraLines();
            for (Line eline : extra_lines) {
                lines.add(eline);
            }

            // check show debug option here - drawn on top of others
            lines.add(treatments[8]); // noise poly predict

            if (showSMB) {
                lines.addAll(smbLines());
            }
            lines.addAll(iconLines());

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

    public Line badValuesLine() {
        Line badValuesLine = new Line(badValues);
        badValuesLine.setColor(getCol(X.color_bad_values));
        badValuesLine.setHasLines(false);
        badValuesLine.setPointRadius(pointSize);
        badValuesLine.setHasPoints(true);
        return badValuesLine;
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

    private Line backFillValuesLine() {
        final Line line = new Line(backfillValues);
        line.setColor(Color.parseColor("#55338833"));
        line.setHasLines(false);
        line.setPointRadius(pointSize + 3);
        line.setHasPoints(true);
        return line;
    }

    private Line remoteValuesLine() {
        final Line line = new Line(remoteValues);
        line.setColor(Color.parseColor("#55333388"));
        line.setHasLines(false);
        line.setPointRadius(pointSize + 4);
        line.setHasPoints(true);
        return line;
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
                    Line line = (Line) JoH.cloneObject(macroline); // aieeee
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

    public List<Line> extraLines() {
        final List<Line> lines = new ArrayList<>();
        Line line = new Line(pluginValues);
        line.setHasLines(false);
        line.setPointRadius(pluginSize);
        line.setHasPoints(true);
        line.setColor(getCol(X.color_secondary_glucose_value));
        lines.add(line);

        Line bloodtest = new Line(bloodTestValues);
        bloodtest.setHasLines(false);
        bloodtest.setPointRadius(pointSize * 3 / 2);
        bloodtest.setHasPoints(true);
        bloodtest.setColor(ChartUtils.darkenColor(getCol(X.color_calibration_dot_background)));
        bloodtest.setShape(ValueShape.SQUARE);
        lines.add(bloodtest);

        Line bloodtesti = new Line(bloodTestValues);
        bloodtesti.setHasLines(false);
        bloodtesti.setPointRadius(pointSize * 3 / 4);
        bloodtesti.setHasPoints(true);
        bloodtesti.setColor(ChartUtils.darkenColor(getCol(X.color_calibration_dot_foreground)));
        bloodtesti.setShape(ValueShape.SQUARE);
        lines.add(bloodtesti);

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
            badValues.clear();
            highValues.clear();
            lowValues.clear();
            inRangeValues.clear();
            backfillValues.clear();
            remoteValues.clear();
            calibrationValues.clear();
            bloodTestValues.clear();
            pluginValues.clear();
            iconValues.clear();
            smbValues.clear();

            final double bgScale = bgScale();
            final double now = JoH.ts();

            final boolean show_pseudo_filtered = prefs.getBoolean("show_pseudo_filtered", false);
            final RollingAverage rollingAverage = show_pseudo_filtered ? new RollingAverage(2) : null;
            final long rollingOffset = show_pseudo_filtered ? (long) (rollingAverage.getPeak() * DEXCOM_PERIOD) : 0;


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
            double last_bloodtest = 0;

            if (doMgdl) {
                Profile.scale_factor = Constants.MMOLL_TO_MGDL;
            } else {
                Profile.scale_factor = 1;
            }

            final long close_to_side_time = (long) (end_time * FUZZER) - (Constants.MINUTE_IN_MS * 10);
            // enumerate calibrations
            try {
                for (Calibration calibration : calibrations) {
                    if (calibration.timestamp < (start_time * FUZZER)) break;
                    if (calibration.slope_confidence != 0) {
                        final long adjusted_timestamp = (calibration.timestamp + (AddCalibration.estimatedInterstitialLagSeconds * 1000));
                        final PointValueExtended this_point = new PointValueExtended((float) (adjusted_timestamp / FUZZER), (float) unitized(calibration.bg));
                        if (adjusted_timestamp >= close_to_side_time) {
                            predictivehours = Math.max(predictivehours, 1);
                        }
                        this_point.real_timestamp = calibration.timestamp;
                        calibrationValues.add(this_point);
                        if (calibration.timestamp > last_calibration) {
                            last_calibration = calibration.timestamp;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception doing calibration values in bggraphbuilder: " + e.toString());
            }

            // enumerate blood tests
            try {
                for (BloodTest bloodtest : bloodtests) {
                    final long adjusted_timestamp = (bloodtest.timestamp + (AddCalibration.estimatedInterstitialLagSeconds * 1000));
                    final PointValueExtended this_point = new PointValueExtended((float) (adjusted_timestamp / FUZZER), (float) unitized(bloodtest.mgdl))
                           .setType(PointValueExtended.BloodTest)
                            .setUUID(bloodtest.uuid);
                    this_point.real_timestamp = bloodtest.timestamp;
                    // exclude any which have been used for calibration
                    boolean matches = false;
                    for (PointValue calibration_point : calibrationValues) {
                        if ((Math.abs(calibration_point.getX() - this_point.getX())) <= ((AddCalibration.estimatedInterstitialLagSeconds * 1000) / FUZZER) && (calibration_point.getY() == calibration_point.getY())) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) bloodTestValues.add(this_point);
                    if (bloodtest.timestamp > last_bloodtest) {
                        last_bloodtest = bloodtest.timestamp;
                    }
                    if (adjusted_timestamp >= close_to_side_time) {
                        predictivehours = Math.max(predictivehours, 1);
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
            final boolean illustrate_backfilled_data = prefs.getBoolean("illustrate_backfilled_data", false);
            final boolean illustrate_remote_data = prefs.getBoolean("illustrate_remote_data", false);

            if ((Home.get_follower()) && (bgReadings.size() < 3)) {
                GcmActivity.requestBGsync();
            }

            final CalibrationAbstract plugin = (show_plugin) ? PluggableCalibration.getCalibrationPluginFromPreferences() : null;
            CalibrationAbstract.CalibrationData cd = (plugin != null) ? plugin.getCalibrationData() : null;
            int cdposition = 0;

            if ((glucose_from_plugin) && (cd != null)) {
                plugin_adjusted = true; // plugin will be adjusting data
            }

            for (final BgReading bgReading : bgReadings) {
                // jamorham special

                if ((cd != null) && (calibrations.size() > 0)) {

                    while ((bgReading.timestamp < calibrations.get(cdposition).timestamp) || (calibrations.get(cdposition).slope == 0)) {

                        Log.d(TAG, "BG reading earlier than calibration at index: " + cdposition + "  " + JoH.dateTimeText(bgReading.timestamp) + " cal: " + JoH.dateTimeText(calibrations.get(cdposition).timestamp));

                        if (cdposition < calibrations.size() - 1) {
                            cdposition++;
                            //  cd = (plugin != null) ? plugin.getCalibrationData(calibrations.get(cdposition).timestamp) : null;
                            final CalibrationAbstract.CalibrationData oldcd = cd;
                            cd = plugin.getCalibrationData(calibrations.get(cdposition).timestamp);
                            if (cd == null) {
                                Log.d(TAG, "cd went to null during adjustment - likely graph spans multiple sensors");
                                cd = oldcd;
                            }
                            Log.d(TAG, "Now using calibration from: " + JoH.dateTimeText(calibrations.get(cdposition).timestamp) + " slope: " + cd.slope + " intercept: " + cd.intercept);
                        } else {
                            Log.d(TAG, "No more calibrations to choose from");
                            break;
                        }
                    }
                }

                // swap main and plugin plot if display glucose is from plugin
                if ((glucose_from_plugin) && (cd != null)) {
                    pluginValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                    // recalculate from plugin - beware floating / cached references!
                    bgReading.calculated_value = plugin.getGlucoseFromBgReading(bgReading, cd);
                    bgReading.filtered_calculated_value = plugin.getGlucoseFromFilteredBgReading(bgReading, cd);
                }

                if ((show_filtered) && (bgReading.filtered_calculated_value > 0) && (bgReading.filtered_calculated_value != bgReading.calculated_value)) {
                    filteredValues.add(new PointValue((float) ((bgReading.timestamp - timeshift) / FUZZER), (float) unitized(bgReading.filtered_calculated_value)));
                } else if (show_pseudo_filtered) {
                    // TODO differentiate between filtered and pseudo-filtered when both may be in play at different times
                    final double rollingValue = rollingAverage.put(bgReading.calculated_value);
                    if (rollingAverage.reachedPeak()) {
                        filteredValues.add(new PointValue((float) ((bgReading.timestamp + rollingOffset) / FUZZER), (float) unitized(rollingValue)));
                    }
                }
                if ((interpret_raw && (bgReading.raw_calculated > 0))) {
                    rawInterpretedValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.raw_calculated)));
                }
                if ((!glucose_from_plugin) && (plugin != null) && (cd != null)) {
                    pluginValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(plugin.getGlucoseFromBgReading(bgReading, cd))));
                }
                if (bgReading.ignoreForStats) {
                    badValues.add(new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value)));
                } else if (bgReading.calculated_value >= 400) {
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

                if (illustrate_backfilled_data && bgReading.calculated_value > 13 && bgReading.calculated_value < 400 && bgReading.isBackfilled()) {
                    backfillValues.add(bgReadingToPoint(bgReading));
                }
                if (illustrate_remote_data && bgReading.calculated_value > 13 && bgReading.calculated_value < 400 && bgReading.isRemote()) {
                    remoteValues.add(bgReadingToPoint(bgReading));
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

            try {
                if (DexCollectionType.getDexCollectionType() == DexCollectionType.LibreReceiver && prefs.getBoolean("Libre2_showRawGraph", false)) {
                    for (final Libre2RawValue bgLibre : Libre2RawValues) {
                        if (bgLibre.glucose > 0) {
                            rawInterpretedValues.add(new PointValue((float) (bgLibre.timestamp / FUZZER), (float) unitized(bgLibre.glucose)));
                        }
                    }
                }
            } catch (Exception e) {
                Log.wtf(TAG, "Exception to generate Raw-Graph Libre2");
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
                                    if (d) Log.d(TAG, "set forecast best model to: " + poly.getClass().getSimpleName() + " with varience of: " + JoH.qs(poly.errorVarience(),14));
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
//                                plow_timestamp = plow_timestamp - FUZZER;
                                plow_timestamp = plow_timestamp - (1000 * 30 * 5);
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
                if (last_noise > NOISE_TRIGGER ||
                        (last_noise > BgGraphBuilder.NOISE_TRIGGER_ULTRASENSITIVE
                                && Pref.getBooleanDefaultFalse("engineering_mode")
                                && Pref.getBooleanDefaultFalse("bg_compensate_noise_ultrasensitive")
                        )) {
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


                readings_lock.lock();
                try {
                    // display treatment blobs and annotations
                    long lastIconTimestamp = 0;
                    int consecutiveCloseIcons = 0;
                    for (Treatments treatment : treatments) {

                        if (!treatment.hasContent()) continue;

                        if (showSMB && treatment.likelySMB()) {
                            final Pair<Float, Float> yPositions = GraphTools.bestYPosition(bgReadings, treatment.timestamp, doMgdl, false, highMark, 10 + (100d * treatment.insulin));
                            if (yPositions.first > 0) {
                                final PointValueExtended pv = new PointValueExtended(treatment.timestamp / FUZZER, yPositions.first); // TEST VALUES
                                pv.setPlumbPos(GraphTools.yposRatio(yPositions.second, yPositions.first, 0.1f));
                                BitmapLoader.loadAndSetKey(pv, R.drawable.triangle, 180);
                                pv.setBitmapTint(getCol(X.color_smb_icon));
                                pv.setBitmapScale((float) (0.5f + (treatment.insulin * 5f))); // 0.1U == 100% 0.2U = 150%
                                pv.note = "SMB: " + JoH.qs(treatment.insulin, 2) + "U" + (treatment.notes != null ? " " + treatment.notes : "");
                                smbValues.add(pv);
                                continue;
                            } else {
                                UserError.Log.d(TAG, "Could not determine a good position to use for SMB");
                            }
                        }

                        if (treatment.noteOnly()) {
                            if (hidePriming && treatment.isPrimingDose()) {
                                continue;
                            }
                            final PointValue pv = NoteClassifier.noteToPointValue(treatment.notes);
                            if (pv != null) {
                                final boolean tooClose = Math.abs(treatment.timestamp - lastIconTimestamp) < Constants.MINUTE_IN_MS * 6;
                                if (tooClose) {
                                    consecutiveCloseIcons++;
                                } else {
                                    consecutiveCloseIcons = 0;
                                }
                                final Pair<Float, Float> yPositions = GraphTools.bestYPosition(bgReadings, treatment.timestamp, doMgdl, false, highMark, 27d + (18d * consecutiveCloseIcons));
                                pv.set(treatment.timestamp / FUZZER, yPositions.first);
                                //pv.setPlumbPos(yPositions.second);
                                iconValues.add(pv);
                                lastIconTimestamp = treatment.timestamp;
                                continue;
                            }
                        }


                        double height = 6 * bgScale;
                        if (treatment.insulin > 0)
                            height = treatment.insulin; // some scaling needed I think
                        if (height > highMark) height = highMark;
                        if (height < lowMark) height = lowMark;
                        final PointValueExtended pv = new PointValueExtended((float) (treatment.timestamp / FUZZER), (float) height);
                        if (treatment.isPenSyncedDose()) {
                            pv.setType(PointValueExtended.AdjustableDose).setUUID(treatment.uuid);
                        }
                        String mylabel = "";
                        if (treatment.insulin > 0) {
                            if (mylabel.length() > 0)
                                mylabel = mylabel + System.getProperty("line.separator");
                            mylabel = mylabel + (JoH.qs(treatment.insulin, 2) + "u").replace(".0u", "u");
                        }
                        if (treatment.carbs > 0) {
                            if (mylabel.length() > 0)
                                mylabel = mylabel + System.getProperty("line.separator");
                            mylabel = mylabel + (JoH.qs(treatment.carbs, 1) + "g").replace(".0g", "g");
                        }
                        pv.setLabel(mylabel); // standard label

                        // show basal dose as blue syringe icon
                        if (treatment.isBasalOnly()) {
                            //pv.setBitmapScale((float) (0.5f + (treatment.insulin * 5f))); // 0.1U == 100% 0.2U = 150%
                            BitmapLoader.loadAndSetKey(pv, R.drawable.ic_eyedropper_variant_grey600_24dp, 0);
                            pv.setBitmapTint(getCol(X.color_basal_tbr));
                            final Pair<Float, Float> yPositions = GraphTools.bestYPosition(bgReadings, treatment.timestamp, doMgdl, false, highMark, 27d + (18d * consecutiveCloseIcons));
                            pv.set(treatment.timestamp / FUZZER, yPositions.first);
                            pv.note = treatment.getBestShortText();
                            iconValues.add(pv);
                            lastIconTimestamp = treatment.timestamp;
                            continue;
                        }

                        //Log.d(TAG, "watchkeypad pv.mylabel: " + mylabel);
                        if ((treatment.notes != null) && (treatment.notes.length() > 0)) {
                            pv.note = treatment.getBestShortText();
                            //Log.d(TAG, "watchkeypad pv.note: " + pv.note + " mylabel: " + mylabel);
                            try {
                                final Matcher m = posPattern.matcher(treatment.enteredBy);
                                if (m.matches()) {
                                    pv.set(pv.getX(), (float)Math.min(tolerantParseDouble(m.group(1)), 18 * bgScale)); // don't allow pos note to exceed 18mmol on chart
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "Exception matching position: " + e);
                            }
                        } else {
                            pv.note = treatment.getBestShortText();
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
                } finally {
                    readings_lock.unlock();
                }

                try {


                    // we need to check we actually have sufficient data for this
                    double predictedbg = -1000;
                    BgReading mylastbg = bgReadings.get(0);
                    long lasttimestamp = 0;

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
                        boolean iob_shown_already = false;
                        for (Iob iob : iobinfo) {

                            //double activity = iob.activity;
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
                                    //KS Log.d(TAG, "IOB DEBUG: " + (fuzzed_timestamp - end_time) + " " + iob.iob);
                                    if (!iob_shown_already && (Math.abs(fuzzed_timestamp - end_time) < 5) && (iob.iob > 0)) {
                                        iob_shown_already = true;
                                        // show current iob
                                        //  double position = 12.4 * bgScale; // this is for mmol - needs generic for mg/dl
                                        //  if (Math.abs(predictedbg - position) < (2 * bgScale)) {
                                        //      position = 7.0 * bgScale;
                                        //  }

                                        // PointValue iv = new PointValue((float) fuzzed_timestamp, (float) position);
                                        DecimalFormat df = new DecimalFormat("#");
                                        df.setMaximumFractionDigits(2);
                                        df.setMinimumIntegerDigits(1);
                                        //  iv.setLabel("IoB: " + df.format(iob.iob));
                                        Home.updateStatusLine("iob", df.format(iob.iob));
                                        //  annotationValues.add(iv); // needs to be different value list so we can make annotation nicer

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
                        // if (doMgdl) {
                        // These routines need to understand how the profile is defined to use native instead of scaled
                        evaluation = Profile.evaluateEndGameMmol(predictedbg, lasttimestamp * FUZZER, end_time * FUZZER);
                        // } else {
                        //    evaluation = Profile.evaluateEndGameMmol(predictedbg, lasttimestamp * FUZZER, end_time * FUZZER);

                        // }

                        String bwp_update = "";
                        keyStore.putL("bwp_last_insulin_timestamp", -1);
                        if (d)
                            Log.i(TAG, "Predictive BWP: Current prediction: " + JoH.qs(predictedbg) + " / carbs: " + JoH.qs(evaluation[0]) + " insulin: " + JoH.qs(evaluation[1]));
                        if (!BgReading.isDataStale()) {
                            if (((low_occurs_at < 1) || Pref.getBooleanDefaultFalse("always_show_bwp")) && (Pref.getBooleanDefaultFalse("show_bwp"))) {
                                if (evaluation[0] > Profile.minimum_carb_recommendation) {
                                    //PointValue iv = new PointValue((float) fuzzed_timestamp, (float) (10 * bgScale));
                                    //iv.setLabel("+Carbs: " + JoH.qs(evaluation[0], 0));
                                    bwp_update = "\u224F" + " Carbs: " + JoH.qs(evaluation[0], 0);
                                    //annotationValues.add(iv); // needs to be different value list so we can make annotation nicer
                                } else if (evaluation[1] > Profile.minimum_insulin_recommendation) {
                                    //PointValue iv = new PointValue((float) fuzzed_timestamp, (float) (11 * bgScale));
                                    //iv.setLabel("+Insulin: " + JoH.qs(evaluation[1], 1));
                                    keyStore.putS("bwp_last_insulin", JoH.qs(evaluation[1], 1) + ((low_occurs_at > 0) ? ("!") : ""));
                                    keyStore.putL("bwp_last_insulin_timestamp", JoH.tsl());
                                    bwp_update = "\u224F" + " Insulin: " + JoH.qs(evaluation[1], 1) + ((low_occurs_at > 0) ? (" " + "\u26A0") : ""); // warning symbol
                                    //annotationValues.add(iv); // needs to be different value list so we can make annotation nicer
                                }
                            }
                        }
                        Home.updateStatusLine("bwp", bwp_update); // always send so we can blank if needed
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
            // TODO remove any duplication by using refreshNoiseIfOlderThan()
            if (low_occurs_at_processed_till_timestamp < last_bg_reading_timestamp) {
                Log.d(TAG, "Recalculating lowOccursAt: " + JoH.dateTimeText((long) low_occurs_at_processed_till_timestamp) + " vs " + JoH.dateTimeText(last_bg_reading_timestamp));
                // new only the last hour worth of data for this
                (new BgGraphBuilder(xdrip.getAppContext(), System.currentTimeMillis() - 60 * 60 * 1000, System.currentTimeMillis() + 5 * 60 * 1000, 24, true)).addBgReadingValues(false);
            } else {
                Log.d(TAG, "Cached current low timestamp ok: " + JoH.dateTimeText((long) low_occurs_at_processed_till_timestamp) + " vs " + JoH.dateTimeText(last_bg_reading_timestamp));
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

    private PointValue bgReadingToPoint(BgReading bgReading) {
        return new PointValue((float) (bgReading.timestamp / FUZZER), (float) unitized(bgReading.calculated_value));
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
        myLineValues.add(new PointValue((float) start_time, (float) Profile.getTargetRangeInUnits(start_time)));
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

    private Line libreTrendLine() {
        final List<PointValue> libreTrendValues = LibreTrendGraph.getTrendDataPoints(doMgdl, (long) (start_time * FUZZER), (long) (end_time * FUZZER));
        final Line line = new Line(libreTrendValues);
        line.setHasPoints(true);
        line.setHasLines(false);
        line.setCubic(false);
        line.setStrokeWidth(2);
        line.setPointRadius(1);
        line.setColor(Color.argb(240, 25, 206, 244)); // temporary pending preference
        return line;
    }

    private List<Line> smbLines() {
        final List<Line> lines = new LinkedList<>();
        final Line line = new Line(smbValues);
        line.setTag("smb");
        line.setHasPoints(true);
        line.setHasLines(false);
        line.setPointRadius(4);
        line.setPointColor(Color.TRANSPARENT);
        line.setStrokeWidth(1);
        line.setColor(getCol(X.color_smb_line));
        line.setPointColor(ColorUtil.blendColor(getCol(X.color_smb_line), Color.TRANSPARENT, 0.99f));
        line.setBitmapScale(0.5f);
        line.setBitmapLabels(true);
        line.setBitmapCacheProvider(BitmapLoader.getInstance());
        lines.add(line);
        return lines;
    }

    private List<Line> iconLines() {
        final List<Line> lines = new LinkedList<>();

        final Line line = new Line(iconValues);
        line.setTag("icon");
        line.setHasPoints(true);
        line.setHasLines(false);
        line.setPointRadius(5);
        line.setPointColor(ColorUtil.blendColor(Color.BLACK, Color.TRANSPARENT, 0.99f));
        line.setBitmapScale(1f);
        line.setBitmapLabels(true);
        line.setBitmapLabelShadowColor(Color.WHITE);
        line.setFullShadow(true);
        line.setBitmapCacheProvider(BitmapLoader.getInstance());
        lines.add(line);
        return lines;
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
        yAxis.setHasLines(prefs.getBoolean("show_graph_grid_glucose", true));
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
        calendar.setTimeInMillis((long) (start_time * FUZZER));
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < (start_time * FUZZER)) {
            calendar.add(Calendar.HOUR, 1);
        }
        while (calendar.getTimeInMillis() < ((end_time * FUZZER) + (predictivehours * 60 * 60 * 1000))) {
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

    public Axis previewXAxis() {
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
    public Viewport advanceViewport(Chart chart, Chart previewChart, float hours) {
        viewport = new Viewport(previewChart.getMaximumViewport());
        viewport.inset((float) ((86400000 / hours) / FUZZER), 0);
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

    public static String unitized_string_static(double value) {
        return unitized_string(value, Pref.getString("units", "mgdl").equals("mgdl"));
    }

    public static String unitized_string_with_units_static(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl) + " " + (domgdl ? "mg/dl" : "mmol/l");
    }

    public static String unitized_string_with_units_static_short(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        return unitized_string(value, domgdl) + " " + (domgdl ? "mgdl" : "mmol");
    }

    public static String unitized_string_static_no_interpretation_short(double value) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        final DecimalFormat df = new DecimalFormat("#");
        if (domgdl) {
            df.setMaximumFractionDigits(0);
        } else {
            df.setMaximumFractionDigits(1);
        }
        return df.format(unitized(value, domgdl)) + " " + (domgdl ? "mgdl" : "mmol");
    }

    public static String unitized_string(double value, boolean doMgdl) {
        final DecimalFormat df = new DecimalFormat("#");
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
        return unitizedDeltaString(showUnit, highGranularity, Home.get_follower());
    }

    public String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean is_follower) {
        return unitizedDeltaString(showUnit, highGranularity, is_follower, doMgdl);
    }

    public static String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean is_follower, boolean doMgdl) {

        List<BgReading> last2 = BgReading.latest(2, is_follower);
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

    public static String unitizedDeltaStringRaw(boolean showUnit, boolean highGranularity, double value, boolean doMgdl) {


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

            return delta_sign + df.format(unitized(value, doMgdl)) + (showUnit ? " mg/dl" : "");
        } else {
            // only show 2 decimal places on mmol/l delta when less than 0.1 mmol/l
            if (highGranularity && (Math.abs(value) < (Constants.MMOLL_TO_MGDL * 0.1))) {
                df.setMaximumFractionDigits(2);
            } else {
                df.setMaximumFractionDigits(1);
            }

            df.setMinimumFractionDigits(1);
            df.setMinimumIntegerDigits(1);
            return delta_sign + df.format(unitized(value, doMgdl)) + (showUnit ? " mmol/l" : "");
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
            String uuid = "";
            int type = 0;
            long real_timestamp = 0;
            try {
                PointValueExtended pve = (PointValueExtended) pointValue;
                type = pve.type;
                if (pve.calculatedFilteredValue != -1) {
                    filtered = " (" + Math.round(pve.calculatedFilteredValue * 10) / 10d + ")";
                }
                if (pve.note != null) {
                    alternate = pve.note;
                }
                if (pve.uuid != null) {
                    uuid = pve.uuid;
                }
                real_timestamp = pve.real_timestamp;

            } catch (ClassCastException e) {
                // Log.e(TAG, "Error casting a point from pointValue to PointValueExtended", e);
            }

            final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
            //Won't give the exact time of the reading but the time on the grid: close enough.
            final Long time = (real_timestamp > 0) ? real_timestamp : ((long) pointValue.getX()) * FUZZER;
            final double ypos = pointValue.getY();

            final String message;

            if (alternate.length() > 0) {
                message = timeFormat.format(time) + "    " + alternate;
            } else {
                message = timeFormat.format(time) + "      " + (Math.round(pointValue.getY() * 10) / 10d) + " " + unit() + filtered;
            }
            final String fuuid = uuid;
            switch (type) {
                case PointValueExtended.BloodTest:

                    final View.OnClickListener mBtOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Home.startHomeWithExtra(xdrip.getAppContext(), Home.BLOOD_TEST_ACTION, time.toString(), fuuid);
                        }
                    };
                    Home.snackBar(R.string.blood_test, message, mBtOnClickListener, callerActivity);
                    break;
                case PointValueExtended.AdjustableDose:
                    Home.snackBar(R.string.Dose, message,
                            v -> DoseAdjustDialog.show(callerActivity, fuuid), callerActivity);
                    break;
                default:
                    final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Home.startHomeWithExtra(xdrip.getAppContext(), Home.CREATE_TREATMENT_NOTE, time.toString(), Double.toString(ypos));
                        }
                    };
                    Home.snackBar(R.string.add_note, message, mOnClickListener, callerActivity);
                    break;
            }
        }

        @Override
        public void onValueDeselected() {
            // do nothing
        }
    }
}
