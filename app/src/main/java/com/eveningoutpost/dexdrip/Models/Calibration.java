package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by stephenblack on 10/29/14.
 */
@Table(name = "Calibration", id = BaseColumns._ID)
public class Calibration extends Model {
    private final static String TAG = Calibration.class.getSimpleName();

    @Expose
    @Column(name = "timestamp", index = true)
    public double timestamp;

    @Expose
    @Column(name = "sensor_age_at_time_of_estimation")
    public double sensor_age_at_time_of_estimation;

    @Column(name = "sensor", index = true)
    public Sensor sensor;


    @Expose
    @Column(name = "bg")
    public double bg;

    @Expose
    @Column(name = "raw_value")
    public double raw_value;
//
//    @Expose
//    @Column(name = "filtered_value")
//    public double filtered_value;

    @Expose
    @Column(name = "adjusted_raw_value")
    public double adjusted_raw_value;

    @Expose
    @Column(name = "sensor_confidence")
    public double sensor_confidence;

    @Expose
    @Column(name = "slope_confidence")
    public double slope_confidence;

    @Expose
    @Column(name = "raw_timestamp")
    public double raw_timestamp;

    @Expose
    @Column(name = "slope")
    public double slope;

    @Expose
    @Column(name = "intercept")
    public double intercept;

    @Expose
    @Column(name = "distance_from_estimate")
    public double distance_from_estimate;

    @Expose
    @Column(name = "estimate_raw_at_time_of_calibration")
    public double estimate_raw_at_time_of_calibration;

    @Expose
    @Column(name = "estimate_bg_at_time_of_calibration")
    public double estimate_bg_at_time_of_calibration;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    @Expose
    @Column(name = "sensor_uuid", index = true)
    public String sensor_uuid;

    public static void initialCalibration(int bg1, int bg2, Context context) {
        CalibrationRequest.clearAll();
        List<Calibration> pastCalibrations = Calibration.allForSensor();
        if (pastCalibrations != null) {
            for(Calibration calibration : pastCalibrations){
                calibration.slope_confidence = 0;
                calibration.sensor_confidence = 0;
                calibration.save();
            }
        }
        Calibration higherCalibration = new Calibration();
        Calibration lowerCalibration = new Calibration();
        Sensor sensor = Sensor.currentSensor();
        List<BgReading> bgReadings = BgReading.latest_by_size(2);
        BgReading bgReading1 = bgReadings.get(0);
        BgReading bgReading2 = bgReadings.get(1);
        BgReading highBgReading;
        BgReading lowBgReading;
        int higher_bg;
        int lower_bg;
        if (bg1 > bg2) {
            higher_bg = bg1;
            lower_bg = bg2;
        } else if (bg2 > bg1) {
            higher_bg = bg2;
            lower_bg = bg1;
        } else {
            higher_bg = bg1;
            lower_bg = bg1 - 1;
        }

        if (bgReading1.raw_data > bgReading2.raw_data) {
            highBgReading = bgReading1;
            lowBgReading = bgReading2;
        } else if (bgReading2.raw_data  > bgReading1.raw_data ) {
            highBgReading = bgReading2;
            lowBgReading = bgReading1;
        } else {
            highBgReading = bgReading2;
            lowBgReading = bgReading1;
        }

        int lowerAdjust = 0;
        if (highBgReading.age_adjusted_raw_value == lowBgReading.age_adjusted_raw_value) { lowerAdjust = 2; }
        higherCalibration.bg = higher_bg;
        higherCalibration.slope = 0;
        higherCalibration.intercept = higher_bg;
        higherCalibration.sensor = sensor;
        higherCalibration.estimate_raw_at_time_of_calibration = highBgReading.age_adjusted_raw_value;
        higherCalibration.raw_value = highBgReading.raw_data;
        higherCalibration.save();

        lowerCalibration.bg = lower_bg;
        lowerCalibration.slope = 0;
        lowerCalibration.intercept = lower_bg;
        lowerCalibration.sensor = sensor;
        lowerCalibration.estimate_raw_at_time_of_calibration = lowBgReading.age_adjusted_raw_value - lowerAdjust;
        lowerCalibration.raw_value = lowBgReading.raw_data;
        lowerCalibration.save();

        highBgReading.calculated_value = higher_bg;
        highBgReading.calibration_flag = true;
        highBgReading.calibration = higherCalibration;
        highBgReading.save();
        higherCalibration.raw_timestamp = highBgReading.timestamp;
        higherCalibration.save();

        lowBgReading.calculated_value = lower_bg;
        lowBgReading.calibration_flag = true;
        lowBgReading.calibration = lowerCalibration;
        lowBgReading.save();
        lowerCalibration.raw_timestamp = lowBgReading.timestamp;
        lowerCalibration.save();

        highBgReading.find_new_curve();
        highBgReading.find_new_raw_curve();
        lowBgReading.find_new_curve();
        lowBgReading.find_new_raw_curve();

        List<Calibration> calibrations = new ArrayList<Calibration>();
        calibrations.add(lowerCalibration);
        calibrations.add(higherCalibration);

        for(Calibration calibration : calibrations) {
            calibration.timestamp = new Date().getTime();
            calibration.sensor_uuid = sensor.uuid;
            calibration.slope_confidence = .5;
            calibration.distance_from_estimate = 0;
            calibration.sensor_confidence = ((-0.0018 * calibration.bg * calibration.bg) + (0.6657 * calibration.bg) + 36.7505) / 100;

            calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
            calibration.uuid = UUID.randomUUID().toString();
            calibration.save();

            calculate_w_l_s();
            adjustRecentBgReadings();
            CalibrationSendQueue.addToQueue(calibration, context);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();
            Log.w("CALIBRATION: ", calibration.toS());
            CalibrationRequest.createOffset(calibration.bg, 45);
        }
        Notifications.notificationSetter(context);
    }

