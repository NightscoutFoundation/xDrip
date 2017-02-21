package com.eveningoutpost.dexdrip.Models;

/**
 * Created by jamorham on 01/02/2017.
 */


import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.google.gson.annotations.Expose;

@Table(name = "Accuracy", id = BaseColumns._ID)
public class Accuracy extends PlusModel {
    private static final String TAG = "Accuracy";
    static final String[] schema = {
            "CREATE TABLE Accuracy (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE Accuracy ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE Accuracy ADD COLUMN bg REAL;",
            "ALTER TABLE Accuracy ADD COLUMN bgtimestamp INTEGER;",
            "ALTER TABLE Accuracy ADD COLUMN bgsource TEXT;",
            "ALTER TABLE Accuracy ADD COLUMN plugin TEXT;",
            "ALTER TABLE Accuracy ADD COLUMN calculated REAL;",
            "ALTER TABLE Accuracy ADD COLUMN lag INTEGER;",
            "ALTER TABLE Accuracy ADD COLUMN difference REAL;",
            "CREATE INDEX index_Accuracy_timestamp on Accuracy(timestamp);",
            "CREATE INDEX index_Accuracy_bgtimestamp on Accuracy(bgtimestamp);"
    };

    @Expose
    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Expose
    @Column(name = "bg")
    public double bg;

    @Expose
    @Column(name = "bgtimestamp", index = true)
    public long bgtimestamp;

    @Expose
    @Column(name = "bgsource")
    public String bgsource;

    @Expose
    @Column(name = "plugin")
    public String plugin;

    @Expose
    @Column(name = "calculated")
    public double calculated;

    @Expose
    @Column(name = "lag")
    public boolean lag;

    @Expose
    @Column(name = "difference")
    public double difference;


    public static Accuracy create(BloodTest bloodTest, BestGlucose.DisplayGlucose dg) {
        if (dg == null) return null;
        final BgReading from_dg = new BgReading();
        from_dg.timestamp = dg.timestamp;
        from_dg.calculated_value = dg.mgdl;
        return create(bloodTest, from_dg, dg.plugin_name);
    }


    public static Accuracy create(BloodTest bloodTest, BgReading bgReading, String plugin) {
        if ((bloodTest == null) || (bgReading == null)) return null;
        fixUpTable(schema);
        if (getForPreciseTimestamp(bgReading.timestamp, Constants.MINUTE_IN_MS, plugin) != null) {
            UserError.Log.d(TAG, "Duplicate accuracy timestamp for: " + JoH.dateTimeText(bgReading.timestamp));
            return null;
        }
        final Accuracy ac = new Accuracy();
        ac.timestamp = bgReading.timestamp;
        ac.bg = bloodTest.mgdl;
        ac.bgtimestamp = bloodTest.timestamp;
        ac.bgsource = bloodTest.source;
        ac.plugin = plugin;
        ac.calculated = bgReading.calculated_value;
        //ac.lag = bgReading.timestamp-bloodTest.timestamp;
        ac.difference = bgReading.calculated_value - bloodTest.mgdl;
        ac.save();
        return ac;
    }

    static Accuracy getForPreciseTimestamp(double timestamp, double precision, String plugin) {
        fixUpTable(schema);
        final Accuracy accuracy = new Select()
                .from(Accuracy.class)
                .where("timestamp <= ?", (timestamp + precision))
                .where("timestamp >= ?", (timestamp - precision))
                .where("plugin = ?", plugin)
                .orderBy("abs(timestamp - " + timestamp + ") asc")
                .executeSingle();
        if (accuracy != null && Math.abs(accuracy.timestamp - timestamp) < precision) {
            return accuracy;
        }
        return null;
    }


}
