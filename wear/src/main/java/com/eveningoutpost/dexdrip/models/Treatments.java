package com.eveningoutpost.dexdrip.models;

/**
 * Created by jamorham on 31/12/15.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// TODO Switchable Carb models
// TODO Linear array timeline optimization

@Table(name = "Treatments", id = BaseColumns._ID)
public class Treatments extends Model {
    private final static String TAG = "jamorham " + Treatments.class.getSimpleName();
    public final static String XDRIP_TAG = "xdrip";
    public static double activityMultipler = 8.4; // somewhere between 8.2 and 8.8
    private static Treatments lastCarbs;
    private static boolean patched = false;

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;
    @Column(name = "systimestamp", index = true)
    public long systimestamp;
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
    @Column(name = "created_at")
    public String created_at;

    public static synchronized Treatments create(final double carbs, final double insulin, final String notes, long timestamp) {
        return create(carbs, insulin, notes, timestamp, null);
    }

    public static synchronized Treatments create(final double carbs, final double insulin, final String notes, long timestamp, String suggested_uuid) {
        // if treatment more than 1 minutes in the future
        final long future_seconds = (timestamp - JoH.tsl()) / 1000;
        if (future_seconds > (60 * 60)) {
            JoH.static_toast_long("Refusing to create a treatement more than 1 hours in the future!");
            return null;
        }
        if ((future_seconds > 60) && (future_seconds < 86400) && ((carbs > 0) || (insulin > 0))) {
            final Context context = xdrip.getAppContext();
            JoH.scheduleNotification(context, "Treatment Reminder", "@" + JoH.hourMinuteString(timestamp) + " : "
                    + carbs + " g " + "carbs" + " / "//context.getString(R.string.carbs)
                    + insulin + " " + "units", (int) future_seconds, 34026);//context.getString(R.string.units)
        }
        return create(carbs, insulin, notes, timestamp, -1, suggested_uuid);
    }

    public static synchronized Treatments create(final double carbs, final double insulin, final String notes, long timestamp, double position, String suggested_uuid) {
        // TODO sanity check values
        fixUpTable();
        Log.d(TAG, "Creating treatment: Insulin: " + Double.toString(insulin) + " / Carbs: " + Double.toString(carbs));

        if ((carbs == 0) && (insulin == 0) && (notes == null)) return null;

        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        Treatments Treatment = new Treatments();

        if (position > 0) {
            Treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
        } else {
            Treatment.enteredBy = XDRIP_TAG;
        }

        Treatment.eventType = "<none>";
        Treatment.carbs = carbs;
        Treatment.insulin = insulin;
        Treatment.notes = notes;
        Treatment.timestamp = timestamp;
        Treatment.created_at = DateUtil.toISOString(timestamp);
        Treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();
        if (suggested_uuid == null) {
            Treatment.systimestamp = new Date().getTime();
        }
        Treatment.save();
        // GcmActivity.pushTreatmentAsync(Treatment);
        //  NSClientChat.pushTreatmentAsync(Treatment);
        //pushTreatmentSync(Treatment);
        //UndoRedo.addUndoTreatment(Treatment.uuid);
        Log.d(TAG, "Treatment.created_at: " + Treatment.created_at + " notes: " + notes);
        return Treatment;
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
        Treatments Treatment = byTimestamp(timestamp, 60 * 1000 * 5);

        // if unknown create
        if (Treatment == null) {
            Treatment = new Treatments();
            Log.d(TAG, "Creating new treatment entry for note");
            is_new = true;

            Treatment.eventType = "<none>";
            Treatment.carbs = 0;
            Treatment.insulin = 0;
            Treatment.notes = note;
            Treatment.timestamp = timestamp;
            Treatment.created_at = DateUtil.toISOString(timestamp);
            Treatment.uuid = suggested_uuid != null ? suggested_uuid : UUID.randomUUID().toString();

        } else {
            if (Treatment.notes == null) Treatment.notes = "";
            Log.d(TAG, "Found existing treatment for note: " + Treatment.uuid + " distance:" + Long.toString(timestamp - Treatment.timestamp) + " " + Treatment.notes);
            // append existing note or treatment
            if (Treatment.notes.length() > 0) Treatment.notes += " \u2192 ";
            Treatment.notes += note;
        }

        if (position > 0) {
            Treatment.enteredBy = XDRIP_TAG + " pos:" + JoH.qs(position, 2);
        } else {
            Treatment.enteredBy = XDRIP_TAG;
        }


        Treatment.save();
        pushTreatmentSync(Treatment, is_new, suggested_uuid);
        //KS if (is_new) UndoRedo.addUndoTreatment(Treatment.uuid);
        return Treatment;
    }

    public static synchronized Treatments SensorStart(long timestamp) {
        if (timestamp == 0) {
            timestamp = new Date().getTime();
        }

        final Treatments Treatment = new Treatments();
        Treatment.enteredBy = XDRIP_TAG;
        Treatment.eventType = "Sensor Start";
        Treatment.created_at = DateUtil.toISOString(timestamp);
        Treatment.uuid = UUID.randomUUID().toString();
        Treatment.save();
        pushTreatmentSync(Treatment);
        return Treatment;
    }

    private static void pushTreatmentSync(Treatments treatment) {
        pushTreatmentSync(treatment, true, null); // new entry by default
    }

    private static void pushTreatmentSync(Treatments treatment, boolean is_new, String suggested_uuid) {
/*        if (Home.get_master_or_follower()) GcmActivity.pushTreatmentAsync(treatment);

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
        }*/
    }

    // This shouldn't be needed but it seems it is
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE Treatments (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE Treatments ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE Treatments ADD COLUMN systimestamp INTEGER;",
                "ALTER TABLE Treatments ADD COLUMN uuid TEXT;",
                "ALTER TABLE Treatments ADD COLUMN eventType TEXT;",
                "ALTER TABLE Treatments ADD COLUMN enteredBy TEXT;",
                "ALTER TABLE Treatments ADD COLUMN notes TEXT;",
                "ALTER TABLE Treatments ADD COLUMN created_at TEXT;",
                "ALTER TABLE Treatments ADD COLUMN insulin REAL;",
                "ALTER TABLE Treatments ADD COLUMN carbs REAL;",
                "CREATE INDEX index_Treatments_timestamp on Treatments(timestamp);",
                "CREATE INDEX index_Treatments_systimestamp on Treatments(systimestamp);",
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

    public static Treatments lastSystime() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .orderBy("systimestamp desc")
                .executeSingle();
    }
    /*public static Treatments last() {
        fixUpTable();
        return new Select()
                .from(Treatments.class)
                .orderBy("_ID desc")
                .executeSingle();
    }*/

    public static Treatments lastCarbsOrInsulin() {
        fixUpTable();
        //.where("carbs > 0 OR insulin > 0 OR (notes IS NOT NULL AND notes != '' AND notes NOT LIKE '%watchkeypad%'")
        List<Treatments> treatl =  new Select()
                .from(Treatments.class)
                .where("carbs > 0 OR insulin > 0 OR notes IS NOT NULL")
                .orderBy("timestamp desc")
                .execute();
        if ((treatl != null) && (treatl.size() > 0)) {
            return treatl.get(0);
        } else {
            return null;
        }
    }

    public static Treatments last() {
        final List<Treatments> treatl = last(1);
        if ((treatl != null) && (treatl.size() > 0)) {
            return treatl.get(0);
        } else {
            return null;
        }
    }

    public static List<Treatments> last(int num) {
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

    public static Treatments byTimestamp(long timestamp, int plus_minus_millis) {
        return new Select()
                .from(Treatments.class)
                .where("timestamp <= ? and timestamp >= ?", (timestamp + plus_minus_millis), (timestamp - plus_minus_millis)) // window
                .orderBy("abs(timestamp-" + Long.toString(timestamp) + ") asc")
                .executeSingle();
    }

    public static void cleanup(long timestamp) {
        try {
            List<Treatments> data = new Select()
                    .from(Treatments.class)
                    .where("systimestamp < ?", timestamp)//timestamp
                    .orderBy("systimestamp desc")
                    .execute();
            if (data != null) Log.d(TAG, "cleanup Treatments size=" + data.size());
            new Cleanup().execute(data);
        } catch (Exception e) {
            Log.e(TAG, "Got exception running cleanup " + e.toString());
        }
    }

    public static void cleanupBloodTest(long timestamp) {
        try {
            List<Treatments> data = new Select()
                    .from(Treatments.class)
                    .where("systimestamp < ? and carbs = 0 and insulin = 0", timestamp)//timestamp
                    .orderBy("systimestamp desc")
                    .execute();
            if (data != null) Log.d(TAG, "cleanup Treatments size=" + data.size());
            new Cleanup().execute(data);
        } catch (Exception e) {
            Log.e(TAG, "Got exception running cleanup " + e.toString());
        }
    }

    private static class Cleanup extends AsyncTask<List<Treatments>, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(List<Treatments>... errors) {
            try {
                for(Treatments data : errors[0]) {
                    data.delete();
                }
                return true;
            } catch(Exception e) {
                return false;
            }
        }
    }

    public static void delete_all() {
        delete_all(false);
    }

    public static void delete_all(boolean from_interactive) {
        if (from_interactive) {
            //GcmActivity.push_delete_all_treatments();
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

    public static void delete_by_uuid(String uuid)
    {
        delete_by_uuid(uuid,false);
    }

    public static void delete_by_uuid(String uuid, boolean from_interactive) {
        Treatments thistreat = byuuid(uuid);
        if (thistreat != null) {

            //KS UploaderQueue.newEntry("delete", thistreat);
            if (from_interactive) {
                //KS GcmActivity.push_delete_treatment(thistreat);
                //KS SyncService.startSyncService(3000); // sync in 3 seconds
            }

            thistreat.delete();
            //KS Home.staticRefreshBGCharts();
        }
    }

    public static Treatments delete_last(boolean from_interactive) {
        Treatments thistreat = last();
        if (thistreat != null) {

            if (from_interactive) {
                //KS GcmActivity.push_delete_treatment(thistreat);
                //GoogleDriveInterface gdrive = new GoogleDriveInterface();
                //gdrive.deleteTreatmentAtRemote(thistreat.uuid);
            }
            //KS UploaderQueue.newEntry("delete",thistreat);
            thistreat.delete();
        }
        return null;
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
        Log.d(TAG, "converting treatment from json: ");
        Treatments mytreatment = fromJSON(json);
        if (mytreatment != null) {
            if (mytreatment.uuid == null) {
                try {
                    final JSONObject jsonobj = new JSONObject(json);
                    if (jsonobj.has("_id")) mytreatment.uuid = jsonobj.getString("_id");
                } catch (JSONException e) {
                    //
                }
                if (mytreatment.uuid == null) mytreatment.uuid = UUID.randomUUID().toString();
            }
            Treatments dupe_treatment = byTimestamp(mytreatment.timestamp);
            if (dupe_treatment != null) {
                Log.i(TAG, "Duplicate treatment for: " + mytreatment.timestamp);

                if ((dupe_treatment.uuid !=null) && (mytreatment.uuid !=null) && (dupe_treatment.uuid.equals(mytreatment.uuid)) && (mytreatment.notes != null))
                {
                    if ((dupe_treatment.notes == null) || (dupe_treatment.notes.length() < mytreatment.notes.length()))
                    {
                        dupe_treatment.notes = mytreatment.notes;
                        fixUpTable();
                        dupe_treatment.save();
                        Log.d(TAG,"Saved updated treatement notes");
                        // should not end up needing to append notes and be from_interactive via undo as these
                        // would be mutually exclusive operations so we don't need to handle that here.
                        //KS Home.staticRefreshBGCharts();
                    }
                }

                return false;
            }
            Log.d(TAG, "Saving pushed treatment: " + mytreatment.uuid);
            if ((mytreatment.enteredBy == null) || (mytreatment.enteredBy.equals(""))) {
                mytreatment.enteredBy = "sync";
            }
            if ((mytreatment.eventType == null) || (mytreatment.eventType.equals(""))) {
                mytreatment.eventType = "<none>"; // should have a default
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
            //KS Home.staticRefreshBGCharts();
            return true;
        } else {
            return false;
        }
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

    public static List<Treatments> latestForGraphSystime(int number, double startTime) {
        return latestForGraphSystime(number, startTime, JoH.ts());
    }

    public static List<Treatments> latestForGraphSystime(int number, double startTime, double endTime) {
        fixUpTable();
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1); // are there decimal points in the database??
        return new Select()
                .from(Treatments.class)
                .where("systimestamp >= ? and systimestamp <= ?", df.format(startTime), df.format(endTime))
                .orderBy("systimestamp asc")
                .limit(number)
                .execute();
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
                .orderBy("timestamp desc")
                .limit(number)
                .execute();
    }
    public static long getTimeStampWithOffset(double offset) {
        //  optimisation instead of creating a new date each time?
        return (long) (new Date().getTime() - offset);
    }

    /*
        public static CobCalc cobCalc(Treatments treatment, double lastDecayedBy, double time) {

            double delay = 20; // minutes till carbs start decaying

            double delayms = delay * 60 * 1000;
            if (treatment.carbs > 0) {

                CobCalc thisCobCalc = new CobCalc();
                thisCobCalc.carbTime = treatment.timestamp;

                // no previous carb treatment? Set to our start time
                if (lastDecayedBy == 0) {
                    lastDecayedBy = thisCobCalc.carbTime;
                }

                double carbs_hr = Profile.getCarbAbsorptionRate(time);
                double carbs_min = carbs_hr / 60;
                double carbs_ms = carbs_min / (60 * 1000);

                thisCobCalc.decayedBy = thisCobCalc.carbTime; // initially set to start time for this treatment

                double minutesleft = (lastDecayedBy - thisCobCalc.carbTime) / 1000 / 60;
                double how_long_till_carbs_start_ms = (lastDecayedBy - thisCobCalc.carbTime);
                thisCobCalc.decayedBy += (Math.max(delay, minutesleft) + treatment.carbs / carbs_min) * 60 * 1000;

                if (delay > minutesleft) {
                    thisCobCalc.initialCarbs = treatment.carbs;
                } else {
                    thisCobCalc.initialCarbs = treatment.carbs + minutesleft * carbs_min;
                }
                double startDecay = thisCobCalc.carbTime + (delay * 60 * 1000);

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

        public static Iob calcTreatment(Treatments treatment, double time, double lastDecayedby) {

            final double dia = Profile.insulinActionTime(time); // duration insulin action in hours
            final double peak = 75; // minutes in based on a 3 hour DIA - scaled proportionally (orig 75)
            //final double sens = Profile.getSensitivity(time); // sensitivity currently in mmol
            double insulin_delay_minutes = 0;

            double insulin_timestamp = treatment.timestamp + (insulin_delay_minutes * 60 * 1000);

            Iob response = new Iob();

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
            response.iob = iobContrib;
            // response.activity = activityContrib;
            return response;
        }

        // requires stepms granularity which we should already have
        private static double timesliceIactivityAtTime(Map<Double, Iob> timeslices, double thistime) {
            if (timeslices.containsKey(thistime)) {
                return timeslices.get(thistime).jActivity;
            } else {
                return 0;
            }
        }

        private static void timesliceCarbWriter(Map<Double, Iob> timeslices, double thistime, double carbs) {
            // offset for carb action time??
            Iob tempiob;
            if (timeslices.containsKey(thistime)) {
                tempiob = timeslices.get(thistime);
                tempiob.cob = tempiob.cob + carbs;
            } else {
                tempiob = new Iob();
                tempiob.timestamp = (long) thistime;
                tempiob.cob = carbs;
            }
            timeslices.put(thistime, tempiob);
        }

        private static void timesliceWriter(Map<Double, Iob> timeslices, Iob thisiob, double thistime) {
            if (thisiob.iob > 0) {
                if (timeslices.containsKey(thistime)) {
                    Iob tempiob = timeslices.get(thistime);
                    tempiob.iob += thisiob.iob;
                    timeslices.put(thistime, tempiob);
                } else {
                    thisiob.timestamp = (long) thistime;
                    timeslices.put(thistime, thisiob); // first entry at timeslice so put the record in as is
                }
            }
        }

        // NEW NEW NEW
        public static List<Iob> ioBForGraph_new(int number, double startTime) {

            Log.d(TAG, "Processing iobforgraph2: main  ");
            JoH.benchmark_method_start();

            // number param currently ignored

            // get all treatments from 24 hours earlier than our current time
            final double dontLookThisFar = 10 * 60 * 60 * 1000; // 10 hours max look
            List<Treatments> theTreatments = latestForGraph(2000, startTime - dontLookThisFar);
            if (theTreatments.size() == 0) return null;


            int counter = 0; // iteration counter

            final double step_minutes = 5;
            final double stepms = step_minutes * 60 * 1000; // 600s = 10 mins
            double mytime = startTime;
            double tendtime = startTime;


            final double carb_delay_minutes = Profile.carbDelayMinutes(mytime); // not likely a time dependent parameter
            final double carb_delay_ms_stepped = ((long) (carb_delay_minutes / step_minutes)) * step_minutes * (60 * 1000);

            Log.d(TAG, "Carb delay ms: " + carb_delay_ms_stepped);

            Map<String, Boolean> carbsEaten = new HashMap<String, Boolean>();

            // linear array populated as needed and layered by each treatment etc
            SortedMap<Double, Iob> timeslices = new TreeMap<Double, Iob>();

            Iob calcreply;

            // First process all IoB calculations
            for (Treatments thisTreatment : theTreatments) {
                // early optimisation exclusion

                mytime = ((long) (thisTreatment.timestamp / stepms)) * stepms; // effects of treatment occur only after it is given / fit to slot time
                tendtime = mytime + dontLookThisFar;

                if (thisTreatment.insulin > 0) {
                    // lay down insulin on board
                    while (mytime < tendtime) {

                        calcreply = calcTreatment(thisTreatment, mytime, 0); // last param now not used - only insulin

                        if (mytime >= startTime) {
                            timesliceWriter(timeslices, calcreply, mytime);
                        }
                        mytime = mytime + stepms; // advance time counter
                    }
                }
            } // per insulin treatment


            Log.d(TAG, "insulin iteration counter: " + counter);


            // evaluate insulin impact
            Iob lastiob = null;
            for (Map.Entry<Double, Iob> entry : timeslices.entrySet()) {
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

            // calculate carb treatments
            for (Treatments thisTreatment : theTreatments) {

                if (thisTreatment.carbs > 0) {

                    mytime = ((long) (thisTreatment.timestamp / stepms)) * stepms; // effects of treatment occur only after it is given / fit to slot time
                    tendtime = mytime + dontLookThisFar;

                    double cob_time = mytime + carb_delay_ms_stepped;
                    double stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / (60 * 60 * 1000)); // initial value
                    double newdelayedCarbs = 0;
                    double cob_remain = thisTreatment.carbs;
                    while ((cob_remain > 0) && (stomachDiff > 0) && (cob_time < tendtime)) {

                        if (cob_time >= startTime) {
                            timesliceCarbWriter(timeslices, cob_time, cob_remain);
                        }
                        cob_time += stepms;

                        stomachDiff = ((Profile.getCarbAbsorptionRate(cob_time) * stepms) / (60 * 60 * 1000));
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
            lastiob = null;
            for (Map.Entry<Double, Iob> entry : timeslices.entrySet()) {
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


        /// OLD ONE BELOW

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
        }
    */
    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", uuid);
            jsonObject.put("insulin", insulin);
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
}



