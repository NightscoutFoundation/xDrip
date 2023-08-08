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

/**
 * Created by jamorham on 01/11/2016.
 */


@Table(name = "HeartRate", id = BaseColumns._ID)
public class HeartRate extends Model {

    private final static String TAG = "HeartRate";
    private static boolean patched = false;
    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "bpm")
    public int bpm;

    @Expose
    @Column(name = "accuracy")
    public int accuracy;

    public static HeartRate last() {
        try {
            return new Select()
                    .from(HeartRate.class)
                    .where("timestamp >= " + (JoH.tsl() - Constants.DAY_IN_MS))
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    public static void create(long timestamp, int bpm, int accuracy) {
        final HeartRate hr = new HeartRate();
        hr.timestamp = timestamp;
        hr.bpm = bpm;
        hr.accuracy = accuracy;
        hr.saveit();
    }

    public static List<HeartRate> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<HeartRate> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    // TODO efficient record creation?

    public static List<HeartRate> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(HeartRate.class)
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

    public static List<HeartRate> cleanup(int retention_days) {
        return new Delete()
                .from(HeartRate.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * 86400000L))
                .execute();
    }

    // create the table ourselves without worrying about model versioning and downgrading
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE HeartRate (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE HeartRate ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE HeartRate ADD COLUMN bpm INTEGER;",
                "ALTER TABLE HeartRate ADD COLUMN accuracy INTEGER;",
                "CREATE UNIQUE INDEX index_HeartRate_timestamp on HeartRate(timestamp);"};

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

    // patches and saves
    public Long saveit() {
        fixUpTable();
        return save();
    }

    // TODO cache gson statically
    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }
}



