package com.eveningoutpost.dexdrip.models;

/**
 * Created by jamorham on 01/02/2017.
 */


import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Table(name = "Accuracy", id = BaseColumns._ID)
public class Accuracy extends PlusModel {
    private static final String TAG = "Accuracy";
    private static boolean patched = false;
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

    private static final boolean d = false;

    public static Accuracy create(BloodTest bloodTest, BestGlucose.DisplayGlucose dg) {
        if (dg == null) return null;
        final BgReading from_dg = new BgReading();
        from_dg.timestamp = dg.timestamp;
        from_dg.calculated_value = dg.mgdl;
        return create(bloodTest, from_dg, dg.plugin_name);
    }


    public static Accuracy create(BloodTest bloodTest, BgReading bgReading, String plugin) {
        if ((bloodTest == null) || (bgReading == null)) return null;
        patched = fixUpTable(schema, patched);
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
        patched = fixUpTable(schema, patched);
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

    public static List<Accuracy> latestForGraph(int number, long startTime, long endTime) {
        try {
            return new Select()
                    .from(Accuracy.class)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp desc, _id asc")
                    .limit(number)
                    .execute();
        } catch (android.database.sqlite.SQLiteException e) {
            patched = fixUpTable(schema, patched);
            return new ArrayList<>();
        }
    }

    public static String evaluateAccuracy(long period) {
        // TODO CACHE ?
        final boolean domgdl = Pref.getString("units", "mgdl").equals("mgdl");
        final Map<String, Double> totals = new HashMap<>();
        final Map<String, Double> signed_totals = new HashMap<>();
        final Map<String, Integer> count = new HashMap<>();
        final List<Accuracy> alist = latestForGraph(500, JoH.tsl() - period, JoH.tsl());

        // total up differences
        for (Accuracy entry : alist) {
            if (totals.containsKey(entry.plugin)) {
                totals.put(entry.plugin, totals.get(entry.plugin) + Math.abs(entry.difference));
                signed_totals.put(entry.plugin, signed_totals.get(entry.plugin) + entry.difference);
                count.put(entry.plugin, count.get(entry.plugin) + 1);
            } else {
                totals.put(entry.plugin, Math.abs(entry.difference));
                signed_totals.put(entry.plugin, entry.difference);
                count.put(entry.plugin, 1);
            }
        }
        String result = "";
        int plugin_count = 0;
        for (Map.Entry<String, Double> total : totals.entrySet()) {
            plugin_count++;
            final String plugin = total.getKey();
            final int this_count = count.get(plugin);
            final double this_total = total.getValue();
            // calculate the abs mean, 0 = perfect
            final double this_mean = this_total / this_count;
            final double signed_total = signed_totals.get(plugin);
            final double signed_mean = signed_total / this_count;
            // calculate the bias ratio. 0% means totally unbiased, 100% means all data skewed towards signed mean
            final double signed_ratio = (Math.abs(signed_mean) / this_mean) * 100;

            if (d) UserError.Log.d(TAG, plugin + ": total: " + JoH.qs(this_total) + " count: " + this_count + " avg: " + JoH.qs(this_mean) + " mmol: " + JoH.qs((this_mean) * Constants.MGDL_TO_MMOLL) + " bias: " + JoH.qs(signed_mean) + " " + JoH.qs(signed_ratio, 0) + "%");
            String plugin_result = plugin.substring(0, 1).toLowerCase() + ": " + asString(this_mean, signed_mean, signed_ratio, domgdl);
            UserError.Log.d(TAG, plugin_result);
            if (result.length() > 0) result += " ";
            result += plugin_result;
        }

        return plugin_count == 1 ? result : result.replaceFirst(" mmol", "").replaceFirst(" mgdl", " ");
    }

    private static String asString(double mean, double signed_mean, double signed_ratio, boolean domgdl) {

        String symbol = "err";
        if (signed_ratio < 90) {
            symbol = "\u00B1";   // +- symbol
        } else {
            if (signed_mean < 0) {
                symbol = "\u207B"; // superscript minus
            } else {
                symbol = "\u207A"; // superscript plus
            }
        }
        return symbol + (!domgdl ? JoH.qs(mean * Constants.MGDL_TO_MMOLL, 2) + " mmol" : JoH.qs(mean, 1) + " mgdl");
    }

}
