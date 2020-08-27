package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.google.gson.annotations.Expose;
import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;
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
            if(byte_start == 0 && blocks.length == 344 && allowUpload) {
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
        UserError.Log.e(TAG,"Backtrack: "+JoH.backTrace());
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


    public static LibreBlock getLatestForTrend(long start_time, long end_time) {

        return new Select()
                .from(LibreBlock.class)
                .where("bytestart == 0")
                .where("byteend >= 344")
                .where("timestamp >= ?", start_time)
                .where("timestamp <= ?", end_time)
                .orderBy("timestamp desc")
                .executeSingle();
    }
    
    public static List<LibreBlock> getForTrend(long start_time, long end_time) {

        return new Select()
                .from(LibreBlock.class)
                .where("bytestart == 0")
                .where("byteend >= 344")
                .where("timestamp >= ?", start_time)
                .where("timestamp <= ?", end_time)
                .orderBy("timestamp asc")
                .execute();
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
        LibreBlock libreBlock = getForTimestamp(timestamp);
        if (libreBlock == null) {
            return;
        }
        Log.e(TAG, "Updating bg for timestamp " + timestamp);
        libreBlock.calculated_bg = calculated_value;
        libreBlock.save();
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
         public int nfc_sensor_age;
         @Expose
         public LibreBlock libreBlock;
     }

     public String toExtendedJson() {
        ExtendedLibreBlock elb = new ExtendedLibreBlock();
        elb.bridge_battery = Pref.getInt("bridge_battery", 0);
        elb.Tomatobattery = PersistentStore.getStringToInt("Tomatobattery", 0);
        elb.Bubblebattery = PersistentStore.getStringToInt("Bubblebattery", 0);
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
