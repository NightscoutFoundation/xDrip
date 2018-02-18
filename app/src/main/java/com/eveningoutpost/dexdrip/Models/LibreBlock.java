package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import java.text.DecimalFormat;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 19/10/2017.
 */

@Table(name = "LibreBlock", id = BaseColumns._ID)
public class LibreBlock extends PlusModel {

    private static final String TAG = "LibreBlock";
    static final String[] schema = {
            "CREATE TABLE LibreBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE LibreBlock ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN reference TEXT;",
            "ALTER TABLE LibreBlock ADD COLUMN blockbytes BLOB;",
            "ALTER TABLE LibreBlock ADD COLUMN bytestart INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN byteend INTEGER;",
            "ALTER TABLE LibreBlock ADD COLUMN calculatedbg REAL;",
            "CREATE INDEX index_LibreBlock_timestamp on LibreBlock(timestamp);",
            "CREATE INDEX index_LibreBlock_bytestart on LibreBlock(bytestart);",
            "CREATE INDEX index_LibreBlock_byteend on LibreBlock(byteend);"
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
    
    // if you are indexing by block then just * 8 to get byte start
    public static LibreBlock createAndSave(String reference, long timestamp, byte[] blocks, int byte_start) {
        final LibreBlock lb = create(reference, timestamp, blocks, byte_start);
        if (lb != null) {
            lb.save();
        }
        return lb;
    }

    public static LibreBlock create(String reference, long timestamp, byte[] blocks, int byte_start) {
        if (reference == null || blocks == null) {
            UserError.Log.e(TAG, "Cannot save block will null data");
            return null;
        }
        final LibreBlock lb = new LibreBlock();
        lb.reference = reference;
        lb.blockbytes = blocks;
        lb.byte_start = byte_start;
        lb.byte_end = byte_start + blocks.length;
        lb.timestamp = timestamp;
        return lb;
    }
    
public static LibreBlock getLatestForTrend() {
        
        return new Select()
                .from(LibreBlock.class)
                .where("bytestart == 0")
                .where("byteend >= 344")
                .orderBy("timestamp desc")
                .executeSingle();
    }

    public static LibreBlock getForTimestamp(long timestamp) {
        
        final double margin = (3 * 1000);
        final DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(1);
 
        return new Select()
                .from(LibreBlock.class)
                .where("timestamp >= " + df.format(timestamp-margin))
                .where("timestamp <= " + df.format(timestamp + margin))
                .executeSingle();
    }
    
    public static void UpdateBgVal(long timestamp, double calculated_value) {
        LibreBlock libreBlock = getForTimestamp(timestamp);
        if(libreBlock == null) {
            return;
        }
        Log.e(TAG, "Updating bg for timestamp " + timestamp);
        libreBlock.calculated_bg = calculated_value;
        libreBlock.save();
    }
    
    private static final boolean d = false;

    public static void updateDB() {
        fixUpTable(schema, false);
    }


}
