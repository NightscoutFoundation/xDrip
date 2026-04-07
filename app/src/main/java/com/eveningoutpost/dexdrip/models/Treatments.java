package com.eveningoutpost.dexdrip.models;

/**
 * Created by jamorham on 31/12/15.
 */

import android.content.Context;
import android.provider.BaseColumns;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.g5model.DexSessionKeeper;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.SyncService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utilitymodels.UndoRedo;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;
import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;
import com.eveningoutpost.dexdrip.insulin.MultipleInsulins;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static java.lang.StrictMath.abs;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

// TODO Switchable Carb models
// TODO Linear array timeline optimization

@Table(name = "Treatments", id = BaseColumns._ID)
public class Treatments extends Model {
    private static final String TAG = "jamorham " + Treatments.class.getSimpleName();

    public static final String SENSOR_START_EVENT_TYPE = "Sensor Start";
    public static final String SENSOR_STOP_EVENT_TYPE = "Sensor Stop";
    private static final String DEFAULT_EVENT_TYPE = "<none>";

    public final static String XDRIP_TAG = "xdrip";

    //public static double activityMultipler = 8.4; // somewhere between 8.2 and 8.8
    private static Treatments lastCarbs;
    private static boolean patched = false;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Expose
    @Column(name = "eventType")
    public String eventType;
    @Expose
    @Column(name = "enteredBy")
    public String enteredBy;
    @Expose
    @Column(name = "notes")
    public String notes;
    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;
    @Expose
    @Column(name = "carbs")
    public double carbs;
    @Expose
    @Column(name = "insulin")
    public double insulin;
    @Expose
    @Column(name = "insulinJSON")
    public String insulinJSON;
    @Expose
    @Column(name = "created_at")
    public String created_at;

    // don't access this directly use getInsulinInjections()
    private List<InsulinInjection> insulinInjections = null;

    private boolean hasInsulinInjections() {
        final List<InsulinInjection> injections = getInsulinInjections();
        return ((injections != null) && (injections.size() > 0));
    }

    public boolean isBasalOnly() {
        if (!hasInsulinInjections()) return false;
        boolean foundBasal = false;
        final List<InsulinInjection> injections = getInsulinInjections();
        for (InsulinInjection injection : injections) {
            Log.d(TAG,"isBasalOnly: "+injection.isBasal()+" "+injection.getInsulin());
            if (!injection.isBasal()) {
                return false;
            } else {
                foundBasal = true;
            }
        }
        return foundBasal;
    }

    private String getInsulinInjectionsShortString() {
        final StringBuilder sb = new StringBuilder();
        for (InsulinInjection injection : insulinInjections) {
            sb.append(injection.getProfile().getName());
            sb.append(" ");
            sb.append(injection.getUnits() + "U ");
        }
        return sb.toString();
    }

    private void setInsulinInjections(List<InsulinInjection> i)
    {
        // TODO possiblity here to preserve null if Multiple Injections is not enabled
        if (i == null) {
            i = new ArrayList<>();
        }
        insulinInjections = i;
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
               // .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        insulinJSON = gson.toJson(i);
    }

    // lazily populate and return InsulinInjection array from json
    List<InsulinInjection> getInsulinInjections() {
       // Log.d(TAG,"get injections: "+insulinJSON);
        if (insulinInjections == null) {
            if (insulinJSON != null) {
                try {

                    insulinInjections = new Gson().fromJson(insulinJSON, new TypeToken<ArrayList<InsulinInjection>>() {
                    }.getType());

                    StringBuilder x = new StringBuilder();
                    for (InsulinInjection y : insulinInjections) {
                        x.append(y.getProfile().getName() + " " + y.getUnits()+" ");
                    }


                } catch (Exception e) {
                    if (JoH.ratelimit("ij-json-error", 60)) {
                        UserError.Log.wtf(TAG, "Error converting insulinJson: " + e + " " + insulinJSON);
                    }
                    notes = "CORRUPT DATA";
                    // state of insulinInjections is basically undefined here as we cannot recover from corrupt data
                    // we could neutralise the treatment data in other ways perhaps.
                }
            } else {
                // return empty if not set // TODO do we want to cache this or not to avoid memory creation?
                return new ArrayList<>();
            }
        }
        return insulinInjections;
    }

    // take a simple insulin value and produce a list assuming it is bolus insulin - for legacy conversion
    static private List<InsulinInjection> convertLegacyDoseToBolusInjectionList(final double insulinSum) {
        final ArrayList<InsulinInjection> injections = new ArrayList<>();
        val profile = InsulinManager.getBolusProfile();
        if (profile != null) {
            injections.add(new InsulinInjection(profile, insulinSum));
        } else {
            UserError.Log.e(TAG, "bolus profile is null");
        }
        return injections;
    }

    // take a simple insulin value and produce a list by name of insulin - for general quick conversion
    public static List<InsulinInjection> convertLegacyDoseToInjectionListByName(final String insulinName, final double insulinSum) {
        Log.d(TAG,"convertingLegacyDoseByName: "+insulinName+" "+insulinSum);
        final Insulin insulin = InsulinManager.getProfile(insulinName);
        if (insulin == null) return null; // TODO should we actually throw an exception here as this should never happen and the result would be invalid
        final ArrayList<InsulinInjection> injections = new ArrayList<>();
        injections.add(new InsulinInjection(insulin, insulinSum));
        return injections;
    }


    public void setInsulinJSON(String json) {
        if ((json == null) || json.isEmpty())
            json = "[]";
        try {
            insulinInjections = new Gson().fromJson(json, new TypeToken<ArrayList<InsulinInjection>>() {
            }.getType());
            insulinJSON = json; // set json only if we didn't get exception processing it
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in setInsulinJson: " + e + " for " + json);
        }
    }

    public Treatments()
    {
        eventType = DEFAULT_EVENT_TYPE;
        carbs = 0;
        insulin = 0;
        //setInsulinInjections(null);
    }

