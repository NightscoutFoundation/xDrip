package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
//KS import com.eveningoutpost.dexdrip.GcmActivity;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalSubrecord;
import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.BgSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
//KS import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

//KS import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.newFingerStickData;


class DexParameters extends SlopeParameters {
    DexParameters() {
        LOW_SLOPE_1 = 0.75;
        LOW_SLOPE_2 = 0.70;
        HIGH_SLOPE_1 = 1.3;
        HIGH_SLOPE_2 = 1.4;
        DEFAULT_LOW_SLOPE_LOW = 0.75;
        DEFAULT_LOW_SLOPE_HIGH = 0.70;
        DEFAULT_SLOPE = 1;
        DEFAULT_HIGH_SLOPE_HIGH = 1.3;
        DEFAULT_HIGH_SLOPE_LOW = 1.2;
    }

}

class DexOldSchoolParameters extends SlopeParameters {
    /*
    Previous defaults up until 20th March 2017
     */
    DexOldSchoolParameters() {
        LOW_SLOPE_1 = 0.95;
        LOW_SLOPE_2 = 0.85;
        HIGH_SLOPE_1 = 1.3;
        HIGH_SLOPE_2 = 1.4;
        DEFAULT_LOW_SLOPE_LOW = 1.08;
        DEFAULT_LOW_SLOPE_HIGH = 1.15;
        DEFAULT_SLOPE = 1;
        DEFAULT_HIGH_SLOPE_HIGH = 1.3;
        DEFAULT_HIGH_SLOPE_LOW = 1.2;
    }

}

class DexParametersAdrian extends SlopeParameters {

    /*
    * Other default vlaues and thresholds that can be only activated in settings, when in engineering mode.
    * promoted to be the regular defaults 20th March 2017
    * */

    DexParametersAdrian() {
        LOW_SLOPE_1 = 0.75;
        LOW_SLOPE_2 = 0.70;
        HIGH_SLOPE_1 = 1.3;
        HIGH_SLOPE_2 = 1.4;
        DEFAULT_LOW_SLOPE_LOW = 0.75;
        DEFAULT_LOW_SLOPE_HIGH = 0.70;
        DEFAULT_SLOPE = 1;
        DEFAULT_HIGH_SLOPE_HIGH = 1.3;
        DEFAULT_HIGH_SLOPE_LOW = 1.2;
    }

}

class LiParameters extends SlopeParameters {
    LiParameters() {
        LOW_SLOPE_1 = 1;
        LOW_SLOPE_2 = 1;
        HIGH_SLOPE_1 = 1;
        HIGH_SLOPE_2 = 1;
        DEFAULT_LOW_SLOPE_LOW = 1;
        DEFAULT_LOW_SLOPE_HIGH = 1;
        DEFAULT_SLOPE = 1;
        DEFAULT_HIGH_SLOPE_HIGH = 1;
        DEFAULT_HIGH_SLOPE_LOW = 1;
    }
}

class TestParameters extends SlopeParameters {
    TestParameters() {
        LOW_SLOPE_1 = 0.85; //0.95
        LOW_SLOPE_2 = 0.80; //0.85
        HIGH_SLOPE_1 = 1.3;
        HIGH_SLOPE_2 = 1.4;
        DEFAULT_LOW_SLOPE_LOW = 0.9; //1.08
        DEFAULT_LOW_SLOPE_HIGH = 0.95; //1.15
        DEFAULT_SLOPE = 1;
        DEFAULT_HIGH_SLOPE_HIGH = 1.3;
        DEFAULT_HIGH_SLOPE_LOW = 1.2;
    }
}

/**
 * Created by Emma Black on 10/29/14.
 */
@Table(name = "Calibration", id = BaseColumns._ID)
public class Calibration extends Model {
    private final static String TAG = Calibration.class.getSimpleName();
    private final static double note_only_marker = 0.000001d;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

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
    public long raw_timestamp;

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

    public Calibration () {
        super ();
    }

