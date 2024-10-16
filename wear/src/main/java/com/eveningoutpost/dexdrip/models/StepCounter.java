package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

/**
 * Created by jamorham on 01/11/2016.
 */


@Table(name = "PebbleMovement", id = BaseColumns._ID)
public class StepCounter extends Model {

    private static boolean patched = false;
    private final static String TAG = "StepCounter";
    private final static boolean d = false;

    private static final int ABSOLUTE_MASK = 1;

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "metric")
    public int metric;

    @Expose
    @Column(name = "source")
    public int source;


    // patches and saves
    public Long saveit() {
        try {
            fixUpTable();
            return save();
        } catch (Exception e) {
            return null;
        }
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    public boolean isAbsolute() {
        return ((source & ABSOLUTE_MASK) != 0);
    }

    // static methods

    public static StepCounter getForTimestamp(final long timestamp) {
        return new Select()
                .from(StepCounter.class)
                .where("timestamp = ?", timestamp)
                .executeSingle();
    }


    public static synchronized StepCounter createUniqueRecord(final long timestamp_ms, final int data, final boolean absolute) {
        if (getForTimestamp(timestamp_ms) == null) {
            val pm = new StepCounter();
            pm.timestamp = timestamp_ms;
            pm.metric = data;
            if (absolute) {
                pm.source |= ABSOLUTE_MASK;
            }
            pm.saveit();
            UserError.Log.d(TAG, "Created new record: " + pm.toS() + " " + JoH.dateTimeText(pm.timestamp));
            return pm;
        }
        return null;
    }

    public static StepCounter createEfficientRecord(long timestamp_ms, int data) {
        StepCounter pm = last();
        if ((pm == null) || (data < pm.metric) || ((timestamp_ms - pm.timestamp) > (1000 * 30 * 5))) {
            pm = new StepCounter();
            pm.timestamp = timestamp_ms;
            if (d)
                UserError.Log.d(TAG, "Creating new record for timestamp: " + JoH.dateTimeText(timestamp_ms));
        } else {
            if (d)
                UserError.Log.d(TAG, "Merging pebble movement record: " + JoH.dateTimeText(timestamp_ms) + " vs old " + JoH.dateTimeText(pm.timestamp));
        }

        pm.metric = (int) (long) data;
        if (d) UserError.Log.d(TAG, "Saving Movement: " + pm.toS());
        pm.saveit();
        return pm;
    }

    public static StepCounter last() {
        try {
            return new Select()
                    .from(StepCounter.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static int getDailyTotal() {
        int accumulator = 0;
        val list = latestForGraph(5000, JoH.tsl() - Constants.DAY_IN_MS, JoH.tsl()); // TODO since midnight vs 24 hours?
        for (val item : list) {
            if (item.isAbsolute()) {
                accumulator += item.metric;
            }
        }
        if (accumulator == 0) {
            val last = last();
            if (last != null) {
                return last.metric;
            } else {
                return 0;
            }
        } else {
            return accumulator; // total from absolutes
        }
    }

    public static List<StepCounter> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<StepCounter> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<StepCounter> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(StepCounter.class)
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

    // expects pre-sorted in asc order?
    public static List<StepCounter> deltaListFromMovementList(List<StepCounter> mList) {
        int last_metric = -1;
        int temp_metric = -1;
        for (StepCounter pm : mList) {
            if (pm.isAbsolute()) continue;
            // first item in list
            if (last_metric == -1) {
                last_metric = pm.metric;
                pm.metric = 0;
            } else {
                // normal incrementing calculate delta
                if (pm.metric >= last_metric) {
                    temp_metric = pm.metric - last_metric;
                    last_metric = pm.metric;
                    pm.metric = temp_metric;
                } else {
                    last_metric = pm.metric;
                }
            }
        }
        return mList;
    }

    public static List<StepCounter> cleanup(int retention_days) {
        return new Delete()
                .from(StepCounter.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * 86400000L))
                .execute();
    }


    // create the table ourselves without worrying about model versioning and downgrading
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE PebbleMovement (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE PebbleMovement ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE PebbleMovement ADD COLUMN metric INTEGER;",
                "ALTER TABLE PebbleMovement ADD COLUMN source INTEGER;",
                "CREATE INDEX index_PebbleMovement_source on PebbleMovement(source);",
                "CREATE UNIQUE INDEX index_PebbleMovement_timestamp on PebbleMovement(timestamp);"};

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



