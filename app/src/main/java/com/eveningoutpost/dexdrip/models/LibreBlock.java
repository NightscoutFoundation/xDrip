package com.eveningoutpost.dexdrip.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.activeandroid.Cache;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;
import com.eveningoutpost.dexdrip.utils.LibreTrendUtil;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by jamorham on 19/10/2017.
 */

@Table(name = "LibreBlock", id = BaseColumns._ID)
public class LibreBlock  extends PlusModel {

    private static final String TAG = "LibreBlock";
    static final String[] schema = {
            "CREATE TABLE LibreBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE LibreBlock ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN reference TEXT;",
            "ALTER TABLE LibreBlock ADD COLUMN blockbytes BLOB;",
            "ALTER TABLE LibreBlock ADD COLUMN bytestart INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN byteend INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN calculatedbg REAL;",
            "ALTER TABLE LibreBlock ADD COLUMN uuid TEXT;",
            "ALTER TABLE LibreBlock ADD COLUMN patchUid BLOB;",
            "ALTER TABLE LibreBlock ADD COLUMN patchInfo BLOB;",
            "CREATE INDEX index_LibreBlock_timestamp on LibreBlock(timestamp);",
            "CREATE INDEX index_LibreBlock_bytestart on LibreBlock(bytestart);",
            "CREATE INDEX index_LibreBlock_byteend on LibreBlock(byteend);",
            "CREATE INDEX index_LibreBlock_uuid on LibreBlock(uuid);"
    };


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "bytestart", index = true)
    public long byte_start;

    @Expose
    @Column(name = "byteend", index = true)
    public long byte_end;

    @Expose
    @Column(name = "reference", index = true)
    public String reference;

    @Expose
    @Column(name = "blockbytes")
    public byte[] blockbytes;

    @Expose
    @Column(name = "calculatedbg")
    public double calculated_bg;
    
    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;
    
    @Expose
    @Column(name = "patchUid")
    public byte[] patchUid;
    
    @Expose
    @Column(name = "patchInfo")
    public byte[] patchInfo;
    
    // Fields to store battery value. Not persistent in the DB.

    // Only called by blucon with partial data.
    public static LibreBlock createAndSave(String reference, long timestamp, byte[] blocks, int byte_start) {
        return createAndSave(reference, timestamp, blocks, byte_start, false, null, null);
    }
    
    // if you are indexing by block then just * 8 to get byte start
    public static LibreBlock createAndSave(String reference, long timestamp, byte[] blocks, int byte_start, boolean allowUpload, byte[] patchUid, byte[] patchInfo) {
        final LibreBlock lb = create(reference, timestamp, blocks, byte_start, patchUid, patchInfo);
        if (lb != null) {
            lb.save();
            if(byte_start == 0 && blocks.length == Constants.LIBRE_1_2_FRAM_SIZE && allowUpload) {
                Log.d(TAG, "sending new item to queue");
                UploaderQueue.newTransmitterDataEntry("create" ,lb);
            }
        }
        return lb;
    }
    
    public static void Save(LibreBlock lb){
        lb.save();
    }

    public static LibreBlock create(String reference, long timestamp, byte[] blocks, int byte_start, byte[] patchUid, byte[] patchInfo) {
        if (reference == null) {
            UserError.Log.e(TAG, "Cannot save block with null reference");
            return null;
        }
        if (blocks == null) {
            UserError.Log.e(TAG, "Cannot save block with null data");
            return null;
        }

        final LibreBlock lb = new LibreBlock();
        lb.reference = reference;
        lb.blockbytes = blocks;
        lb.byte_start = byte_start;
        lb.byte_end = byte_start + blocks.length;
        lb.timestamp = timestamp;
        lb.patchUid = patchUid;
        lb.patchInfo = patchInfo;
        lb.uuid = UUID.randomUUID().toString();
        return lb;
    }

    public static LibreBlock getLatestForTrend() {
        return getLatestForTrend(JoH.tsl() - Constants.DAY_IN_MS, JoH.tsl() );
    }

    static LibreBlock getFromCursor(Cursor cursor) {
        LibreBlock libreBlock  = new LibreBlock();

        libreBlock.timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
        libreBlock.byte_start = cursor.getLong(cursor.getColumnIndex("bytestart"));
        libreBlock.byte_end = cursor.getInt(cursor.getColumnIndex("byteend"));
        libreBlock.reference = cursor.getString(cursor.getColumnIndex("reference"));
        libreBlock.blockbytes = cursor.getBlob(cursor.getColumnIndex("blockbytes"));
        libreBlock.calculated_bg = cursor.getDouble(cursor.getColumnIndex("calculatedbg"));
        libreBlock.uuid = cursor.getString(cursor.getColumnIndex("uuid"));
        libreBlock.patchUid = cursor.getBlob(cursor.getColumnIndex("patchUid"));

        libreBlock.patchInfo = cursor.getBlob(cursor.getColumnIndex("patchInfo"));
        return libreBlock;
    }

    public static LibreBlock getLatestForTrend(long start_time, long end_time) {

        SQLiteDatabase db = Cache.openDatabase();
        // Using this syntax since there is no way to tell the DB which index to use.
        // Using ActiveAndroid method would take up to 8 seconds to complete.
        try (Cursor cursor = db.rawQuery("select * from libreblock  INDEXED BY  index_LibreBlock_timestamp " +
                "WHERE bytestart == 0 AND (byteend == " + Constants.LIBRE_1_2_FRAM_SIZE + " OR byteend == 44) " +
                "AND timestamp BETWEEN " + start_time + " AND " + end_time +
                " ORDER BY timestamp DESC LIMIT 1", null)) {

            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            LibreBlock libreBlock = getFromCursor(cursor);
            return libreBlock;
        }
    }

    public static List<LibreBlock> getForTrend(long start_time, long end_time) {
        List<LibreBlock> res1 =  new Select()
                .from(LibreBlock.class)
                .where("timestamp >= ?", start_time)
                .where("timestamp <= ?", end_time)
                .orderBy("timestamp asc")
                .execute();
        // One can think that we could do this filtering as part of the SQL. practically speaking
        // the wrong key was used for the query, and it takes 2-3 minutes.
        List<LibreBlock> res = new ArrayList<LibreBlock>();
        for (LibreBlock lb: res1) {
            if (lb.byte_start == 0 && (lb.byte_end == Constants.LIBRE_1_2_FRAM_SIZE || lb.byte_end == 44)) {
                res.add(lb);
            }
        }
        return res;
    }

    public static LibreBlock getForTimestamp(long timestamp) {
        final long margin = (3 * 1000);
        return new Select()
                .from(LibreBlock.class)
                .where("timestamp >= ?", (timestamp - margin))
                .where("timestamp <= ?", (timestamp + margin))
                .executeSingle();
    }

    public static void UpdateBgVal(long timestamp, double calculated_value) {
        Log.d(TAG, "UpdateBgVal called " + JoH.dateTimeText(timestamp) + " bgval = " + calculated_value);
        LibreBlock libreBlock = getForTimestamp(timestamp);
        if (libreBlock == null) {
            return;
        }
        Log.d(TAG, "Updating bg for timestamp " + JoH.dateTimeText(timestamp) + " bg = " + calculated_value);
        libreBlock.calculated_bg = calculated_value;
        libreBlock.save();
        LibreTrendUtil.getInstance().updateLastReading(libreBlock);
    }

    public static LibreBlock findByUuid(String uuid) {
        try {
            return new Select()
                .from(LibreBlock.class)
                .where("uuid = ?", uuid)
                .executeSingle();
        } catch (Exception e) {
            Log.e(TAG,"findByUuid() Got exception on Select : "+e.toString());
            return null;
        }
    }
    
    private static final boolean d = false;

    public String toJson() {
        return JoH.defaultGsonInstance().toJson(this);        
    }

    public static LibreBlock createFromJson(String json) {
        if (json == null) {
            return null;
        }
        LibreBlock fresh;
        try {
            fresh = JoH.defaultGsonInstance().fromJson(json, LibreBlock.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e );
            return null;
        }
        Log.e(TAG, "Successfuly created LibreBlock value " + json);
        return fresh;
    }

     class ExtendedLibreBlock {
         @Expose
         public int bridge_battery;
         @Expose
         public int Tomatobattery;
         @Expose
         public int Bubblebattery;
         @Expose
         public int Atombattery;
         @Expose
         public int nfc_sensor_age;
         @Expose
         public LibreBlock libreBlock;
     }

     public String toExtendedJson() {
        ExtendedLibreBlock elb = new ExtendedLibreBlock();
        elb.bridge_battery = Pref.getInt("bridge_battery", 0);
        elb.Tomatobattery = PersistentStore.getStringToInt("Tomatobattery", 0);
        elb.Bubblebattery = PersistentStore.getStringToInt("Bubblebattery", 0);
        elb.Atombattery = PersistentStore.getStringToInt("Atombattery", 0);
        elb.nfc_sensor_age = Pref.getInt("nfc_sensor_age", 0);
        elb.libreBlock = this;
        return JoH.defaultGsonInstance().toJson(elb);
    }
    
    // This also saves the batteries data to the global state.
    public static LibreBlock createFromExtendedJson(String json) {
        if (json == null) {
            return null;
        }
        ExtendedLibreBlock elb;
        try {
            elb = JoH.defaultGsonInstance().fromJson(json, ExtendedLibreBlock.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e );
            return null;
        }
        Log.e(TAG, "Successfuly created LibreBlock value " + json);
        Pref.setInt("bridge_battery", elb.bridge_battery);
        PersistentStore.setString("Tomatobattery", Integer.toString(elb.Tomatobattery));
        PersistentStore.setString("Bubblebattery", Integer.toString(elb.Bubblebattery));
        PersistentStore.setString("Atombattery", Integer.toString(elb.Atombattery));
        Pref.setInt("nfc_sensor_age", elb.nfc_sensor_age);
        return elb.libreBlock;
    }
    
    public static void updateDB() {
        fixUpTable(schema, false);
    }
    
    public static LibreBlock byid(long id) {
        return new Select()
                .from(LibreBlock.class)
                .where("_ID = ?", id)
                .executeSingle();
    }
}
