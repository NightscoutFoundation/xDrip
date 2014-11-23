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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by stephenblack on 10/29/14.
 */
@Table(name = "CalibrationDecay", id = BaseColumns._ID)
public class CalibrationDecay extends Model {
    private final static String TAG = CalibrationDecay.class.getSimpleName();

    @Expose
    @Column(name = "timestamp", index = true)
    public double timestamp;

    @Expose
    @Column(name = "sensor_age_at_time_of_estimation")
    public double sensor_age_at_time_of_estimation;

    @Column(name = "sensor", index = true)
    public Sensor sensor;

    @Column(name = "bgReading")
    public BgReadingDecay bgReading;

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

    public static void initialCalibration(int bg1, int bg2) {
        CalibrationDecay higherCalibration = new CalibrationDecay();
        CalibrationDecay lowerCalibration = new CalibrationDecay();
        Sensor sensor = Sensor.currentSensor();
        List<BgReadingDecay> bgReadings = BgReadingDecay.latest_by_size(2);
        BgReadingDecay bgReading1 = bgReadings.get(0);
        BgReadingDecay bgReading2 = bgReadings.get(1);

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

        int lowerAdjust = 0;
        if (bgReading1.age_adjusted_raw_value == bgReading2.age_adjusted_raw_value) {
            lowerAdjust = 2;
        }
        higherCalibration.bg = higher_bg;
        higherCalibration.slope = 0;
        higherCalibration.intercept = higher_bg;
        higherCalibration.sensor = sensor;
        higherCalibration.estimate_raw_at_time_of_calibration = bgReading1.age_adjusted_raw_value;
        higherCalibration.raw_value = bgReading1.raw_data;
        higherCalibration.save();

        lowerCalibration.bg = lower_bg;
        lowerCalibration.slope = 0;
        lowerCalibration.intercept = lower_bg;
        lowerCalibration.sensor = sensor;
        lowerCalibration.estimate_raw_at_time_of_calibration = bgReading2.age_adjusted_raw_value - lowerAdjust;
        lowerCalibration.raw_value = bgReading2.raw_data;
        lowerCalibration.save();

        bgReading1.calculated_value = higher_bg;
        bgReading1.calibration_flag = true;
        bgReading1.save();
        higherCalibration.bgReading = bgReading1;
        higherCalibration.save();

        bgReading2.calculated_value = lower_bg;
        bgReading2.calibration_flag = true;
        bgReading2.save();
        lowerCalibration.bgReading = bgReading2;
        lowerCalibration.save();

        bgReading1.find_new_curve();
        bgReading1.find_new_raw_curve();
        bgReading2.find_new_curve();
        bgReading2.find_new_raw_curve();

        List<CalibrationDecay> calibrations = new ArrayList<CalibrationDecay>();
        calibrations.add(lowerCalibration);
        calibrations.add(higherCalibration);

        for(CalibrationDecay calibration : calibrations) {
            BgReadingDecay bgReading = calibration.bgReading;
            calibration.timestamp = new Date().getTime();
            calibration.sensor_uuid = sensor.uuid;
            calibration.slope_confidence = 1;
            calibration.raw_timestamp = bgReading.timestamp;
            Log.w("Using estimated RAW: ", "" + calibration.bg);
            calibration.distance_from_estimate = 0;
            calibration.sensor_confidence = ((-0.0018 * calibration.bg * calibration.bg) + (0.6657 * calibration.bg) + 36.7505) / 100;
            calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
            calibration.uuid = UUID.randomUUID().toString();
            calibration.save();

            Log.w("Sensor Confidence: ", "" + calibration.sensor_confidence);
            calculate_w_l_s();
            adjustRecentBgReadingDecays();
//            CalibrationSendQueue.addToQueue(calibration);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();
            Log.w("CALIBRATION GSON: ", gson.toJson(calibration));
            Log.w("BG GSON: ", gson.toJson(bgReading));
        }
    }



    public static CalibrationDecay create(int bg) {
        CalibrationDecay calibration = new CalibrationDecay();
        Sensor sensor = Sensor.currentSensor();

        if (sensor != null) {
            BgReadingDecay bgReading = BgReadingDecay.last();
            if (bgReading == null) {
                //TODO: add something here to handle calibration with no data to compare against
            } else {
                calibration.sensor = sensor;
                calibration.bg = bg;

                bgReading.calibration_flag = true;
                bgReading.save();
//                BgSendQueue.addToQueue(bgReading, "update");

                calibration.timestamp = new Date().getTime();
                calibration.raw_value = bgReading.raw_data;
                calibration.adjusted_raw_value = bgReading.age_adjusted_raw_value;
                calibration.sensor_uuid = sensor.uuid;

                double slope_percentage = ((4 - Math.abs((bgReading.calculated_value_slope) * 60000))/4);
                if (slope_percentage > 1) {
                    slope_percentage = 1;
                } else if (slope_percentage < 0) {
                    slope_percentage = 0;
                }
                calibration.slope_confidence = slope_percentage;

                double estimated_raw_bg = BgReadingDecay.estimated_raw_bg(new Date().getTime());
                calibration.raw_timestamp = bgReading.timestamp;
                if (Math.abs(estimated_raw_bg - bgReading.age_adjusted_raw_value) > 40) {
                    calibration.estimate_raw_at_time_of_calibration = bgReading.age_adjusted_raw_value;
                    Log.w("Using previous RAW: ", "" + estimated_raw_bg);
                } else {
                    calibration.estimate_raw_at_time_of_calibration = estimated_raw_bg;
                    Log.w("Using estimated RAW: ", "" + estimated_raw_bg);
                }
                double distance = calibration.bg - estimated_raw_bg;
                calibration.distance_from_estimate = (distance < 0) ? -distance : distance;
                calibration.sensor_confidence = ((-0.0018 * bg * bg) + (0.6657 * bg) + 36.7505) / 100;
                calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
                calibration.uuid = UUID.randomUUID().toString();
                calibration.save();

                calculate_w_l_s();
                adjustRecentBgReadingDecays();
//                CalibrationSendQueue.addToQueue(calibration);
            }
        } else {
            Log.w("CALIBRATION", "No sesnor, cant save!");
        }

        return calibration;
    }

