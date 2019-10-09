package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.text.format.DateFormat;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.sql.SQLException;
import java.util.List;

@Table(name = "Libre2Sensors", id = BaseColumns._ID)
public class Libre2Sensor extends PlusModel {
    static final String TAG = "Libre2Sensor";

    static final String[] schema = {
            "CREATE VIEW Libre2Sensors AS SELECT MIN(_id) as _id, serial, MIN(ts) as ts_from, MAX(ts) AS ts_to, COUNT(*) AS readings FROM Libre2RawValue2 GROUP BY serial ORDER BY ts DESC;"
    };

    @Column(name = "serial", index = true)
    public String serial;

    @Column(name = "ts_from", index = false)
    public long ts_from;

    @Column(name = "ts_to", index = false)
    public long ts_to;

    @Column(name = "readings", index = false)
    public long readings;

    private static volatile String cachedStringSensors = null;

    public static String Libre2Sensors() {
        String Sum = "";

        if ((cachedStringSensors == null) || (JoH.ratelimit("libre2sensor-report", 120))) {

            List<Libre2Sensor> rs = new Select()
                    .from(Libre2Sensor.class)
                    .execute();

            for (Libre2Sensor Sensorpart : rs) {
                Long Diff_ts = Sensorpart.ts_to - Sensorpart.ts_from;
                Sum = Sum + Sensorpart.serial +
                        "\n" + DateFormat.format("dd.MM.yy", Sensorpart.ts_from) +
                        " to: " + DateFormat.format("dd.MM.yy", Sensorpart.ts_to) +
                        " (" + JoH.niceTimeScalarShortWithDecimalHours(Diff_ts) + ")" +
                        " readings: " + ((Sensorpart.readings * 100) / (Diff_ts / 60000)) + "%\n" +
                        "------------------\n";
            }
            cachedStringSensors=Sum;
        }

        return cachedStringSensors;
    }

    public static void updateDB() {
        fixUpTable(schema, false);
    }
}
