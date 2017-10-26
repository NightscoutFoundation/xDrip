package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
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

            "CREATE INDEX index_LibreBlock_timestamp on LibreBlock(timestamp);"
    };


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    // TODO some numerical reference to start + end byte position?

    @Expose
    @Column(name = "reference", index = true)
    public String reference;

    @Expose
    @Column(name = "blockbytes")
    public byte[] blockbytes;


    public static LibreBlock create(String reference, byte[] blocks) {
        if (reference == null || blocks == null) {
            UserError.Log.e(TAG, "Cannot save block will null data");
            return null;
        }
        final LibreBlock lb = new LibreBlock();
        lb.reference = reference;
        lb.blockbytes = blocks;
        lb.timestamp = JoH.tsl();
        return lb;
    }

    private static final boolean d = false;

    public static void updateDB() {
        fixUpTable(schema, false);
    }


}
