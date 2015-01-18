package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Table(name = "BgReadings", id = BaseColumns._ID)
public class BgReading extends Model {
    private final static String TAG = BgReading.class.getSimpleName();
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
        double slope = (2 * bgReading.a * (new Date().getTime() + BESTOFFSET)) + bgReading.b;
        Log.w(TAG, "ESTIMATE SLOPE" + slope);
        return slope;
    }
    public double staticSlope() {
        double slope = (2 * this.a * this.timestamp) + this.b;
        Log.w(TAG, "ESTIMATE SLOPE" + slope);
        return slope;
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
    public static BgReading create(double raw_data, Context context, Long timestamp) {
        BgReading bgReading = new BgReading();
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            Calibration calibration = Calibration.last();
            if (calibration == null) {
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.raw_data = (raw_data / 1000);
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;
                bgReading.calibration_flag = false;

                bgReading.calculateAgeAdjustedRawValue();

                bgReading.save();
                bgReading.perform_calculations();
            } else {

                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = (raw_data/1000);
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;

                bgReading.calculateAgeAdjustedRawValue();

                if(calibration.check_in) {
                    double firstAdjSlope = calibration.first_slope + (calibration.first_decay * (Math.ceil(new Date().getTime() - calibration.timestamp)/(1000 * 60 * 10)));
//                    double secondAdjSlope = calibration.first_slope + (calibration.first_decay * ((new Date().getTime() - calibration.timestamp)/(1000 * 60 * 10)));
                    double calSlope = (calibration.first_scale / firstAdjSlope)*1000;
                    double calIntercept = ((calibration.first_scale * calibration.first_intercept) / firstAdjSlope)*-1;
//                    double calSlope = ((calibration.second_scale / secondAdjSlope) + (3 * calibration.first_scale / firstAdjSlope)) * 250;
//                    double calIntercept = (((calibration.second_scale * calibration.second_intercept) / secondAdjSlope) + ((3 * calibration.first_scale * calibration.first_intercept) / firstAdjSlope)) / -4;
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
                Notifications.notificationSetter(context);
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
        } else {
            arrow = "DoubleUp";
        }
        return arrow;
    }

    public static BgReading last() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .executeSingle();
    }
    public static List<BgReading> latest_by_size(int number) {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .limit(number)
                .execute();
    }

    public static BgReading lastNoSenssor() {
        return new Select()
                .from(BgReading.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static List<BgReading> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> latestForGraph(int number, double startTime) {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);

        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + df.format(startTime))
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> last30Minutes() {
        double timestamp = (new Date().getTime()) - (60000 * 30);
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + timestamp)
                .orderBy("_ID desc")
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

            Log.w(TAG, "BG PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);

            save();
        } else if (last_3.size() == 2) {

            Log.w(TAG, "Not enough data to calculate parabolic rates - assume Linear");
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
            Log.w(TAG, "Not enough data to calculate parabolic rates - assume static data");
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
            Log.w("RAW VALUE ADJUSTMENT: ", "FROM:" + raw_data + " TO: " + age_adjusted_raw_value);
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

            Log.w(TAG, "RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
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

            Log.w(TAG, "Not enough data to calculate parabolic rates - assume Linear data");

            Log.w(TAG, "RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
            save();
        } else {
            Log.w(TAG, "Not enough data to calculate parabolic rates - assume static data");
            BgReading latest_entry = BgReading.lastNoSenssor();
            ra = 0;
            rb = 0;
            rc = latest_entry.age_adjusted_raw_value;

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
}