    public static void initialCalibration(double bg1, double bg2, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String unit = prefs.getString("units", "mgdl");

        if (unit.compareTo("mgdl") != 0) {
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

        for (Calibration calibration : calibrations) {
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
            //KS newFingerStickData();
            CalibrationSendQueue.addToQueue(calibration, context);
        }
        adjustRecentBgReadings(5);
        CalibrationRequest.createOffset(lowerCalibration.bg, 35);
        context.startService(new Intent(context, Notifications.class));
    }

    //Create Calibration Checkin
    public static void create(CalRecord[] calRecords, long addativeOffset, Context context) {
        create(calRecords, context, false, addativeOffset);
    }

    public static void create(CalRecord[] calRecords, Context context) {
        create(calRecords, context, false, 0);
    }

    public static void create(CalRecord[] calRecords, Context context, boolean override, long addativeOffset) {
        //TODO: Change calibration.last and other queries to order calibrations by timestamp rather than ID
        Log.i("CALIBRATION-CHECK-IN: ", "Creating Calibration Record");
        Sensor sensor = Sensor.currentSensor();
        CalRecord firstCalRecord = calRecords[0];
        CalRecord secondCalRecord = calRecords[0];
//        CalRecord secondCalRecord = calRecords[calRecords.length - 1];
        //TODO: Figgure out how the ratio between the two is determined
        double calSlope = ((secondCalRecord.getScale() / secondCalRecord.getSlope()) + (3 * firstCalRecord.getScale() / firstCalRecord.getSlope())) * 250;

        double calIntercept = (((secondCalRecord.getScale() * secondCalRecord.getIntercept()) / secondCalRecord.getSlope()) + ((3 * firstCalRecord.getScale() * firstCalRecord.getIntercept()) / firstCalRecord.getSlope())) / -4;
        if (sensor != null) {
            for (int i = 0; i < firstCalRecord.getCalSubrecords().length - 1; i++) {
                if (((firstCalRecord.getCalSubrecords()[i] != null && Calibration.is_new(firstCalRecord.getCalSubrecords()[i], addativeOffset))) || (i == 0 && override)) {
                    CalSubrecord calSubrecord = firstCalRecord.getCalSubrecords()[i];

                    Calibration calibration = new Calibration();
                    calibration.bg = calSubrecord.getCalBGL();
                    calibration.timestamp = calSubrecord.getDateEntered().getTime() + addativeOffset;
                    calibration.raw_timestamp = calibration.timestamp;
                    if (calibration.timestamp > new Date().getTime()) {
                        Log.d(TAG, "ERROR - Calibration timestamp is from the future, wont save!");
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
                    //KS newFingerStickData();
                }
            }
            if (firstCalRecord.getCalSubrecords()[0] != null && firstCalRecord.getCalSubrecords()[2] == null) {
                if (Calibration.latest(2).size() == 1) {
                    Calibration.create(calRecords, context, true, 0);
                }
            }
            context.startService(new Intent(context, Notifications.class));
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
        if (calibration != null && Math.abs(calibration.timestamp - (calSubrecord.getDateEntered().getTime() + addativeOffset)) < (4 * 60 * 1000)) {
            Log.d("CAL CHECK IN ", "Already have that calibration!");
            return false;
        } else {
            Log.d("CAL CHECK IN ", "Looks like a new calibration!");
            return true;
        }
    }

    public static Calibration findByUuid(String uuid) {
        return new Select()
                .from(Calibration.class)
                .where("uuid = ?", uuid)
                .executeSingle();
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

    public static Calibration getByTimestamp(double timestamp) {//KS
        Sensor sensor = Sensor.currentSensor();
        if(sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("timestamp = ?", timestamp)
                .executeSingle();
    }

    // without timeoffset
    public static Calibration create(double bg, Context context) {
        return create(bg, 0, context);

    }

    public static Calibration create(double bg, long timeoffset, Context context) {
        return create(bg, timeoffset, context, false, 0);
    }

    public static Calibration create(double bg, long timeoffset, Context context, boolean note_only, long estimatedInterstitialLagSeconds) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String unit = prefs.getString("units", "mgdl");
        final boolean adjustPast = prefs.getBoolean("rewrite_history", true);

        if (unit.compareTo("mgdl") != 0) {
            bg = bg * Constants.MMOLL_TO_MGDL;
        }

        if ((bg < 40) || (bg > 400)) {
            Log.wtf(TAG, "Invalid out of range calibration glucose mg/dl value of: " + bg);
            JoH.static_toast_long("Calibration out of range: " + bg + " mg/dl");
            return null;
        }

        if (!note_only) CalibrationRequest.clearAll();
        final Calibration calibration = new Calibration();
        Sensor sensor = Sensor.currentSensor();

        boolean is_follower = prefs.getString("dex_collection_method", "").equals("Follower");
        if ((sensor == null)
                && (is_follower)) {
            Sensor.create(Math.round(JoH.ts())); // no sensor? no problem, create virtual one for follower
            sensor = Sensor.currentSensor();
        }

        if (sensor != null) {
            BgReading bgReading = null;
            if (timeoffset == 0) {
                bgReading = BgReading.last(is_follower);
            } else {
                // get closest bg reading we can find with a cut off at 15 minutes max time
                bgReading = BgReading.getForPreciseTimestamp(new Date().getTime() - ((timeoffset - estimatedInterstitialLagSeconds) * 1000 ), (15 * 60 * 1000));
            }
            if (bgReading != null) {
                calibration.sensor = sensor;
                calibration.bg = bg;
                calibration.check_in = false;
                calibration.timestamp = new Date().getTime() - (timeoffset * 1000); //  potential historical bg readings
                calibration.raw_value = bgReading.raw_data;
                calibration.adjusted_raw_value = bgReading.age_adjusted_raw_value;
                calibration.sensor_uuid = sensor.uuid;
                calibration.slope_confidence = Math.min(Math.max(((4 - Math.abs((bgReading.calculated_value_slope) * 60000)) / 4), 0), 1);

                double estimated_raw_bg = BgReading.estimated_raw_bg(new Date().getTime());
                calibration.raw_timestamp = bgReading.timestamp;
                if (Math.abs(estimated_raw_bg - bgReading.age_adjusted_raw_value) > 20) {
                    calibration.estimate_raw_at_time_of_calibration = bgReading.age_adjusted_raw_value;
                } else {
                    calibration.estimate_raw_at_time_of_calibration = estimated_raw_bg;
                }
                calibration.distance_from_estimate = Math.abs(calibration.bg - bgReading.calculated_value);
                if (!note_only) {
                    calibration.sensor_confidence = Math.max(((-0.0018 * bg * bg) + (0.6657 * bg) + 36.7505) / 100, 0);
                } else {
                    calibration.sensor_confidence = 0; // exclude from calibrations but show on graph
                    calibration.slope = 0;
                    calibration.intercept = 0;
                }
                calibration.sensor_age_at_time_of_estimation = calibration.timestamp - sensor.started_at;
                calibration.uuid = UUID.randomUUID().toString();
                calibration.save();

                if (!note_only) {
                    bgReading.calibration = calibration;
                    bgReading.calibration_flag = true;
                    bgReading.save();
                }

                if ((!is_follower) && (!note_only)) {
                    BgSendQueue.handleNewBgReading(bgReading, "update", context);
                    // TODO probably should add a more fine grained prefs option in future
                    calculate_w_l_s(prefs.getBoolean("infrequent_calibration", false));
                    adjustRecentBgReadings(adjustPast ? 30 : 2);
                    CalibrationSendQueue.addToQueue(calibration, context);
                    context.startService(new Intent(context, Notifications.class));
                    Calibration.requestCalibrationIfRangeTooNarrow();
                    //KS newFingerStickData();
                } else {
                    Log.d(TAG, "Follower mode or note so not processing calibration deeply");
                }
            } else {
                // we couldn't get a reading close enough to the calibration timestamp
                if (!is_follower) {
                    JoH.static_toast(context, "No close enough reading for Calib (15 min)", Toast.LENGTH_LONG);
                }
            }
        } else {
            Log.d("CALIBRATION", "No sensor, cant save!");
        }
        return Calibration.last();
    }

    public static List<Calibration> allForSensorInLastFiveDays() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 5)))
                .orderBy("timestamp desc")
                .execute();
    }

