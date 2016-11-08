package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 01/11/2016.
 */


@Table(name = "HeartRate", id = BaseColumns._ID)
public class HeartRate extends Model {

    private static boolean patched = false;
    private final static String TAG = "HeartRate";

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "bpm")
    public int bpm;


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

    public static HeartRate last() {
        try {
            return new Select()
                    .from(HeartRate.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            fixUpTable();
            return null;
        }
    }

    // create the table ourselves without worrying about model versioning and downgrading
    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE HeartRate (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
                "ALTER TABLE HeartRate ADD COLUMN timestamp INTEGER;",
                "ALTER TABLE HeartRate ADD COLUMN bpm INTEGER;",
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
}