    public static synchronized Treatments create(final double carbs, final double insulin, long timestamp) {
        return create(carbs, insulin, timestamp, null);
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, final long timestamp, final String suggested_uuid) {

        if (MultipleInsulins.isEnabled()) {
            return create(carbs, insulinSum, convertLegacyDoseToBolusInjectionList(insulinSum), timestamp, suggested_uuid);
        } else {
            return create(carbs, insulinSum, null, timestamp, suggested_uuid);
        }

    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, final List<InsulinInjection> insulin, long timestamp) {
        return create(carbs, insulinSum, insulin, timestamp, null);
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, final List<InsulinInjection> insulin, long timestamp, String suggested_uuid) {
        final long future_seconds = (timestamp - JoH.tsl()) / 1000;
        // if treatment more than 1 hour in the future
        if (future_seconds > (60 * 60)) {
            JoH.static_toast_long(xdrip.gs(R.string.toast_treatment_more_than_one_hour_in_the_future));
            UserError.Log.e(TAG, "Refusing to create a treatment more than 1 hour in the future!");
            return null;
        }
        // if treatment more than 3 minutes in the future
        if ((future_seconds > (3 * 60)) && (future_seconds < 86400) && ((carbs > 0) || (insulinSum > 0))) {
            final Context context = xdrip.getAppContext();
            JoH.scheduleNotification(context, "Treatment Reminder", "@" + JoH.hourMinuteString(timestamp) + " : "
                    + carbs + " g " + context.getString(R.string.carbs) + " / "
                    + insulinSum + " " + context.getString(R.string.units), (int) future_seconds, 34026);
        }
        return create(carbs, insulinSum, insulin, timestamp, -1, suggested_uuid);
    }

    public static synchronized Treatments create(final double carbs, final double insulinSum, final List<InsulinInjection> insulin, long timestamp, double position, String suggested_uuid) {
        // TODO sanity check values
        Log.d(TAG, "Creating treatment: " +
                "Insulin: " + insulinSum + " / " +
                "Carbs: " + carbs +
                (suggested_uuid != null && !suggested_uuid.isEmpty()
                        ? " " + "uuid: " + suggested_uuid
                        : ""));

        if ((carbs == 0) && (insulinSum == 0)) return null;

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();

        if (position > 0) {
            treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
        } else {
            treatment.enteredBy = XDRIP_TAG;
        }

        treatment.carbs = carbs;
        treatment.insulin = insulinSum;
        treatment.setInsulinInjections(insulin);
        treatment.timestamp = timestamp;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();
        treatment.save();
        // GcmActivity.pushTreatmentAsync(Treatment);
        //  NSClientChat.pushTreatmentAsync(Treatment);

        pushTreatmentSync(treatment);
        UndoRedo.addUndoTreatment(treatment.uuid);
        return treatment;
    }

    // Note
    public static synchronized Treatments create_note(String note, long timestamp) {
        return create_note(note, timestamp, -1, null);
    }

    public static synchronized Treatments create_note(String note, long timestamp, double position) {
        return create_note(note, timestamp, position, null);
    }

    public static synchronized Treatments create_note(String note, long timestamp, double position, String suggested_uuid) {
        // TODO sanity check values
        Log.d(TAG, "Creating treatment note: " + note);

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        if ((note == null || (note.length() == 0))) {
            Log.i(TAG, "Empty treatment note - not saving");
            return null;
        }

        boolean is_new = false;
        // find treatment

        Treatments treatment = byTimestamp(timestamp, MINUTE_IN_MS * 5);
        // if unknown create
        if (treatment == null) {
            treatment = new Treatments();
            Log.d(TAG, "Creating new treatment entry for note");
            is_new = true;

            treatment.notes = note;
            treatment.timestamp = timestamp;
            treatment.created_at = DateUtil.toISOString(timestamp);
            treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();

        } else {
            if (treatment.notes == null) treatment.notes = "";
            Log.d(TAG, "Found existing treatment for note: " + treatment.uuid + ((suggested_uuid != null) ? " vs suggested: " + suggested_uuid : "") + " distance:" + Long.toString(timestamp - treatment.timestamp) + " " + treatment.notes);
            if (treatment.notes.contains(note)) {
                Log.d(TAG, "Suggested note update already present - skipping");
                return null;
            }
            // append existing note or treatment
            if (treatment.notes.length() > 0) treatment.notes += " \u2192 ";
            treatment.notes += note;
            Log.d(TAG, "Final notes: " + treatment.notes);
        }
        //    if ((treatment.enteredBy == null) || (!treatment.enteredBy.contains(NightscoutUploader.VIA_NIGHTSCOUT_TAG))) {
        // tag it as from xdrip if it isn't being synced from nightscout right now to allow local updates to nightscout sourced notes
        if (suggested_uuid == null) {
            if (position > 0) {
                treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
            } else {
                treatment.enteredBy = XDRIP_TAG;
            }
        }

        treatment.save();

        pushTreatmentSync(treatment, is_new, suggested_uuid);
        if (is_new) UndoRedo.addUndoTreatment(treatment.uuid);

        return treatment;
    }

    static void createForTest(long timestamp, double insulin) {
        fixUpTable();
        val treatment = new Treatments();
        treatment.notes = "test";
        treatment.timestamp = timestamp;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.uuid = UUID.randomUUID().toString();
        treatment.insulin = insulin;
        treatment.save();
    }

