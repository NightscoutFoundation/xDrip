package com.eveningoutpost.dexdrip.Models;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.Services.SyncService;
import com.eveningoutpost.dexdrip.ShareModels.ShareUploadableBg;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.UtilityModels.WholeHouse;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.messages.BgReadingMessage;
import com.eveningoutpost.dexdrip.messages.BgReadingMultiMessage;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.SqliteRejigger;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.squareup.wire.Wire;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;
import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.newCloseSensorData;

@Table(name = "BgReadings", id = BaseColumns._ID)
public class BgReading extends Model implements ShareUploadableBg {

    private final static String TAG = BgReading.class.getSimpleName();
    private final static String TAG_ALERT = TAG + " AlertBg";
    private final static String PERSISTENT_HIGH_SINCE = "persistent_high_since";
    public static final double AGE_ADJUSTMENT_TIME = 86400000 * 1.9;
    public static final double AGE_ADJUSTMENT_FACTOR = .45;
    //TODO: Have these as adjustable settings!!
    public final static double BESTOFFSET = (60000 * 0); // Assume readings are about x minutes off from actual!

    public static final int BG_READING_ERROR_VALUE = 38; // error marker
    public static final int BG_READING_MINIMUM_VALUE = 39;
    public static final int BG_READING_MAXIMUM_VALUE = 400;

    private static volatile long earliest_backfill = 0;

    @Column(name = "sensor", index = true)
    public Sensor sensor;