    public static Calibration create(int bg, Context context) {
        CalibrationRequest.clearAll();
        Calibration calibration = new Calibration();
        Sensor sensor = Sensor.currentSensor();

        if (sensor != null) {
            BgReading bgReading = BgReading.last();
            if (bgReading != null) {
                calibration.sensor = sensor;
                calibration.bg = bg;

                bgReading.calibration_flag = true;
                bgReading.save();
                BgSendQueue.addToQueue(bgReading, "update", context);

                calibration.timestamp = new Date().getTime();
                calibration.raw_value = bgReading.raw_data;
                calibration.adjusted_raw_value = bgReading.age_adjusted_raw_value;
                calibration.sensor_uuid = sensor.uuid;

                double slope_percentage = ((4 - Math.abs((bgReading.calculated_value_slope) * 60000))/4);
                if (slope_percentage > 1) { slope_percentage = 1; }
                else if (slope_percentage < 0) { slope_percentage = 0; }
                calibration.slope_confidence = slope_percentage;

                double estimated_raw_bg = BgReading.estimated_raw_bg(new Date().getTime());
                calibration.raw_timestamp = bgReading.timestamp;
                if (Math.abs(estimated_raw_bg - bgReading.age_adjusted_raw_value) > 20) {
                    calibration.estimate_raw_at_time_of_calibration = bgReading.age_adjusted_raw_value;
                    Log.w("Using previous RAW: ", "" + estimated_raw_bg);
                } else {
                    calibration.estimate_raw_at_time_of_calibration = estimated_raw_bg;
                    Log.w("Using estimated RAW: ", "" + estimated_raw_bg);
                }
                calibration.distance_from_estimate = Math.abs(calibration.bg - estimated_raw_bg);
                calibration.sensor_confidence = ((-0.0018 * bg * bg) + (0.6657 * bg) + 36.7505) / 100;
                if (calibration.sensor_confidence <= 0) { calibration.sensor_confidence = 0; }
                calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
                calibration.uuid = UUID.randomUUID().toString();
                calibration.save();

                calculate_w_l_s();
                adjustRecentBgReadings();
                CalibrationSendQueue.addToQueue(calibration, context);
                Notifications.notificationSetter(context);
                Calibration.requestCalibrationIfRangeTooNarrow();
            }
        } else {
            Log.w("CALIBRATION", "No sensor, cant save!");
        }
        return Calibration.last();
    }