    /**
     * Returns a newly created treatment entry in the database for Sensor Start,
     * and pushes the new treatment to followers.
     * @param timestamp is optional, defaults to right now
     * @param notes is optional
     */
    public static synchronized Treatments sensorStart(@Nullable Long timestamp, @Nullable String notes) {
        if (timestamp == null || timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();
        treatment.enteredBy = XDRIP_TAG;
        treatment.eventType = SENSOR_START_EVENT_TYPE;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        if (notes != null && notes.length() > 0) {
            treatment.notes = notes;
        }
        treatment.save();
        pushTreatmentSync(treatment);
        return treatment;
    }

    /**
     * Returns a newly created treatment entry in the database for Sensor Stop,
     * and pushes the new treatment to followers.
     * @param timestamp is optional, defaults to right now
     * @param notes is optional
     */
    public static synchronized Treatments sensorStop(@Nullable Long timestamp, @Nullable String notes) {
        if (timestamp == null || timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments treatment = new Treatments();
        treatment.enteredBy = XDRIP_TAG;
        treatment.eventType = SENSOR_STOP_EVENT_TYPE;
        treatment.created_at = DateUtil.toISOString(timestamp);
        treatment.timestamp = timestamp;
        treatment.uuid = UUID.randomUUID().toString();
        if (notes != null && notes.length() > 0) {
            treatment.notes = notes;
        }
        treatment.save();
        pushTreatmentSync(treatment);
        return treatment;
    }

    public static void sensorStartIfNeeded() {

        // Create treatment entry in the database if the sensor was started by another
        // device (e.g. receiver) and not xDrip. If the sensor was started by
        // xDrip, then there will be a Sensor Start treatment already in the db.
        val lastSensorStart = Treatments.lastEventTypeFromXdrip(Treatments.SENSOR_START_EVENT_TYPE);

        // If there isn't an existing sensor start in the xDrip db, or the most recently tracked
        // sensor start was more than 15 minutes ago, then we assume the sensor was actually
        // started from a non-xDrip device and so we track it.
        if (lastSensorStart == null || JoH.msSince(lastSensorStart.timestamp) >= 15 * Constants.MINUTE_IN_MS) {
            UserError.Log.i(TAG, "Creating treatment for Sensor Start initiated by another device");
            Treatments.sensorStart(null, "Started by transmitter");
        } else {
            UserError.Log.i(TAG, "Not creating treatment for Sensor Start because one was created too recently: " + JoH.msSince(lastSensorStart.timestamp) + "ms ago");
        }
    }

    public static void sensorUpdateStartTimeIfNeeded() {
        val lastSensorStart = Treatments.lastEventTypeFromXdrip(Treatments.SENSOR_START_EVENT_TYPE);
        long localStartedAt = lastSensorStart.timestamp; // When the xDrip local session started
        long dexStartedAt = DexSessionKeeper.getStart(); // When the current session on the transmitter started
        if (dexStartedAt > 0 && !(dexStartedAt - localStartedAt < MINUTE_IN_MS * 5)) { // If the start time of the local session is more than 5 minutes older than the one on the transmitter
            Treatments.sensorStart(dexStartedAt, "Start time updated");
        }
    }

    private static void pushTreatmentSync(Treatments treatment) {
        pushTreatmentSync(treatment, true, null); // new entry by default
    }

    private static void pushTreatmentSync(final Treatments treatment, boolean is_new, String suggested_uuid) {

        BackgroundQueue.postDelayed(() -> {
            if (Home.get_master_or_follower()) {
                GcmActivity.pushTreatmentAsync(treatment);
            }

            if (!(Pref.getBoolean("cloud_storage_api_enable", false) || Pref.getBoolean("cloud_storage_mongodb_enable", false))) {
                NSClientChat.pushTreatmentAsync(treatment);
            } else {
                Log.d(TAG, "Skipping NSClient treatment broadcast as nightscout direct sync is enabled");
            }

            if (suggested_uuid == null) {
                // only sync to nightscout if source of change was not from nightscout
                if (UploaderQueue.newEntry(is_new ? "insert" : "update", treatment) != null) {
                    SyncService.startSyncService(3000); // sync in 3 seconds
                }
            }
        },1000);

    }

    public static void pushTreatmentSyncToWatch(Treatments treatment, boolean is_new) {
        Log.d(TAG, "pushTreatmentSyncToWatch Add treatment to UploaderQueue.");
        if (Pref.getBooleanDefaultFalse("wear_sync")) {
            if (UploaderQueue.newEntryForWatch(is_new ? "insert" : "update", treatment) != null) {
                SyncService.startSyncService(3000); // sync in 3 seconds
            }
        }
    }

    // This shouldn't be needed but it seems it is
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE Treatments (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Treatments ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Treatments ADD COLUMN uuid TEXT;",
                "ALTER TABLE Treatments ADD COLUMN eventType TEXT;",
                "ALTER TABLE Treatments ADD COLUMN enteredBy TEXT;",
                "ALTER TABLE Treatments ADD COLUMN notes TEXT;",
                "ALTER TABLE Treatments ADD COLUMN created_at TEXT;",
                "ALTER TABLE Treatments ADD COLUMN insulin REAL;",
                "ALTER TABLE Treatments ADD COLUMN insulinJSON TEXT;",
                "ALTER TABLE Treatments ADD COLUMN carbs REAL;",
                "CREATE INDEX index_Treatments_timestamp on Treatments(timestamp);",
                "CREATE UNIQUE INDEX index_Treatments_uuid on Treatments(uuid);"};

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
                //Log.e(TAG, "Processed patch should not have succeeded!!: " + patch);
            } catch (Exception e) {
                // Log.d(TAG, "Patch: " + patch + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }

    public static Treatments last() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Treatments lastNotFromXdrip() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("enteredBy NOT LIKE '" + XDRIP_TAG + "%'")
                .orderBy("_ID DESC")
                .executeSingle();
    }

    public static Treatments lastEventTypeFromXdrip(final String eventType) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("enteredBy LIKE '" + XDRIP_TAG + "%' and eventType = ?", eventType)
                .orderBy("_ID DESC")
                .executeSingle();       // TODO does the where clause order affect optimization ref database indexes?
    }

