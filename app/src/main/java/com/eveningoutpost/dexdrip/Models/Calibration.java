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
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.CalSubrecord;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
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

    @Expose
    @Column(name = "possible_bad")
    public Boolean possible_bad;

    @Expose
    @Column(name = "check_in")
    public boolean check_in;

    @Expose
    @Column(name = "first_decay")
    public double first_decay;

    @Expose
    @Column(name = "second_decay")
    public double second_decay;

    @Expose
    @Column(name = "first_slope")
    public double first_slope;

    @Expose
    @Column(name = "second_slope")
    public double second_slope;

    @Expose
    @Column(name = "first_intercept")
    public double first_intercept;

    @Expose
    @Column(name = "second_intercept")
    public double second_intercept;

    @Expose
    @Column(name = "first_scale")
    public double first_scale;

    @Expose
    @Column(name = "second_scale")
    public double second_scale;

    public static void initialCalibration(double bg1, double bg2, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") != 0 ) {
            bg1 = bg1 * Constants.MMOLL_TO_MGDL;
            bg2 = bg2 * Constants.MMOLL_TO_MGDL;
        }
        clear_all_existing_calibrations();

        Calibration higherCalibration = new Calibration();
        Calibration lowerCalibration = new Calibration();
        Sensor sensor = Sensor.currentSensor();
        List<BgReading> bgReadings = BgReading.latest_by_size(2);
        BgReading bgReading1 = bgReadings.get(0);
        BgReading bgReading2 = bgReadings.get(1);
        BgReading highBgReading;
        BgReading lowBgReading;
        double higher_bg = Math.max(bg1, bg2);
        double lower_bg = Math.min(bg1, bg2);

        if (bgReading1.raw_data > bgReading2.raw_data) {
            highBgReading = bgReading1;
            lowBgReading = bgReading2;
        } else {
            highBgReading = bgReading2;
            lowBgReading = bgReading1;
        }

        higherCalibration.bg = higher_bg;
        higherCalibration.slope = 1;
        higherCalibration.intercept = higher_bg;
        higherCalibration.sensor = sensor;
        higherCalibration.estimate_raw_at_time_of_calibration = highBgReading.age_adjusted_raw_value;
        higherCalibration.adjusted_raw_value = highBgReading.age_adjusted_raw_value;
        higherCalibration.raw_value = highBgReading.raw_data;
        higherCalibration.raw_timestamp = highBgReading.timestamp;
        higherCalibration.save();

        highBgReading.calculated_value = higher_bg;
        highBgReading.calibration_flag = true;
        highBgReading.calibration = higherCalibration;
        highBgReading.save();
        higherCalibration.save();

        lowerCalibration.bg = lower_bg;
        lowerCalibration.slope = 1;
        lowerCalibration.intercept = lower_bg;
        lowerCalibration.sensor = sensor;
        lowerCalibration.estimate_raw_at_time_of_calibration = lowBgReading.age_adjusted_raw_value;
        lowerCalibration.adjusted_raw_value = lowBgReading.age_adjusted_raw_value;
        lowerCalibration.raw_value = lowBgReading.raw_data;
        lowerCalibration.raw_timestamp = lowBgReading.timestamp;
        lowerCalibration.save();

        lowBgReading.calculated_value = lower_bg;
        lowBgReading.calibration_flag = true;
        lowBgReading.calibration = lowerCalibration;
        lowBgReading.save();
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
            calibration.check_in = false;
            calibration.sensor_confidence = ((-0.0018 * calibration.bg * calibration.bg) + (0.6657 * calibration.bg) + 36.7505) / 100;

            calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
            calibration.uuid = UUID.randomUUID().toString();
            calibration.save();

            calculate_w_l_s();
            CalibrationSendQueue.addToQueue(calibration, context);
        }
        adjustRecentBgReadings(5);
        CalibrationRequest.createOffset(lowerCalibration.bg, 35);
        Notifications.notificationSetter(context);
    }

    //Create Calibration Checkin
    public static void create(CalRecord[] calRecords, long addativeOffset, Context context) { create(calRecords, context, false, addativeOffset); }
    public static void create(CalRecord[] calRecords, Context context) { create(calRecords, context, false, 0); }
    public static void create(CalRecord[] calRecords, Context context, boolean override, long addativeOffset) {
        //TODO: Change calibration.last and other queries to order calibrations by timestamp rather than ID
        Log.w("CALIBRATION-CHECK-IN: ", "Creating Calibration Record");
        Sensor sensor = Sensor.currentSensor();
        CalRecord firstCalRecord = calRecords[0];
        CalRecord secondCalRecord = calRecords[0];
//        CalRecord secondCalRecord = calRecords[calRecords.length - 1];
        //TODO: Figgure out how the ratio between the two is determined
        double calSlope = ((secondCalRecord.getScale() / secondCalRecord.getSlope()) + (3 * firstCalRecord.getScale() / firstCalRecord.getSlope())) * 250;

        double calIntercept = (((secondCalRecord.getScale() * secondCalRecord.getIntercept()) / secondCalRecord.getSlope()) + ((3 * firstCalRecord.getScale() * firstCalRecord.getIntercept()) / firstCalRecord.getSlope())) / -4;
        if (sensor != null) {
            for(int i = 0; i < firstCalRecord.getCalSubrecords().length - 1; i++) {
                if (((firstCalRecord.getCalSubrecords()[i] != null && Calibration.is_new(firstCalRecord.getCalSubrecords()[i], addativeOffset))) || (i == 0 && override)) {
                    CalSubrecord calSubrecord = firstCalRecord.getCalSubrecords()[i];

                    Calibration calibration = new Calibration();
                    calibration.bg = calSubrecord.getCalBGL();
                    calibration.timestamp = calSubrecord.getDateEntered().getTime() + addativeOffset;
                    if (calibration.timestamp > new Date().getTime()) {
                        Log.e(TAG, "ERROR - Calibration timestamp is from the future, wont save!");
                        return;
                    }
                    calibration.raw_value = calSubrecord.getCalRaw() / 1000;
                    calibration.slope = calSlope;
                    calibration.intercept = calIntercept;

                    calibration.sensor_confidence = ((-0.0018 * calibration.bg * calibration.bg) + (0.6657 * calibration.bg) + 36.7505) / 100;
                    if (calibration.sensor_confidence <= 0) {
                        calibration.sensor_confidence = 0;
                    }
                    calibration.slope_confidence = 0.8; //TODO: query backwards to find this value near the timestamp
                    calibration.estimate_raw_at_time_of_calibration = calSubrecord.getCalRaw() / 1000;
                    calibration.sensor = sensor;
                    calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
                    calibration.uuid = UUID.randomUUID().toString();
                    calibration.sensor_uuid = sensor.uuid;
                    calibration.check_in = true;

                    calibration.first_decay = firstCalRecord.getDecay();
                    calibration.second_decay = secondCalRecord.getDecay();
                    calibration.first_slope = firstCalRecord.getSlope();
                    calibration.second_slope = secondCalRecord.getSlope();
                    calibration.first_scale = firstCalRecord.getScale();
                    calibration.second_scale = secondCalRecord.getScale();
                    calibration.first_intercept = firstCalRecord.getIntercept();
                    calibration.second_intercept = secondCalRecord.getIntercept();

                    calibration.save();
                    CalibrationSendQueue.addToQueue(calibration, context);
                    Calibration.requestCalibrationIfRangeTooNarrow();
                }
            }
            if(firstCalRecord.getCalSubrecords()[0] != null && firstCalRecord.getCalSubrecords()[2] == null) {
                if(Calibration.latest(2).size() == 1) {
                    Calibration.create(calRecords, context, true, 0);
                }
            }
            Notifications.notificationSetter(context);
        }
    }

    public static boolean is_new(CalSubrecord calSubrecord, long addativeOffset) {
        Sensor sensor = Sensor.currentSensor();
        Calibration calibration = new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("timestamp <= ?", calSubrecord.getDateEntered().getTime() + addativeOffset + (1000 * 60 * 2))
                .orderBy("timestamp desc")
                .executeSingle();
        if(calibration != null && Math.abs(calibration.timestamp - (calSubrecord.getDateEntered().getTime() + addativeOffset)) < (4*60*1000)) {
            Log.d("CAL CHECK IN ", "Already have that calibration!");
            return false;
        } else {
            Log.d("CAL CHECK IN ", "Looks like a new calibration!");
            return true;
        }
    }
    public static Calibration getForTimestamp(double timestamp) {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp < ?", timestamp)
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static Calibration create(double bg, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "mgdl");

        if(unit.compareTo("mgdl") != 0 ) {
            bg = bg * Constants.MMOLL_TO_MGDL;
        }

        CalibrationRequest.clearAll();
        Calibration calibration = new Calibration();
        Sensor sensor = Sensor.currentSensor();

        if (sensor != null) {
            BgReading bgReading = BgReading.last();
            if (bgReading != null) {
                calibration.sensor = sensor;
                calibration.bg = bg;
                calibration.check_in = false;
                calibration.timestamp = new Date().getTime();
                calibration.raw_value = bgReading.raw_data;
                calibration.adjusted_raw_value = bgReading.age_adjusted_raw_value;
                calibration.sensor_uuid = sensor.uuid;
                calibration.slope_confidence = Math.min(Math.max(((4 - Math.abs((bgReading.calculated_value_slope) * 60000))/4), 0), 1);

                double estimated_raw_bg = BgReading.estimated_raw_bg(new Date().getTime());
                calibration.raw_timestamp = bgReading.timestamp;
                if (Math.abs(estimated_raw_bg - bgReading.age_adjusted_raw_value) > 20) {
                    calibration.estimate_raw_at_time_of_calibration = bgReading.age_adjusted_raw_value;
                } else {
                    calibration.estimate_raw_at_time_of_calibration = estimated_raw_bg;
                }
                calibration.distance_from_estimate = Math.abs(calibration.bg - bgReading.calculated_value);
                calibration.sensor_confidence = Math.max(((-0.0018 * bg * bg) + (0.6657 * bg) + 36.7505) / 100, 0);
                calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
                calibration.uuid = UUID.randomUUID().toString();
                calibration.save();

                bgReading.calibration = calibration;
                bgReading.calibration_flag = true;
                bgReading.save();
                BgSendQueue.addToQueue(bgReading, "update", context);

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

    public static List<Calibration> allForSensorInLastFiveDays() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 5)))
                .orderBy("timestamp desc")
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
            List<Calibration> calibrations = allForSensorInLastFourDays(); //5 days was a bit much, dropped this to 4
            if (calibrations.size() == 1) {
                Calibration calibration = Calibration.last();
                calibration.slope = 1;
                calibration.intercept = calibration.bg - (calibration.raw_value * calibration.slope);
                calibration.save();
            } else {
                for (Calibration calibration : calibrations) {
                    w = calibration.calculateWeight();
                    l += (w);
                    m += (w * calibration.estimate_raw_at_time_of_calibration);
                    n += (w * calibration.estimate_raw_at_time_of_calibration * calibration.estimate_raw_at_time_of_calibration);
                    p += (w * calibration.bg);
                    q += (w * calibration.estimate_raw_at_time_of_calibration * calibration.bg);
                }

                Calibration last_calibration = Calibration.last();
                w = (last_calibration.calculateWeight() * (calibrations.size() * 0.14));
                l += (w);
                m += (w * last_calibration.estimate_raw_at_time_of_calibration);
                n += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.estimate_raw_at_time_of_calibration);
                p += (w * last_calibration.bg);
                q += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.bg);

                double d = (l * n) - (m * m);
                Calibration calibration = Calibration.last();
                calibration.intercept = ((n * p) - (m * q)) / d;
                calibration.slope = ((l * q) - (m * p)) / d;
                if ((calibrations.size() == 2 && calibration.slope < 0.95) || (calibration.slope < 0.85)) { // I have not seen a case where a value below 7.5 proved to be accurate but we should keep an eye on this
                    calibration.slope = calibration.slopeOOBHandler(0);
                    if(calibrations.size() > 2) { calibration.possible_bad = true; }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 25);
                }
                if ((calibrations.size() == 2 && calibration.slope > 1.3) || (calibration.slope > 1.4)) {
                    calibration.slope = calibration.slopeOOBHandler(1);
                    if(calibrations.size() > 2) { calibration.possible_bad = true; }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 25);
                }
                Log.d(TAG, "Calculated Calibration Slope: " + calibration.slope);
                Log.d(TAG, "Calculated Calibration intercept: " + calibration.intercept);
                calibration.save();
            }
        } else {
            Log.w(TAG, "NO Current active sensor found!!");
        }
    }

    private double slopeOOBHandler(int status) {
    // If the last slope was reasonable and reasonably close, use that, otherwise use a slope that may be a little steep, but its best to play it safe when uncertain
        List<Calibration> calibrations = Calibration.latest(3);
        Calibration thisCalibration = calibrations.get(0);
        if(status == 0) {
            if (calibrations.size() == 3) {
                if ((Math.abs(thisCalibration.bg - thisCalibration.estimate_bg_at_time_of_calibration) < 30) && (calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad == true)) {
                    return calibrations.get(1).slope;
                } else {
                    return Math.max(((-0.048) * (thisCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.1, 1.08);
                }
            } else if (calibrations.size() == 2) {
                return Math.max(((-0.048) * (thisCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.1, 1.15);
            }
            return 1;
        } else {
            if (calibrations.size() == 3) {
                if ((Math.abs(thisCalibration.bg - thisCalibration.estimate_bg_at_time_of_calibration) < 30) && (calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad == true)) {
                    return calibrations.get(1).slope;
                } else {
                    return 1.3;
                }
            } else if (calibrations.size() == 2) {
                return 1.2;
            }
        }
        return 1;
    }

    private static List<Calibration> calibrations_for_sensor(Sensor sensor) {
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ?", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .orderBy("timestamp desc")
                .execute();
    }

    private double calculateWeight() {
        double firstTimeStarted =   Calibration.first().sensor_age_at_time_of_estimation;
        double lastTimeStarted =   Calibration.last().sensor_age_at_time_of_estimation;
        double time_percentage = Math.min(((sensor_age_at_time_of_estimation - firstTimeStarted) / (lastTimeStarted - firstTimeStarted)) / (.85), 1);
        time_percentage = (time_percentage + .01);
        Log.w(TAG, "CALIBRATIONS TIME PERCENTAGE WEIGHT: " + time_percentage);
        return Math.max((((((slope_confidence + sensor_confidence) * (time_percentage))) / 2) * 100), 1);
    }
    public static void adjustRecentBgReadings() {// This just adjust the last 30 bg readings transition from one calibration point to the next
        adjustRecentBgReadings(30);
    }
    public static void adjustRecentBgReadings(int adjustCount) {
        //TODO: add some handling around calibration overrides as they come out looking a bit funky
        List<Calibration> calibrations = Calibration.latest(3);
        List<BgReading> bgReadings = BgReading.latestUnCalculated(adjustCount);
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

    public void rawValueOverride(double rawValue, Context context) {
        estimate_raw_at_time_of_calibration = rawValue;
        save();
        calculate_w_l_s();
        CalibrationSendQueue.addToQueue(this, context);
    }

    public static void requestCalibrationIfRangeTooNarrow() {
        double max = Calibration.max_recent();
        double min = Calibration.min_recent();
        if ((max - min) < 55) {
            double avg = ((min + max) / 2);
            double dist = max - avg;
            CalibrationRequest.createOffset(avg, dist + 20);
        }
    }

    public static void clear_all_existing_calibrations() {
        CalibrationRequest.clearAll();
        List<Calibration> pastCalibrations = Calibration.allForSensor();
        if (pastCalibrations != null) {
            for(Calibration calibration : pastCalibrations){
                calibration.slope_confidence = 0;
                calibration.sensor_confidence = 0;
                calibration.save();
            }
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

    //COMMON SCOPES!
    public static Calibration last() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static Calibration first() {
        Sensor sensor = Sensor.currentSensor();
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .orderBy("timestamp asc")
                .executeSingle();
    }
    public static double max_recent() {
        Sensor sensor = Sensor.currentSensor();
        Calibration calibration = new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 4)))
                .orderBy("bg desc")
                .executeSingle();
        if(calibration != null) {
            return calibration.bg;
        } else {
            return 120;
        }
    }

    public static double min_recent() {
        Sensor sensor = Sensor.currentSensor();
        Calibration calibration = new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 4)))
                .orderBy("bg asc")
                .executeSingle();
        if(calibration != null) {
            return calibration.bg;
        } else {
            return 100;
        }
    }

    public static List<Calibration> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("timestamp desc")
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
                .orderBy("timestamp desc")
                .execute();
    }

    public static List<Calibration> allForSensorInLastFourDays() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 4)))
                .orderBy("timestamp desc")
                .execute();
    }

}