    public static CalibrationDecay last() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(CalibrationDecay.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .executeSingle();
    }
    public static CalibrationDecay first() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(CalibrationDecay.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID asc")
                .executeSingle();
    }

    public static boolean can_calibrate() {
        BgReadingDecay bgReading = BgReadingDecay.last();
        double time = new Date().getTime();
        if (Sensor.isActive()) {
            if (time - bgReading.timestamp < 900) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static List<CalibrationDecay> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(CalibrationDecay.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("_ID desc")
                .limit(number)
                .execute();

    }

    public static void calculate_w_l_s() {
        if (Sensor.isActive()) {
//            double l = 0;
//            double m = 0;
//            double n = 0;
//            double p = 0;
//            double q = 0;
//            double w = 0;
//            Sensor sensor = Sensor.currentSensor();
//            List<CalibrationDecay> calibrations = calibrations_for_sensor(sensor);
//            if (calibrations.size() == 1) {
//                CalibrationDecay calibration = CalibrationDecay.last();
//                calibration.intercept = calibration.bg;
//                calibration.slope = 0;
//                calibration.save();
//            } else {
//                Log.w(TAG, "CALIBRATIONS USED: " + calibrations.size());
//                for (CalibrationDecay calibration : calibrations) {
//                    Log.w(TAG, "Calibration estimate: " + calibration.estimate_raw_at_time_of_calibration);
//
//                    Log.w(TAG, "Calibration bg: " + calibration.bg);
//                    w = calibration.calculateWeight();
//                    l += (w * (calibrations.size() * 0.2));
//                    m += (w * calibration.estimate_raw_at_time_of_calibration);
//                    n += (w * calibration.estimate_raw_at_time_of_calibration * calibration.estimate_raw_at_time_of_calibration);
//                    p += (w * calibration.bg);
//                    q += (w * calibration.estimate_raw_at_time_of_calibration * calibration.bg);
//                }
//                CalibrationDecay last_calibration = CalibrationDecay.last();
//                w = last_calibration.calculateWeight();
//                l += (w);
//                m += (w * last_calibration.estimate_raw_at_time_of_calibration);
//                n += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.estimate_raw_at_time_of_calibration);
//                p += (w * last_calibration.bg);
//                q += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.bg);
//
//                double d = (l * n) - (m * m);
//                CalibrationDecay calibration = CalibrationDecay.last();
//                calibration.intercept = ((n * p) - (m * q)) / d;
//                calibration.slope = ((l * q) - (m * p)) / d;
//
//                Log.w(TAG, "Calculated Calibration Slope: " + calibration.slope);
//                Log.w(TAG, "Calculated Calibration intercept: " + calibration.intercept);
//                calibration.save();
            CalibrationDecay last_calibration = CalibrationDecay.last();
//            double slope = () / (calibration.timestamp - sensor.started_at);
            last_calibration.slope =  0.689;
            last_calibration.intercept = last_calibration.bg - (last_calibration.estimate_raw_at_time_of_calibration * 0.689);
            last_calibration.save();


        } else {
            Log.w(TAG, "NO Current active sensor found!!");
        }
    }

    private static List<CalibrationDecay> calibrations_for_sensor(Sensor sensor) {
        return new Select()
                .from(CalibrationDecay.class)
                .where("Sensor = ?", sensor.getId())
                .orderBy("_ID desc")
                .execute();
    }

    private double calculateWeight() {
        double time_percentage = sensor_age_at_time_of_estimation / CalibrationDecay.first().sensor_age_at_time_of_estimation;
        return  ((((slope_confidence + sensor_confidence) * time_percentage)) / 2);
    }

    public static void adjustRecentBgReadingDecays() {
        List<CalibrationDecay> calibrations = CalibrationDecay.latest(3);
        List<BgReadingDecay> bgReadings = BgReadingDecay.latest(20);
        if (calibrations.size() == 3) {
            int denom = bgReadings.size();
            CalibrationDecay latestCalibration = calibrations.get(0);
            int i = 0;
            for (BgReadingDecay bgReading : bgReadings) {
                double oldYValue = bgReading.calculated_value;
                double newYvalue = (bgReading.age_adjusted_raw_value * latestCalibration.slope) + latestCalibration.intercept;
                bgReading.calculated_value = ((newYvalue * (denom - i)) + (oldYValue * ( i ))) / denom;
                bgReading.save();
                i += 1;
            }
        }
        bgReadings.get(0).find_new_raw_curve();
        bgReadings.get(0).find_new_curve();
    }

    public void overrideCalibration(int value) {
        bg = value;
        estimate_raw_at_time_of_calibration = raw_value;
        save();
        calculate_w_l_s();
        adjustRecentBgReadingDecays();
//        CalibrationSendQueue.addToQueue(this);
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