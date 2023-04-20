package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

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

import static com.eveningoutpost.dexdrip.models.JoH.cloneObject;

/**
 * Created by Emma Black on 11/15/14.
 */

class PointValueExtended extends PointValue {

    public static final int BloodTest = 1;

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
    int type = 0;
    String uuid;
    long real_timestamp = 0;
}

public class BgGraphBuilder {
    public static final int FUZZER = (1000 * 60 * 1);//(1000 * 30 * 5); // 2.5 mins?
    private int timespan;
    public double end_time;
    public double start_time;
    public double fuzzyTimeDenom = (1000 * 60 * 1);
    public Context context;
    public double highMark;
    public double lowMark;
    public List<BgWatchData> bgDataList = new ArrayList<BgWatchData>();
    public List<BgWatchData> treatsDataList = new ArrayList<BgWatchData>();
    public List<BgWatchData> calDataList = new ArrayList<BgWatchData>();
    public List<BgWatchData> btDataList = new ArrayList<BgWatchData>();
    private final static String TAG = "jamorham graph";
    private final static boolean d = false; // debug flag, could be read from preferences
    public boolean doMgdl;
    public SharedPreferences prefs;
    public boolean is24;

    public int pointSize;
    public int highColor;
    public int lowColor;
    public int midColor;
    public boolean singleLine = false;

    private double endHour;
    private List<PointValue> inRangeValues = new ArrayList<PointValue>();
    private List<PointValue> highValues = new ArrayList<PointValue>();
    private List<PointValue> lowValues = new ArrayList<PointValue>();
    private final List<PointValue> treatmentValues = new ArrayList<PointValue>();
    private final List<PointValue> calibrationValues = new ArrayList<PointValue>();
    private final List<PointValue> bloodTestValues = new ArrayList<PointValue>();
    public Viewport viewport;

    // ambient mode version
    BgGraphBuilder(Context context, List<BgWatchData> aBgList, List<BgWatchData> aTreatsList, List<BgWatchData> aCalList, List<BgWatchData> aBtList, int aPointSize, int aMidColor, int timespan, boolean doMgdl, boolean is24) {
        end_time = new Date().getTime() + (1000 * 60 * 6 * timespan); //Now plus 30 minutes padding (for 5 hours. Less if less.)
        start_time = new Date().getTime()  - (1000 * 60 * 60 * timespan); //timespan hours ago
        this.bgDataList = aBgList;
        this.treatsDataList = aTreatsList;
        this.calDataList = aCalList;
        this.btDataList = aBtList;
        this.context = context;
        this.highMark = aBgList.get(aBgList.size() - 1).high;
        this.lowMark = aBgList.get(aBgList.size() - 1).low;
        this.pointSize = aPointSize;
        this.singleLine = true;
        this.midColor = aMidColor;
        this.lowColor = aMidColor;
        this.highColor = aMidColor;
        this.timespan = timespan;
        Collections.sort(aBgList);
        this.doMgdl = doMgdl;
        this.is24 = is24;
    }

    public BgGraphBuilder(Context context, List<BgWatchData> aBgList, List<BgWatchData> aTreatsList, List<BgWatchData> aCalList, List<BgWatchData> aBtList, int aPointSize, int aHighColor, int aLowColor, int aMidColor, int timespan, boolean doMgdl, boolean is24) {
        end_time = new Date().getTime() + (1000 * 60 * 6 * timespan); //Now plus 30 minutes padding (for 5 hours. Less if less.)
        start_time = new Date().getTime()  - (1000 * 60 * 60 * timespan); //timespan hours ago
        this.bgDataList = aBgList;
        this.treatsDataList = aTreatsList;
        this.calDataList = aCalList;
        this.btDataList = aBtList;
        this.context = context;
        this.highMark = aBgList.get(aBgList.size() - 1).high;
        this.lowMark = aBgList.get(aBgList.size() - 1).low;
        this.pointSize = aPointSize;
        this.highColor = aHighColor;
        this.lowColor = aLowColor;
        this.midColor = aMidColor;
        this.timespan = timespan;
        this.doMgdl = doMgdl;
        this.is24 = is24;
    }

    public LineChartData lineData() {
        LineChartData lineData = new LineChartData(defaultLines());
        lineData.setAxisYLeft(yAxis());
        lineData.setAxisXBottom(xAxis());
        return lineData;
    }

