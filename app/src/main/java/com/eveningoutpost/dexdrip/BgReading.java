package com.eveningoutpost.dexdrip;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
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
    public final static double SLOPEREADAHEAD = (60000 * 5); // Forcast the rate of change 5 minutes out!

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

    public static double activeSlope() {
        BgReading bgReading = BgReading.lastNoSenssor();
        double slope = (2 * bgReading.a * (new Date().getTime() + BESTOFFSET)) + bgReading.b;
        Log.w(TAG, "ESTIMATE SLOPE" + slope);
        return slope;
    }

    public static String activePrediction() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        BgReading bgReading = BgReading.lastNoSenssor();
        if (bgReading != null) {
            double time = new Date().getTime() + BESTOFFSET;
            double estimate = ((bgReading.a * time * time) + (bgReading.b * time) + bgReading.c);
            Log.w(TAG, "ESTIMATE BG" + estimate);
            return df.format(estimate);
        } else {
            return "120";
        }
    }

    //*******CLASS METHODS***********//
    public static BgReading create(double raw_data) {
        BgReading bgReading = new BgReading();
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            Calibration calibration = Calibration.last();
            if (calibration == null) {
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.raw_data = (raw_data / 1000);
                bgReading.timestamp = new Date().getTime();
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;


                //TODO: THIS IS A BIG SILLY IDEA, THIS WILL HAVE TO CHANGE ONCE WE GET SOME REAL DATA FROM THE START OF SENSOR LIFE
                double adjust_for = (86400000 * 1.8) - bgReading.time_since_sensor_started;
                if (adjust_for > 0) {
                    bgReading.age_adjusted_raw_value = ((50 / 20) * (adjust_for / (86400000 * 1.8))) * (raw_data / 1000);
                    Log.w("RAW VALUE ADJUSTMENT: ", "FROM:" + raw_data + " TO: " + bgReading.age_adjusted_raw_value);
                } else {
                    bgReading.age_adjusted_raw_value = (raw_data / 1000);
                }

                bgReading.save();

                bgReading.perform_calculations();
            } else {

                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = (raw_data/1000);
                bgReading.timestamp = new Date().getTime();
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.synced = false;

                //TODO: THIS IS A BIG SILLY IDEA, THIS WILL HAVE TO CHANGE ONCE WE GET SOME REAL DATA FROM THE START OF SENSOR LIFE
                double adjust_for = (86400000 * 2.1) - bgReading.time_since_sensor_started;
                if (adjust_for > 0) {
                    bgReading.age_adjusted_raw_value = (((3 / 2) * (adjust_for / (86400000 * 2.1))) * (raw_data/1000)) + (raw_data/1000);
                    Log.w("RAW VALUE ADJUSTMENT: ", "FROM:" + (raw_data/1000) + " TO: " + bgReading.age_adjusted_raw_value);
                } else {
                    bgReading.age_adjusted_raw_value = (raw_data/1000);
                }

                bgReading.calculated_value = ((calibration.slope * bgReading.age_adjusted_raw_value) + calibration.intercept);

                Log.w(TAG, "NEW VALUE CALCULATED AT: " + bgReading.calculated_value);

                bgReading.save();

                bgReading.perform_calculations();

                BgSendQueue.addToQueue(bgReading, "create");
            }
        }
         Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        Log.w("BG GSON: ", gson.toJson(bgReading));

        return bgReading;
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

    public static List<BgReading> last24Minutes() {
        double timestamp = (new Date().getTime()) - (60000 * 25);
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + timestamp)
                .orderBy("_ID desc")
                .execute();
    }

    public static double estimated_bg(double timestamp) {
        timestamp = timestamp + BESTOFFSET;
        double estimate;
        BgReading latest = BgReading.last();
        if (latest == null) {
            Log.w(TAG, "No data yet, assume perfect!");
            estimate = 120;
        } else {
            estimate = (latest.a * timestamp * timestamp) + (latest.b * timestamp) + latest.c;
        }

        Log.w(TAG, "ESTIMATE CALC BG" + estimate);
        return estimate;
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
        Calibration calibration = Calibration.last();
        if (calibration == null) {
            Log.w(TAG, "NO CALIBRATION DATA, CANNOT CALCULATE CURRENT VALUES.");
        } else {
            calculated_value = ((calibration.slope * age_adjusted_raw_value) + calibration.intercept);
            Log.w(TAG, "NEW VALUE CALCULATED AT: " + calculated_value);
        }
        save();
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

            double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
            a = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
            b = (x3 * x3 * (y1 - y2) + x2 * x2 * (y3 - y1) + x1 * x1 * (y2 - y3)) / denom;
            c = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

            Log.w(TAG, "BG PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);

            save();
        } else if (last_3.size() == 2) {

            Log.w(TAG, "Not enough data to calculate parabolic rates - assume Linear");
                BgReading latest = last_3.get(0);
                BgReading second_latest = last_3.get(1);

                double y2 = (double)latest.calculated_value;
                double x2 = timestamp;
                double y1 = (double)second_latest.calculated_value;
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

            double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
            ra = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
            rb = (x3 * x3 * (y1 - y2) + x2 * x2 * (y3 - y1) + x1 * x1 * (y2 - y3)) / denom;
            rc = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;
            Log.w(TAG, "RAW PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);
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
            rc = -1 * ((latest.b * x1) - y1);

            Log.w(TAG, "Not enough data to calculate parabolic rates - assume Linear data");

            Log.w(TAG, "RAW PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);
            save();
        } else {
            Log.w(TAG, "Not enough data to calculate parabolic rates - assume static data");
            BgReading latest_entry = BgReading.lastNoSenssor();
            ra = 0;
            rb = 0;
            rc = latest_entry.age_adjusted_raw_value;

            Log.w(TAG, "RAW PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);
            save();
        }
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    //TODO: Add Sync instance method

}