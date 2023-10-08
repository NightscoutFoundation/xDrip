package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.google.gson.annotations.Expose;

/**
 * Created by jamorham on 19/10/2017.
 */

@Table(name = "LibreData", id = BaseColumns._ID)
public class LibreData extends PlusModel {
    private static final String TAG = "LibreData";
    static final String[] schema = {
            "CREATE TABLE LibreData (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE LibreData ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE LibreData ADD COLUMN temperature REAL;",
            "ALTER TABLE LibreData ADD COLUMN temperatureraw INTEGER;",

            "CREATE INDEX index_LibreData_timestamp on LibreData(timestamp);"
    };


    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "temperature")
    public double temperature;

    @Expose
    @Column(name = "temperatureraw")
    public long temperatureraw;


    public static LibreData create(byte[] temp_bytes) {
        final LibreData ld = new LibreData();
        ld.timestamp = JoH.tsl();
        // TODO
        //ld.temperatureraw = get byte order value from temp_bytes
        //ld.temperature = evaluate temperature from temperature raw
        return ld;
    }

    private static final boolean d = false;

    public static void updateDB() {
        fixUpTable(schema, false);
    }

}
