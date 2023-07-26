package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
//KS import com.eveningoutpost.dexdrip.AddCalibration;
//KS import com.eveningoutpost.dexdrip.GlucoseMeter.GlucoseReadingRx;
import com.eveningoutpost.dexdrip.Home;
//KS import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
//KS import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
//KS import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
//KS import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
//KS import com.eveningoutpost.dexdrip.messages.BloodTestMessage;
//KS import com.eveningoutpost.dexdrip.messages.BloodTestMultiMessage;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by jamorham on 11/12/2016.
 */

@Table(name = "BloodTest", id = BaseColumns._ID)
public class BloodTest extends Model {

    public static final long STATE_VALID = 1 << 0;
    public static final long STATE_CALIBRATION = 1 << 1;
    public static final long STATE_NOTE = 1 << 2;
    public static final long STATE_UNDONE = 1 << 3;
    public static final long STATE_OVERWRITTEN = 1 << 4;

    private static long highest_timestamp = 0;
    private static boolean patched = false;
    private final static String TAG = "BloodTest";
    private final static boolean d = false;

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "mgdl")
    public double mgdl;

    @Expose
    @Column(name = "created_timestamp")
    public long created_timestamp;

    @Expose
    @Column(name = "state")
    public long state; // bitfield

    @Expose
    @Column(name = "source")
    public String source;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;


    //KS public GlucoseReadingRx glucoseReadingRx;

    // patches and saves
    public Long saveit() {
        fixUpTable();
        return save();
    }

    public void addState(long flag) {
        state |= flag;
        save();
    }

    public void removeState(long flag) {
        state &= ~flag;
        save();
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }
/* //KS
    private BloodTestMessage toMessageNative() {
        return new BloodTestMessage.Builder()
                .timestamp(timestamp)
                .mgdl(mgdl)
                .created_timestamp(created_timestamp)
                .state(state)
                .source(source)
                .uuid(uuid)
                .build();
    }

    public byte[] toMessage() {
        final List<BloodTest> btl = new ArrayList<>();
        btl.add(this);
        return toMultiMessage(btl);
    }
*/

    // static methods
    private static final long CLOSEST_READING_MS = 30000; // 30 seconds

    public static BloodTest create(long timestamp_ms, double mgdl, String source) {
        return create(timestamp_ms, mgdl, source, null);
    }

    public static BloodTest create(long timestamp_ms, double mgdl, String source, String suggested_uuid) {

        UserError.Log.d(TAG, "Create BloodTest mgdl=" + mgdl + " timestamp=" + JoH.dateTimeText(timestamp_ms));

        if ((timestamp_ms == 0) || (mgdl == 0)) {
            UserError.Log.e(TAG, "Either timestamp or mgdl is zero - cannot create reading");
            return null;
        }

        if (timestamp_ms < 1487759433000L) {
            UserError.Log.d(TAG, "Timestamp really too far in the past @ " + timestamp_ms);
            return null;
        }

        final long now = JoH.tsl();
        if (timestamp_ms > now) {
            if ((timestamp_ms - now) > 600000) {
                UserError.Log.wtf(TAG, "Timestamp is > 10 minutes in the future! Something is wrong: " + JoH.dateTimeText(timestamp_ms));
                return null;
            }
            timestamp_ms = now; // force to now if it showed up to 10 mins in the future
        }

        final BloodTest match = getForPreciseTimestamp(timestamp_ms, CLOSEST_READING_MS);
        if (match == null) {
            final BloodTest bt = new BloodTest();
            bt.timestamp = timestamp_ms;
            bt.mgdl = mgdl;
            bt.uuid = suggested_uuid == null ? UUID.randomUUID().toString() : suggested_uuid;
            bt.created_timestamp = JoH.tsl();
            bt.state = STATE_VALID;
            bt.source = source;
            bt.saveit();
            UserError.Log.d(TAG, "Created BloodTest uuid=" + bt.uuid + " mgdl=" + bt.mgdl + " timestamp=" + JoH.dateTimeText(bt.timestamp));
           /*KS if (UploaderQueue.newEntry("insert", bt) != null) {
                SyncService.startSyncService(3000); // sync in 3 seconds
            }*/
            return bt;
        } else {
            UserError.Log.d(TAG, "Not creating new reading as timestamp is too close");
        }
        return null;
    }

    public static BloodTest createFromCal(double bg, double timeoffset, String source) {
        return createFromCal(bg, timeoffset, source, null);
    }

    public static BloodTest createFromCal(double bg, double timeoffset, String source, String suggested_uuid) {
        UserError.Log.d(TAG, "createFromCal call create");
        final String unit = Pref.getString("units", "mgdl");

        if (unit.compareTo("mgdl") != 0) {
            bg = bg * Constants.MMOLL_TO_MGDL;
        }

        if ((bg < 40) || (bg > 400)) {
            Log.wtf(TAG, "Invalid out of range bloodtest glucose mg/dl value of: " + bg);
            JoH.static_toast_long("Bloodtest out of range: " + bg + " mg/dl");
            return null;
        }

        return create((long) (new Date().getTime() - timeoffset), bg, source, suggested_uuid);
    }

    public static BloodTest last() {
        final List<BloodTest> btl = last(1);
        if ((btl != null) && (btl.size() > 0)) {
            return btl.get(0);
        } else {
            return null;
        }
    }

    public static List<BloodTest> last(int num) {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static List<BloodTest> lastMatching(int num, String match) {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .where("source like ?", match)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static BloodTest lastValid() {
        final List<BloodTest> btl = lastValid(1);
        if ((btl != null) && (btl.size() > 0)) {
            return btl.get(0);
        } else {
            return null;
        }
    }

    public static List<BloodTest> lastValid(int num) {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .where("state & ? != 0", BloodTest.STATE_VALID)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }


    public static BloodTest byUUID(String uuid) {
        if (uuid == null) return null;
        try {
            return new Select()
                    .from(BloodTest.class)
                    .where("uuid = ?", uuid)
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static BloodTest byid(long id) {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .where("_ID = ?", id)
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }
/*//KS
    public static byte[] toMultiMessage(List<BloodTest> btl) {
        if (btl == null) return null;
        final List<BloodTestMessage> BloodTestMessageList = new ArrayList<>();
        for (BloodTest bt : btl) {
            BloodTestMessageList.add(bt.toMessageNative());
        }
        return BloodTestMultiMessage.ADAPTER.encode(new BloodTestMultiMessage(BloodTestMessageList));
    }

    private static void processFromMessage(BloodTestMessage btm) {
        if ((btm != null) && (btm.uuid != null) && (btm.uuid.length() == 36)) {
            boolean is_new = false;
            BloodTest bt = byUUID(btm.uuid);
            if (bt == null) {
                bt = getForPreciseTimestamp(Wire.get(btm.timestamp, BloodTestMessage.DEFAULT_TIMESTAMP), CLOSEST_READING_MS);
                if (bt != null) {
                    UserError.Log.wtf(TAG, "Error matches a different uuid with the same timestamp: " + bt.uuid + " vs " + btm.uuid + " skipping!");
                    return;
                }
                bt = new BloodTest();
                is_new = true;
            }
            bt.timestamp = Wire.get(btm.timestamp, BloodTestMessage.DEFAULT_TIMESTAMP);
            bt.mgdl = Wire.get(btm.mgdl, BloodTestMessage.DEFAULT_MGDL);
            bt.created_timestamp = Wire.get(btm.created_timestamp, BloodTestMessage.DEFAULT_CREATED_TIMESTAMP);
            bt.state = Wire.get(btm.state, BloodTestMessage.DEFAULT_STATE);
            bt.source = Wire.get(btm.source, BloodTestMessage.DEFAULT_SOURCE);
            bt.uuid = btm.uuid;
            bt.saveit(); // de-dupe by uuid
            if (is_new) { // cannot handle updates yet
                if (UploaderQueue.newEntry(is_new ? "insert" : "update", bt) != null) {
                    if (JoH.quietratelimit("start-sync-service", 5)) {
                        SyncService.startSyncService(3000); // sync in 3 seconds
                    }
                }
            }
        } else {
            UserError.Log.wtf(TAG, "processFromMessage uuid is null or invalid");
        }
    }

    public static void processFromMultiMessage(byte[] payload) {
        try {
            final BloodTestMultiMessage btmm = BloodTestMultiMessage.ADAPTER.decode(payload);
            if ((btmm != null) && (btmm.bloodtest_message != null)) {
                for (BloodTestMessage btm : btmm.bloodtest_message) {
                    processFromMessage(btm);
                }
                Home.staticRefreshBGCharts();
            }
        } catch (IOException | NullPointerException | IllegalStateException e) {
            UserError.Log.e(TAG, "exception processFromMessage: " + e);
        }
    }
*/
    public static BloodTest fromJSON(String json) {
        if ((json == null) || (json.length() == 0)) {
            UserError.Log.d(TAG, "Empty json received in bloodtest fromJson");
            return null;
        }
        try {
            UserError.Log.d(TAG, "Processing incoming json: " + json);
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, BloodTest.class);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Got exception parsing bloodtest json: " + e.toString());
            Home.toaststaticnext("Error on Bloodtest sync, probably decryption key mismatch");
            return null;
        }
    }

    public static BloodTest getForPreciseTimestamp(long timestamp, long precision) {
        BloodTest bloodTest = new Select()
            .from(BloodTest.class)
            .where("timestamp <= ?", (timestamp + precision))
            .where("timestamp >= ?", (timestamp - precision))
            .orderBy("abs(timestamp - " + timestamp + ") asc")
            .executeSingle();
        if ((bloodTest != null) && (Math.abs(bloodTest.timestamp - timestamp) < precision)) {
            return bloodTest;
        }
        return null;
    }

    public static List<BloodTest> latest(int number) {
            try {
                return new Select()
                .from(BloodTest.class)
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return new ArrayList<>();
        }
    }

    public static List<BloodTest> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<BloodTest> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<BloodTest> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .where("state & ? != 0", BloodTest.STATE_VALID)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp desc") // warn asc!
                    .limit(number)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return new ArrayList<>();
        }
    }
/*//KS
    synchronized static void opportunisticCalibration() {
        if (Pref.getBooleanDefaultFalse("bluetooth_meter_for_calibrations_auto")) {
            final BloodTest bt = lastValid();
            if (bt == null) {
                Log.d(TAG, "opportunistic: No blood tests");
                return;
            }
            if (JoH.msSince(bt.timestamp) > Constants.DAY_IN_MS) {
                Log.d(TAG, "opportunistic: Blood test older than 1 days ago");
                return;
            }

            if ((bt.uuid != null) && (bt.uuid.length() > 1) && PersistentStore.getString("last-bt-auto-calib-uuid").equals(bt.uuid)) {
                Log.d(TAG, "Already processed uuid: " + bt.uuid);
                return;
            }

            final Calibration calibration = Calibration.lastValid();
            if (calibration == null) {
                Log.d(TAG, "opportunistic: No calibrations");
                return;
            }

            if (JoH.msSince(calibration.timestamp) < Constants.HOUR_IN_MS) {
                Log.d(TAG, "opportunistic: Last calibration less than 1 hour ago");
                return;
            }

            if (bt.timestamp <= calibration.timestamp) {
                Log.d(TAG, "opportunistic: Blood test isn't more recent than last calibration");
                return;
            }

            // get closest bgreading - must be within dexcom period and locked to sensor
            final BgReading bgReading = BgReading.getForPreciseTimestamp(bt.timestamp + (AddCalibration.estimatedInterstitialLagSeconds * 1000), BgGraphBuilder.DEXCOM_PERIOD);
            if (bgReading == null) {
                Log.d(TAG, "opportunistic: No matching bg reading");
                return;
            }

            if (bt.timestamp > highest_timestamp) {
                Accuracy.create(bt, bgReading, "xDrip Original");
                final CalibrationAbstract plugin = PluggableCalibration.getCalibrationPluginFromPreferences();
                final CalibrationAbstract.CalibrationData cd = (plugin != null) ? plugin.getCalibrationData(bgReading.timestamp) : null;
                if (plugin != null) {
                    BgReading pluginBgReading = plugin.getBgReadingFromBgReading(bgReading, cd);
                    Accuracy.create(bt, pluginBgReading, plugin.getAlgorithmName());
                }
                highest_timestamp = bt.timestamp;
            }

            if (!CalibrationRequest.isSlopeFlatEnough(bgReading)) {
                Log.d(TAG, "opportunistic: Slope is not flat enough at: " + JoH.dateTimeText(bgReading.timestamp));
                return;
            }

            // TODO store evaluation failure for this record in cache for future optimization

            // TODO Check we have prior reading as well perhaps

            UserError.Log.ueh(TAG, "Opportunistic calibration for Blood Test at " + JoH.dateTimeText(bt.timestamp) + " of " + BgGraphBuilder.unitized_string_with_units_static(bt.mgdl) + " matching sensor slope at: " + JoH.dateTimeText(bgReading.timestamp));
            final long time_since = JoH.msSince(bt.timestamp);


            Log.d(TAG, "opportunistic: attempting auto calibration");
            PersistentStore.setString("last-bt-auto-calib-uuid", bt.uuid);
            Home.startHomeWithExtra(xdrip.getAppContext(),
                    Home.BLUETOOTH_METER_CALIBRATION,
                    BgGraphBuilder.unitized_string_static(bt.mgdl),
                    Long.toString(time_since),
                    "auto");
        }
    }

    public static String evaluateAccuracy(long period) {

        // CACHE??

        final List<BloodTest> bloodTests = latestForGraph(1000, JoH.tsl() - period, JoH.tsl() - AddCalibration.estimatedInterstitialLagSeconds);
        final List<Double> difference = new ArrayList<>();
        final List<Double> plugin_difference = new ArrayList<>();
        if ((bloodTests == null) || (bloodTests.size() == 0)) return null;

        final boolean show_plugin = true;
        final CalibrationAbstract plugin = (show_plugin) ? PluggableCalibration.getCalibrationPluginFromPreferences() : null;


        for (BloodTest bt : bloodTests) {
            final BgReading bgReading = BgReading.getForPreciseTimestamp(bt.timestamp + (AddCalibration.estimatedInterstitialLagSeconds * 1000), BgGraphBuilder.DEXCOM_PERIOD);

            if (bgReading != null) {
                final Calibration calibration = bgReading.calibration;
                if (calibration == null) {
                    Log.d(TAG,"Calibration for bgReading is null! @ "+JoH.dateTimeText(bgReading.timestamp));
                    continue;
                }
                final double diff = Math.abs(bgReading.calculated_value - bt.mgdl);
                difference.add(diff);
                if (d) {
                    Log.d(TAG, "Evaluate Accuracy: difference: " + JoH.qs(diff));
                }
                final CalibrationAbstract.CalibrationData cd = (plugin != null) ? plugin.getCalibrationData(bgReading.timestamp) : null;
                if ((plugin != null) && (cd != null)) {
                    final double plugin_diff = Math.abs(bt.mgdl - plugin.getGlucoseFromBgReading(bgReading, cd));
                    plugin_difference.add(plugin_diff);
                    if (d)
                        Log.d(TAG, "Evaluate Plugin Accuracy: " + BgGraphBuilder.unitized_string_with_units_static(bt.mgdl) + " @ " + JoH.dateTimeText(bt.timestamp) + "  difference: " + JoH.qs(plugin_diff) + "/" + JoH.qs(plugin_diff * Constants.MGDL_TO_MMOLL, 2) + " calibration: " + JoH.qs(cd.slope, 2) + " " + JoH.qs(cd.intercept, 2));
                }
            }
        }

        if (difference.size() == 0) return null;
        double avg = DoubleMath.mean(difference);
        Log.d(TAG, "Average accuracy: " + accuracyAsString(avg) + "  (" + JoH.qs(avg, 5) + ")");

        if (plugin_difference.size() > 0) {
            double plugin_avg = DoubleMath.mean(plugin_difference);
            Log.d(TAG, "Plugin Average accuracy: " + accuracyAsString(plugin_avg) + "  (" + JoH.qs(plugin_avg, 5) + ")");
            return accuracyAsString(plugin_avg) + " / " + accuracyAsString(avg);
        }
        return accuracyAsString(avg);
    }

    public static String accuracyAsString(double avg) {
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        // +- symbol
        return "\u00B1" + (!domgdl ? JoH.qs(avg * Constants.MGDL_TO_MMOLL, 2) + " mmol" : JoH.qs(avg, 1) + " mgdl");
    }
*/
    public static List<BloodTest> cleanup(int retention_days) {
        return new Delete()
                .from(BloodTest.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    // create the table ourselves without worrying about model versioning and downgrading
    private static void fixUpTable() {
        if (patched) return;
        final String[] patchup = {
                "CREATE TABLE BloodTest (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE BloodTest ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE BloodTest ADD COLUMN created_timestamp INTEGER;",
                "ALTER TABLE BloodTest ADD COLUMN state INTEGER;",
                "ALTER TABLE BloodTest ADD COLUMN mgdl REAL;",
                "ALTER TABLE BloodTest ADD COLUMN source TEXT;",
                "ALTER TABLE BloodTest ADD COLUMN uuid TEXT;",
                "CREATE UNIQUE INDEX index_Bloodtest_uuid on BloodTest(uuid);",
                "CREATE UNIQUE INDEX index_Bloodtest_timestamp on BloodTest(timestamp);",
                "CREATE INDEX index_Bloodtest_created_timestamp on BloodTest(created_timestamp);",
                "CREATE INDEX index_Bloodtest_state on BloodTest(state);"};

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
                //  UserError.Log.e(TAG, "Processed patch should not have succeeded!!: " + patch);
            } catch (Exception e) {
                //  UserError.Log.d(TAG, "Patch: " + patch + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }

    public static void opportunisticCalibration() {
        // stub placeholder on wear
    }
}

