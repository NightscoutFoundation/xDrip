package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.CalSubrecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Table(name = "BgReadings", id = BaseColumns._ID)
public class BgReading extends Model {
    private static boolean predictBG;
    private final static String TAG = BgReading.class.getSimpleName();
    private final static String TAG_ALERT = TAG +" AlertBg";
    //TODO: Have these as adjustable settings!!
    public final static double BESTOFFSET = (60000 * 0); // Assume readings are about x minutes off from actual!

    @Column(name = "sensor", index = true)
    public Sensor sensor;

    @Column(name = "calibration", index = true)
    public Calibration calibration;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "time_since_sensor_started")
    public double time_since_sensor_started;

    @Expose
    @Column(name = "raw_data")
    public double raw_data;

    @Expose
    @Column(name = "filtered_data")
    public double filtered_data;

    @Expose
    @Column(name = "age_adjusted_raw_value")
    public double age_adjusted_raw_value;

    @Expose
    @Column(name = "calibration_flag")
    public boolean calibration_flag;

    @Expose
    @Column(name = "calculated_value")
    public double calculated_value;

    @Expose
    @Column(name = "calculated_value_slope")
    public double calculated_value_slope;

    @Expose
    @Column(name = "a")
    public double a;

    @Expose
    @Column(name = "b")
    public double b;

    @Expose
    @Column(name = "c")
    public double c;

    @Expose
    @Column(name = "ra")
    public double ra;

    @Expose
    @Column(name = "rb")
    public double rb;

    @Expose
    @Column(name = "rc")
    public double rc;
    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    @Expose
    @Column(name = "calibration_uuid")
    public String calibration_uuid;

    @Expose
    @Column(name = "sensor_uuid", index = true)
    public String sensor_uuid;

    @Column(name = "snyced")
    public boolean synced;

    @Column(name = "raw_calculated")
    public double raw_calculated;

    @Column(name = "hide_slope")
    public boolean hide_slope;

    @Column(name = "noise")
    public String noise;

    public double calculated_value_mmol() {
        return mmolConvert(calculated_value);
    }

    public double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public String displayValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "mgdl");
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        if (calculated_value >= 400) {
            return "HIGH";
        } else if (calculated_value >= 40) {
            if(unit.compareTo("mgdl") == 0) {
                df.setMaximumFractionDigits(0);
                return df.format(calculated_value);
            } else {
                df.setMaximumFractionDigits(1);
                return df.format(calculated_value_mmol());
            }
        } else {
            return "LOW";
        }
    }

    public static double activeSlope() {
        BgReading bgReading = BgReading.lastNoSenssor();
        if (bgReading != null) {
            double slope = (2 * bgReading.a * (new Date().getTime() + BESTOFFSET)) + bgReading.b;
            Log.w(TAG, "ESTIMATE SLOPE" + slope);
            return slope;
        }
        return 0;
    }

    public static double activePrediction() {
        BgReading bgReading = BgReading.lastNoSenssor();
        if (bgReading != null) {
            double currentTime = new Date().getTime();
            if (currentTime >=  bgReading.timestamp + (60000 * 7)) { currentTime = bgReading.timestamp + (60000 * 7); }
            double time = currentTime + BESTOFFSET;
            return ((bgReading.a * time * time) + (bgReading.b * time) + bgReading.c);
        }
        return 0;
    }

    //*******CLASS METHODS***********//
    public static void create(EGVRecord[] egvRecords, long addativeOffset, Context context) {
        for(EGVRecord egvRecord : egvRecords) { BgReading.create(egvRecord, addativeOffset, context); }
    }

    public static void create(SensorRecord[] sensorRecords, long addativeOffset, Context context) {
        for(SensorRecord sensorRecord : sensorRecords) { BgReading.create(sensorRecord, addativeOffset, context); }
    }

    public static void create(SensorRecord sensorRecord, long addativeOffset, Context context) {
        Log.w(TAG, "create: gonna make some sensor records: " + sensorRecord.getUnfiltered());
        if(BgReading.is_new(sensorRecord, addativeOffset)) {
            BgReading bgReading = new BgReading();
            Sensor sensor = Sensor.currentSensor();
            Calibration calibration = Calibration.getForTimestamp(sensorRecord.getSystemTime().getTime() + addativeOffset);
            if(sensor != null && calibration != null) {
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = (sensorRecord.getUnfiltered() / 1000);
                bgReading.filtered_data = (sensorRecord.getFiltered() / 1000);
                bgReading.timestamp = sensorRecord.getSystemTime().getTime() + addativeOffset;
                if(bgReading.timestamp > new Date().getTime()) { return; }
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;
                bgReading.calculateAgeAdjustedRawValue();
                bgReading.save();
            }
        }
    }

    public static void create(EGVRecord egvRecord, long addativeOffset, Context context) {
        BgReading bgReading = BgReading.getForTimestamp(egvRecord.getSystemTime().getTime() + addativeOffset);
        Log.w(TAG, "create: Looking for BG reading to tag this thing to: " + egvRecord.getBGValue());
        if(bgReading != null) {
            bgReading.calculated_value = egvRecord.getBGValue();
            if (egvRecord.getBGValue() <= 13) {
                Calibration calibration = bgReading.calibration;
                double firstAdjSlope = calibration.first_slope + (calibration.first_decay * (Math.ceil(new Date().getTime() - calibration.timestamp)/(1000 * 60 * 10)));
                double calSlope = (calibration.first_scale / firstAdjSlope)*1000;
                double calIntercept = ((calibration.first_scale * calibration.first_intercept) / firstAdjSlope)*-1;
                bgReading.raw_calculated = (((calSlope * bgReading.raw_data) + calIntercept) - 5);
                bgReading.noise = egvRecord.noiseValue();
            }
            Log.w(TAG, "create: NEW VALUE CALCULATED AT: " + bgReading.calculated_value);
            bgReading.calculated_value_slope = bgReading.slopefromName(egvRecord.getTrend().friendlyTrendName());
            if(egvRecord.getTrend().friendlyTrendName().compareTo("NOT_COMPUTABLE") == 0 || egvRecord.getTrend().friendlyTrendName().compareTo("OUT_OF_RANGE") == 0) {
                bgReading.hide_slope = true;
            }

            bgReading.save();
            bgReading.find_new_curve();
            bgReading.find_new_raw_curve();
            context.startService(new Intent(context, Notifications.class));
            BgSendQueue.addToQueue(bgReading, "create", context);
        }
    }

    public static BgReading getForTimestamp(double timestamp) {
        Sensor sensor = Sensor.currentSensor();
        if(sensor != null) {
            BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp <= ?", (timestamp + (60 * 1000))) // 1 minute padding (should never be that far off, but why not)
                    .where("calculated_value = 0")
                    .where("raw_calculated = 0")
                    .orderBy("timestamp desc")
                    .executeSingle();
            if(bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3*60*1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                Log.w(TAG, "getForTimestamp: Found a BG timestamp match");
                return bgReading;
            }
        }
        Log.w(TAG, "getForTimestamp: No luck finding a BG timestamp match");
        return null;
    }

    public static boolean is_new(SensorRecord sensorRecord, long addativeOffset) {
        double timestamp = sensorRecord.getSystemTime().getTime() + addativeOffset;
        Sensor sensor = Sensor.currentSensor();
        if(sensor != null) {
            BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp <= ?",  (timestamp + (60*1000))) // 1 minute padding (should never be that far off, but why not)
                    .orderBy("timestamp desc")
                    .executeSingle();
            if(bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3*60*1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                Log.w(TAG, "isNew; Old Reading");
                return false;
            }
        }
        Log.w(TAG, "isNew: New Reading");
        return true;
    }

    public static BgReading create(double raw_data, double filtered_data, Context context, Long timestamp) {
        BgReading bgReading = new BgReading();
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            Calibration calibration = Calibration.last();
            if (calibration == null) {
                Log.d(TAG,"create: No calibration yet");
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.raw_data = (raw_data / 1000);
                bgReading.filtered_data = (filtered_data / 1000);
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;
                bgReading.calibration_flag = false;

                bgReading.calculateAgeAdjustedRawValue();

                bgReading.save();
                bgReading.perform_calculations();
            } else {
                Log.d(TAG,"Calibrations, so doing everything");
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = (raw_data/1000);
                bgReading.filtered_data = (filtered_data/1000);
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;

                bgReading.calculateAgeAdjustedRawValue();

                if(calibration.check_in) {
                    double firstAdjSlope = calibration.first_slope + (calibration.first_decay * (Math.ceil(new Date().getTime() - calibration.timestamp)/(1000 * 60 * 10)));
                    double calSlope = (calibration.first_scale / firstAdjSlope)*1000;
                    double calIntercept = ((calibration.first_scale * calibration.first_intercept) / firstAdjSlope)*-1;
                    bgReading.calculated_value = (((calSlope * bgReading.raw_data) + calIntercept) - 5);

                } else {
                    BgReading lastBgReading = BgReading.last();
                    if (lastBgReading != null && lastBgReading.calibration != null) {
                        if (lastBgReading.calibration_flag == true && ((lastBgReading.timestamp + (60000 * 20)) > bgReading.timestamp) && ((lastBgReading.calibration.timestamp + (60000 * 20)) > bgReading.timestamp)) {
                            lastBgReading.calibration.rawValueOverride(BgReading.weightedAverageRaw(lastBgReading.timestamp, bgReading.timestamp, lastBgReading.calibration.timestamp, lastBgReading.age_adjusted_raw_value, bgReading.age_adjusted_raw_value), context);
                        }
                    }
                    bgReading.calculated_value = ((calibration.slope * bgReading.age_adjusted_raw_value) + calibration.intercept);
                }

                bgReading.calculated_value = Math.min(400, Math.max(40, bgReading.calculated_value));
                Log.w(TAG, "NEW VALUE CALCULATED AT: " + bgReading.calculated_value);

                bgReading.save();
                bgReading.perform_calculations();
                context.startService(new Intent(context, Notifications.class));
                BgSendQueue.addToQueue(bgReading, "create", context);
            }
        }
        Log.w("BG GSON: ",bgReading.toS());

        return bgReading;
    }

    public static String slopeArrow() {
        double slope = (float) (BgReading.activeSlope() * 60000);
        return slopeArrow(slope);
    }

    public static String slopeArrow(double slope) {
        String arrow;
        if (slope <= (-3.5)) {
            arrow = "\u21ca";
        } else if (slope <= (-2)) {
            arrow = "\u2193";
        } else if (slope <= (-1)) {
            arrow = "\u2198";
        } else if (slope <= (1)) {
            arrow = "\u2192";
        } else if (slope <= (2)) {
            arrow = "\u2197";
        } else if (slope <= (3.5)) {
            arrow = "\u2191";
        } else {
            arrow = "\u21c8";
        }
        return arrow;
    }

    public String slopeName() {
        double slope_by_minute = calculated_value_slope * 60000;
        String arrow = "NONE";
        if (slope_by_minute <= (-3.5)) {
            arrow = "DoubleDown";
        } else if (slope_by_minute <= (-2)) {
            arrow = "SingleDown";
        } else if (slope_by_minute <= (-1)) {
            arrow = "FortyFiveDown";
        } else if (slope_by_minute <= (1)) {
            arrow = "Flat";
        } else if (slope_by_minute <= (2)) {
            arrow = "FortyFiveUp";
        } else if (slope_by_minute <= (3.5)) {
            arrow = "SingleUp";
        } else if (slope_by_minute <= (40)) {
            arrow = "DoubleUp";
        }
        if(hide_slope) {
            arrow = "9";
        }
        return arrow;
    }

    public double slopefromName(String slope_name) {
        double slope_by_minute = 0;
        if (slope_name.compareTo("DoubleDown") == 0) {
            slope_by_minute = -3.5;
        } else if (slope_name.compareTo("SingleDown") == 0) {
            slope_by_minute = -2;
        } else if (slope_name.compareTo("FortyFiveDown") == 0) {
            slope_by_minute = -1;
        } else if (slope_name.compareTo("Flat") == 0) {
            slope_by_minute = 0;
        } else if (slope_name.compareTo("FortyFiveUp") == 0) {
            slope_by_minute = 2;
        } else if (slope_name.compareTo("SingleUp") == 0) {
            slope_by_minute = 2;
        } else if (slope_name.compareTo("DoubleUp") == 0) {
            slope_by_minute = 4;
        } else if (slope_name.compareTo("NOT_COMPUTABLE") == 0 || slope_name.compareTo("OUT_OF_RANGE") == 0) {
            slope_by_minute = 0;
        }
        return slope_by_minute /60000;
    }

    public static BgReading last() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            return new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
                    .orderBy("timestamp desc")
                    .executeSingle();
        }
        return null;
    }
    public static List<BgReading> latest_by_size(int number) {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static BgReading lastNoSenssor() {
        return new Select()
                .from(BgReading.class)
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static List<BgReading> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> latestUnCalculated(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> latestForGraph(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + df.format(startTime))
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> last30Minutes() {
        double timestamp = (new Date().getTime()) - (60000 * 30);
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + timestamp)
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .execute();
    }

    public static double estimated_bg(double timestamp) {
        timestamp = timestamp + BESTOFFSET;
        BgReading latest = BgReading.last();
        if (latest == null) {
            return 0;
        } else {
            return (latest.a * timestamp * timestamp) + (latest.b * timestamp) + latest.c;
        }
    }

    public static double estimated_raw_bg(double timestamp) {
        timestamp = timestamp + BESTOFFSET;
        double estimate;
        BgReading latest = BgReading.last();
        if (latest == null) {
            Log.w(TAG, "No data yet, assume perfect!");
            estimate = 160;
        } else {
            estimate = (latest.ra * timestamp * timestamp) + (latest.rb * timestamp) + latest.rc;
        }
        Log.w(TAG, "ESTIMATE RAW BG" + estimate);
        return estimate;
    }

    //*******INSTANCE METHODS***********//
    public void perform_calculations() {
        find_new_curve();
        find_new_raw_curve();
        find_slope();
    }

    public void find_slope() {
        List<BgReading> last_2 = BgReading.latest(2);
        if (last_2.size() == 2) {
            BgReading second_latest = last_2.get(1);
            double y1 = calculated_value;
            double x1 = timestamp;
            double y2 = second_latest.calculated_value;
            double x2 = second_latest.timestamp;
            if(y1 == y2) {
                calculated_value_slope = 0;
            } else {
                calculated_value_slope = (y2 - y1)/(x2 - x1);
            }
            save();
        } else if (last_2.size() == 1) {
            calculated_value_slope = 0;
            save();
        } else {
            Log.w(TAG, "NO BG? COULDNT FIND SLOPE!");
        }
    }

    public void find_new_curve() {
        List<BgReading> last_3 = BgReading.latest(3);
        if (last_3.size() == 3) {
            BgReading second_latest = last_3.get(1);
            BgReading third_latest = last_3.get(2);

            double y3 = calculated_value;
            double x3 = timestamp;
            double y2 = second_latest.calculated_value;
            double x2 = second_latest.timestamp;
            double y1 = third_latest.calculated_value;
            double x1 = third_latest.timestamp;

            a = y1/((x1-x2)*(x1-x3))+y2/((x2-x1)*(x2-x3))+y3/((x3-x1)*(x3-x2));
            b = (-y1*(x2+x3)/((x1-x2)*(x1-x3))-y2*(x1+x3)/((x2-x1)*(x2-x3))-y3*(x1+x2)/((x3-x1)*(x3-x2)));
            c = (y1*x2*x3/((x1-x2)*(x1-x3))+y2*x1*x3/((x2-x1)*(x2-x3))+y3*x1*x2/((x3-x1)*(x3-x2)));

            Log.w(TAG, "find_new_curve: BG PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);

            save();
        } else if (last_3.size() == 2) {

            Log.w(TAG, "find_new_curve: Not enough data to calculate parabolic rates - assume Linear");
                BgReading latest = last_3.get(0);
                BgReading second_latest = last_3.get(1);

                double y2 = latest.calculated_value;
                double x2 = timestamp;
                double y1 = second_latest.calculated_value;
                double x1 = second_latest.timestamp;

                if(y1 == y2) {
                    b = 0;
                } else {
                    b = (y2 - y1)/(x2 - x1);
                }
                a = 0;
                c = -1 * ((latest.b * x1) - y1);

            Log.w(TAG, ""+latest.a+"x^2 + "+latest.b+"x + "+latest.c);
                save();
            } else {
            Log.w(TAG, "find_new_curve: Not enough data to calculate parabolic rates - assume static data");
            a = 0;
            b = 0;
            c = calculated_value;

            Log.w(TAG, ""+a+"x^2 + "+b+"x + "+c);
            save();
        }
    }

    public void calculateAgeAdjustedRawValue(){
        double adjust_for = (86400000 * 1.9) - time_since_sensor_started;
        if (adjust_for > 0) {
            age_adjusted_raw_value = (((.45) * (adjust_for / (86400000 * 1.9))) * raw_data) + raw_data;
            Log.w(TAG, "calculateAgeAdjustedRawValue: RAW VALUE ADJUSTMENT FROM:" + raw_data + " TO: " + age_adjusted_raw_value);
        } else {
            age_adjusted_raw_value = raw_data;
        }
    }

    public void find_new_raw_curve() {
        List<BgReading> last_3 = BgReading.latest(3);
        if (last_3.size() == 3) {
            BgReading second_latest = last_3.get(1);
            BgReading third_latest = last_3.get(2);

            double y3 = age_adjusted_raw_value;
            double x3 = timestamp;
            double y2 = second_latest.age_adjusted_raw_value;
            double x2 = second_latest.timestamp;
            double y1 = third_latest.age_adjusted_raw_value;
            double x1 = third_latest.timestamp;

            ra = y1/((x1-x2)*(x1-x3))+y2/((x2-x1)*(x2-x3))+y3/((x3-x1)*(x3-x2));
            rb = (-y1*(x2+x3)/((x1-x2)*(x1-x3))-y2*(x1+x3)/((x2-x1)*(x2-x3))-y3*(x1+x2)/((x3-x1)*(x3-x2)));
            rc = (y1*x2*x3/((x1-x2)*(x1-x3))+y2*x1*x3/((x2-x1)*(x2-x3))+y3*x1*x2/((x3-x1)*(x3-x2)));

            Log.w(TAG, "find_new_raw_curve: RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
            save();
        } else if (last_3.size() == 2) {
            BgReading latest = last_3.get(0);
            BgReading second_latest = last_3.get(1);

            double y2 = latest.age_adjusted_raw_value;
            double x2 = timestamp;
            double y1 = second_latest.age_adjusted_raw_value;
            double x1 = second_latest.timestamp;
            if(y1 == y2) {
                rb = 0;
            } else {
                rb = (y2 - y1)/(x2 - x1);
            }
            ra = 0;
            rc = -1 * ((latest.rb * x1) - y1);

            Log.w(TAG, "find_new_raw_curve: Not enough data to calculate parabolic rates - assume Linear data");

            Log.w(TAG, "RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
            save();
        } else {
            Log.w(TAG, "find_new_raw_curve: Not enough data to calculate parabolic rates - assume static data");
            BgReading latest_entry = BgReading.lastNoSenssor();
            ra = 0;
            rb = 0;
            if (latest_entry != null) {
                rc = latest_entry.age_adjusted_raw_value;
            } else {
                rc = 105;
            }

            save();
        }
    }
    public static double weightedAverageRaw(double timeA, double timeB, double calibrationTime, double rawA, double rawB) {
        double relativeSlope = (rawB -  rawA)/(timeB - timeA);
        double relativeIntercept = rawA - (relativeSlope * timeA);
        return ((relativeSlope * calibrationTime) + relativeIntercept);
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    public String noiseValue() {
        if(noise == null || noise.compareTo("") == 0) {
            return "1";
        } else {
            return String.valueOf(noise);
        }
    }
    
    // Should that be combined with noiseValue?
    private Boolean Unclear() {
        Log.e(TAG_ALERT, "Unclear filtered_data=" + filtered_data + " raw_data=" + raw_data);
        if (raw_data > filtered_data * 1.3 || raw_data < filtered_data * 0.7) {
            return true;
        }
        return false;
    }

    /*
     * returns the time (in ms) that the state is not clear and no alerts should work
     * The base of the algorithm is that any period can be bad or not. bgReading.Unclear() tells that.
     * a non clear bgReading means MAX_INFLUANCE time after it we are in a bad position
     * Since this code is based on hurstics, and since times are not acurate, boundery issues can be ignored.
     *
     * interstingTime is the period to check. That is if the last period is bad, we want to know how long does it go bad...
     * */

    static final int MAX_INFLUANCE = 30 * 60000; // A bad point means data is untrusted for 30 minutes.
    private static Long getUnclearTimeHelper(List<BgReading> latest, Long interstingTime, final Long now) {

        // The code ignores missing points (that is they some times are treated as good and some times as bad.
        // If this bothers someone, I believe that the list should be filled with the missing points as good and continue to run.

        Long LastGoodTime = 0l; // 0 represents that we are not in a good part

        Long UnclearTime = 0l;
        for(BgReading bgReading : latest) {
            // going over the readings from latest to first
            if(bgReading.timestamp < now - (interstingTime + MAX_INFLUANCE)) {
                // Some readings are missing, we can stop checking
                break;
            }
            if(bgReading.timestamp <= now - MAX_INFLUANCE && UnclearTime == 0) {
                Log.e(TAG_ALERT, "We did not have a problematic reading for MAX_INFLUANCE time, so now all is well");
                return 0l;

            }
            if (bgReading.Unclear()) {
                // here we assume that there are no missing points. Missing points might join the good and bad values as well...
                // we should have checked if we have a period, but it is hard to say how to react to them.
                Log.e(TAG_ALERT, "We have a bad reading, so setting UnclearTime to " + bgReading.timestamp);
                UnclearTime = bgReading.timestamp;
                LastGoodTime = 0l;
            } else {
                if (LastGoodTime == 0l) {
                    Log.e(TAG_ALERT, "We are starting a good period at "+ bgReading.timestamp);
                    LastGoodTime = bgReading.timestamp;
                } else {
                    // we have some good period, is it good enough?
                    if(LastGoodTime - bgReading.timestamp >= MAX_INFLUANCE) {
                        Log.e(TAG_ALERT, "We have a good period from " + bgReading.timestamp + " to " + LastGoodTime + "returning " + (now - UnclearTime +60000));
                        return now - UnclearTime + 5 *60000;
                    }
                }
            }
        }
        // if we are here, we have a problem... or not.
        if(UnclearTime == 0l) {
            Log.e(TAG_ALERT, "Since we did not find a good period, but we also did not find a single bad value, we assume things are good");
            return 0l;
        }
        Log.e(TAG_ALERT, "We scanned all over, but could not find a good period. we have a bad value, so assuming that the whole period is bad" +
                " returning " + interstingTime);
        // Note that we might now have all the points, and in this case, since we don't have a good period I return a bad period.
        return interstingTime;

    }

    // This is to enable testing of the function, by passing different values
    public static Long getUnclearTime(Long interstingTime) {
        List<BgReading> latest = BgReading.latest((interstingTime.intValue() + MAX_INFLUANCE)/ 60000 /5 );
        if (latest == null) {
            return 0L;
        }
        final Long now = new Date().getTime();
        return getUnclearTimeHelper(latest, interstingTime, now);

    }

    public static Long getTimeSinceLastReading() {
        BgReading bgReading = BgReading.last();
        if (bgReading != null) {
            return (new Date().getTime() - bgReading.timestamp);
        }
        return (long) 0;
    }

    // the input of this function is a string. each char can be g(=good) or b(=bad) or s(=skip, point unmissed).
    static List<BgReading> createlatestTest(String input, Long now) {
        List<BgReading> out = new LinkedList<BgReading> ();
        char[] chars=  input.toCharArray();
        for(int i=0; i < chars.length; i++) {
            BgReading bg = new BgReading();
            bg.timestamp = now - i * 5 * 60000;
            bg.raw_data = 150;
            if(chars[i] == 'g') {
                bg.filtered_data = 151;
            } else if (chars[i] == 'b') {
                bg.filtered_data = 130;
            } else {
                continue;
            }
            out.add(bg);
        }
        return out;


    }
    static void TestgetUnclearTime(String input, Long interstingTime, Long expectedResult) {
        final Long now = new Date().getTime();
        List<BgReading> readings = createlatestTest(input, now);
        Long result = getUnclearTimeHelper(readings, interstingTime * 60000, now);
        if (result == expectedResult * 60000) {
            Log.e(TAG_ALERT, "Test passed");
        } else {
            Log.e(TAG_ALERT, "Test failed expectedResult = " + expectedResult + " result = "+ result /5 / 60000);
        }

    }

    public static void TestgetUnclearTimes() {
        TestgetUnclearTime("gggggggggggggggggggggggg", 90l, 0l * 5);
        TestgetUnclearTime("bggggggggggggggggggggggg", 90l, 1l * 5);
        TestgetUnclearTime("bbgggggggggggggggggggggg", 90l, 2l *5 );
        TestgetUnclearTime("gbgggggggggggggggggggggg", 90l, 2l * 5);
        TestgetUnclearTime("gbgggbggbggbggbggbggbgbg", 90l, 18l * 5);
        TestgetUnclearTime("bbbgggggggbbgggggggggggg", 90l, 3l * 5);
        TestgetUnclearTime("ggggggbbbbbbgggggggggggg", 90l, 0l * 5);
        TestgetUnclearTime("ggssgggggggggggggggggggg", 90l, 0l * 5);
        TestgetUnclearTime("ggssbggssggggggggggggggg", 90l, 5l * 5);
        TestgetUnclearTime("bb",                       90l, 18l * 5);

        // intersting time is 2 minutes, we should always get 0 (in 5 minutes units
        TestgetUnclearTime("gggggggggggggggggggggggg", 2l, 0l  * 5);
        TestgetUnclearTime("bggggggggggggggggggggggg", 2l, 2l);
        TestgetUnclearTime("bbgggggggggggggggggggggg", 2l, 2l);
        TestgetUnclearTime("gbgggggggggggggggggggggg", 2l, 2l);
        TestgetUnclearTime("gbgggbggbggbggbggbggbgbg", 2l, 2l);

        // intersting time is 10 minutes, we should always get 0 (in 5 minutes units
        TestgetUnclearTime("gggggggggggggggggggggggg", 10l, 0l  * 5);
        TestgetUnclearTime("bggggggggggggggggggggggg", 10l, 1l * 5);
        TestgetUnclearTime("bbgggggggggggggggggggggg", 10l, 2l * 5);
        TestgetUnclearTime("gbgggggggggggggggggggggg", 10l, 2l * 5);
        TestgetUnclearTime("gbgggbggbggbggbggbggbgbg", 10l, 2l * 5);
        TestgetUnclearTime("bbbgggggggbbgggggggggggg", 10l, 2l * 5);
        TestgetUnclearTime("ggggggbbbbbbgggggggggggg", 10l, 0l * 5);
        TestgetUnclearTime("ggssgggggggggggggggggggg", 10l, 0l * 5);
        TestgetUnclearTime("ggssbggssggggggggggggggg", 10l, 2l * 5);
        TestgetUnclearTime("bb",                       10l, 2l * 5);
    }

}