    public static Calibration last() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Calibration first() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID asc")
                .executeSingle();
    }
    public static double max() {
        Sensor sensor = Sensor.currentSensor();
        Calibration calibration = new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 5)))
                .orderBy("bg asc")
                .executeSingle();
        return calibration.bg;
    }

    public static double min() {
        Sensor sensor = Sensor.currentSensor();
        Calibration calibration = new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 5)))
                .orderBy("bg asc")
                .executeSingle();
        return calibration.bg;
    }

    public static List<Calibration> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .limit(number)
                .execute();
    }

    public static List<Calibration> allForSensor() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .orderBy("_ID desc")
                .execute();
    }

    public static List<Calibration> allForSensorInLastFiveDays() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 5)))
                .orderBy("_ID desc")
                .execute();
    }

    public static void calculate_w_l_s() {
        if (Sensor.isActive()) {
            double l = 0;
            double m = 0;
            double n = 0;
            double p = 0;
            double q = 0;
            double w;
//            Sensor sensor = Sensor.currentSensor();
//            List<Calibration> calibrations = calibrations_for_sensor(sensor);
            List<Calibration> calibrations = allForSensorInLastFiveDays(); //Lets see the impact of 5 days of history, still may be a bit much!
            if (calibrations.size() == 1) {
                Calibration calibration = Calibration.last();
                calibration.intercept = calibration.bg;
                calibration.slope = 0;
                calibration.save();
            } else {
                Log.w(TAG, "CALIBRATIONS USED: " + calibrations.size());
                for (Calibration calibration : calibrations) {
                    Log.w(TAG, "Calibration estimate: " + calibration.estimate_raw_at_time_of_calibration);

                    Log.w(TAG, "Calibration bg: " + calibration.bg);
                    w = calibration.calculateWeight();
                    Log.w(TAG, "=====CALIBRATIONS WEIGHT: " + ""+w);
                    l += (w);
                    m += (w * calibration.estimate_raw_at_time_of_calibration);
                    n += (w * calibration.estimate_raw_at_time_of_calibration * calibration.estimate_raw_at_time_of_calibration);
                    p += (w * calibration.bg);
                    q += (w * calibration.estimate_raw_at_time_of_calibration * calibration.bg);
                }
                Calibration last_calibration = Calibration.last();
                w = (last_calibration.calculateWeight() * (calibrations.size() * 0.15));
                l += (w);
                Log.w(TAG, "=====CALIBRATIONS WEIGHT: " + ""+w);
                m += (w * last_calibration.estimate_raw_at_time_of_calibration);
                n += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.estimate_raw_at_time_of_calibration);
                p += (w * last_calibration.bg);
                q += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.bg);

                double d = (l * n) - (m * m);
                Calibration calibration = Calibration.last();
                calibration.intercept = ((n * p) - (m * q)) / d;
                calibration.slope = ((l * q) - (m * p)) / d;
                if (calibration.slope < 0.5) {
                    calibration.slope = calibration.slopeOOBHandler();
                    if (calibration.slope == 0) { calibration.slope = 0.5; }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 35);
                }
                if (calibration.slope > 1.4) {
                    calibration.slope = calibration.slopeOOBHandler();
                    if (calibration.slope == 0) { calibration.slope = 1.4; }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 35);
                }
                Log.w(TAG, "Calculated Calibration Slope: " + calibration.slope);
                Log.w(TAG, "Calculated Calibration intercept: " + calibration.intercept);
                calibration.save();
            }
        } else {
            Log.w(TAG, "NO Current active sensor found!!");
        }
    }
    private double slopeOOBHandler() {
        double adjustedSlope = 0;
        List<Calibration> calibrations = Calibration.latest(3);
        if (calibrations.size() == 3) {
            Calibration lastUsedCalibration = calibrations.get(1);
            if (lastUsedCalibration.slope >= 0.5 && lastUsedCalibration.slope <= 1.4 && lastUsedCalibration.distance_from_estimate < 35) {
                return lastUsedCalibration.slope;
            } else {
                if (lastUsedCalibration.sensor_age_at_time_of_estimation < (60000 * 60 * 24 * 5)) { // 432000000
                    return ((-0.048) * (lastUsedCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.12;
                } else {
                    return 0.88;
                }
            }
        } else if (calibrations.size() == 2) {
            Calibration lastUsedCalibration = calibrations.get(1);
            if (lastUsedCalibration.slope >= 0.5 && lastUsedCalibration.slope <= 1.4) {
                if (lastUsedCalibration.sensor_age_at_time_of_estimation < (60000 * 60 * 24 * 5)) { // 432000000
                    return ((-0.048) * (lastUsedCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.12;
                } else {
                    return 0.88;
                }
            }
        }
        return adjustedSlope;
    }

    private static List<Calibration> calibrations_for_sensor(Sensor sensor) {
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ?", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .orderBy("_ID desc")
                .execute();
    }

    private double calculateWeight() {
        double firstTimeStarted =   Calibration.first().sensor_age_at_time_of_estimation;
        double lastTimeStarted =   Calibration.last().sensor_age_at_time_of_estimation;
        double time_percentage = (sensor_age_at_time_of_estimation - firstTimeStarted) / (lastTimeStarted - firstTimeStarted);
        Log.w(TAG, "CALIBRATIONS TIME PERCENTAGE WEIGHT: " + ""+(time_percentage + 0.1));
        time_percentage = (time_percentage + .01);
        if(sensor_confidence == 0 && slope_confidence == 0){ return 0; }
        if (time_percentage > 1) { time_percentage = 1; }
        double calculated_confidence = (((((slope_confidence + sensor_confidence) * (time_percentage))) / 2) * 100);
        if (calculated_confidence <= 1) { return 1; }
        return calculated_confidence;
    }

    public static void adjustRecentBgReadings() {
        List<Calibration> calibrations = Calibration.latest(3);
        List<BgReading> bgReadings = BgReading.latest(30);
        if (calibrations.size() == 3) {
            int denom = bgReadings.size();
            Calibration latestCalibration = calibrations.get(0);
            int i = 0;
            for (BgReading bgReading : bgReadings) {
                double oldYValue = bgReading.calculated_value;
                double newYvalue = (bgReading.age_adjusted_raw_value * latestCalibration.slope) + latestCalibration.intercept;
                bgReading.calculated_value = ((newYvalue * (denom - i)) + (oldYValue * ( i ))) / denom;
                bgReading.save();
                i += 1;
            }
        } else if (calibrations.size() == 2) {
            Calibration latestCalibration = calibrations.get(0);
            for (BgReading bgReading : bgReadings) {
                double newYvalue = (bgReading.age_adjusted_raw_value * latestCalibration.slope) + latestCalibration.intercept;
                bgReading.calculated_value = newYvalue;
                bgReading.save();

            }
        }
        bgReadings.get(0).find_new_raw_curve();
        bgReadings.get(0).find_new_curve();
    }

    public void overrideCalibration(int value, Context context) {
        bg = value;
        estimate_raw_at_time_of_calibration = raw_value;
        save();
        calculate_w_l_s();
        adjustRecentBgReadings();
        CalibrationSendQueue.addToQueue(this, context);
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    public void rawValueOverride(double rawValue, Context context) {
        estimate_bg_at_time_of_calibration = rawValue;
        save();
        calculate_w_l_s();
        adjustRecentBgReadings();
        CalibrationSendQueue.addToQueue(this, context);
    }

    public static void requestCalibrationIfRangeTooNarrow() {
        double max = Calibration.max();
        double min = Calibration.min();
        if ((max - min) < 50) {
            double avg = ((min + max) / 2);
            double dist = max - avg;
            CalibrationRequest.createOffset(avg, dist + 30);
        }
    }
}
