package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import java.util.Date;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.json.JSONException;
import org.json.JSONObject;

@Table(name = "Libre2RawValue2", id = BaseColumns._ID)
public class Libre2RawValue extends PlusModel {
    private static final String TAG = "libre2rawvalue";

    static final String[] schema = {
        "DROP TABLE Libre2RawValue;",
        "CREATE TABLE Libre2RawValue2 (_id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER, serial STRING, glucose REAL);",
        "CREATE INDEX index_Libre2RawValue2_ts on Libre2RawValue2(ts);"
    };

    @Column(name = "serial", index = true)
    public String serial;

    @Column(name = "ts", index = true)
    public long timestamp;

    @Column(name = "glucose", index = false)
    public double glucose;

    public static List<Libre2RawValue> weightedAverageInterval(long min) {
        double timestamp = (new Date().getTime()) - (60000 * min);
        return new Select()
                .from(Libre2RawValue.class)
                .where("ts >= " + timestamp)
                .orderBy("ts asc")
                .execute();
    }

    public static List<Libre2RawValue> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<Libre2RawValue> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<Libre2RawValue> latestForGraph(int number, long startTime, long endTime) {
        return new Select()
                .from(Libre2RawValue.class)
                .where("ts >= " + Math.max(startTime, 0))
                .where("ts <= " + endTime)
                .where("glucose != 0")
                .orderBy("ts desc")
                .limit(number)
                .execute();
    }

    public static List<Libre2RawValue> cleanup(final int retention_days) {
        updateDB();
        return new Delete()
                .from(Libre2RawValue.class)
                .where("ts < ?", JoH.tsl() - (retention_days * Constants.DAY_IN_MS))
                .execute();
    }

    public static void updateDB() {
        fixUpTable(schema, false);
    }

    public String toJSON() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("serial", serial);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("glucose", glucose);

            return jsonObject.toString();
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Error producing in toJSON: " + e);
            if (Double.isNaN(glucose)) UserError.Log.e(TAG, "glucose is NaN");
            return "";
        }
    }

    public static Libre2RawValue fromJSON(String json) {
        NewLibre2RawValue nl2rv = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(json, NewLibre2RawValue.class);
        Libre2RawValue l2rv = new Libre2RawValue();
        l2rv.serial = nl2rv.serial;
        l2rv.glucose = nl2rv.glucose;
        l2rv.timestamp = nl2rv.timestamp;
        Log.d(TAG, "Object form json: " + l2rv.serial + ", " + l2rv.glucose + ", " + l2rv.timestamp);
        return l2rv;
    }
}

class NewLibre2RawValue {
    @Expose
    double glucose;

    @Expose
    long timestamp;

    @Expose
    String serial;
}
