package com.eveningoutpost.dexdrip.UtilityModels;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jamorham on 15/11/2016.
 */

@Table(name = "UploaderQueue", id = BaseColumns._ID)
public class UploaderQueue extends Model {
    private static final boolean d = false;
    private final static String TAG = "UploaderQueue";
    private final static String[] schema = {
            "CREATE TABLE UploaderQueue (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE UploaderQueue ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE UploaderQueue ADD COLUMN action TEXT;",
            "ALTER TABLE UploaderQueue ADD COLUMN otype TEXT;",
            "ALTER TABLE UploaderQueue ADD COLUMN reference_id INTEGER;",
            "ALTER TABLE UploaderQueue ADD COLUMN reference_uuid TEXT;",
            "ALTER TABLE UploaderQueue ADD COLUMN bitfield_wanted INTEGER;",
            "ALTER TABLE UploaderQueue ADD COLUMN bitfield_complete INTEGER;",

            "CREATE INDEX index_UploaderQueue_action on UploaderQueue(action);",
            "CREATE INDEX index_UploaderQueue_otype on UploaderQueue(otype);",
            "CREATE INDEX index_UploaderQueue_timestamp on UploaderQueue(timestamp);",
            "CREATE INDEX index_UploaderQueue_complete on UploaderQueue(bitfield_complete);",
            "CREATE INDEX index_UploaderQueue_wanted on UploaderQueue(bitfield_wanted);"};

    // table creation
    private static boolean patched = false;

    // Bitfields
    public static final long MONGO_DIRECT = 1;
    public static final long NIGHTSCOUT_RESTAPI = 1 << 1;
    public static final long TEST_OUTPUT_PLUGIN = 1 << 2;


    public static final long DEFAULT_UPLOAD_CIRCUITS = 0;
    //...


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "action", index = true)
    public String action;

    @Expose
    @Column(name = "otype", index = true)
    public String type;

    @Expose
    @Column(name = "reference_id")
    public long reference_id;

    @Expose
    @Column(name = "reference_uuid")
    public String reference_uuid;

    @Expose
    @Column(name = "bitfield_wanted", index = true)
    public long bitfield_wanted;

    @Expose
    @Column(name = "bitfield_complete", index = true)
    public long bitfield_complete;

    //////////////////////////////////////////

    // patches and saves
    public Long saveit() {
        fixUpTable();
        return save();
    }

    public Long completed(long bitfield) {
        UserError.Log.d(TAG, "Marking bitfield " + bitfield + " completed on: " + getId() + " / " + action + " " + type + " " + reference_id);
        bitfield_complete |= bitfield;
        return saveit();
    }

    public String toS() {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        return gson.toJson(this);
    }

    //////////////////////////////////////////

    public static UploaderQueue newEntry(String action, Model obj) {
        UserError.Log.d(TAG, "new entry called");
        final UploaderQueue result = new UploaderQueue();
        result.bitfield_wanted = DEFAULT_UPLOAD_CIRCUITS
                | (Home.getPreferencesBooleanDefaultFalse("cloud_storage_mongodb_enable") ? MONGO_DIRECT : 0)
                | (Home.getPreferencesBooleanDefaultFalse("cloud_storage_api_enable") ? NIGHTSCOUT_RESTAPI : 0);
        if (result.bitfield_wanted == 0) return null; // no queue required
        result.timestamp = JoH.tsl();
        result.reference_id = obj.getId();
        // TODO this probably could be neater
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof BgReading ? ((BgReading) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Treatments ? ((Treatments) obj).uuid : null;
        if (result.reference_uuid == null)
            result.reference_uuid = obj instanceof Calibration ? ((Calibration) obj).uuid : null;
        result.action = action;

        result.bitfield_complete = 0;
        result.type = obj.getClass().getSimpleName();
        result.saveit();
        if (d) UserError.Log.d(TAG, result.toS());
        return result;
    }

    public static List<UploaderQueue> getPendingbyType(String className, long bitfield) {
        return getPendingbyType(className, bitfield, 500);
    }

    public static List<UploaderQueue> getPendingbyType(String className, long bitfield, int limit) {
        if (d) UserError.Log.d(TAG, "get Pending by type: " + className);
        try {
            final String bitfields = Long.toString(bitfield);
            return new Select()
                    .from(UploaderQueue.class)
                    .where("otype = ?", className)
                    .where("(bitfield_wanted & " + bitfields + ") == " + bitfields)
                    .where("(bitfield_complete & " + bitfields + ") != " + bitfields)
                    .orderBy("timestamp asc, _id asc") // would _id asc be sufficient?
                    .limit(limit)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            if (d) UserError.Log.d(TAG, "Exception: " + e.toString());
            fixUpTable();
            return new ArrayList<UploaderQueue>();
        }
    }

    public static void cleanQueue() {
        // delete all completed records > 24 hours old
        new Delete()
                .from(UploaderQueue.class)
                .where("timestamp < ?", JoH.tsl() - 86400000L)
                .where("bitfield_wanted == bitfield_complete")
                .execute();

        // delete everything > 7 days old
        new Delete()
                .from(UploaderQueue.class)
                .where("timestamp < ?", JoH.tsl() - 86400000L * 7L)
                .execute();
    }


    private static void fixUpTable() {
        if (patched) return;

        for (String patch : schema) {
            try {
                SQLiteUtils.execSql(patch);
            } catch (Exception e) {
                if (d)
                    UserError.Log.d(TAG, "Patch: " + patch + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }

}
