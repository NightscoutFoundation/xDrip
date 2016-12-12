package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
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


    private static boolean patched = false;
    private final static String TAG = "BloodTest";
    private final static boolean d = true;

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


    // patches and saves
    public Long saveit() {
        fixUpTable();
        return save();
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }


    // static methods
    private static final long CLOSEST_READING_MS = 60000; // 1 minute

    public static BloodTest create(long timestamp_ms, double mgdl, String source) {

        if ((timestamp_ms == 0) || (mgdl == 0)) {
            UserError.Log.e(TAG, "Either timestamp or mgdl is zero - cannot create reading");
            return null;
        }
        final BloodTest match = getForPreciseTimestamp(timestamp_ms, CLOSEST_READING_MS);
        if (match == null) {
            // TODO wider check for dupes?
            final BloodTest bt = new BloodTest();
            bt.timestamp = timestamp_ms;
            bt.mgdl = mgdl;
            bt.uuid = UUID.randomUUID().toString();
            bt.created_timestamp = JoH.tsl();
            bt.state = STATE_VALID;
            bt.source = source;
            bt.saveit();
            return bt;
        } else {
            UserError.Log.d(TAG, "Not creating new reading as timestamp is too close");
        }
        return null;
    }

    public static BloodTest last() {
        try {
            return new Select()
                    .from(BloodTest.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }


    public static BloodTest getForPreciseTimestamp(long timestamp, long precision) {
        // really we need to iterate the range of +- precision here instead of selecting a single entry
        BloodTest bloodTest = new Select()
                .from(BloodTest.class)
                .where("timestamp <= ?", (timestamp + precision))
                .orderBy("timestamp desc")
                .executeSingle();
        if (bloodTest != null && Math.abs(bloodTest.timestamp - timestamp) < precision) {
            return bloodTest;
        }
        return null;
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
                    .orderBy("timestamp asc") // warn asc!
                    .limit(number)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return new ArrayList<>();
        }
    }


    public static List<BloodTest> cleanup(int retention_days) {
        return new Delete()
                .from(BloodTest.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * 86400000L))
                .execute();
    }

    // create the table ourselves without worrying about model versioning and downgrading
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
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
}

