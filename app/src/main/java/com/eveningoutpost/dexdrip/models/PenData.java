package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.UUID;

@Table(name = "PenData", id = BaseColumns._ID)
public class PenData extends Model {

    private static final String TAG = "PenData";

    @Expose
    @Column(name = "pen_mac", index = true)
    public String mac;

    @Expose
    @Column(name = "typ", index = true)
    public String type;

    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "idx")
    public long index;

    @Expose
    @Column(name = "created_timestamp")
    public long created_timestamp;

    @Expose
    @Column(name = "units")
    public double units;

    @Expose
    @Column(name = "temperature")
    public double temperature;

    @Expose
    @Column(name = "battery")
    public int battery;

    @Expose
    @Column(name = "flags")
    public long flags;

    @Expose
    @Column(name = "bitmap")
    public long bitmap_flags;

    @Expose
    @Column(name = "uuid", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public String uuid;

    @Expose
    @Column(name = "insulin_name")
    public String insulin_name;


    @Expose
    @Column(name = "raw")
    public byte[] raw;

    private static final String[] schema = {
            "CREATE TABLE PenData (_id INTEGER PRIMARY KEY AUTOINCREMENT, battery INTEGER, created_timestamp INTEGER, flags INTEGER, idx INTEGER, pen_mac TEXT, raw BLOB, temperature REAL, timestamp INTEGER UNIQUE ON CONFLICT FAIL, typ TEXT, units REAL, uuid TEXT UNIQUE ON CONFLICT FAIL);",
            "ALTER TABLE PenData ADD COLUMN bitmap INTEGER;",
            "ALTER TABLE PenData ADD COLUMN insulin_name TEXT;",
            "CREATE INDEX index_PenData_pen_mac on PenData(pen_mac);",
            "CREATE INDEX index_PenData_timestamp on PenData(timestamp);",
            "CREATE INDEX index_PenData_typ on PenData(typ);",
            "CREATE UNIQUE INDEX index_unq on PenData(pen_mac,idx);"
    };

    public static void updateDB() {
        PlusModel.fixUpTable(schema, false);
    }


    public static PenData create(final String mac, final String type, final int index, final double units, final long timestamp, final double temperature, final byte[] raw) {

        // TODO baulk on very old records

        if (mac == null || type == null || index < 0 || units == -1 || timestamp < 0) {
            UserError.Log.wtf(TAG, "Invalid data sent to PenData.create() - skipping");
            return null;
        }

        // NOTE negative units indicates rewind
        final PenData penData = new PenData();
        penData.created_timestamp = JoH.tsl();
        penData.uuid = UUID.randomUUID().toString();
        penData.index = index;
        penData.units = units;
        penData.timestamp = timestamp;
        penData.temperature = temperature;
        penData.mac = mac;
        penData.type = type;
        penData.raw = raw;
        return penData;
    }

    public static long getHighestIndex(final String mac) {
        if (mac == null) return -1;
        final PenData penData = new Select()
                .from(PenData.class)
                .where("pen_mac = ?", mac)
                .orderBy("idx desc")
                .executeSingle();
        return penData != null ? penData.index : -1;
    }


    public static long getMissingIndex(final String mac) {
        if (mac == null) return -1;
        final List<PenData> list = new Select()
                .from(PenData.class)
                .where("pen_mac = ?", mac)
                .where("timestamp > ?", JoH.tsl() - Constants.WEEK_IN_MS)
                .orderBy("idx desc")
                .execute();
        long got = -1;
        for (final PenData pd : list) {
            if (got != -1 && pd.index != got - 1) {
                UserError.Log.d(TAG, "Tripped missing index on: " + got + " vs " + pd.index);
                return got - 1;
            }
            got = pd.index;
        }
        return -1;
    }

    public static List<PenData> getAllRecordsBetween(final long start, final long end) {
        final List<PenData> list = new Select()
                .from(PenData.class)
                .where("timestamp >= ?", start)
                .where("timestamp <= ?", end)
                .orderBy("typ asc, pen_mac asc, timestamp asc")
                .execute();
        return list;
    }


    public String brief() {
        return mac + " " + JoH.dateTimeText(timestamp) + " " + units + "U";
    }

    public String penName() {
        return type + " " + mac; // TODO have some way to name pen better
    }
}
