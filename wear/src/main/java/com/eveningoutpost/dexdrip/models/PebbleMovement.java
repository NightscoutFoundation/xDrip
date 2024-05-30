package com.eveningoutpost.dexdrip.models;

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

/**
 * Created by jamorham on 01/11/2016.
 */

// TODO change classname to match phone
@Table(name = "PebbleMovement", id = BaseColumns._ID)
public class PebbleMovement extends Model {

    private static boolean patched = false;
    private final static String TAG = "PebbleMovement";
    private final static boolean d = true; //false;

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "metric")
    public int metric;


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

    public static PebbleMovement createEfficientRecord(long timestamp_ms, int data)
    {
        PebbleMovement pm = last();
        if ((pm == null) || (data < pm.metric) || ((timestamp_ms - pm.timestamp) > (1000 * 30 * 5))) {
            pm = new PebbleMovement();
            pm.timestamp = timestamp_ms;
            if (d) UserError.Log.d(TAG,"Creating new record for timestamp: "+JoH.dateTimeText(timestamp_ms));
        } else {
            if (d) UserError.Log.d(TAG,"Merging pebble movement record: "+JoH.dateTimeText(timestamp_ms)+" vs old "+JoH.dateTimeText(pm.timestamp));
        }

        pm.metric = (int) (long) data;
        if(d) UserError.Log.d(TAG, "Saving Movement: " + pm.toS());
        pm.saveit();
        return pm;
    }

    public static PebbleMovement last() {
        try {
            return new Select()
                    .from(PebbleMovement.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static List<PebbleMovement> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<PebbleMovement> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<PebbleMovement> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(PebbleMovement.class)
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
    public static List<PebbleMovement> deltaListFromMovementList(List<PebbleMovement> mList) {
        int last_metric = -1;
        int temp_metric = -1;
        for (PebbleMovement pm : mList) {
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

    public static List<PebbleMovement> cleanup(int retention_days) {
        return new Delete()
                .from(PebbleMovement.class)
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