    public static List<Treatments> latest(int num) {
        try {
            return new Select()
                    .from(Treatments.class)
                    .orderBy("timestamp desc")
                    .limit(num)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static Treatments byuuid(String uuid) {
        if (uuid == null) return null;
        return new Select()
                .from(Treatments.class)
                .where("uuid = ?", uuid)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static Treatments byid(long id) {
        return new Select()
                .from(Treatments.class)
                .where("_ID = ?", id)
                .executeSingle();
    }

    public static Treatments byTimestamp(long timestamp) {
        return byTimestamp(timestamp, 1500);
    }

    public static Treatments byTimestamp(long timestamp, long plus_minus_millis) {
        if (plus_minus_millis > Integer.MAX_VALUE) {
            throw new RuntimeException("Treatment by TimeStamp out of range value: " + plus_minus_millis);
        }
        return byTimestamp(timestamp, (int) plus_minus_millis);
    }

    public static Treatments byTimestamp(long timestamp, int plus_minus_millis) {
        return new Select()
                .from(Treatments.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + plus_minus_millis), (timestamp - plus_minus_millis)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static List<Treatments> listByTimestamp(long timestamp) {
        return new Select()
                .from(Treatments.class)
                .where("timestamp = ?", timestamp)
                .orderBy("timestamp desc")
                .execute();
    }

    public static void delete_all() {
        delete_all(false);
    }

    public static void delete_all(boolean from_interactive) {
        if (from_interactive) {
            GcmActivity.push_delete_all_treatments();
        }
        new Delete()
                .from(Treatments.class)
                .execute();
        // not synced with uploader queue - should we?
    }

    public static Treatments delete_last() {
        return delete_last(false);
    }

    public static void delete_by_timestamp(long timestamp) {
        delete_by_timestamp(timestamp, 1500, false);
    }

    public static void delete_by_timestamp(long timestamp, int accuracy, boolean from_interactive) {
        final Treatments t = byTimestamp(timestamp, accuracy); // do we need to alter default accuracy?
        if (t != null) {
            Log.d(TAG, "Deleting treatment closest to: " + JoH.dateTimeText(timestamp) + " matches uuid: " + t.uuid);
            delete_by_uuid(t.uuid, from_interactive);
        } else {
            Log.e(TAG, "Couldn't find a treatment near enough to " + JoH.dateTimeText(timestamp) + " to delete!");
        }
    }

    public static void delete_by_uuid(String uuid) {
        delete_by_uuid(uuid, false);
    }

    public static void delete_by_uuid(String uuid, boolean from_interactive) {
        Treatments thistreat = byuuid(uuid);
        if (thistreat != null) {

            UploaderQueue.newEntry("delete", thistreat);
            if (from_interactive) {
                GcmActivity.push_delete_treatment(thistreat);
                SyncService.startSyncService(3000); // sync in 3 seconds
            }

            thistreat.delete();
            Home.staticRefreshBGCharts();
        }
    }

    public static Treatments delete_last(boolean from_interactive) {
        Treatments thistreat = last();
        if (thistreat != null) {

            if (from_interactive) {
                GcmActivity.push_delete_treatment(thistreat);
                //GoogleDriveInterface gdrive = new GoogleDriveInterface();
                //gdrive.deleteTreatmentAtRemote(thistreat.uuid);
            }
            UploaderQueue.newEntry("delete", thistreat);
            thistreat.delete();
        }
        return null;
    }

    public static void cleanup(final int retention_days) {
        fixUpTable();
        new Delete()
                .from(Treatments.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static Treatments fromJSON(String json) {
        try {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, Treatments.class);
        } catch (Exception e) {
            Log.d(TAG, "Got exception parsing treatment json: " + e.toString());
            Home.toaststatic("Error on treatment, probably decryption key mismatch");
            return null;
        }
    }

    public static synchronized boolean pushTreatmentFromJson(String json) {
        return pushTreatmentFromJson(json, false);
    }

    public static synchronized boolean pushTreatmentFromJson(String json, boolean from_interactive) {
        Log.d(TAG, "converting treatment from json: " + json);
        final Treatments mytreatment = fromJSON(json);
        if (mytreatment != null) {
            if ((mytreatment.carbs == 0) && (mytreatment.insulin == 0)
                    && (mytreatment.notes != null) && (mytreatment.notes.startsWith("AndroidAPS started"))) {
                Log.d(TAG, "Skipping AndroidAPS started message");
                return false;
            }
            if ((mytreatment.eventType != null) && (mytreatment.eventType.equals("Temp Basal"))) {
                // we don't yet parse or process these
                Log.d(TAG, "Skipping Temp Basal msg");
                return false;
            }
            if (mytreatment.timestamp < 1) {
                Log.e(TAG, "Invalid treatment timestamp or 0 or less");
                return false;
            }

            if (mytreatment.uuid == null) {
                try {
                    final JSONObject jsonobj = new JSONObject(json);
                    if (jsonobj.has("_id")) mytreatment.uuid = jsonobj.getString("_id");
                } catch (JSONException e) {
                    //
                }
                if (mytreatment.uuid == null) mytreatment.uuid = UUID.randomUUID().toString();
            }
            // anything received +- 1500 ms is going to be treated as a duplicate
            final Treatments dupe_treatment = byTimestamp(mytreatment.timestamp);
            if (dupe_treatment != null) {
                Log.i(TAG, "Duplicate treatment for: " + mytreatment.timestamp);

                if ((dupe_treatment.insulin == 0) && (mytreatment.insulin > 0)) {
                    dupe_treatment.setInsulinJSON(mytreatment.insulinJSON);
                    dupe_treatment.insulin = mytreatment.insulin;
                    dupe_treatment.save();
                    Home.staticRefreshBGChartsOnIdle();
                }

                if ((dupe_treatment.carbs == 0) && (mytreatment.carbs > 0)) {
                    dupe_treatment.carbs = mytreatment.carbs;
                    dupe_treatment.save();
                    Home.staticRefreshBGChartsOnIdle();
                }

                if ((dupe_treatment.uuid != null) && (mytreatment.uuid != null) && (dupe_treatment.uuid.equals(mytreatment.uuid)) && (mytreatment.notes != null)) {

                    if ((dupe_treatment.notes == null) || (dupe_treatment.notes.length() < mytreatment.notes.length())) {
                        dupe_treatment.notes = mytreatment.notes;
                        fixUpTable();
                        dupe_treatment.save();
                        Log.d(TAG, "Saved updated treatement notes");
                        // should not end up needing to append notes and be from_interactive via undo as these
                        // would be mutually exclusive operations so we don't need to handle that here.
                        Home.staticRefreshBGChartsOnIdle();
                        // TODO review if this is correct place for new notes only
                        evaluateNotesForNotification(mytreatment);
                    }
                }

                return false;
            }
            Log.d(TAG, "Saving pushed treatment: " + mytreatment.uuid);
            if ((mytreatment.enteredBy == null) || (mytreatment.enteredBy.equals(""))) {
                mytreatment.enteredBy = "sync";
            }
            if ((mytreatment.eventType == null) || (mytreatment.eventType.equals(""))) {
                mytreatment.eventType = DEFAULT_EVENT_TYPE; // should have a default
            }
            if ((mytreatment.created_at == null) || (mytreatment.created_at.equals(""))) {
                try {
                    mytreatment.created_at = DateUtil.toISOString(mytreatment.timestamp); // should have a default
                } catch (Exception e) {
                    Log.e(TAG, "Could not convert timestamp to isostring");
                }
            }

            fixUpTable();
            long x = mytreatment.save();
            Log.d(TAG, "Saving treatment result: " + x);
            if (from_interactive) {
                pushTreatmentSync(mytreatment);
            }
            // TODO review if this is correct place for new notes only
            evaluateNotesForNotification(mytreatment);
            Home.staticRefreshBGChartsOnIdle();
            return true;
        } else {
            return false;
        }
    }

    private static void evaluateNotesForNotification(final Treatments mytreatment) {
        if (!emptyString(mytreatment.notes) && mytreatment.notes.startsWith("-")) {
            BlueJayEntry.sendNotifyIfEnabled(mytreatment.notes);
        }
    }

    public static List<Treatments> latestForGraph(int number, double startTime) {
        return latestForGraph(number, startTime, JoH.ts());
    }

    public static List<Treatments> latestForGraph(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static List<Treatments> latestForGraph(final int number, final long startTime, final long endTime) {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .where("timestamp >= ? and timestamp <= ?", startTime, endTime)
                .orderBy("timestamp asc")
                .limit(number)
                .execute();
    }

    public static long getTimeStampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }

    /// this is no longer used
    /* public static CobCalc cobCalc(Treatments treatment, double lastDecayedBy, double time) {

        double delay = 20; // minutes till carbs start decaying

        double delayms = delay * Constants.MINUTE_IN_MS;
        if (treatment.carbs > 0) {

            CobCalc thisCobCalc = new CobCalc();
            thisCobCalc.carbTime = treatment.timestamp;

            // no previous carb treatment? Set to our start time
            if (lastDecayedBy == 0) {
                lastDecayedBy = thisCobCalc.carbTime;
            }

            double carbs_hr = Profile.getCarbAbsorptionRate(time);
            double carbs_min = carbs_hr / 60;
            double carbs_ms = carbs_min / Constants.MINUTE_IN_MS;

            thisCobCalc.decayedBy = thisCobCalc.carbTime; // initially set to start time for this treatment

            double minutesleft = (lastDecayedBy - thisCobCalc.carbTime) / Constants.MINUTE_IN_MS;
            double how_long_till_carbs_start_ms = (lastDecayedBy - thisCobCalc.carbTime);
            thisCobCalc.decayedBy += (Math.max(delay, minutesleft) + treatment.carbs / carbs_min) * Constants.MINUTE_IN_MS;

            if (delay > minutesleft) {
                thisCobCalc.initialCarbs = treatment.carbs;
            } else {
                thisCobCalc.initialCarbs = treatment.carbs + minutesleft * carbs_min;
            }
            double startDecay = thisCobCalc.carbTime + (delay * Constants.MINUTE_IN_MS);

            if (time < lastDecayedBy || time > startDecay) {
                thisCobCalc.isDecaying = 1;
            } else {
                thisCobCalc.isDecaying = 0;
            }
            return thisCobCalc;

        } else {
            return null;
        }
    }
*/

    // when using multiple insulins
    private static Pair<Double, Double> calculateIobActivityFromTreatmentAtTime(final Treatments treatment, final long time, final boolean useBasal) {

        double iobContrib = 0, activityContrib = 0;
        if (treatment.insulin > 0) {
           // Log.d(TAG,"NEW TYPE insulin: "+treatment.insulin+ " "+treatment.insulinJSON);
            // translate a legacy entry to be bolus insulin
            List<InsulinInjection> injectionsList = treatment.getInsulinInjections();
            if (injectionsList == null || injectionsList.size() == 0) {
                Log.d(TAG,"CONVERTING LEGACY: "+treatment.insulinJSON+ " "+injectionsList);
                injectionsList = convertLegacyDoseToBolusInjectionList(treatment.insulin);
                treatment.insulinInjections = injectionsList; // cache but best not to save it
            }

            for (final InsulinInjection injection : injectionsList)
                if (injection.getUnits() > 0 && (useBasal || !injection.isBasal())) {
                    iobContrib += injection.getUnits() * abs(injection.getProfile().calculateIOB((time - treatment.timestamp) / MINUTE_IN_MS));
                    activityContrib += injection.getUnits() * abs(injection.getProfile().calculateActivity((time - treatment.timestamp) / MINUTE_IN_MS));
                }
            if (iobContrib < 0) iobContrib = 0;
            if (activityContrib < 0) activityContrib = 0;
        }
        return new Pair<>(iobContrib, activityContrib);
    }

    // using the original calculation
    private static Pair<Double, Double> calculateLegacyIobActivityFromTreatmentAtTime(final Treatments treatment, final long time) {

        final double dia = Profile.insulinActionTime(time); // duration insulin action in hours
        final double peak = 75; // minutes in based on a 3 hour DIA - scaled proportionally (orig 75)

        double insulin_delay_minutes = 0;

        double insulin_timestamp = treatment.timestamp + (insulin_delay_minutes * 60 * 1000);

        //Iob response = new Iob();

        final double scaleFactor = 3.0 / dia;
        double iobContrib = 0;
        //double activityContrib = 0;

        // only use treatments with insulin component which have already happened
        if ((treatment.insulin > 0) && (insulin_timestamp < time)) {
            //  double bolusTime = insulin_timestamp; // bit of a dupe
            double minAgo = scaleFactor * (((time - insulin_timestamp) / 1000) / 60);

            if (minAgo < peak) {
                double x1 = minAgo / 5 + 1;
                iobContrib = treatment.insulin * (1 - 0.001852 * x1 * x1 + 0.001852 * x1);
                // units: BG (mg/dL)  = (BG/U) *    U insulin     * scalar
                // activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 / peak) * minAgo;

            } else if (minAgo < 180) {
                double x2 = (minAgo - peak) / 5;
                iobContrib = treatment.insulin * (0.001323 * x2 * x2 - .054233 * x2 + .55556);
                //   activityContrib = sens * activityMultipler * treatment.insulin * (2 / dia / 60 - (minAgo - peak) * 2 / dia / 60 / (60 * dia - peak));
            }

        }
        if (iobContrib < 0) iobContrib = 0;
        //if (activityContrib < 0) activityContrib = 0;
        return new Pair<>(iobContrib, 0d);
    }



    private static Iob calcTreatment(final Treatments treatment, final long time, final boolean useBasal) {
        final Iob response = new Iob();

        if (MultipleInsulins.isEnabled()) {
            Pair<Double,Double> result = calculateIobActivityFromTreatmentAtTime(treatment, time, useBasal);
            response.iob = result.first;
            response.jActivity = result.second;
        } else {
            Pair<Double,Double> result = calculateLegacyIobActivityFromTreatmentAtTime(treatment, time);
            response.iob = result.first;
           // response.jActivity = result.second;
        }

        return response;
    }

    // requires stepms granularity which we should already have
    private static double timesliceIactivityAtTime(Map<Long, Iob> timeslices, long thistime) {
        if (timeslices.containsKey(thistime)) {
            return timeslices.get(thistime).jActivity;
        } else {
            return 0;
        }
    }

    private static void timesliceCarbWriter(Map<Long, Iob> timeslices, long thistime, double carbs) {
        // offset for carb action time??
        Iob tempiob;
        if (timeslices.containsKey(thistime)) {
            tempiob = timeslices.get(thistime);
            tempiob.cob = tempiob.cob + carbs;
        } else {
            tempiob = new Iob();
            tempiob.timestamp = (long) thistime;
         //   tempiob.date = new Date((long)thistime);
            tempiob.cob = carbs;
        }
        timeslices.put(thistime, tempiob);
    }

    private static void timesliceInsulinWriter(Map<Long, Iob> timeslices, Iob thisiob, long thistime) {
        if (thisiob.iob > 0) {
            if (timeslices.containsKey(thistime)) {
                Iob tempiob = timeslices.get(thistime);
                tempiob.iob += thisiob.iob;
                tempiob.jActivity+= thisiob.jActivity;
                timeslices.put(thistime, tempiob);
            } else {
                thisiob.timestamp = (long) thistime;
             //   thisiob.date = new Date((long)thistime);
                timeslices.put(thistime, thisiob); // first entry at timeslice so put the record in as is
            }
        }
    }

    // NEW NEW NEW
    public static List<Iob> ioBForGraph_new(long startTime) {

       // Log.d(TAG, "Processing iobforgraph2: main  ");
        JoH.benchmark_method_start();
        final boolean multipleInsulins = MultipleInsulins.isEnabled();
        final boolean useBasal = MultipleInsulins.useBasalActivity();
        // number param currently ignored

        // 10 hours max look or from insulin manager if enabled
        final double dontLookThisFar = MultipleInsulins.isEnabled() ? MINUTE_IN_MS * InsulinManager.getMaxEffect(true) : 10 * HOUR_IN_MS;
// look back the longest effect period of all enabled insulin profiles (startTime is always 24h behind NOW)
        List<Treatments> theTreatments = latestForGraph(2000, startTime - dontLookThisFar);
        Log.d(TAG,"TREATMENT LIST: "+theTreatments.size()+" "+JoH.dateTimeText((long)(startTime - dontLookThisFar)));
        if (theTreatments.size() == 0) return null;

        int counter = 0; // iteration counter

        final double step_minutes = 5;
        final long stepms = (long) (step_minutes * MINUTE_IN_MS); // 300s = 5 mins
        long mytime = startTime;
        long tendtime = startTime;


        final double carb_delay_minutes = Profile.carbDelayMinutes(mytime); // not likely a time dependent parameter
        final double carb_delay_ms_stepped = ((long) (carb_delay_minutes / step_minutes)) * step_minutes * MINUTE_IN_MS;

        Log.d(TAG, "Carb delay ms: " + carb_delay_ms_stepped);

        Map<String, Boolean> carbsEaten = new HashMap<String, Boolean>();

        // linear array populated as needed and layered by each treatment etc
        SortedMap<Long, Iob> timeslices = new TreeMap<>();
        Iob calcreply;

        // First process all IoB calculations
        for (Treatments thisTreatment : theTreatments) {
            // early optimisation exclusion

            mytime = (long) ((thisTreatment.timestamp / stepms) * stepms); // effects of treatment occur only after it is given / fit to slot time
            tendtime = mytime + 36 * HOUR_IN_MS;     // 36 hours max look (24h history plus 12h forecast)
            if (tendtime > startTime + 30 * HOUR_IN_MS)
                tendtime = startTime + 30 * HOUR_IN_MS;   // dont look more than 6h in future // TODO review time limit
            if (thisTreatment.insulin > 0) {
                // lay down insulin on board
                do {

                    calcreply = calcTreatment(thisTreatment, mytime, useBasal);
                    calcreply.jActivity *= step_minutes;    // has to be multiplied because derivation function of IOB calculates a step_minutes lower activity as the "old" logic
                    calcreply.jActivity *= Profile.getSensitivity(mytime);

                    if (mytime >= startTime) {
                        timesliceInsulinWriter(timeslices, calcreply, mytime);
                    }
                    mytime = mytime + stepms; // advance time counter
                } while ((mytime < tendtime) &&
                        ((calcreply.iob == 0) || (calcreply.iob > 0.01)));
            }
        } // per insulin treatment

        // legacy jActivity calculation
        if (!multipleInsulins) {
            Log.d(TAG, "Single insulin type iteration counter: " + counter);

            // evaluate insulin impact
            Iob lastiob = null;
            for (Map.Entry<Long, Iob> entry : timeslices.entrySet()) {
                Iob thisiob = entry.getValue();
                if (lastiob != null) {
                    if ((thisiob.iob != 0) || (lastiob.iob != 0)) {
                        if (thisiob.iob < lastiob.iob) {
                            // decaying iob
                            thisiob.jActivity = (lastiob.iob - thisiob.iob) * Profile.getSensitivity(thisiob.timestamp);
                        } else {
                            // more insulin added
                            thisiob.jActivity = 0; // TODO THIS IS NOT RIGHT IT MISSES ONE DECAY STEP
                        }
                    }
                }

                //Log.d(TAG,"iobinfo2 iob debug: "+JoH.qs(thisiob.timestamp)+" C:"+JoH.qs(thisiob.cob,4)+" I:"+JoH.qs(thisiob.iob,4)+" CA:"+JoH.qs(thisiob.jCarbImpact)+" IA:"+JoH.qs(thisiob.jActivity));
                counter++;
                lastiob = thisiob;
            }
            //
        }

        // calculate carb treatments
        for (Treatments thisTreatment : theTreatments) {

            if (thisTreatment.carbs > 0) {

                mytime = ((long) (thisTreatment.timestamp / stepms)) * stepms; // effects of treatment occur only after it is given / fit to slot time
                tendtime = mytime + 6 * HOUR_IN_MS;     // 6 hours max look

                long cob_time = (long) (mytime + carb_delay_ms_stepped);
                double stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / HOUR_IN_MS); // initial value
                double newdelayedCarbs = 0;
                double cob_remain = thisTreatment.carbs;
                while ((cob_remain > 0) && (stomachDiff > 0) && (cob_time < tendtime)) {

                    if (cob_time >= startTime) {
                        timesliceCarbWriter(timeslices, cob_time, cob_remain);
                    }
                    cob_time += stepms;

                    stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / HOUR_IN_MS);
                    cob_remain -= stomachDiff;

                    newdelayedCarbs = (timesliceIactivityAtTime(timeslices, cob_time) * Profile.getLiverSensRatio(cob_time) / Profile.getSensitivity(cob_time)) * Profile.getCarbRatio(cob_time);

                    if (newdelayedCarbs > 0) {
                        final double maximpact = stomachDiff * Profile.maxLiverImpactRatio(cob_time);
                        if (newdelayedCarbs > maximpact) newdelayedCarbs = maximpact;
                        cob_remain += newdelayedCarbs; // add back on liverfactor adjustment
                    }

                    counter++;

                }
                // end record if not present
                if (cob_time >= startTime) {
                    timesliceCarbWriter(timeslices, cob_time, 0);
                }
            }
        }

        // evaluate carb impact
        Iob lastiob = null;
        for (Map.Entry<Long, Iob> entry : timeslices.entrySet()) {
            Iob thisiob = entry.getValue();
            if (lastiob != null) {
                if ((thisiob.cob != 0 || (lastiob.cob != 0))) {
                    if (thisiob.cob < lastiob.cob) {
                        // decaying cob
                        thisiob.jCarbImpact = (lastiob.cob - thisiob.cob) / Profile.getCarbRatio(thisiob.timestamp) * Profile.getSensitivity(thisiob.timestamp);
                    } else {
                        // more carbs added
                        thisiob.jCarbImpact = 0; // TODO THIS IS NOT RIGHT IT MISSES ONE DECAY STEP
                    }
                }
            }

            //   Log.d(TAG,"iobinfo2carb  debug: "+JoH.qs(thisiob.timestamp)+" C:"+JoH.qs(thisiob.cob,4)+" I:"+JoH.qs(thisiob.iob,4)+" CA:"+JoH.qs(thisiob.jCarbImpact)+" IA:"+JoH.qs(thisiob.jActivity));
            counter++;
            lastiob = thisiob;
        }

        Log.d(TAG, "second iteration counter: " + counter);
        Log.d(TAG, "Timeslices size: " + timeslices.size());
        JoH.benchmark_method_end();
        return new ArrayList<Iob>(timeslices.values());
    }


   /* /// OLD ONE BELOW

    public static List<Iob> ioBForGraph_old(int number, double startTime) {

        JoH.benchmark_method_start();
        //JoH.benchmark_method_end();

        Log.d(TAG, "Processing iobforgraph: main  ");
        // get all treatments from 24 hours earlier than our current time
        List<Treatments> theTreatments = latestForGraph(2000, startTime - 86400000);
        Map<String, Boolean> carbsEaten = new HashMap<String, Boolean>();
        // this could be much more optimized with linear array instead of loops

        final double dontLookThisFar = 10 * 60 * 60 * 1000; // 10 hours max look

        double stomachCarbs = 0;

        final double step_minutes = 10;
        final double stepms = step_minutes * 60 * 1000; // 600s = 10 mins

        if (theTreatments.size() == 0) return null;

        Map ioblookup = new HashMap<Double, Double>(); // store for iob total vs time

        List<Iob> responses = new ArrayList<Iob>();
        Iob calcreply;

        double mytime = startTime;
        double lastmytime = mytime;
        double max_look_time = startTime + (30 * 60 * 60 * 1000);
        int counter = 0;
        // 30 hours max look at
        while ((responses.size() < number) && (mytime < max_look_time)) {

            double lastDecayedBy = 0, isDecaying = 0, delayMinutes = 0; // reset per time slot
            double totalIOB = 0, totalCOB = 0, totalActivity = 0;
            // per treatment per timeblock
            for (Treatments thisTreatment : theTreatments) {
                // early optimisation exclusion
                if ((thisTreatment.timestamp <= mytime) && (mytime - thisTreatment.timestamp) < dontLookThisFar) {
                    calcreply = calcTreatment(thisTreatment, mytime, lastDecayedBy); // was last decayed by but that offset wrongly??
                    totalIOB += calcreply.iob;
                    //totalCOB += calcreply.cob;
                    totalActivity += calcreply.activity;
                } // endif excluding a treatment
            } // per treatment

            //
            ioblookup.put(mytime, totalIOB);
            if (ioblookup.containsKey(lastmytime)) {
                double iobdiff = (double) ioblookup.get(lastmytime) - totalIOB;
                if (iobdiff < 0) iobdiff = 0;
                if ((iobdiff != 0) || (totalActivity != 0)) {
                    Log.d(TAG, "New IOB diffi @: " + JoH.qs(mytime) + " = " + JoH.qs(iobdiff) + " old activity: " + JoH.qs(totalActivity));
                }
                totalActivity = iobdiff; // WARNING OVERRIDE
            }

            double stomachDiff = ((Profile.getCarbAbsorptionRate(mytime) * stepms) / (60 * 60 * 1000));
            double newdelayedCarbs = (totalActivity * Profile.getLiverSensRatio(mytime) / Profile.getSensitivity(mytime)) * Profile.getCarbRatio(mytime);

            // calculate carbs
            for (Treatments thisTreatment : theTreatments) {
                // early optimisation exclusion
                if ((thisTreatment.timestamp <= mytime) && (mytime - thisTreatment.timestamp) < dontLookThisFar) {
                    if ((thisTreatment.carbs > 0) && (thisTreatment.timestamp < mytime)) {
                        // factor carbs delay in above when complete
                        if (!carbsEaten.containsKey(thisTreatment.uuid)) {
                            carbsEaten.put(thisTreatment.uuid, true);
                            stomachCarbs = stomachCarbs + thisTreatment.carbs;
                            stomachCarbs = stomachCarbs + stomachDiff; // offset first subtraction
                            // pre-subtract for granularity or just reduce granularity
                            Log.d(TAG, "newcarbs: " + thisTreatment.carbs + " " + thisTreatment.uuid + " @ " + thisTreatment.timestamp + " mytime: " + JoH.qs(mytime) + " diff: " + JoH.qs((thisTreatment.timestamp - mytime) / 1000) + " stomach: " + JoH.qs(stomachCarbs));
                        }
                        lastCarbs = thisTreatment;
                        CobCalc cCalc = cobCalc(thisTreatment, lastDecayedBy, mytime); // need to handle last decayedby shunting
                        double decaysin_hr = (cCalc.decayedBy - mytime) / 1000 / 60 / 60;
                        if (decaysin_hr > -10) {
                            // units: BG
                            double avgActivity = totalActivity;
                            // units:  g     =       BG      *      scalar     /          BG / U                           *     g / U
                            double delayedCarbs = (avgActivity * Profile.getLiverSensRatio(mytime) / Profile.getSensitivity(mytime)) * Profile.getCarbRatio(mytime);

                            delayMinutes = Math.round(delayedCarbs / (Profile.getCarbAbsorptionRate(mytime) / 60));
                            Log.d(TAG, "Avg activity: " + JoH.qs(avgActivity) + " Decaysin_hr: " + JoH.qs(decaysin_hr) + " delay minutes: " + JoH.qs(delayMinutes) + " delayed carbs: " + JoH.qs(delayedCarbs));
                            if (delayMinutes > 0) {
                                Log.d(TAG, "Delayed Carbs: " + JoH.qs(delayedCarbs) + " Delay minutes: " + JoH.qs(delayMinutes) + " Average activity: " + JoH.qs(avgActivity));
                                cCalc.decayedBy += delayMinutes * 60 * 1000;
                                decaysin_hr = (cCalc.decayedBy - mytime) / 1000 / 60 / 60;
                            }
                        }

                        lastDecayedBy = cCalc.decayedBy;

                        if (decaysin_hr > 0) {
                            Log.d(TAG, "cob: Adding " + JoH.qs(delayMinutes) + " minutes to decay of " + JoH.qs(thisTreatment.carbs) + "g bolus at " + JoH.qs(thisTreatment.timestamp));
                            totalCOB += Math.min(thisTreatment.carbs, decaysin_hr * Profile.getCarbAbsorptionRate(thisTreatment.timestamp));
                            Log.d(TAG, "cob: " + JoH.qs(Math.min(cCalc.initialCarbs, decaysin_hr * Profile.getCarbAbsorptionRate(thisTreatment.timestamp)))
                                    + " inital carbs:" + JoH.qs(cCalc.initialCarbs) + " decaysin_hr:" + JoH.qs(decaysin_hr) + " absorbrate:" + JoH.qs(Profile.getCarbAbsorptionRate(thisTreatment.timestamp)));
                            isDecaying = cCalc.isDecaying;
                        } else {
                            //    totalCOB = 0; //nix this?
                        }
                    } // if this treatment has carbs
                } // end if processing this treatment
            } // per carb treatment

            if (stomachCarbs > 0) {

                Log.d(TAG, "newcarbs Stomach Diff: " + JoH.qs(stomachDiff) + " Old total: " + JoH.qs(stomachCarbs) + " Delayed carbs: " + JoH.qs(newdelayedCarbs));

                stomachCarbs = stomachCarbs - stomachDiff;
                if (newdelayedCarbs > 0) {
                    double maximpact = stomachDiff * Profile.maxLiverImpactRatio(mytime);
                    if (newdelayedCarbs > maximpact) newdelayedCarbs = maximpact;
                    stomachCarbs = stomachCarbs + newdelayedCarbs; // add back on liverfactor ones
                }
                if (stomachCarbs < 0) stomachCarbs = 0;
            }

            if ((totalIOB > Profile.minimum_shown_iob) || (totalCOB > Profile.minimum_shown_cob) || (stomachCarbs > Profile.minimum_shown_cob)) {
                Iob thisrecord = new Iob();

                thisrecord.timestamp = (long) mytime;
                thisrecord.iob = totalIOB;
                thisrecord.activity = totalActivity; // hacky cruft
                thisrecord.cob = stomachCarbs;
                thisrecord.jCarbImpact = 0; // calculated below
                thisrecord.rawCarbImpact = (isDecaying * Profile.getSensitivity(mytime)) / Profile.getCarbRatio(mytime) * Profile.getCarbAbsorptionRate(mytime) / 60;

                // don't get confused with cob totals from previous treatments
                if ((responses.size() > 0) && (Math.abs(responses.get(responses.size() - 1).timestamp - thisrecord.timestamp) <= stepms)) {
                    double cobdiff = responses.get(responses.size() - 1).cob - thisrecord.cob;
                    if (cobdiff > 0) {
                        thisrecord.jCarbImpact = (cobdiff / Profile.getCarbRatio(mytime)) * Profile.getSensitivity(mytime);
                    }

                    double iobdiff = responses.get(responses.size() - 1).iob - totalIOB;
                    if (iobdiff > 0) {
                        thisrecord.jActivity = (iobdiff * Profile.getSensitivity(mytime));
                    }
                }

                Log.d(TAG, "added record: cob raw impact: " + Double.toString(thisrecord.rawCarbImpact) + " Isdecaying: "
                        + JoH.qs(isDecaying) + " jCarbImpact: " + JoH.qs(thisrecord.jCarbImpact) +
                        " jActivity: " + JoH.qs(thisrecord.jActivity) + " old activity: " + JoH.qs(thisrecord.activity));

                responses.add(thisrecord);
            }
            lastmytime = mytime;
            mytime = mytime + stepms;
            counter++;
        } // while time period in range

        Log.d(TAG, "Finished Processing iobforgraph: main - processed:  " + Integer.toString(counter) + " Timeslot records");
        JoH.benchmark_method_end();
        return responses;
    }*/

    public static Double getCurrentIoB() {
        if (Pref.getBooleanDefaultFalse("fetch_iob_from_companion_app")) {
            return getCurrentIoBFromCompanionApp();
        } else {
            return getCurrentIoBFromGraphCalculation();
        }
    }

    public static Double getCurrentIoBFromCompanionApp() {
        // UiBasedCollector reads IoB data from companion apps and saves it to PumpStatus.
        return PumpStatus.getBolusIoB();
    }

    public static Double getCurrentIoBFromGraphCalculation() {
        long now = System.currentTimeMillis();

        final List<Iob> iobInfo = Treatments.ioBForGraph_new(now - 1 * Constants.DAY_IN_MS);

        if (iobInfo != null) {
            for (Iob iob : iobInfo) {
                // Find IoB sample close to the current timestamp.
                if (iob.timestamp > now - 5 * MINUTE_IN_MS && iob.timestamp < now + 5 * MINUTE_IN_MS) {
                    return iob.iob;
                }
            }
        }

        return null;
    }

    public String getBestShortText() {
        if (!eventType.equals(DEFAULT_EVENT_TYPE)) {
            return eventType;
        } else {
            if (hasInsulinInjections()) {
                return getInsulinInjectionsShortString()
                        + (noteHasContent() ? (" " + notes) : "");
            } else {
                return noteHasContent() ? notes : "Treatment";
            }
        }
    }

    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("insulin", insulin);
            jsonObject.put("insulinJSON", insulinJSON);
            jsonObject.put("carbs", carbs);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("notes", notes);
            jsonObject.put("enteredBy", enteredBy);
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

    private static final double MAX_SMB_UNITS = 0.3;
    private static final double MAX_OPENAPS_SMB_UNITS = 0.4;

    public boolean likelySMB() {
        return (carbs == 0 && insulin > 0
                && ((insulin <= MAX_SMB_UNITS && (notes == null || notes.length() == 0)) || (enteredBy != null && enteredBy.startsWith("openaps:") && insulin <= MAX_OPENAPS_SMB_UNITS)));
    }

    public boolean wasCreatedRecently() {
        return msSince(DateUtil.tolerantFromISODateString(created_at).getTime()) < HOUR_IN_MS * 12;
    }

    public boolean noteOnly() {
        return carbs == 0 && insulin == 0 && noteHasContent();
    }

    public boolean hasContent() {
        return insulin != 0 || carbs != 0 || noteHasContent() || !isEventTypeDefault();
    }

    public boolean noteHasContent() {
        return notes != null && notes.length() > 0;
    }

    public boolean isEventTypeDefault() {
        return eventType == null || eventType.equalsIgnoreCase(DEFAULT_EVENT_TYPE);
    }

    public static boolean matchUUID(final List<Treatments> treatments, final String uuid) {
        for (final Treatments treatment : treatments) {
            if (treatment.uuid.equalsIgnoreCase(uuid)) return true;
        }
        return false;
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    public boolean isPenSyncedDose() {
        return notes != null && notes.startsWith("PEN");
    }

    public String getPenSerial() {
        if (isPenSyncedDose()) {
            final Pattern penPattern = Pattern.compile(".*PEN ([A-Z0-9]+).*", Pattern.DOTALL);
            final Matcher m = penPattern.matcher(notes);
            if (m.matches()) {
                return m.group(1);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isPrimingDose() {
        return notes != null && notes.startsWith("Priming");
    }
}