    @Column(name = "calibration", index = true, onDelete = Column.ForeignKeyAction.CASCADE)
    public Calibration calibration;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "time_since_sensor_started")
    public double time_since_sensor_started;

    @Expose
    @Column(name = "raw_data")
    public volatile double raw_data;

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
    @Column(name = "filtered_calculated_value")
    public double filtered_calculated_value;

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
    // TODO unification with wear support ConflictAction.REPLACE for wear, done with rejig below
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    @Expose
    @Column(name = "calibration_uuid")
    public String calibration_uuid;

    @Expose
    @Column(name = "sensor_uuid", index = true)
    public String sensor_uuid;

    // mapped to the no longer used "synced" to keep DB Scheme compatible
    @Expose
    @Column(name = "snyced")
    public boolean ignoreForStats;

    @Expose
    @Column(name = "raw_calculated")
    public double raw_calculated;

    @Expose
    @Column(name = "hide_slope")
    public boolean hide_slope;

    @Expose
    @Column(name = "noise")
    public String noise;

    @Expose
    @Column(name = "dg_mgdl")
    public double dg_mgdl = 0d;

    @Expose
    @Column(name = "dg_slope")
    public double dg_slope = 0d;

    @Expose
    @Column(name = "dg_delta_name")
    public String dg_delta_name;

    @Expose
    @Column(name = "source_info")
    public volatile String source_info;

    public synchronized static void updateDB() {
        final String[] updates = new String[]{"ALTER TABLE BgReadings ADD COLUMN dg_mgdl REAL;",
                "ALTER TABLE BgReadings ADD COLUMN dg_slope REAL;",
                "ALTER TABLE BgReadings ADD COLUMN dg_delta_name TEXT;",
                "ALTER TABLE BgReadings ADD COLUMN source_info TEXT;"};
        for (String patch : updates) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
            }
        }

        // needs different handling on wear
        if (JoH.areWeRunningOnAndroidWear()) {
            BgSendQueue.emptyQueue();
            SqliteRejigger.rejigSchema("BgReadings", "uuid TEXT UNIQUE ON CONFLICT FAIL", "uuid TEXT UNIQUE ON CONFLICT REPLACE");
            SqliteRejigger.rejigSchema("BgReadings", "uuid TEXT UNIQUE ON CONFLICT IGNORE", "uuid TEXT UNIQUE ON CONFLICT REPLACE");
            SqliteRejigger.rejigSchema("BgSendQueue", "BgReadings_temp", "BgReadings");
        }

    }

    public double getDg_mgdl(){
        if(dg_mgdl != 0) return dg_mgdl;
        return calculated_value;
    }

    public double getDg_slope(){
        if(dg_mgdl != 0) return dg_slope;
        if(calculated_value_slope !=0) return calculated_value_slope;
        return currentSlope();
    }

    public String getDg_deltaName(){
        if(dg_mgdl != 0 && dg_delta_name != null) return dg_delta_name;
        return slopeName();
    }

    public double calculated_value_mmol() {
        return mmolConvert(calculated_value);
    }

    public void injectDisplayGlucose(BestGlucose.DisplayGlucose displayGlucose) {
        //displayGlucose can be null. E.g. when out of order values come in
        if (displayGlucose != null) {
            if (Math.abs(displayGlucose.timestamp - timestamp) < Constants.MINUTE_IN_MS * 10) {
                dg_mgdl = displayGlucose.mgdl;
                dg_slope = displayGlucose.slope;
                dg_delta_name = displayGlucose.delta_name;
                // TODO we probably should reflect the display glucose delta here as well for completeness
                this.save();
            } else {
                if (JoH.ratelimit("cannotinjectdg", 30)) {
                    UserError.Log.e(TAG, "Cannot inject display glucose value as time difference too great: " + JoH.dateTimeText(displayGlucose.timestamp) + " vs " + JoH.dateTimeText(timestamp));
                }
            }
        }
    }

    public double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public String displayValue(Context context) {
        final String unit = Pref.getString("units", "mgdl");
        final DecimalFormat df = new DecimalFormat("#");
        final double this_value = getDg_mgdl();
        if (this_value >= 400) {
            return "HIGH";
        } else if (this_value >= 40) {
            if (unit.equals("mgdl")) {
                df.setMaximumFractionDigits(0);
                return df.format(this_value);
            } else {
                df.setMaximumFractionDigits(1);
                return df.format(mmolConvert(this_value));
            }
        } else {
            return "LOW";
            // TODO doesn't understand special low values
        }
    }

    public static double activeSlope() {
        BgReading bgReading = BgReading.lastNoSenssor();
        if (bgReading != null) {
            double slope = (2 * bgReading.a * (new Date().getTime() + BESTOFFSET)) + bgReading.b;
            Log.i(TAG, "ESTIMATE SLOPE" + slope);
            return slope;
        }
        return 0;
    }

    public static double activePrediction() {
        BgReading bgReading = BgReading.lastNoSenssor();
        if (bgReading != null) {
            double currentTime = new Date().getTime();
            if (currentTime >= bgReading.timestamp + (60000 * 7)) {
                currentTime = bgReading.timestamp + (60000 * 7);
            }
            double time = currentTime + BESTOFFSET;
            return ((bgReading.a * time * time) + (bgReading.b * time) + bgReading.c);
        }
        return 0;
    }


    public static double calculateSlope(BgReading current, BgReading last) {
        if (current.timestamp == last.timestamp || current.calculated_value == last.calculated_value) {
            return 0;
        } else {
            return (last.calculated_value - current.calculated_value) / (last.timestamp - current.timestamp);
        }
    }

    public static double currentSlope() {
        return currentSlope(Home.get_follower());
    }

    public static double currentSlope(boolean is_follower) {
        List<BgReading> last_2 = BgReading.latest(2, is_follower);
        if ((last_2 != null) && (last_2.size() == 2)) {
            double slope = calculateSlope(last_2.get(0), last_2.get(1));
            return slope;
        } else {
            return 0d;
        }
    }


    //*******CLASS METHODS***********//
    // Dexcom Bluetooth Share
    public static void create(EGVRecord[] egvRecords, long addativeOffset, Context context) {
        for (EGVRecord egvRecord : egvRecords) {
            BgReading.create(egvRecord, addativeOffset, context);
        }
    }

    public static void create(SensorRecord[] sensorRecords, long addativeOffset, Context context) {
        for (SensorRecord sensorRecord : sensorRecords) {
            BgReading.create(sensorRecord, addativeOffset, context);
        }
    }

    public static void create(SensorRecord sensorRecord, long addativeOffset, Context context) {
        Log.i(TAG, "create: gonna make some sensor records: " + sensorRecord.getUnfiltered());
        if (BgReading.is_new(sensorRecord, addativeOffset)) {
            BgReading bgReading = new BgReading();
            Sensor sensor = Sensor.currentSensor();
            Calibration calibration = Calibration.getForTimestamp(sensorRecord.getSystemTime().getTime() + addativeOffset);
            if (sensor != null && calibration != null) {
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = (sensorRecord.getUnfiltered() / 1000);
                bgReading.filtered_data = (sensorRecord.getFiltered() / 1000);
                bgReading.timestamp = sensorRecord.getSystemTime().getTime() + addativeOffset;
                if (bgReading.timestamp > new Date().getTime()) {
                    return;
                }
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
                bgReading.calculateAgeAdjustedRawValue();
                bgReading.save();
            }
        }
    }

    // Dexcom Bluetooth Share
    public static void create(EGVRecord egvRecord, long addativeOffset, Context context) {
        BgReading bgReading = BgReading.getForTimestamp(egvRecord.getSystemTime().getTime() + addativeOffset);
        Log.i(TAG, "create: Looking for BG reading to tag this thing to: " + egvRecord.getBGValue());
        if (bgReading != null) {
            bgReading.calculated_value = egvRecord.getBGValue();
            if (egvRecord.getBGValue() <= 13) {
                Calibration calibration = bgReading.calibration;
                double firstAdjSlope = calibration.first_slope + (calibration.first_decay * (Math.ceil(new Date().getTime() - calibration.timestamp) / (1000 * 60 * 10)));
                double calSlope = (calibration.first_scale / firstAdjSlope) * 1000;
                double calIntercept = ((calibration.first_scale * calibration.first_intercept) / firstAdjSlope) * -1;
                bgReading.raw_calculated = (((calSlope * bgReading.raw_data) + calIntercept) - 5);
            }
            Log.i(TAG, "create: NEW VALUE CALCULATED AT: " + bgReading.calculated_value);
            bgReading.calculated_value_slope = bgReading.slopefromName(egvRecord.getTrend().friendlyTrendName());
            bgReading.noise = egvRecord.noiseValue();
            String friendlyName = egvRecord.getTrend().friendlyTrendName();
            if (friendlyName.compareTo("NONE") == 0 ||
                    friendlyName.compareTo("NOT_COMPUTABLE") == 0 ||
                    friendlyName.compareTo("NOT COMPUTABLE") == 0 ||
                    friendlyName.compareTo("OUT OF RANGE") == 0 ||
                    friendlyName.compareTo("OUT_OF_RANGE") == 0) {
                bgReading.hide_slope = true;
            }
            bgReading.save();
            bgReading.find_new_curve();
            bgReading.find_new_raw_curve();
            //context.startService(new Intent(context, Notifications.class));
            Notifications.start(); // this may not be needed as it is duplicated in handleNewBgReading
            BgSendQueue.handleNewBgReading(bgReading, "create", context);
        }
    }

    public static BgReading getForTimestamp(double timestamp) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp <= ?", (timestamp + (60 * 1000))) // 1 minute padding (should never be that far off, but why not)
                    .where("calculated_value = 0")
                    .where("raw_calculated = 0")
                    .orderBy("timestamp desc")
                    .executeSingle();
            if (bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3 * 60 * 1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                Log.i(TAG, "getForTimestamp: Found a BG timestamp match");
                return bgReading;
            }
        }
        Log.d(TAG, "getForTimestamp: No luck finding a BG timestamp match");
        return null;
    }

    // used in wear
    public static BgReading getForTimestampExists(double timestamp) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp <= ?", (timestamp + (60 * 1000))) // 1 minute padding (should never be that far off, but why not)
                    .orderBy("timestamp desc")
                    .executeSingle();
            if (bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3 * 60 * 1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                Log.i(TAG, "getForTimestamp: Found a BG timestamp match");
                return bgReading;
            }
        }
        Log.d(TAG, "getForTimestamp: No luck finding a BG timestamp match");
        return null;
    }

    public static BgReading getForPreciseTimestamp(long timestamp, long precision) {
        return getForPreciseTimestamp(timestamp, precision, true);
    }

    public static BgReading getForPreciseTimestamp(long timestamp, long precision, boolean lock_to_sensor) {
        final Sensor sensor = Sensor.currentSensor();
        if ((sensor != null) || !lock_to_sensor) {
            final BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where(lock_to_sensor ? "Sensor = ?" : "timestamp > ?", (lock_to_sensor ? sensor.getId() : 0))
                    .where("timestamp <= ?", (timestamp + precision))
                    .where("timestamp >= ?", (timestamp - precision))
                    .orderBy("abs(timestamp - " + timestamp + ") asc")
                    .executeSingle();
            if (bgReading != null && Math.abs(bgReading.timestamp - timestamp) < precision) { //cool, so was it actually within precision of that bg reading?
                //Log.d(TAG, "getForPreciseTimestamp: Found a BG timestamp match");
                return bgReading;
            }
        }
        Log.d(TAG, "getForPreciseTimestamp: No luck finding a BG timestamp match: " + JoH.dateTimeText((long) timestamp) + " precision:" + precision + " Sensor: " + ((sensor == null) ? "null" : sensor.getId()));
        return null;
    }


    public static boolean is_new(SensorRecord sensorRecord, long addativeOffset) {
        double timestamp = sensorRecord.getSystemTime().getTime() + addativeOffset;
        Sensor sensor = Sensor.currentSensor();
        if (sensor != null) {
            BgReading bgReading = new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp <= ?", (timestamp + (60 * 1000))) // 1 minute padding (should never be that far off, but why not)
                    .orderBy("timestamp desc")
                    .executeSingle();
            if (bgReading != null && Math.abs(bgReading.timestamp - timestamp) < (3 * 60 * 1000)) { //cool, so was it actually within 4 minutes of that bg reading?
                Log.i(TAG, "isNew; Old Reading");
                return false;
            }
        }
        Log.i(TAG, "isNew: New Reading");
        return true;
    }

    public static BgReading create(double raw_data, double filtered_data, Context context, Long timestamp) {
        return create(raw_data, filtered_data, context, timestamp, false);
    }

    public static BgReading create(double raw_data, double filtered_data, Context context, Long timestamp, boolean quick) {
        if (context == null) context = xdrip.getAppContext();
        BgReading bgReading = new BgReading();
        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i("BG GSON: ", bgReading.toS());
            return bgReading;
        }

        if (raw_data == 0) {
            Log.e(TAG,"Warning: raw_data is 0 in BgReading.create()");
        }

        Calibration calibration = Calibration.lastValid();
        if (calibration == null) {
            Log.d(TAG, "create: No calibration yet");
            bgReading.sensor = sensor;
            bgReading.sensor_uuid = sensor.uuid;
            bgReading.raw_data = (raw_data / 1000);
            bgReading.filtered_data = (filtered_data / 1000);
            bgReading.timestamp = timestamp;
            bgReading.uuid = UUID.randomUUID().toString();
            bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;
            bgReading.calibration_flag = false;

            bgReading.calculateAgeAdjustedRawValue();

            bgReading.save();
            bgReading.perform_calculations();
            BgSendQueue.sendToPhone(context);
        } else {
            Log.d(TAG, "Calibrations, so doing everything: " + calibration.uuid);
            bgReading = createFromRawNoSave(sensor, calibration, raw_data, filtered_data, timestamp);

            bgReading.save();

            // used when we are not fast inserting data
            if (!quick) {
                bgReading.perform_calculations();

                if (JoH.ratelimit("opportunistic-calibration", 60)) {
                    BloodTest.opportunisticCalibration();
                }

                //context.startService(new Intent(context, Notifications.class));
                // allow this instead to be fired inside handleNewBgReading when noise will have been injected already
            }

            bgReading.postProcess(quick);

        }

        Log.i("BG GSON: ", bgReading.toS());

        return bgReading;
    }

    public void postProcess(final boolean quick) {
        injectNoise(true); // Add noise parameter for nightscout
        injectDisplayGlucose(BestGlucose.getDisplayGlucose()); // Add display glucose for nightscout
        BgSendQueue.handleNewBgReading(this, "create", xdrip.getAppContext(), Home.get_follower(), quick);
    }

    public static BgReading createFromRawNoSave(Sensor sensor, Calibration calibration, double raw_data, double filtered_data, long timestamp) {
        final BgReading bgReading = new BgReading();
        if (sensor == null) {
            sensor = Sensor.currentSensor();
            if (sensor == null) {
                return bgReading;
            }
        }
        if (calibration == null) {
            calibration = Calibration.lastValid();
            if (calibration == null) {
                return bgReading;
            }
        }

        bgReading.sensor = sensor;
        bgReading.sensor_uuid = sensor.uuid;
        bgReading.calibration = calibration;
        bgReading.calibration_uuid = calibration.uuid;
        bgReading.raw_data = (raw_data / 1000);
        bgReading.filtered_data = (filtered_data / 1000);
        bgReading.timestamp = timestamp;
        bgReading.uuid = UUID.randomUUID().toString();
        bgReading.time_since_sensor_started = bgReading.timestamp - sensor.started_at;

        bgReading.calculateAgeAdjustedRawValue();

        if (calibration.check_in) {
            double firstAdjSlope = calibration.first_slope + (calibration.first_decay * (Math.ceil(new Date().getTime() - calibration.timestamp) / (1000 * 60 * 10)));
            double calSlope = (calibration.first_scale / firstAdjSlope) * 1000;
            double calIntercept = ((calibration.first_scale * calibration.first_intercept) / firstAdjSlope) * -1;
            bgReading.calculated_value = (((calSlope * bgReading.raw_data) + calIntercept) - 5);
            bgReading.filtered_calculated_value = (((calSlope * bgReading.ageAdjustedFiltered()) + calIntercept) - 5);

        } else {
            BgReading lastBgReading = BgReading.last();
            if (lastBgReading != null && lastBgReading.calibration != null) {
                Log.d(TAG, "Create calibration.uuid=" + calibration.uuid + " bgReading.uuid: " + bgReading.uuid + " lastBgReading.calibration_uuid: " + lastBgReading.calibration_uuid + " lastBgReading.calibration.uuid: " + lastBgReading.calibration.uuid);
                Log.d(TAG, "Create lastBgReading.calibration_flag=" + lastBgReading.calibration_flag + " bgReading.timestamp: " + bgReading.timestamp + " lastBgReading.timestamp: " + lastBgReading.timestamp + " lastBgReading.calibration.timestamp: " + lastBgReading.calibration.timestamp);
                Log.d(TAG, "Create lastBgReading.calibration_flag=" + lastBgReading.calibration_flag + " bgReading.timestamp: " + JoH.dateTimeText(bgReading.timestamp) + " lastBgReading.timestamp: " + JoH.dateTimeText(lastBgReading.timestamp) + " lastBgReading.calibration.timestamp: " + JoH.dateTimeText(lastBgReading.calibration.timestamp));
                if (lastBgReading.calibration_flag == true && ((lastBgReading.timestamp + (60000 * 20)) > bgReading.timestamp) && ((lastBgReading.calibration.timestamp + (60000 * 20)) > bgReading.timestamp)) {
                    lastBgReading.calibration.rawValueOverride(BgReading.weightedAverageRaw(lastBgReading.timestamp, bgReading.timestamp, lastBgReading.calibration.timestamp, lastBgReading.age_adjusted_raw_value, bgReading.age_adjusted_raw_value), xdrip.getAppContext());
                    newCloseSensorData();
                }
            }

            if ((bgReading.raw_data != 0) && (bgReading.raw_data * 2 == bgReading.filtered_data)) {
                Log.wtf(TAG, "Filtered data is exactly double raw - this is completely wrong - dead transmitter? - blocking glucose calculation");
                bgReading.calculated_value = 0;
                bgReading.filtered_calculated_value = 0;
                bgReading.hide_slope = true;
            } else if (!SensorSanity.isRawValueSane(bgReading.raw_data)) {
                Log.wtf(TAG, "Raw data fails sanity check! " + bgReading.raw_data);
                bgReading.calculated_value = 0;
                bgReading.filtered_calculated_value = 0;
                bgReading.hide_slope = true;
            } else {

                // calculate glucose number from raw
                final CalibrationAbstract.CalibrationData pcalibration;
                final CalibrationAbstract plugin = getCalibrationPluginFromPreferences(); // make sure do this only once

                if ((plugin != null) && ((pcalibration = plugin.getCalibrationData()) != null) && (Pref.getBoolean("use_pluggable_alg_as_primary", false))) {
                    Log.d(TAG, "USING CALIBRATION PLUGIN AS PRIMARY!!!");
                    if (plugin.isCalibrationSane(pcalibration)) {
                        bgReading.calculated_value = (pcalibration.slope * bgReading.age_adjusted_raw_value) + pcalibration.intercept;
                        bgReading.filtered_calculated_value = (pcalibration.slope * bgReading.ageAdjustedFiltered()) + calibration.intercept;
                    } else {
                        UserError.Log.wtf(TAG, "Calibration plugin failed intercept sanity check: " + pcalibration.toS());
                        Home.toaststaticnext("Calibration plugin failed intercept sanity check");
                    }
                } else {
                    bgReading.calculated_value = ((calibration.slope * bgReading.age_adjusted_raw_value) + calibration.intercept);
                    bgReading.filtered_calculated_value = ((calibration.slope * bgReading.ageAdjustedFiltered()) + calibration.intercept);
                }

                updateCalculatedValueToWithinMinMax(bgReading);
            }
        }

        // LimiTTer can send 12 to indicate problem with NFC reading.
        if ((!calibration.check_in) && (raw_data == 12) && (filtered_data == 12)) {
            // store the raw value for sending special codes, note updateCalculatedValue would try to nix it
            bgReading.calculated_value = raw_data;
            bgReading.filtered_calculated_value = filtered_data;
        }
        return  bgReading;
    }

    public static boolean isRawMarkerValue(final double raw_data) {
        return raw_data == BgReading.SPECIAL_G5_PLACEHOLDER
                || raw_data == BgReading.SPECIAL_RAW_NOT_AVAILABLE;
    }


    static void updateCalculatedValueToWithinMinMax(BgReading bgReading) {
        // TODO should this really be <10 other values also special??
        if (bgReading.calculated_value < 10) {
            bgReading.calculated_value = BG_READING_ERROR_VALUE;
            bgReading.hide_slope = true;
        } else {
            bgReading.calculated_value = Math.min(BG_READING_MAXIMUM_VALUE, Math.max(BG_READING_MINIMUM_VALUE, bgReading.calculated_value));
        }
        Log.i(TAG, "NEW VALUE CALCULATED AT: " + bgReading.calculated_value);
    }

    // Used by xDripViewer
    public static void create(Context context, double raw_data, double age_adjusted_raw_value, double filtered_data, Long timestamp,
                              double calculated_bg, double calculated_current_slope, boolean hide_slope) {

        BgReading bgReading = new BgReading();
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.w(TAG, "No sensor, ignoring this bg reading");
            return;
        }

        Calibration calibration = Calibration.lastValid();
        if (calibration == null) {
            Log.d(TAG, "create: No calibration yet");
            bgReading.sensor = sensor;
            bgReading.sensor_uuid = sensor.uuid;
            bgReading.raw_data = (raw_data / 1000);
            bgReading.age_adjusted_raw_value = age_adjusted_raw_value;
            bgReading.filtered_data = (filtered_data / 1000);
            bgReading.timestamp = timestamp;
            bgReading.uuid = UUID.randomUUID().toString();
            bgReading.calculated_value = calculated_bg;
            bgReading.calculated_value_slope = calculated_current_slope;
            bgReading.hide_slope = hide_slope;

            bgReading.save();
            bgReading.perform_calculations();
        } else {
            Log.d(TAG, "Calibrations, so doing everything bgReading = " + bgReading);
            bgReading.sensor = sensor;
            bgReading.sensor_uuid = sensor.uuid;
            bgReading.calibration = calibration;
            bgReading.calibration_uuid = calibration.uuid;
            bgReading.raw_data = (raw_data / 1000);
            bgReading.age_adjusted_raw_value = age_adjusted_raw_value;
            bgReading.filtered_data = (filtered_data / 1000);
            bgReading.timestamp = timestamp;
            bgReading.uuid = UUID.randomUUID().toString();
            bgReading.calculated_value = calculated_bg;
            bgReading.calculated_value_slope = calculated_current_slope;
            bgReading.hide_slope = hide_slope;

            bgReading.save();
        }
        BgSendQueue.handleNewBgReading(bgReading, "create", context);

        Log.i("BG GSON: ", bgReading.toS());
    }

    public static void pushBgReadingSyncToWatch(BgReading bgReading, boolean is_new) {
        Log.d(TAG, "pushTreatmentSyncToWatch Add treatment to UploaderQueue.");
        if (Pref.getBooleanDefaultFalse("wear_sync")) {
            if (UploaderQueue.newEntryForWatch(is_new ? "insert" : "update", bgReading) != null) {
                SyncService.startSyncService(3000); // sync in 3 seconds
            }
        }
    }

    public String displaySlopeArrow() {
        return slopeToArrowSymbol(this.dg_mgdl > 0 ? this.dg_slope * 60000 : this.calculated_value_slope * 60000);
    }

    public static String activeSlopeArrow() {
        double slope = (float) (BgReading.activeSlope() * 60000);
        return slopeToArrowSymbol(slope);
    }

    public static String slopeToArrowSymbol(double slope) {
        if (slope <= (-3.5)) {
            return "\u21ca";// ⇊
        } else if (slope <= (-2)) {
            return "\u2193"; // ↓
        } else if (slope <= (-1)) {
            return "\u2198"; // ↘
        } else if (slope <= (1)) {
            return "\u2192"; // →
        } else if (slope <= (2)) {
            return "\u2197"; // ↗
        } else if (slope <= (3.5)) {
            return "\u2191"; // ↑
        } else {
            return "\u21c8"; // ⇈
        }
    }

    public String slopeArrow() {
        return slopeToArrowSymbol(this.calculated_value_slope * 60000);
    }

    public  String slopeName() {
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
        if (hide_slope) {
            arrow = "NOT COMPUTABLE";
        }
        return arrow;
    }

    public static String slopeName(double slope_by_minute) {
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
        return arrow;
    }

    public static double slopefromName(String slope_name) {
        if (slope_name == null) return 0;
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
            slope_by_minute = 3.5;
        } else if (slope_name.compareTo("DoubleUp") == 0) {
            slope_by_minute = 4;
        } else if (isSlopeNameInvalid(slope_name)) {
            slope_by_minute = 0;
        }
        return slope_by_minute / 60000;
    }

    public static boolean isSlopeNameInvalid(String slope_name) {
        if (slope_name.compareTo("NOT_COMPUTABLE") == 0 ||
                slope_name.compareTo("NOT COMPUTABLE") == 0 ||
                slope_name.compareTo("OUT_OF_RANGE") == 0 ||
                slope_name.compareTo("OUT OF RANGE") == 0 ||
                slope_name.compareTo("NONE") == 0) {
            return true;
        } else {
            return false;
        }
    }

    // Get a slope arrow based on pure guessed defaults so we can show it prior to calibration
    public static String getSlopeArrowSymbolBeforeCalibration() {
        final List<BgReading> last = BgReading.latestUnCalculated(2);
        if ((last!=null) && (last.size()==2)) {
            final double guess_slope = 1; // This is the "Default" slope for Dex and LimiTTer
            final double time_delta = (last.get(0).timestamp-last.get(1).timestamp);
            if (time_delta<=(BgGraphBuilder.DEXCOM_PERIOD * 2)) {
                final double estimated_delta = (last.get(0).age_adjusted_raw_value * guess_slope) - (last.get(1).age_adjusted_raw_value * guess_slope);
                final double estimated_delta2 = (last.get(0).raw_data * guess_slope) - (last.get(1).raw_data * guess_slope);
                Log.d(TAG, "SlopeArrowBeforeCalibration: guess delta: " + estimated_delta + " delta2: " + estimated_delta2 + " timedelta: " + time_delta);
                return slopeToArrowSymbol(estimated_delta / (time_delta / 60000));
            } else { return ""; }
        } else {
            return "";
        }
    }

    public static boolean last_within_minutes(final int mins) {
        return last_within_millis(mins * 60000);
    }

    public static boolean last_within_millis(final long millis) {
        final BgReading reading = last();
        return reading != null && ((JoH.tsl() - reading.timestamp) < millis);
    }

    public boolean within_millis(final long millis) {
        return ((JoH.tsl() - this.timestamp) < millis);
    }

    public boolean isStale() {
        return !within_millis(Home.stale_data_millis());
    }

    public static BgReading last()
    {
        return BgReading.last(Home.get_follower());
    }

    public static BgReading last(boolean is_follower) {
        if (is_follower) {
            return new Select()
                    .from(BgReading.class)
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
              //      .where("timestamp <= ?", JoH.tsl())
                    .orderBy("timestamp desc")
                    .executeSingle();
        } else {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                return new Select()
                        .from(BgReading.class)
                        .where("Sensor = ? ", sensor.getId())
                        .where("calculated_value != 0")
                        .where("raw_data != 0")
                //        .where("timestamp <= ?", JoH.tsl())
                        .orderBy("timestamp desc")
                        .executeSingle();
            }
        }
        return null;
    }

    public static List<BgReading> latest_by_size(int number) {
        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) return null;
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
            //    .where("timestamp <= ?", JoH.tsl())
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static List<BgReading> latest(int number) {
        return latest(number, Home.get_follower());
    }

    public static List<BgReading> latest(int number, boolean is_follower) {
        if (is_follower) {
            // exclude sensor information when working as a follower
            return new Select()
                    .from(BgReading.class)
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
            //        .where("timestamp <= ?", JoH.tsl())
                    .orderBy("timestamp desc")
                    .limit(number)
                    .execute();
        } else {
            Sensor sensor = Sensor.currentSensor();
            if (sensor == null) {
                return null;
            }
            return new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
              //      .where("timestamp <= ?", JoH.tsl())
                    .orderBy("timestamp desc")
                    .limit(number)
                    .execute();
        }
    }

    public static boolean isDataStale() {
        final BgReading last = lastNoSenssor();
        if (last == null) return true;
        return JoH.msSince(last.timestamp) > Home.stale_data_millis();
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
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<BgReading> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<BgReading> latestForGraph(int number, long startTime, long endTime) {
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + Math.max(startTime, 0))
                .where("timestamp <= " + endTime)
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> latestForGraphSensor(int number, long startTime, long endTime) {
        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) { return null; }
        return new Select()
                .from(BgReading.class)
                .where("Sensor = ? ", sensor.getId())
                .where("timestamp >= " + Math.max(startTime, 0))
                .where("timestamp <= " + endTime)
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .where("calibration_uuid != \"\"")
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }

    public static List<BgReading> latestForSensorAsc(int number, long startTime, long endTime, boolean follower) {
        if (follower) {
            return new Select()
                    .from(BgReading.class)
                    .where("timestamp >= ?", Math.max(startTime, 0))
                    .where("timestamp <= ?", endTime)
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
                    .orderBy("timestamp asc")
                    .limit(number)
                    .execute();
        } else {
            final Sensor sensor = Sensor.currentSensor();
            if (sensor == null) {
                return null;
            }
            return new Select()
                    .from(BgReading.class)
                    .where("Sensor = ? ", sensor.getId())
                    .where("timestamp >= ?", Math.max(startTime, 0))
                    .where("timestamp <= ?", endTime)
                    .where("calculated_value != 0")
                    .where("raw_data != 0")
                    .orderBy("timestamp asc")
                    .limit(number)
                    .execute();
        }
    }

    public static List<BgReading> latestForSensorAsc(int number, long startTime, long endTime) {
        return latestForSensorAsc(number, startTime, endTime, false);
    }


    public static List<BgReading> latestForGraphAsc(int number, long startTime) {//KS
        return latestForGraphAsc(number, startTime, Long.MAX_VALUE);
    }

    public static List<BgReading> latestForGraphAsc(int number, long startTime, long endTime) {//KS
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + Math.max(startTime, 0))
                .where("timestamp <= " + endTime)
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static BgReading readingNearTimeStamp(double startTime) {
        final double margin = (4 * 60 * 1000);
        final DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
        return new Select()
                .from(BgReading.class)
                .where("timestamp >= " + df.format(startTime - margin))
                .where("timestamp <= " + df.format(startTime + margin))
                .where("calculated_value != 0")
                .where("raw_data != 0")
                .executeSingle();
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

    public static boolean isDataSuitableForDoubleCalibration() {
        final List<BgReading> uncalculated = BgReading.latestUnCalculated(3);
        if (uncalculated.size() < 3) return false;
        final ProcessInitialDataQuality.InitialDataQuality idq = ProcessInitialDataQuality.getInitialDataQuality(uncalculated);
        if (!idq.pass) {
            UserError.Log.d(TAG, "Data quality failure for double calibration: " + idq.advice);
        }
        return idq.pass || Pref.getBooleanDefaultFalse("bypass_calibration_quality_check");
    }


    public static List<BgReading> futureReadings() {
        double timestamp = new Date().getTime();
        return new Select()
                .from(BgReading.class)
                .where("timestamp > " + timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    // used in wear
    public static BgReading findByUuid(String uuid) {
        return new Select()
                .from(BgReading.class)
                .where("uuid = ?", uuid)
                .executeSingle();
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
            Log.i(TAG, "No data yet, assume perfect!");
            estimate = 160;
        } else {
            estimate = (latest.ra * timestamp * timestamp) + (latest.rb * timestamp) + latest.rc;
        }
        Log.i(TAG, "ESTIMATE RAW BG" + estimate);
        return estimate;
    }

    public static void bgReadingInsertFromJson(String json)
    {
        bgReadingInsertFromJson(json, true);
    }

    private static void FixCalibration(BgReading bgr) {
        if (bgr.calibration_uuid == null || "".equals(bgr.calibration_uuid)) {
            Log.d(TAG, "Bgr with no calibration, doing nothing");
            return;
        }
        Calibration calibration = Calibration.byuuid(bgr.calibration_uuid);
        if (calibration == null) {
            Log.i(TAG, "received Unknown calibration: " + bgr.calibration_uuid + " asking for sensor upate...");
            GcmActivity.requestSensorCalibrationsUpdate();
        } else {
            bgr.calibration = calibration;
        }
    }

    public BgReading noRawWillBeAvailable() {
        raw_data = SPECIAL_RAW_NOT_AVAILABLE;
        save();
        return this;
    }

    public BgReading appendSourceInfo(String info) {
        if ((source_info == null) || (source_info.length() == 0)) {
            source_info = info;
        } else {
            if (!source_info.startsWith(info) && (!source_info.contains("::" + info))) {
                source_info += "::" + info;
            } else {
                UserError.Log.e(TAG, "Ignoring duplicate source info " + source_info + " -> " + info);
            }
        }
        return this;
    }

    public boolean isBackfilled() {
        return raw_data == SPECIAL_G5_PLACEHOLDER;
    }

    public boolean isRemote() {
        return filtered_data == SPECIAL_REMOTE_PLACEHOLDER;
    }

    public static final double SPECIAL_RAW_NOT_AVAILABLE = -0.1279;
    public static final double SPECIAL_G5_PLACEHOLDER = -0.1597;
    public static final double SPECIAL_FOLLOWER_PLACEHOLDER = -0.1486;
    public static final double SPECIAL_REMOTE_PLACEHOLDER = -0.1375;

    public static BgReading bgReadingInsertFromG5(double calculated_value, long timestamp) {
        return bgReadingInsertFromG5(calculated_value, timestamp, null);
    }

                       // TODO can these methods be unified to reduce duplication
                                                               // TODO remember to sync this with wear code base
    public static synchronized BgReading bgReadingInsertFromG5(double calculated_value, long timestamp, String sourceInfoAppend) {

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.w(TAG, "No sensor, ignoring this bg reading");
            return null;
        }
        // TODO slope!!
        final BgReading existing = getForPreciseTimestamp(timestamp, Constants.MINUTE_IN_MS);
        if (existing == null) {
            final BgReading bgr = new BgReading();
            bgr.sensor = sensor;
            bgr.sensor_uuid = sensor.uuid;
            bgr.time_since_sensor_started = JoH.msSince(sensor.started_at); // is there a helper for this?
            bgr.timestamp = timestamp;
            bgr.uuid = UUID.randomUUID().toString();
            bgr.calculated_value = calculated_value;
            bgr.raw_data = SPECIAL_G5_PLACEHOLDER; // placeholder
            bgr.appendSourceInfo("G5 Native");
            if (sourceInfoAppend != null && sourceInfoAppend.length() > 0) {
                bgr.appendSourceInfo(sourceInfoAppend);
            }
            bgr.save();
            if (JoH.ratelimit("sync wakelock", 15)) {
                final PowerManager.WakeLock linger = JoH.getWakeLock("G5 Insert", 4000);
            }
            Inevitable.stackableTask("NotifySyncBgr", 3000, () -> notifyAndSync(bgr));
            return bgr;
        } else {
            return existing;
        }
    }

    public static synchronized BgReading bgReadingInsertMedtrum(double calculated_value, long timestamp, String sourceInfoAppend, double raw_data) {

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.w(TAG, "No sensor, ignoring this bg reading");
            return null;
        }
        // TODO slope!!
        final BgReading existing = getForPreciseTimestamp(timestamp, Constants.MINUTE_IN_MS);
        if (existing == null) {
            final BgReading bgr = new BgReading();
            bgr.sensor = sensor;
            bgr.sensor_uuid = sensor.uuid;
            bgr.time_since_sensor_started = JoH.msSince(sensor.started_at); // is there a helper for this?
            bgr.timestamp = timestamp;
            bgr.uuid = UUID.randomUUID().toString();
            bgr.calculated_value = calculated_value;
            bgr.raw_data = raw_data / 1000d;
            bgr.filtered_data = bgr.raw_data;
            if (sourceInfoAppend != null && sourceInfoAppend.equals("Backfill")) {
                bgr.raw_data = BgReading.SPECIAL_G5_PLACEHOLDER;
            } else {
                bgr.calculateAgeAdjustedRawValue();
            }
            bgr.appendSourceInfo("Medtrum Native");
            if (sourceInfoAppend != null && sourceInfoAppend.length() > 0) {
                bgr.appendSourceInfo(sourceInfoAppend);
            }
            bgr.save();
            if (JoH.ratelimit("sync wakelock", 15)) {
                final PowerManager.WakeLock linger = JoH.getWakeLock("Medtrum Insert", 4000);
            }
            Inevitable.task("NotifySyncBgr" + bgr.timestamp, 3000, () -> notifyAndSync(bgr));
            if (bgr.isBackfilled()) {
                handleResyncWearAfterBackfill(bgr.timestamp);
            }
            return bgr;
        } else {
            return existing;
        }
    }
    public static synchronized BgReading bgReadingInsertLibre2(double calculated_value, long timestamp, double raw_data) {

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.w(TAG, "No sensor, ignoring this bg reading");
            return null;
        }
        // TODO slope!!
        final BgReading existing = getForPreciseTimestamp(timestamp, Constants.MINUTE_IN_MS);
        if (existing == null) {
            Calibration calibration = Calibration.lastValid();
            final BgReading bgReading = new BgReading();
            if (calibration == null) {
                Log.d(TAG, "create: No calibration yet");
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.raw_data = raw_data;
                bgReading.age_adjusted_raw_value = raw_data;
                bgReading.filtered_data = raw_data;
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();
                bgReading.calculated_value = calculated_value;
                bgReading.calculated_value_slope = 0;
                bgReading.hide_slope = false;
                bgReading.appendSourceInfo("Libre2 Native");
                bgReading.find_slope();

                bgReading.save();
                bgReading.perform_calculations();
                bgReading.postProcess(false);

            } else {
                Log.d(TAG, "Calibrations, so doing everything bgReading = " + bgReading);
                bgReading.sensor = sensor;
                bgReading.sensor_uuid = sensor.uuid;
                bgReading.calibration = calibration;
                bgReading.calibration_uuid = calibration.uuid;
                bgReading.raw_data = raw_data ;
                bgReading.age_adjusted_raw_value = raw_data;
                bgReading.filtered_data = raw_data;
                bgReading.timestamp = timestamp;
                bgReading.uuid = UUID.randomUUID().toString();

                bgReading.calculated_value = ((calibration.slope * calculated_value) + calibration.intercept);
                bgReading.filtered_calculated_value = ((calibration.slope * bgReading.ageAdjustedFiltered()) + calibration.intercept);

                bgReading.calculated_value_slope = 0;
                bgReading.hide_slope = false;
                bgReading.appendSourceInfo("Libre2 Native");

                BgReading.updateCalculatedValueToWithinMinMax(bgReading);

                bgReading.find_slope();
                bgReading.save();

                bgReading.postProcess(false);

            }

           return bgReading;
        } else {
            return existing;
        }
    }

    public static void handleResyncWearAfterBackfill(final long earliest) {
        if (earliest_backfill == 0 || earliest < earliest_backfill) earliest_backfill = earliest;
        if (WatchUpdaterService.isEnabled()) {
            Inevitable.task("wear-backfill-sync", 10000, () -> {
                WatchUpdaterService.startServiceAndResendDataIfNeeded(earliest_backfill);
                earliest_backfill = 0;
            });
        }
    }

    public void setRemoteMarker() {
        filtered_data = SPECIAL_REMOTE_PLACEHOLDER;
    }


    public static void notifyAndSync(final BgReading bgr) {
        final boolean recent = bgr.isCurrent();
        if (recent) {
            Notifications.start(); // may not be needed as this is duplicated in handleNewBgReading
            // probably not wanted for G5 internal values?
            //bgr.injectNoise(true); // Add noise parameter for nightscout
            //bgr.injectDisplayGlucose(BestGlucose.getDisplayGlucose()); // Add display glucose for nightscout
        }
        BgSendQueue.handleNewBgReading(bgr, "create", xdrip.getAppContext(), Home.get_follower(), !recent); // pebble and widget and follower
    }

    public static BgReading bgReadingInsertFromJson(String json, boolean do_notification) {
        return bgReadingInsertFromJson(json, do_notification, WholeHouse.isEnabled());
    }

    public static BgReading bgReadingInsertFromJson(String json, boolean do_notification, boolean force_sensor) {
        if ((json == null) || (json.length() == 0)) {
            Log.e(TAG, "bgreadinginsertfromjson passed a null or zero length json");
            return null;
        }
        final BgReading bgr = fromJSON(json);
        if (bgr != null) {
            try {
                if (readingNearTimeStamp(bgr.timestamp) == null) {
                    FixCalibration(bgr);
                    if (force_sensor) {
                        final Sensor forced_sensor = Sensor.currentSensor();
                        if (forced_sensor != null) {
                            bgr.sensor = forced_sensor;
                            bgr.sensor_uuid = forced_sensor.uuid;
                        }
                        if (Pref.getBooleanDefaultFalse("illustrate_remote_data")) {
                            bgr.setRemoteMarker();
                        }
                    }
                    final long now = JoH.tsl();
                    if (bgr.timestamp > now) {
                        UserError.Log.wtf(TAG, "Received a bg reading that appears to be in the future: " + JoH.dateTimeText(bgr.timestamp) + " vs " + JoH.dateTimeText(now));
                    }
                    bgr.save();
                    if (do_notification) {
                        Notifications.start(); // this may not be needed as it fires in handleNewBgReading
                        //xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), Notifications.class)); // alerts et al
                        BgSendQueue.handleNewBgReading(bgr, "create", xdrip.getAppContext(), Home.get_follower()); // pebble and widget and follower
                    }
                } else {
                    Log.d(TAG, "Ignoring duplicate bgr record due to timestamp: " + json);
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not save BGR: " + e.toString());
            }
        } else {
            Log.e(TAG,"Got null bgr from json");
        }
        return bgr;
    }

    // TODO this method shares some code with above.. merge
    public static void bgReadingInsertFromInt(int value, long timestamp, boolean do_notification) {
        // TODO sanity check data!

        if ((value <= 0) || (timestamp <= 0)) {
            Log.e(TAG, "Invalid data fed to InsertFromInt");
            return;
        }

        BgReading bgr = new BgReading();

        if (bgr != null) {
            bgr.uuid = UUID.randomUUID().toString();

            bgr.timestamp = timestamp;
            bgr.calculated_value = value;


            // rough code for testing!
            bgr.filtered_calculated_value = value;
            bgr.raw_data = value;
            bgr.age_adjusted_raw_value = value;
            bgr.filtered_data = value;

            final Sensor forced_sensor = Sensor.currentSensor();
            if (forced_sensor != null) {
                bgr.sensor = forced_sensor;
                bgr.sensor_uuid = forced_sensor.uuid;
            }

            try {
                if (readingNearTimeStamp(bgr.timestamp) == null) {
                    bgr.save();
                    bgr.find_slope();
                    if (do_notification) {
                       // xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), Notifications.class)); // alerts et al
                        Notifications.start(); // this may not be needed as it is duplicated in handleNewBgReading
                    }
                    BgSendQueue.handleNewBgReading(bgr, "create", xdrip.getAppContext(), false, !do_notification); // pebble and widget
                } else {
                    Log.d(TAG, "Ignoring duplicate bgr record due to timestamp: " + timestamp);
                }
            } catch (Exception e) {
                Log.d(TAG, "Could not save BGR: " + e.toString());
            }
        } else {
            Log.e(TAG,"Got null bgr from create");
        }
    }

    public static BgReading byUUID(String uuid) {
        if (uuid == null) return null;
        return new Select()
                .from(BgReading.class)
                .where("uuid = ?", uuid)
                .executeSingle();
    }

    public static BgReading byid(long id) {
        return new Select()
                .from(BgReading.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static BgReading fromJSON(String json) {
        if (json.length()==0)
        {
            Log.d(TAG,"Empty json received in bgreading fromJson");
            return null;
        }
        try {
            Log.d(TAG, "Processing incoming json: " + json);
           return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json,BgReading.class);
        } catch (Exception e) {
            Log.d(TAG, "Got exception parsing BgReading json: " + e.toString());
            Home.toaststaticnext("Error on BGReading sync, probably decryption key mismatch");
            return null;
        }
    }

    private BgReadingMessage toMessageNative() {
        return new BgReadingMessage.Builder()
                .timestamp(timestamp)
                //.a(a)
                //.b(b)
                //.c(c)
                .age_adjusted_raw_value(age_adjusted_raw_value)
                .calculated_value(calculated_value)
                .filtered_calculated_value(filtered_calculated_value)
                .calibration_flag(calibration_flag)
                .raw_calculated(raw_calculated)
                .raw_data(raw_data)
                .calculated_value_slope(calculated_value_slope)
                //.calibration_uuid(calibration_uuid)
                .uuid(uuid)
                .build();
    }

    public byte[] toMessage() {
        final List<BgReading> btl = new ArrayList<>();
        btl.add(this);
        return toMultiMessage(btl);
    }

    public static byte[] toMultiMessage(List<BgReading> bgl) {
        if (bgl == null) return null;
        final List<BgReadingMessage> BgReadingMessageList = new ArrayList<>();
        for (BgReading bg : bgl) {
            BgReadingMessageList.add(bg.toMessageNative());
        }
        return BgReadingMultiMessage.ADAPTER.encode(new BgReadingMultiMessage(BgReadingMessageList));
    }

    private static final long CLOSEST_READING_MS = 290000;
    private static void processFromMessage(BgReadingMessage btm) {
        if ((btm != null) && (btm.uuid != null) && (btm.uuid.length() == 36)) {
            BgReading bg = byUUID(btm.uuid);
            if (bg != null) {
                // we already have this uuid and we don't have a circumstance to update the record, so quick return here
                return;
            }
            if (bg == null) {
                bg = getForPreciseTimestamp(Wire.get(btm.timestamp, BgReadingMessage.DEFAULT_TIMESTAMP), CLOSEST_READING_MS, false);
                if (bg != null) {
                    UserError.Log.wtf(TAG, "Error matches a different uuid with the same timestamp: " + bg.uuid + " vs " + btm.uuid + " skipping!");
                    return;
                }
                bg = new BgReading();
            }

            bg.timestamp = Wire.get(btm.timestamp, BgReadingMessage.DEFAULT_TIMESTAMP);
            bg.calculated_value = Wire.get(btm.calculated_value, BgReadingMessage.DEFAULT_CALCULATED_VALUE);
            bg.filtered_calculated_value = Wire.get(btm.filtered_calculated_value, BgReadingMessage.DEFAULT_FILTERED_CALCULATED_VALUE);
            bg.calibration_flag = Wire.get(btm.calibration_flag, BgReadingMessage.DEFAULT_CALIBRATION_FLAG);
            bg.raw_calculated = Wire.get(btm.raw_calculated, BgReadingMessage.DEFAULT_RAW_CALCULATED);
            bg.raw_data = Wire.get(btm.raw_data, BgReadingMessage.DEFAULT_RAW_DATA);
            bg.calculated_value_slope = Wire.get(btm.calculated_value_slope, BgReadingMessage.DEFAULT_CALCULATED_VALUE_SLOPE);
            bg.calibration_uuid = btm.calibration_uuid;
            bg.uuid = btm.uuid;
            bg.save();
        } else {
            UserError.Log.wtf(TAG, "processFromMessage uuid is null or invalid");
        }
    }

    public synchronized static void processFromMultiMessage(byte[] payload) {
        try {
            final BgReadingMultiMessage bgmm = BgReadingMultiMessage.ADAPTER.decode(payload);
            if ((bgmm != null) && (bgmm.bgreading_message != null)) {
                for (BgReadingMessage btm : bgmm.bgreading_message) {
                    processFromMessage(btm);
                }
                Home.staticRefreshBGCharts();
            }
        } catch (IOException | NullPointerException | IllegalStateException e) {
            UserError.Log.e(TAG, "exception processFromMessage: " + e);
        }
    }

    public String toJSON(boolean sendCalibration) {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("a", a); // how much of this do we actually need?
            jsonObject.put("b", b);
            jsonObject.put("c", c);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("age_adjusted_raw_value", age_adjusted_raw_value);
            jsonObject.put("calculated_value", calculated_value);
            jsonObject.put("filtered_calculated_value", filtered_calculated_value);
            jsonObject.put("calibration_flag", calibration_flag);
            jsonObject.put("filtered_data", filtered_data);
            jsonObject.put("raw_calculated", raw_calculated);
            jsonObject.put("raw_data", raw_data);
            try {
                jsonObject.put("calculated_value_slope", calculated_value_slope);
            } catch (JSONException e) {
                jsonObject.put("hide_slope", true); // calculated value slope is NaN - hide slope should already be true locally too
            }
            if (sendCalibration) {
                jsonObject.put("calibration_uuid", calibration_uuid);
            }
            //   jsonObject.put("sensor", sensor);
            return jsonObject.toString();
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Error producing in toJSON: " + e);
            if (Double.isNaN(a)) Log.e(TAG, "a is NaN");
            if (Double.isNaN(b)) Log.e(TAG, "b is NaN");
            if (Double.isNaN(c)) Log.e(TAG, "c is NaN");
            if (Double.isNaN(age_adjusted_raw_value)) Log.e(TAG, "age_adjusted_raw_value is NaN");
            if (Double.isNaN(calculated_value)) Log.e(TAG, "calculated_value is NaN");
            if (Double.isNaN(filtered_calculated_value)) Log.e(TAG, "filtered_calculated_value is NaN");
            if (Double.isNaN(filtered_data)) Log.e(TAG, "filtered_data is NaN");
            if (Double.isNaN(raw_calculated)) Log.e(TAG, "raw_calculated is NaN");
            if (Double.isNaN(raw_data)) Log.e(TAG, "raw_data is NaN");
            if (Double.isNaN(calculated_value_slope)) Log.e(TAG, "calculated_value_slope is NaN");
            return "";
        }
    }

    public static void deleteALL() {
        try {
            SQLiteUtils.execSql("delete from BgSendQueue");
            SQLiteUtils.execSql("delete from BgReadings");
            Log.d(TAG, "Deleting all BGReadings");
        } catch (Exception e) {
            Log.e(TAG, "Got exception running deleteALL " + e.toString());
        }
    }

    public static void deleteRandomData() {
        Random rand = new Random();
        int  minutes_ago_end = rand.nextInt(120);
        int  minutes_ago_start = minutes_ago_end + rand.nextInt(35)+5;
        long ts_start = JoH.tsl() - minutes_ago_start * Constants.MINUTE_IN_MS;
        long ts_end = JoH.tsl() - minutes_ago_end * Constants.MINUTE_IN_MS;
        UserError.Log.d(TAG,"Deleting random bgreadings: "+JoH.dateTimeText(ts_start)+" -> "+JoH.dateTimeText(ts_end));
        testDeleteRange(ts_start, ts_end);
    }

    public static void testDeleteRange(long start_time, long end_time) {
        List<BgReading> bgrs = new Delete()
                .from(BgReading.class)
                .where("timestamp < ?", end_time)
                .where("timestamp > ?",start_time)
                .execute();
       // UserError.Log.d("OB1TEST","Deleted: "+bgrs.size()+" records");
    }

    public static List<BgReading> cleanup(int retention_days) {
        return new Delete()
                .from(BgReading.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static void cleanupOutOfRangeValues() {
        new Delete()
                .from(BgReading.class)
                .where("timestamp > ?", JoH.tsl() - (3 * Constants.DAY_IN_MS))
                .where("calculated_value > ?", 324)
                .execute();
    }


    // used in wear
    public static void cleanup(long timestamp) {
        try {
            SQLiteUtils.execSql("delete from BgSendQueue");
            List<BgReading> data = new Select()
                    .from(BgReading.class)
                    .where("timestamp < ?", timestamp)
                    .orderBy("timestamp desc")
                    .execute();
            if (data != null) Log.d(TAG, "cleanup BgReading size=" + data.size());
            new Cleanup().execute(data);
        } catch (Exception e) {
            Log.e(TAG, "Got exception running cleanup " + e.toString());
        }
    }

    // used in wear
    private static class Cleanup extends AsyncTask<List<BgReading>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<BgReading>... errors) {
            try {
                for(BgReading data : errors[0]) {
                    data.delete();
                }
                return true;
            } catch(Exception e) {
                return false;
            }
        }
    }


    //*******INSTANCE METHODS***********//
    public void perform_calculations() {
        find_new_curve();
        find_new_raw_curve();
        find_slope();
    }

    public void find_slope() {
        List<BgReading> last_2 = BgReading.latest(2);

        // FYI: By default, assertions are disabled at runtime. Add "-ea" to commandline to enable.
        // https://docs.oracle.com/javase/7/docs/technotes/guides/language/assert.html
        assert last_2.get(0).uuid.equals(this.uuid)
                : "Invariant condition not fulfilled: calculating slope and current reading wasn't saved before";

        if ((last_2 != null) && (last_2.size() == 2)) {
            calculated_value_slope = calculateSlope(this, last_2.get(1));
            save();
        } else if ((last_2 != null) && (last_2.size() == 1)) {
            calculated_value_slope = 0;
            save();
        } else {
            if (JoH.ratelimit("no-bg-couldnt-find-slope", 15)) {
                Log.w(TAG, "NO BG? COULDNT FIND SLOPE!");
            }
        }
    }


    public void find_new_curve() {
        JoH.clearCache();
        List<BgReading> last_3 = BgReading.latest(3);
        if ((last_3 != null) && (last_3.size() == 3)) {
            BgReading latest = last_3.get(0);
            BgReading second_latest = last_3.get(1);
            BgReading third_latest = last_3.get(2);

            double y3 = latest.calculated_value;
            double x3 = latest.timestamp;
            double y2 = second_latest.calculated_value;
            double x2 = second_latest.timestamp;
            double y1 = third_latest.calculated_value;
            double x1 = third_latest.timestamp;

            a = y1/((x1-x2)*(x1-x3))+y2/((x2-x1)*(x2-x3))+y3/((x3-x1)*(x3-x2));
            b = (-y1*(x2+x3)/((x1-x2)*(x1-x3))-y2*(x1+x3)/((x2-x1)*(x2-x3))-y3*(x1+x2)/((x3-x1)*(x3-x2)));
            c = (y1*x2*x3/((x1-x2)*(x1-x3))+y2*x1*x3/((x2-x1)*(x2-x3))+y3*x1*x2/((x3-x1)*(x3-x2)));

            Log.i(TAG, "find_new_curve: BG PARABOLIC RATES: "+a+"x^2 + "+b+"x + "+c);

            save();
        } else if ((last_3 != null) && (last_3.size() == 2)) {

            Log.i(TAG, "find_new_curve: Not enough data to calculate parabolic rates - assume Linear");
                BgReading latest = last_3.get(0);
                BgReading second_latest = last_3.get(1);

                double y2 = latest.calculated_value;
                double x2 = latest.timestamp;
                double y1 = second_latest.calculated_value;
                double x1 = second_latest.timestamp;

                if(y1 == y2) {
                    b = 0;
                } else {
                    b = (y2 - y1)/(x2 - x1);
                }
                a = 0;
                c = -1 * ((latest.b * x1) - y1);

            Log.i(TAG, ""+latest.a+"x^2 + "+latest.b+"x + "+latest.c);
                save();
            } else {
            Log.i(TAG, "find_new_curve: Not enough data to calculate parabolic rates - assume static data");
            a = 0;
            b = 0;
            c = calculated_value;

            Log.i(TAG, ""+a+"x^2 + "+b+"x + "+c);
            save();
        }
    }

    public void calculateAgeAdjustedRawValue(){
        final double adjust_for = AGE_ADJUSTMENT_TIME - time_since_sensor_started;
        if ((adjust_for > 0) && (!DexCollectionType.hasLibre())) {
            age_adjusted_raw_value = ((AGE_ADJUSTMENT_FACTOR * (adjust_for / AGE_ADJUSTMENT_TIME)) * raw_data) + raw_data;
            Log.i(TAG, "calculateAgeAdjustedRawValue: RAW VALUE ADJUSTMENT FROM:" + raw_data + " TO: " + age_adjusted_raw_value);
        } else {
            age_adjusted_raw_value = raw_data;
        }
    }

    void find_new_raw_curve() {
        JoH.clearCache();
        final List<BgReading> last_3 = BgReading.latest(3);
        if ((last_3 != null) && (last_3.size() == 3)) {

            final BgReading latest = last_3.get(0);
            final BgReading second_latest = last_3.get(1);
            final BgReading third_latest = last_3.get(2);

            double y3 = latest.age_adjusted_raw_value;
            double x3 = latest.timestamp;
            double y2 = second_latest.age_adjusted_raw_value;
            double x2 = second_latest.timestamp;
            double y1 = third_latest.age_adjusted_raw_value;
            double x1 = third_latest.timestamp;

            ra = y1/((x1-x2)*(x1-x3))+y2/((x2-x1)*(x2-x3))+y3/((x3-x1)*(x3-x2));
            rb = (-y1*(x2+x3)/((x1-x2)*(x1-x3))-y2*(x1+x3)/((x2-x1)*(x2-x3))-y3*(x1+x2)/((x3-x1)*(x3-x2)));
            rc = (y1*x2*x3/((x1-x2)*(x1-x3))+y2*x1*x3/((x2-x1)*(x2-x3))+y3*x1*x2/((x3-x1)*(x3-x2)));

            Log.i(TAG, "find_new_raw_curve: RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
            save();
        } else if ((last_3 != null) && (last_3.size()) == 2) {
            BgReading latest = last_3.get(0);
            BgReading second_latest = last_3.get(1);

            double y2 = latest.age_adjusted_raw_value;
            double x2 = latest.timestamp;
            double y1 = second_latest.age_adjusted_raw_value;
            double x1 = second_latest.timestamp;
            if(y1 == y2) {
                rb = 0;
            } else {
                rb = (y2 - y1)/(x2 - x1);
            }
            ra = 0;
            rc = -1 * ((latest.rb * x1) - y1);

            Log.i(TAG, "find_new_raw_curve: Not enough data to calculate parabolic rates - assume Linear data");

            Log.i(TAG, "RAW PARABOLIC RATES: "+ra+"x^2 + "+rb+"x + "+rc);
            save();
        } else {
            Log.i(TAG, "find_new_raw_curve: Not enough data to calculate parabolic rates - assume static data");
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
    private static double weightedAverageRaw(double timeA, double timeB, double calibrationTime, double rawA, double rawB) {
        final double relativeSlope = (rawB -  rawA)/(timeB - timeA);
        final double relativeIntercept = rawA - (relativeSlope * timeA);
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

    public String timeStamp() {
        return JoH.dateTimeText(timestamp);
    }

    public int noiseValue() {
        if(noise == null || noise.compareTo("") == 0) {
            return 1;
        } else {
            return Integer.valueOf(noise);
        }
    }

    public BgReading injectNoise(boolean save) {
        final BgReading bgReading = this;
        if (JoH.msSince(bgReading.timestamp) > Constants.MINUTE_IN_MS * 20) {
            bgReading.noise = "0";
        } else {
            BgGraphBuilder.refreshNoiseIfOlderThan(bgReading.timestamp);
            if (BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_HIGH) {
                bgReading.noise = "4";
            } else if (BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_TOO_HIGH_FOR_PREDICT) {
                bgReading.noise = "3";
            } else if (BgGraphBuilder.last_noise > BgGraphBuilder.NOISE_TRIGGER) {
                bgReading.noise = "2";
            }
        }
        if (save) bgReading.save();
        return bgReading;
    }

    // list(0) is the most recent reading.
    public static List<BgReading> getXRecentPoints(int NumReadings) {
        List<BgReading> latest = BgReading.latest(NumReadings);
        if (latest == null || latest.size() != NumReadings) {
            // for less than NumReadings readings, we can't tell what the situation
            //
            Log.d(TAG_ALERT, "getXRecentPoints we don't have enough readings, returning null");
            return null;
        }
        // So, we have at least three values...
        for(BgReading bgReading : latest) {
            Log.d(TAG_ALERT, "getXRecentPoints - reading: time = " + bgReading.timestamp + " calculated_value " + bgReading.calculated_value);
        }

        // now let's check that they are relevant. the last reading should be from the last 5 minutes,
        // x-1 more readings should be from the last (x-1)*5 minutes. we will allow 5 minutes for the last
        // x to allow one packet to be missed.
        if (new Date().getTime() - latest.get(NumReadings - 1).timestamp > (NumReadings * 5 + 6) * 60 * 1000) {
            Log.d(TAG_ALERT, "getXRecentPoints we don't have enough points from the last " + (NumReadings * 5 + 6) + " minutes, returning null");
            return null;
        }
        return latest;

    }

    public static boolean checkForPersistentHigh() {

        // skip if not enabled
        if (!Pref.getBooleanDefaultFalse("persistent_high_alert_enabled")) return false;


        List<BgReading> last = BgReading.latest(1);
        if ((last != null) && (last.size()>0)) {

            final long now = JoH.tsl();
            final long since = now - last.get(0).timestamp;
            // only process if last reading <10 mins
            if (since < 600000) {
                // check if exceeding high
                if (last.get(0).calculated_value >
                        Home.convertToMgDlIfMmol(
                                JoH.tolerantParseDouble(Pref.getString("highValue", "170")))) {

                    final double this_slope = last.get(0).calculated_value_slope * 60000;
                    //Log.d(TAG, "CheckForPersistentHigh: Slope: " + JoH.qs(this_slope));

                    // if not falling
                    if (this_slope > 0) {
                        final long high_since = Pref.getLong(PERSISTENT_HIGH_SINCE, 0);
                        if (high_since == 0) {
                            // no previous persistent high so set start as now
                            Pref.setLong(PERSISTENT_HIGH_SINCE, now);
                            Log.d(TAG, "Registering start of persistent high at time now");
                        } else {
                            final long high_for_mins = (now - high_since) / (1000 * 60);
                            long threshold_mins;
                            try {
                                threshold_mins = Long.parseLong(Pref.getString("persistent_high_threshold_mins", "60"));
                            } catch (NumberFormatException e) {
                                threshold_mins = 60;
                                Home.toaststaticnext("Invalid persistent high for longer than minutes setting: using 60 mins instead");
                            }
                            if (high_for_mins > threshold_mins) {
                                // we have been high for longer than the threshold - raise alert

                                // except if alerts are disabled
                                if (Pref.getLong("alerts_disabled_until", 0) > new Date().getTime()) {
                                    Log.i(TAG, "checkforPersistentHigh: Notifications are currently disabled cannot alert!!");
                                    return false;
                                }
                                Log.i(TAG, "Persistent high for: " + high_for_mins + " mins -> alerting");
                                Notifications.persistentHighAlert(xdrip.getAppContext(), true, xdrip.getAppContext().getString(R.string.persistent_high_for_greater_than) + (int) high_for_mins + xdrip.getAppContext().getString(R.string.space_mins));

                            } else {
                                Log.d(TAG, "Persistent high below time threshold at: " + high_for_mins);
                            }
                        }
                    }
                } else {
                    // not high - cancel any existing
                    if (Pref.getLong(PERSISTENT_HIGH_SINCE,0)!=0)
                    {
                        Log.i(TAG,"Cancelling previous persistent high as we are no longer high");
                     Pref.setLong(PERSISTENT_HIGH_SINCE, 0); // clear it
                        Notifications.persistentHighAlert(xdrip.getAppContext(), false, ""); // cancel it
                    }
                }
            }
        }
        return false; // actually we should probably return void as we do everything inside this method
    }

    public static void checkForRisingAllert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean rising_alert = prefs.getBoolean("rising_alert", false);
        if(!rising_alert) {
            return;
        }
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.i("NOTIFICATIONS", "checkForRisingAllert: Notifications are currently disabled!!");
            return;
        }

        String riseRate = prefs.getString("rising_bg_val", "2");
        float friseRate = 2;

        try
        {
            friseRate = Float.parseFloat(riseRate);
        }
        catch (NumberFormatException nfe)
        {
            Log.e(TAG_ALERT, "checkForRisingAllert reading falling_bg_val failed, continuing with 2", nfe);
        }
        Log.d(TAG_ALERT, "checkForRisingAllert will check for rate of " + friseRate);

        boolean riseAlert = checkForDropRiseAllert(friseRate, false);
        Notifications.RisingAlert(context, riseAlert);
    }


    public static void checkForDropAllert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean falling_alert = prefs.getBoolean("falling_alert", false);
        if(!falling_alert) {
            return;
        }
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.d("NOTIFICATIONS", "checkForDropAllert: Notifications are currently disabled!!");
            return;
        }

        String dropRate = prefs.getString("falling_bg_val", "2");
        float fdropRate = 2;

        try
        {
            fdropRate = Float.parseFloat(dropRate);
        }
        catch (NumberFormatException nfe)
        {
            Log.e(TAG_ALERT, "reading falling_bg_val failed, continuing with 2", nfe);
        }
        Log.i(TAG_ALERT, "checkForDropAllert will check for rate of " + fdropRate);

        boolean dropAlert = checkForDropRiseAllert(fdropRate, true);
        Notifications.DropAlert(context, dropAlert);
    }

    // true say, alert is on.
    private static boolean checkForDropRiseAllert(float MaxSpeed, boolean drop) {
        Log.d(TAG_ALERT, "checkForDropRiseAllert called drop=" + drop);
        List<BgReading> latest = getXRecentPoints(4);
        if(latest == null) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert we don't have enough points from the last 15 minutes, returning false");
            return false;
        }
        float time3 = (latest.get(0).timestamp - latest.get(3).timestamp) / 60000;
        double bg_diff3 = latest.get(3).calculated_value - latest.get(0).calculated_value;
        if (!drop) {
            bg_diff3 *= (-1);
        }
        Log.i(TAG_ALERT, "bg_diff3=" + bg_diff3 + " time3 = " + time3);
        if(bg_diff3 < time3 * MaxSpeed) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert for latest 4 points not fast enough, returning false");
            return false;
        }
        // we should alert here, but if the last measurement was less than MaxSpeed / 2, I won't.


        float time1 = (latest.get(0).timestamp - latest.get(1).timestamp) / 60000;
        double bg_diff1 = latest.get(1).calculated_value - latest.get(0).calculated_value;
        if (!drop) {
            bg_diff1 *= (-1);
        }

        if(time1 > 7.0) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert the two points are not close enough, returning true");
            return true;
        }
        if(bg_diff1 < time1 * MaxSpeed /2) {
            Log.d(TAG_ALERT, "checkForDropRiseAllert for latest 2 points not fast enough, returning false");
            return false;
        }
        Log.d(TAG_ALERT, "checkForDropRiseAllert returning true speed is " + (bg_diff3 / time3));
        return true;
    }

    // Make sure that this function either sets the alert or removes it.
    public static boolean getAndRaiseUnclearReading(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.d("NOTIFICATIONS", "getAndRaiseUnclearReading Notifications are currently disabled!!");
            UserNotification.DeleteNotificationByType("bg_unclear_readings_alert");
            return false;
        }

        Boolean bg_unclear_readings_alerts = prefs.getBoolean("bg_unclear_readings_alerts", false);
        if (!bg_unclear_readings_alerts
                || !DexCollectionType.hasFiltered()
                || Ob1G5CollectionService.usingG6()
                || Ob1G5CollectionService.usingNativeMode()) {
            Log.d(TAG_ALERT, "getUnclearReading returned false since feature is disabled");
            UserNotification.DeleteNotificationByType("bg_unclear_readings_alert");
            return false;
        }
        Long UnclearTimeSetting = Long.parseLong(prefs.getString("bg_unclear_readings_minutes", "90")) * 60000;

        Long UnclearTime = BgReading.getUnclearTime(UnclearTimeSetting);

        if (UnclearTime >= UnclearTimeSetting ) {
            Log.d("NOTIFICATIONS", "Readings have been unclear for too long!!");
            Notifications.bgUnclearAlert(context);
            return true;
        }

        UserNotification.DeleteNotificationByType("bg_unclear_readings_alert");

        if (UnclearTime > 0 ) {
            Log.d(TAG_ALERT, "We are in an clear state, but not for too long. Alerts are disabled");
            return true;
        }

        return false;
    }
    /*
     * This function comes to check weather we are in a case that we have an allert but since things are
     * getting better we should not do anything. (This is only in the case that the alert was snoozed before.)
     * This means that if this is a low alert, and we have two readings in the last 15 minutes, and
     * either we have gone in 10 in the last two readings, or we have gone in 3 in the last reading, we
     * don't play the alert again, but rather wait for the alert to finish.
     *  I'll start with having the same values for the high alerts.
    */

    public static boolean trendingToAlertEnd(Context context, boolean above) {
        // TODO: check if we are not in an UnclerTime.
        Log.d(TAG_ALERT, "trendingToAlertEnd called");

        List<BgReading> latest = getXRecentPoints(3);
        if(latest == null) {
            Log.d(TAG_ALERT, "trendingToAlertEnd we don't have enough points from the last 15 minutes, returning false");
            return false;
        }

        if(above == false) {
            // This is a low alert, we should be going up
            if((latest.get(0).calculated_value - latest.get(1).calculated_value > 4) ||
               (latest.get(0).calculated_value - latest.get(2).calculated_value > 10)) {
                Log.d(TAG_ALERT, "trendingToAlertEnd returning true for low alert");
                return true;
            }
        } else {
            // This is a high alert we should be heading down
            if((latest.get(1).calculated_value - latest.get(0).calculated_value > 4) ||
               (latest.get(2).calculated_value - latest.get(0).calculated_value > 10)) {
                Log.d(TAG_ALERT, "trendingToAlertEnd returning true for high alert");
                return true;
            }
        }
        Log.d(TAG_ALERT, "trendingToAlertEnd returning false, not in the right direction (or not fast enough)");
        return false;

    }

    // Should that be combined with noiseValue?
    private Boolean Unclear() {
        Log.d(TAG_ALERT, "Unclear filtered_data=" + filtered_data + " raw_data=" + raw_data);
        return raw_data > filtered_data * 1.3 || raw_data < filtered_data * 0.7;
    }

    /*
     * returns the time (in ms) that the state is not clear and no alerts should work
     * The base of the algorithm is that any period can be bad or not. bgReading.Unclear() tells that.
     * a non clear bgReading means MAX_INFLUANCE time after it we are in a bad position
     * Since this code is based on heuristics, and since times are not accurate, boundary issues can be ignored.
     *
     * interstingTime is the period to check. That is if the last period is bad, we want to know how long does it go bad...
     * */

    // The extra 120,000 is to allow the packet to be delayed for some time and still be counted in that group
    // Please don't use for MAX_INFLUANCE a number that is complete multiply of 5 minutes (300,000)
    static final int MAX_INFLUANCE = 30 * 60000 - 120000; // A bad point means data is untrusted for 30 minutes.
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
            if(bgReading.timestamp <= now - MAX_INFLUANCE  && UnclearTime == 0) {
                Log.d(TAG_ALERT, "We did not have a problematic reading for MAX_INFLUANCE time, so now all is well");
                return 0l;

            }
            if (bgReading.Unclear()) {
                // here we assume that there are no missing points. Missing points might join the good and bad values as well...
                // we should have checked if we have a period, but it is hard to say how to react to them.
                Log.d(TAG_ALERT, "We have a bad reading, so setting UnclearTime to " + bgReading.timestamp);
                UnclearTime = bgReading.timestamp;
                LastGoodTime = 0l;
            } else {
                if (LastGoodTime == 0l) {
                    Log.d(TAG_ALERT, "We are starting a good period at "+ bgReading.timestamp);
                    LastGoodTime = bgReading.timestamp;
                } else {
                    // we have some good period, is it good enough?
                    if(LastGoodTime - bgReading.timestamp >= MAX_INFLUANCE) {
                        // Here UnclearTime should be already set, otherwise we will return a toob big value
                        if (UnclearTime ==0) {
                            Log.wtf(TAG_ALERT, "ERROR - UnclearTime must not be 0 here !!!");
                        }
                        Log.d(TAG_ALERT, "We have a good period from " + bgReading.timestamp + " to " + LastGoodTime + "returning " + (now - UnclearTime +5 *60000));
                        return now - UnclearTime + 5 *60000;
                    }
                }
            }
        }
        // if we are here, we have a problem... or not.
        if(UnclearTime == 0l) {
            Log.d(TAG_ALERT, "Since we did not find a good period, but we also did not find a single bad value, we assume things are good");
            return 0l;
        }
        Log.d(TAG_ALERT, "We scanned all over, but could not find a good period. we have a bad value, so assuming that the whole period is bad" +
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

    public double usedRaw() {
        Calibration calibration = Calibration.lastValid();
        if (calibration != null && calibration.check_in) {
            return raw_data;
        }
        return age_adjusted_raw_value;
    }

    public boolean isCurrent() {
        return JoH.msSince(timestamp) < Constants.MINUTE_IN_MS * 2;
    }

    public double ageAdjustedFiltered(){
        double usedRaw = usedRaw();
        if(usedRaw == raw_data || raw_data == 0d){
            return filtered_data;
        } else {
            // adjust the filtered_data with the same factor as the age adjusted raw value
            return filtered_data * (usedRaw/raw_data);
        }
    }

    // ignores calibration checkins for speed
    public double ageAdjustedFiltered_fast() {
        // adjust the filtered_data with the same factor as the age adjusted raw value
        return filtered_data * (age_adjusted_raw_value / raw_data);
    }

    // the input of this function is a string. each char can be g(=good) or b(=bad) or s(=skip, point unmissed).
    static List<BgReading> createlatestTest(String input, Long now) {
        Random randomGenerator = new Random();
        List<BgReading> out = new LinkedList<BgReading> ();
        char[] chars=  input.toCharArray();
        for(int i=0; i < chars.length; i++) {
            BgReading bg = new BgReading();
            int rand = randomGenerator.nextInt(20000) - 10000;
            bg.timestamp = now - i * 5 * 60000 + rand;
            bg.raw_data = 150;
            if(chars[i] == 'g') {
                bg.filtered_data = 151;
            } else if (chars[i] == 'b') {
                bg.filtered_data = 110;
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
        if (result >= expectedResult * 60000 - 20000 && result <= expectedResult * 60000+20000) {
            Log.d(TAG_ALERT, "Test passed");
        } else {
            Log.d(TAG_ALERT, "Test failed expectedResult = " + expectedResult + " result = "+ result / 60000.0);
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

    public int getSlopeOrdinal() {
        double slope_by_minute = calculated_value_slope * 60000;
        int ordinal = 0;
        if(!hide_slope) {
            if (slope_by_minute <= (-3.5)) {
                ordinal = 7;
            } else if (slope_by_minute <= (-2)) {
                ordinal = 6;
            } else if (slope_by_minute <= (-1)) {
                ordinal = 5;
            } else if (slope_by_minute <= (1)) {
                ordinal = 4;
            } else if (slope_by_minute <= (2)) {
                ordinal = 3;
            } else if (slope_by_minute <= (3.5)) {
                ordinal = 2;
            } else {
                ordinal = 1;
            }
        }
        return ordinal;
    }

    public int getMgdlValue() {
        return (int) calculated_value;
    }

    public long getEpochTimestamp() {
        return timestamp;
    }
}