    private synchronized static void calculate_w_l_s() {
        calculate_w_l_s(false);
    }

    private synchronized static void calculate_w_l_s(boolean extended) {
        if (Sensor.isActive()) {
            double l = 0;
            double m = 0;
            double n = 0;
            double p = 0;
            double q = 0;
            double w;

            final SlopeParameters sParams = getSlopeParameters();
            ActiveAndroid.clearCache();
            List<Calibration> calibrations = allForSensorInLastFourDays(); //5 days was a bit much, dropped this to 4

            if (calibrations == null) {
                Log.e(TAG, "Somehow ended up with null calibration list!");
                Home.toaststatic("Somehow ended up with null calibration list!");
                return;
            }

            // less than 5 calibrations in last 4 days? cast the net wider if in extended mode
            final int ccount = calibrations.size();
            if ((ccount < 5) && extended) {
                ActiveAndroid.clearCache();
                calibrations = allForSensorLimited(5);
                if (calibrations.size() > ccount) {
                    Home.toaststaticnext("Calibrated using data beyond last 4 days");
                }
            }
            ActiveAndroid.clearCache();
            if (calibrations.size() <= 1) {
                final Calibration calibration = Calibration.last();
                ActiveAndroid.clearCache();
                calibration.slope = 1;
                calibration.intercept = calibration.bg - (calibration.raw_value * calibration.slope);
                calibration.save();
                CalibrationRequest.createOffset(calibration.bg, 25);
                //KS newFingerStickData();
            } else {
                for (Calibration calibration : calibrations) {
                    w = calibration.calculateWeight();
                    l += (w);
                    m += (w * calibration.estimate_raw_at_time_of_calibration);
                    n += (w * calibration.estimate_raw_at_time_of_calibration * calibration.estimate_raw_at_time_of_calibration);
                    p += (w * calibration.bg);
                    q += (w * calibration.estimate_raw_at_time_of_calibration * calibration.bg);
                }

                final Calibration last_calibration = Calibration.last();
                if (last_calibration != null) {
                    ActiveAndroid.clearCache();
                    w = (last_calibration.calculateWeight() * (calibrations.size() * 0.14));
                    l += (w);
                    m += (w * last_calibration.estimate_raw_at_time_of_calibration);
                    n += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.estimate_raw_at_time_of_calibration);
                    p += (w * last_calibration.bg);
                    q += (w * last_calibration.estimate_raw_at_time_of_calibration * last_calibration.bg);
                }

                double d = (l * n) - (m * m);
                final Calibration calibration = Calibration.last();
                ActiveAndroid.clearCache();
                calibration.intercept = ((n * p) - (m * q)) / d;
                calibration.slope = ((l * q) - (m * p)) / d;
                Log.d(TAG, "Calibration slope debug: slope:" + calibration.slope + " q:" + q + " m:" + m + " p:" + p + " d:" + d);
                if ((calibrations.size() == 2 && calibration.slope < sParams.getLowSlope1()) || (calibration.slope < sParams.getLowSlope2())) { // I have not seen a case where a value below 7.5 proved to be accurate but we should keep an eye on this
                    Log.d(TAG, "calibration.slope 1 : " + calibration.slope);
                    calibration.slope = calibration.slopeOOBHandler(0);
                    Log.d(TAG, "calibration.slope 2 : " + calibration.slope);
                    if (calibrations.size() > 2) {
                        calibration.possible_bad = true;
                    }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 25);
                }
                if ((calibrations.size() == 2 && calibration.slope > sParams.getHighSlope1()) || (calibration.slope > sParams.getHighSlope2())) {
                    Log.d(TAG, "calibration.slope 3 : " + calibration.slope);
                    calibration.slope = calibration.slopeOOBHandler(1);
                    Log.d(TAG, "calibration.slope 4 : " + calibration.slope);
                    if (calibrations.size() > 2) {
                        calibration.possible_bad = true;
                    }
                    calibration.intercept = calibration.bg - (calibration.estimate_raw_at_time_of_calibration * calibration.slope);
                    CalibrationRequest.createOffset(calibration.bg, 25);
                }
                Log.d(TAG, "Calculated Calibration Slope: " + calibration.slope);
                Log.d(TAG, "Calculated Calibration intercept: " + calibration.intercept);

                if ((calibration.slope == 0) && (calibration.intercept == 0)) {
                    calibration.sensor_confidence = 0;
                    calibration.slope_confidence = 0;
                    Home.toaststaticnext("Got invalid zero slope calibration!");
                    calibration.save(); // Save nulled record, lastValid should protect from bad calibrations
                    //KS newFingerStickData();
                } else {
                    calibration.save();
                    //KS newFingerStickData();
                }
            }
        } else {
            Log.d(TAG, "NO Current active sensor found!!");
        }
    }

    @NonNull
    private static SlopeParameters getSlopeParameters() {

        if (CollectionServiceStarter.isLimitter()) {
            return new LiParameters();
        }

        if (Pref.getBooleanDefaultFalse("engineering_mode") && Pref.getBooleanDefaultFalse("old_school_calibration_mode")) {
            JoH.static_toast_long("Using old pre-2017 calibration mode!");
            return new DexOldSchoolParameters();
        }

        return new DexParameters();
    }

    // here be dragons.. at time of writing estimate_bg_at_time_of_calibration is never written to and the possible_bad logic below looks backwards but
    // will never fire because the bg_at_time_of_calibration is not set.
    private double slopeOOBHandler(int status) {

        final SlopeParameters sParams = getSlopeParameters();

        // If the last slope was reasonable and reasonably close, use that, otherwise use a slope that may be a little steep, but its best to play it safe when uncertain
        final List<Calibration> calibrations = Calibration.latest(3);
        final Calibration thisCalibration = calibrations.get(0);
        if (status == 0) {
            if (calibrations.size() == 3) {
                if ((Math.abs(thisCalibration.bg - thisCalibration.estimate_bg_at_time_of_calibration) < 30)
                        && (calibrations.get(1).slope != 0)
                        && (calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad == true)) {
                    return calibrations.get(1).slope;
                } else {
                    return Math.max(((-0.048) * (thisCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.1, sParams.getDefaultLowSlopeLow());
                }
            } else if (calibrations.size() == 2) {
                return Math.max(((-0.048) * (thisCalibration.sensor_age_at_time_of_estimation / (60000 * 60 * 24))) + 1.1, sParams.getDefaultLowSlopeHigh());
            }
            return sParams.getDefaultSlope();
        } else {
            if (calibrations.size() == 3) {
                if ((Math.abs(thisCalibration.bg - thisCalibration.estimate_bg_at_time_of_calibration) < 30)
                        && (calibrations.get(1).slope != 0)
                        && (calibrations.get(1).possible_bad != null && calibrations.get(1).possible_bad == true)) {
                    return calibrations.get(1).slope;
                } else {
                    return sParams.getDefaultHighSlopeHigh();
                }
            } else if (calibrations.size() == 2) {
                return sParams.getDefaulHighSlopeLow();
            }
        }
        return sParams.getDefaultSlope();
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
        double firstTimeStarted = Calibration.first().sensor_age_at_time_of_estimation;
        double lastTimeStarted = Calibration.last().sensor_age_at_time_of_estimation;
        double time_percentage = Math.min(((sensor_age_at_time_of_estimation - firstTimeStarted) / (lastTimeStarted - firstTimeStarted)) / (.85), 1);
        time_percentage = (time_percentage + .01);
        Log.i(TAG, "CALIBRATIONS TIME PERCENTAGE WEIGHT: " + time_percentage);
        return Math.max((((((slope_confidence + sensor_confidence) * (time_percentage))) / 2) * 100), 1);
    }

    // this method no longer used
    public static void adjustRecentBgReadings() {// This just adjust the last 30 bg readings transition from one calibration point to the next
        adjustRecentBgReadings(30);
    }

    public static void adjustRecentBgReadings(int adjustCount) {
        //TODO: add some handling around calibration overrides as they come out looking a bit funky
        final List<Calibration> calibrations = Calibration.latest(3);
        if (calibrations == null) {
            Log.wtf(TAG, "Calibrations is null in adjustRecentBgReadings");
            return;
        }

        final List<BgReading> bgReadings = BgReading.latestUnCalculated(adjustCount);
        if (bgReadings == null) {
            Log.wtf(TAG, "bgReadings is null in adjustRecentBgReadings");
            return;
        }

        // ongoing calibration
        if (calibrations.size() >= 3) {
            final int denom = bgReadings.size();
            //Calibration latestCalibration = calibrations.get(0);
            try {
                final Calibration latestCalibration = Calibration.lastValid();
                int i = 0;
                for (BgReading bgReading : bgReadings) {
                    final double oldYValue = bgReading.calculated_value;
                    final double newYvalue = (bgReading.age_adjusted_raw_value * latestCalibration.slope) + latestCalibration.intercept;
                    final double new_calculated_value = ((newYvalue * (denom - i)) + (oldYValue * (i))) / denom;
                    // if filtered == raw then rewrite them both because this would not happen if filtered data was from real source
                    if (bgReading.filtered_calculated_value == bgReading.calculated_value) {
                        bgReading.filtered_calculated_value = new_calculated_value;
                    }
                    bgReading.calculated_value = new_calculated_value;

                    bgReading.save();
                    i += 1;
                }
            } catch (NullPointerException e) {
                Log.wtf(TAG, "Null pointer in AdjustRecentReadings >=3: " + e);
            }
            // initial calibration
        } else if (calibrations.size() == 2) {
            //Calibration latestCalibration = calibrations.get(0);
            try {
                final Calibration latestCalibration = Calibration.lastValid();
                for (BgReading bgReading : bgReadings) {
                    final double newYvalue = (bgReading.age_adjusted_raw_value * latestCalibration.slope) + latestCalibration.intercept;
                    if (bgReading.filtered_calculated_value == bgReading.calculated_value) {
                        bgReading.filtered_calculated_value = newYvalue;
                    }
                    bgReading.calculated_value = newYvalue;
                    BgReading.updateCalculatedValueToWithinMinMax(bgReading);
                    bgReading.save();
                }
            } catch (NullPointerException e) {
                Log.wtf(TAG, "Null pointer in AdjustRecentReadings ==2: " + e);
            }
        }

        try {
            // TODO this method call is probably only needed when we are called for initial calibration, it should probably be moved
            bgReadings.get(0).find_new_raw_curve();
            bgReadings.get(0).find_new_curve();
        } catch (NullPointerException e) {
            Log.wtf(TAG, "Got null pointer exception in adjustRecentBgReadings");
        }
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
            for (Calibration calibration : pastCalibrations) {
                calibration.slope_confidence = 0;
                calibration.sensor_confidence = 0;
                calibration.save();
                //KS newFingerStickData();
            }
        }

    }

    public static long msSinceLastCalibration() {
        final Calibration calibration = lastValid();
        if (calibration == null) return 86400000000L;
        return JoH.msSince(calibration.timestamp);
    }

    public static void deleteALL() {
        try {
            SQLiteUtils.execSql("delete from CalibrationRequest");
            SQLiteUtils.execSql("delete from Calibration");
            Log.d(TAG, "Deleting all Calibration");
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            Log.wtf(TAG, "Resetting database due to foreign key constraint: " + e.toString());
            ListenerService.resetDatabase();
        } catch (Exception e) {
            Log.e(TAG, "Got exception running deleteALL " + e.toString());
        }
    }

    public static void cleanup(long timestamp) {
        try {
            SQLiteUtils.execSql("delete from CalibrationRequest");
            List<Calibration> data = new Select()
                    .from(BgReading.class)
                    .where("timestamp < ?", timestamp)
                    .orderBy("timestamp desc")
                    .execute();
            if (data != null) Log.d(TAG, "cleanup Calibration size=" + data.size());
            new Calibration.Cleanup().execute(data);
        } catch (Exception e) {
            Log.e(TAG, "Got exception running cleanup " + e.toString());
        }
    }

    private static class Cleanup extends AsyncTask<List<Calibration>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<Calibration>... errors) {
            try {
                for(Calibration data : errors[0]) {
                    data.delete();
                }
                return true;
            } catch(Exception e) {
                return false;
            }
        }
    }

    public static void clearLastCalibration() {
        CalibrationRequest.clearAll();
        Log.d(TAG, "Trying to clear last calibration");
        Calibration calibration = Calibration.last();
        if (calibration != null) {
            calibration.invalidate();
            CalibrationSendQueue.addToQueue(calibration, xdrip.getAppContext());
            //KS newFingerStickData();
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

    public static Calibration byid(long id) {
        return new Select()
                .from(Calibration.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Calibration byuuid(String uuid) {
        if (uuid == null) return null;
        return new Select()
                .from(Calibration.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static void clear_byuuid(String uuid, boolean from_interactive) {
        if (uuid == null) return;
        Calibration calibration = byuuid(uuid);
        if (calibration != null) {
            calibration.invalidate();
            CalibrationSendQueue.addToQueue(calibration, xdrip.getAppContext());
            //KS newFingerStickData();
            //KS if (from_interactive) {
            //KS     GcmActivity.clearLastCalibration();
            //KS }
        }
    }
    
    public static void upsertFromMaster(Calibration jsonCalibration) {
        
        if (jsonCalibration == null) {
            Log.wtf(TAG,"Got null calibration from json");
            return;
        }
        try {
            Sensor sensor = Sensor.getByUuid(jsonCalibration.sensor_uuid);
            if (sensor == null) {
                Log.e(TAG, "No sensor found, ignoring cailbration " + jsonCalibration.sensor_uuid);
                return;
            }
            Calibration existingCalibration = byuuid(jsonCalibration.uuid);
            if (existingCalibration == null) {
                Log.d(TAG, "saving new calibration record. sensor uuid =" + jsonCalibration.sensor_uuid + " calibration uuid = " + jsonCalibration.uuid);
                jsonCalibration.sensor = sensor;
                jsonCalibration.save();
            } else {
                Log.d(TAG, "updating existing calibration record: " + jsonCalibration.uuid);
                existingCalibration.sensor = sensor;
                existingCalibration.timestamp = jsonCalibration.timestamp;
                existingCalibration.sensor_age_at_time_of_estimation = jsonCalibration.sensor_age_at_time_of_estimation;
                existingCalibration.bg = jsonCalibration.bg;
                existingCalibration.raw_value = jsonCalibration.raw_value;
                existingCalibration.adjusted_raw_value = jsonCalibration.adjusted_raw_value;
                existingCalibration.sensor_confidence = jsonCalibration.sensor_confidence;
                existingCalibration.slope_confidence = jsonCalibration.slope_confidence;
                existingCalibration.raw_timestamp = jsonCalibration.raw_timestamp;
                existingCalibration.slope = jsonCalibration.slope;
                existingCalibration.intercept = jsonCalibration.intercept;
                existingCalibration.distance_from_estimate = jsonCalibration.distance_from_estimate;
                existingCalibration.estimate_raw_at_time_of_calibration = jsonCalibration.estimate_raw_at_time_of_calibration;
                existingCalibration.estimate_bg_at_time_of_calibration = jsonCalibration.estimate_bg_at_time_of_calibration;
                existingCalibration.uuid = jsonCalibration.uuid;
                existingCalibration.sensor_uuid = jsonCalibration.sensor_uuid;
                existingCalibration.possible_bad = jsonCalibration.possible_bad;
                existingCalibration.check_in = jsonCalibration.check_in;
                existingCalibration.first_decay = jsonCalibration.first_decay;
                existingCalibration.second_decay = jsonCalibration.second_decay;
                existingCalibration.first_slope = jsonCalibration.first_slope;
                existingCalibration.second_slope = jsonCalibration.second_slope;
                existingCalibration.first_intercept = jsonCalibration.first_intercept;
                existingCalibration.second_intercept = jsonCalibration.second_intercept;
                existingCalibration.first_scale = jsonCalibration.first_scale;
                existingCalibration.second_scale = jsonCalibration.second_scale;
                
                existingCalibration.save();
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not save Calibration: " + e.toString());
        }
    }
    

    //COMMON SCOPES!
    public static Calibration last() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static Calibration lastValid() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("slope != 0")
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
        if (calibration != null) {
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
        if (calibration != null) {
            return calibration.bg;
        } else {
            return 100;
        }
    }

    public static List<Calibration> latest(int number) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<Calibration> latestValid(int number) {
        return latestValid(number, JoH.tsl() + Constants.HOUR_IN_MS);
    }

    public static List<Calibration> latestValid(int number, long until) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("slope != 0")
                .where("timestamp <= ?", until)
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<Calibration> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<Calibration> latestForGraph(int number, long startTime, long endTime) {
        return new Select()
                .from(Calibration.class)
                .where("timestamp >= " + Math.max(startTime, 0))
                .where("timestamp <= " + endTime)
                .where("(slope != 0 or slope_confidence = ?)", note_only_marker)
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<Calibration> allForSensor() {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
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
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .where("timestamp > ?", (new Date().getTime() - (60000 * 60 * 24 * 4)))
                .orderBy("timestamp desc")
                .execute();
    }

    public static List<Calibration> allForSensorLimited(int limit) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            return null;
        }
        return new Select()
                .from(Calibration.class)
                .where("Sensor = ? ", sensor.getId())
                .where("slope_confidence != 0")
                .where("sensor_confidence != 0")
                .orderBy("timestamp desc")
                .limit(limit)
                .execute();
    }

    public static List<Calibration> getCalibrationsForSensor(Sensor sensor, int limit) {
        return new Select()
                .from(Calibration.class)
                .where("sensor_uuid = ? ", sensor.uuid)
                 .orderBy("timestamp desc")
                 .limit(limit)
                .execute();
    }
    
    public static List<Calibration> futureCalibrations() {
        double timestamp = new Date().getTime();
        return new Select()
                .from(Calibration.class)
                .where("timestamp > " + timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public boolean isNote() {
        Calibration calibration = this;
        if ((calibration.slope == 0)
                && (calibration.slope_confidence == note_only_marker)
                && (calibration.sensor_confidence == 0)
                && (calibration.intercept == 0)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isValid() {
        Calibration calibration = this;
        if ((calibration.slope_confidence != 0)
                && (calibration.sensor_confidence != 0)
                && (calibration.slope != 0)
                && (calibration.intercept != 0)) {
            return true;
        } else {
            return false;
        }
    }

    public void invalidate() {
        this.slope_confidence = 0;
        this.sensor_confidence = 0;
        this.slope = 0;
        this.intercept = 0;
        save();
        //KS PluggableCalibration.invalidateAllCaches();
    }

    public static synchronized void invalidateAllForSensor() {
        final List<Calibration> cals = allForSensorLimited(9999999);
        if (cals != null) {
            for (Calibration cal : cals) {
                cal.invalidate();
            }
        }
        String msg = "Deleted all calibrations for sensor";
        Log.ueh(TAG, msg);
        JoH.static_toast_long(msg);
    }

}

abstract class SlopeParameters {
    protected double LOW_SLOPE_1;
    protected double LOW_SLOPE_2;
    protected double HIGH_SLOPE_1;
    protected double HIGH_SLOPE_2;
    protected double DEFAULT_LOW_SLOPE_LOW;
    protected double DEFAULT_LOW_SLOPE_HIGH;
    protected int DEFAULT_SLOPE;
    protected double DEFAULT_HIGH_SLOPE_HIGH;
    protected double DEFAULT_HIGH_SLOPE_LOW;

    public double getLowSlope1() {
        return LOW_SLOPE_1;
    }

    public double getLowSlope2() {
        return LOW_SLOPE_2;
    }

    public double getHighSlope1() {
        return HIGH_SLOPE_1;
    }

    public double getHighSlope2() {
        return HIGH_SLOPE_2;
    }

    public double getDefaultLowSlopeLow() {
        return DEFAULT_LOW_SLOPE_LOW;
    }

    public double getDefaultLowSlopeHigh() {
        return DEFAULT_LOW_SLOPE_HIGH;
    }

    public int getDefaultSlope() {
        return DEFAULT_SLOPE;
    }

    public double getDefaultHighSlopeHigh() {
        return DEFAULT_HIGH_SLOPE_HIGH;
    }

    public double getDefaulHighSlopeLow() {
        return DEFAULT_HIGH_SLOPE_LOW;
    }
}