    public List<Line> defaultLines() {
        addBgReadingValues();
        List<Line> lines = new ArrayList<Line>();
        lines.add(highLine());
        lines.add(lowLine());
        if (singleLine) {
            lines.addAll(autoSplitLine(inRangeValuesLine(),10));
        } else {
            lines.add(inRangeValuesLine());
        }
        lines.add(lowValuesLine());
        lines.add(highValuesLine());
        Line[] treatments = treatmentValuesLine();
        lines.add(treatments[0]); // white circle of treatment in background
        lines.add(treatments[1]); // blue dot in centre // has annotation
        Line[] calib = calibrationValuesLine();
        lines.add(calib[0]); // white circle of calib in background
        lines.add(calib[1]); // red dot of calib in foreground
        List<Line> extra_lines = extraLines();
        for (Line eline : extra_lines) {
            lines.add(eline);
        }

        return lines;
    }
    // auto split a line - jump thresh in minutes
    private ArrayList<Line> autoSplitLine(Line macroline, final float jumpthresh) {
        ArrayList<Line> linearray = new ArrayList<>();
        float lastx = -999999;

        List<PointValue> macropoints = macroline.getValues();
        List<PointValue> thesepoints = new ArrayList<>();

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
                    try {
                        line.setValues(thesepoints);
                        linearray.add(line);
                    } catch (NullPointerException e) {
                    //
                    }
                        thesepoints = new ArrayList<PointValue>();
                }
                lastx = thispoint.getX();
                thesepoints.add(thispoint); // grow current line list
            }
        }
        return linearray;
    }

    public Line highValuesLine() {
        Line highValuesLine = new Line(highValues);
        highValuesLine.setColor(highColor);
        highValuesLine.setHasLines(false);
        highValuesLine.setPointRadius(pointSize);
        highValuesLine.setHasPoints(true);
        return highValuesLine;
    }

    public Line lowValuesLine() {
        Line lowValuesLine = new Line(lowValues);
        lowValuesLine.setColor(lowColor);
        lowValuesLine.setHasLines(false);
        lowValuesLine.setPointRadius(pointSize);
        lowValuesLine.setHasPoints(true);
        return lowValuesLine;
    }

    private Line inRangeValuesLine() {
        Line inRangeValuesLine = new Line(inRangeValues);
        inRangeValuesLine.setColor(midColor);
        // some problem with hellocharts at low resolutions maybe, but lines and cubic lines work so use those instead
        if(singleLine) {
            inRangeValuesLine.setPointRadius(pointSize);
            inRangeValuesLine.setCubic(true); // maybe a bit horrible cpu wise but seems to remove artifacts in tests so far
            inRangeValuesLine.setHasLines(true);
            inRangeValuesLine.setHasPoints(false);
            inRangeValuesLine.setStrokeWidth(pointSize*2); // or maybe should just be 4
        } else {
            inRangeValuesLine.setPointRadius(pointSize);
            inRangeValuesLine.setHasPoints(true);
            inRangeValuesLine.setHasLines(false);
        }
        return inRangeValuesLine;
    }

    public List<Line> extraLines()
    {
        final List<Line> lines = new ArrayList<>();
        Line bloodtest = new Line(bloodTestValues);
        bloodtest.setHasLines(false);
        bloodtest.setPointRadius(pointSize * 5 / 3);//3 / 2
        bloodtest.setHasPoints(true);
        bloodtest.setColor(highColor);//ChartUtils.darkenColor(getCol(X.color_calibration_dot_background))
        bloodtest.setShape(ValueShape.SQUARE);
        lines.add(bloodtest);

        Line bloodtesti = new Line(bloodTestValues);
        bloodtesti.setHasLines(false);
        bloodtesti.setPointRadius(pointSize * 5 / 4);//3 / 4
        bloodtesti.setHasPoints(true);
        bloodtesti.setColor(lowColor);//ChartUtils.darkenColor(getCol(X.color_calibration_dot_foreground))
        bloodtesti.setShape(ValueShape.SQUARE);
        lines.add(bloodtesti);

        return lines;
    }
    public Line[] calibrationValuesLine() {
        Line[] lines = new Line[2];
        lines[0] = new Line(calibrationValues);
        lines[0].setColor(highColor);//getCol(X.color_calibration_dot_background
        lines[0].setHasLines(false);
        lines[0].setPointRadius(pointSize * 5 / 3);//3 / 2
        lines[0].setHasPoints(true);
        lines[1] = new Line(calibrationValues);
        lines[1].setColor(lowColor);//getCol(X.color_calibration_dot_foreground)
        lines[1].setHasLines(false);
        lines[1].setPointRadius(pointSize * 5 / 4);//3 / 4
        lines[1].setHasPoints(true);
        return lines;
    }
    public Line[] treatmentValuesLine() {
        Line[] lines = new Line[2];
        try {

            lines[0] = new Line(treatmentValues);
            lines[0].setColor(highColor);//getCol(X.color_treatment_dot_background) 0xFFFFFF
            lines[0].setHasLines(false);
            lines[0].setPointRadius(pointSize * 5 / 3);//pointSize * 5 / 2
            lines[0].setHasPoints(true);
            lines[0].setShape(ValueShape.DIAMOND);//KS

            lines[1] = new Line(treatmentValues);
            lines[1].setColor(Color.GREEN);//getCol(X.color_treatment_dot_foreground)//0x77aa00 //lowColor
            lines[1].setHasLines(false);
            lines[1].setPointRadius(pointSize * 5 / 4);//pointSize * 5 / 4
            lines[1].setHasPoints(true);
            lines[1].setShape(ValueShape.DIAMOND);
            //lines[1].setHasLabels(true);

            LineChartValueFormatter formatter = new SimpleLineChartValueFormatter(1);
            lines[1].setFormatter(formatter);

        } catch (Exception e) {
            if (d) UserError.Log.i(TAG, "Exception making treatment lines: " + e.toString());
        }
        return lines;
    }
    private void addBgReadingValues() {
        if(singleLine) {
            for (BgWatchData bgReading : bgDataList) {
                if(bgReading.timestamp > start_time) {
                    if (bgReading.sgv >= 400) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 400));
                    } else if (bgReading.sgv >= highMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= lowMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 40) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 11) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 40));
                    }
                }
            }
        } else {
            for (BgWatchData bgReading : bgDataList) {
                if (bgReading.timestamp > start_time) {
                    if (bgReading.sgv >= 400) {
                        highValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 400));
                    } else if (bgReading.sgv >= highMark) {
                        highValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= lowMark) {
                        inRangeValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 40) {
                        lowValues.add(new PointValue(fuzz(bgReading.timestamp), (float) bgReading.sgv));
                    } else if (bgReading.sgv >= 11) {
                        lowValues.add(new PointValue(fuzz(bgReading.timestamp), (float) 40));
                    }
                }
            }
        }
        calibrationValues.clear();
        bloodTestValues.clear();
        treatmentValues.clear();
        if (calDataList != null) addCalibrations();
        if (btDataList != null) addBloodTests();
        if (treatsDataList != null) addTreatments();
    }

    public static double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public double unitized(double value) {
        if (doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }
    private double bgScale() {
        //this.doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
        if (doMgdl)
            return Constants.MMOLL_TO_MGDL;
        else
            return 1;
    }
    private void addTreatments() {
        final double bgScale = bgScale();
        try {
            // display treatment blobs and annotations
            for (BgWatchData treatment : treatsDataList) {
                if (treatment.timestamp > start_time) {
                    double carbs = treatment.high;
                    double insulin = treatment.low;
                    String notes = "";
                    double height = 6 * bgScale;
                    if (insulin > 0)
                        height = insulin; // some scaling needed I think
                    if (height > highMark) height = highMark;
                    if (height < lowMark) height = lowMark;

                    PointValueExtended pv = new PointValueExtended((float) (fuzz(treatment.timestamp)), (float) height);//(treatment.timestamp / FUZZER)
                    String mylabel = "";
                    if (insulin > 0) {
                        if (mylabel.length() > 0)
                            mylabel = mylabel + System.getProperty("line.separator");
                        mylabel = mylabel + (Double.toString(insulin) + "u").replace(".0u", "u");
                    }
                    if (carbs > 0) {
                        if (mylabel.length() > 0)
                            mylabel = mylabel + System.getProperty("line.separator");
                        mylabel = mylabel + (Double.toString(carbs) + "g").replace(".0g", "g");
                    }
                    pv.setLabel(mylabel); // standard label
                    if (d)
                        Log.d(TAG, "watchkeypad pv.mylabel: " + mylabel);
                    if ((notes != null) && (notes.length() > 0)) {
                        pv.note = notes;
                        if (d)
                            Log.d(TAG, "watchkeypad pv.note: " + pv.note + " mylabel: " + mylabel);
                    /*try {
                        final Pattern p = Pattern.compile(".*?pos:([0-9.]+).*");
                        final Matcher m = p.matcher(treatment.enteredBy);
                        if (m.matches()) {
                            pv.set(pv.getX(), (float) JoH.tolerantParseDouble(m.group(1)));
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Exception matching position: " + e);
                    }*/
                    } else {
                        pv.note = "Treatment";
                    }
                    /*pv.setLabel("");//KS do not display label on watch, too large!!
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
                    }*/

                    treatmentValues.add(new PointValue(fuzz(treatment.timestamp), (float) height));//KS TEST

                    //treatmentValues.add(pv); // hover
                    if (d)
                        Log.d(TAG, "Treatment total record: " + Double.toString(height) + " " + " timestamp: " + Double.toString(treatment.timestamp) + " timestamp=" + JoH.dateTimeText((long) treatment.timestamp));
                }
            }

        } catch (Exception e) {

            Log.e(TAG, "Exception doing treatment values in bggraphbuilder: " + e.toString());
        }
    }

    public static final long estimatedInterstitialLagSeconds = 600; //AddCalibrations: how far behind venous glucose do we estimate
    private void addCalibrations() {
        try {
            for (BgWatchData calibration : calDataList) {
                if(calibration.timestamp > start_time) {
                    final long adjusted_timestamp = ((long)(calibration.timestamp) + (estimatedInterstitialLagSeconds * 1000));
                    //final long adjusted_timestamp = ((long)calibration.timestamp + (estimatedInterstitialLagSeconds * 1000));
                    final PointValueExtended this_point = new PointValueExtended((float) (adjusted_timestamp / FUZZER), (float) unitized(calibration.sgv));
                    this_point.real_timestamp = (long)calibration.timestamp;
                    //calibrationValues.add(this_point);
                    calibrationValues.add(new PointValue(fuzz(adjusted_timestamp), (float) unitized(calibration.sgv)));//KS calibration.timestamp
                    if (d)
                        Log.d(TAG, "calibration total record: " + calibration.sgv + " " + " adjusted_timestamp: " + fuzz(calibration.timestamp) + " timestamp=" + JoH.dateTimeText((long) calibration.timestamp));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception doing calibration values in bggraphbuilder: " + e.toString());
        }
    }

    private void addBloodTests() {
        // enumerate blood tests
        try {
            for (BgWatchData bloodtest : btDataList) {
                if(bloodtest.timestamp > start_time) {
                    final long adjusted_timestamp = (Math.round(bloodtest.timestamp) + (estimatedInterstitialLagSeconds * 1000));
                    final PointValueExtended this_point = new PointValueExtended((float) (adjusted_timestamp / FUZZER), (float) unitized(bloodtest.sgv));
                    this_point.type = PointValueExtended.BloodTest;
                    //this_point.uuid = bloodtest.uuid; //TODO
                    this_point.real_timestamp = (long)bloodtest.timestamp;
                    // exclude any which have been used for calibration
                    boolean matches = false;
                    for (PointValue calibration_point : calibrationValues) {
                        if ((Math.abs(calibration_point.getX() - this_point.getX())) <= ((estimatedInterstitialLagSeconds * 1000) / FUZZER) && (calibration_point.getY() == calibration_point.getY())) {
                            matches = true;
                            break;
                        }
                    }
                    //if (!matches) bloodTestValues.add(this_point);
                    if (!matches)
                        bloodTestValues.add(new PointValue(fuzz(adjusted_timestamp), (float) unitized(bloodtest.sgv)));//KS bloodtest.timestamp
                    if (d)
                        Log.d(TAG, "bloodtest total record: " + bloodtest.sgv + " " + " adjusted_timestamp: " + fuzz(bloodtest.timestamp) + " timestamp=" + JoH.dateTimeText((long) bloodtest.timestamp));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception doing bloodtest values in bggraphbuilder: " + e.toString());
        }

    }

    public Line highLine() {
        List<PointValue> highLineValues = new ArrayList<PointValue>();
        highLineValues.add(new PointValue(fuzz(start_time), (float) highMark));
        highLineValues.add(new PointValue(fuzz(end_time), (float) highMark));
        Line highLine = new Line(highLineValues);
        highLine.setHasPoints(false);
        highLine.setStrokeWidth(1);
        highLine.setColor(highColor);
        return highLine;
    }

    public Line lowLine() {
        List<PointValue> lowLineValues = new ArrayList<PointValue>();
        lowLineValues.add(new PointValue(fuzz(start_time), (float) lowMark));
        lowLineValues.add(new PointValue(fuzz(end_time), (float) lowMark));
        Line lowLine = new Line(lowLineValues);
        lowLine.setHasPoints(false);
        lowLine.setColor(lowColor);
        lowLine.setStrokeWidth(1);
        return lowLine;
    }

    /////////AXIS RELATED//////////////
    public Axis yAxis() {
        Axis yAxis = new Axis();
        yAxis.setAutoGenerated(true);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        yAxis.setValues(axisValues);
        yAxis.setHasLines(false);
        return yAxis;
    }

    public Axis xAxis() {
        //final boolean is24 = DateFormat.is24HourFormat(context);
        Axis xAxis = new Axis();
        xAxis.setAutoGenerated(false);
        List<AxisValue> xAxisValues = new ArrayList<AxisValue>();
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar today = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        SimpleDateFormat timeFormat = new SimpleDateFormat(is24? "HH" : "h a");
        timeFormat.setTimeZone(TimeZone.getDefault());
        double start_hour = today.getTime().getTime();
        double timeNow = new Date().getTime();
        for (int l = 0; l <= 24; l++) {
            if ((start_hour + (60000 * 60 * (l))) < timeNow) {
                if ((start_hour + (60000 * 60 * (l + 1))) >= timeNow) {
                    endHour = start_hour + (60000 * 60 * (l));
                    l = 25;
                }
            }
        }
        //Display current time on the graph
        SimpleDateFormat longTimeFormat = new SimpleDateFormat(is24? "HH:mm" : "h:mm a");
        xAxisValues.add(new AxisValue(fuzz(timeNow), (longTimeFormat.format(timeNow)).toCharArray()));

        //Add whole hours to the axis (as long as they are more than 15 mins away from the current time)
        for (int l = 0; l <= 24; l++) {
            double timestamp = endHour - (60000 * 60 * l);
            if((timestamp - timeNow < 0) && (timestamp > start_time)) {
                if(Math.abs(timestamp - timeNow) > (1000 * 60 * 8 * timespan)){
                    xAxisValues.add(new AxisValue(fuzz(timestamp), (timeFormat.format(timestamp)).toCharArray()));
                }else {
                    xAxisValues.add(new AxisValue(fuzz(timestamp), "".toCharArray()));
                }
            }
        }
        xAxis.setValues(xAxisValues);
        xAxis.setTextSize(10);
        xAxis.setHasLines(true);
        return xAxis;
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

    public float fuzz(double value) {
        return (float) Math.round(value / fuzzyTimeDenom);
    }

    public OnValueSelectTooltipListener getOnValueSelectTooltipListener(BaseWatchFace callerActivity) {//Activity
        Log.d(TAG, "getOnValueSelectTooltipListener");
        return new OnValueSelectTooltipListener(callerActivity);
    }

    public class OnValueSelectTooltipListener implements LineChartOnValueSelectListener {

        private Toast tooltip;
        private BaseWatchFace callerActivity;//Activity

        public OnValueSelectTooltipListener(BaseWatchFace callerActivity) {//Activity
            this.callerActivity = callerActivity;
        }

        @Override
        public synchronized void onValueSelected(int i, int i1, PointValue pointValue) {

            Log.d(TAG, "onValueSelected pointValue=" + pointValue.getX() + "," + pointValue.getY());
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
                Log.e(TAG, "Error casting a point from pointValue to PointValueExtended", e);
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
            Log.d(TAG, "onValueSelected message=" + message);
            JoH.static_toast(xdrip.getAppContext(), message, Toast.LENGTH_SHORT);

            /*switch (type) {
                case com.eveningoutpost.dexdrip.UtilityModels.PointValueExtended.BloodTest:
                    final String fuuid = uuid;
                    final View.OnClickListener mBtOnClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Home.startHomeWithExtra(xdrip.getAppContext(), Home.BLOOD_TEST_ACTION, time.toString(), fuuid);
                        }
                    };
                    Home.snackBar(R.string.blood_test, message, mBtOnClickListener, callerActivity);
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
            }*/
        }

        @Override
        public void onValueDeselected() {
            // do nothing
        }
    }
}
