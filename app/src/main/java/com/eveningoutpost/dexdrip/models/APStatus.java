package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.val;

/**
 * Created by jamorham on 11/06/2018.
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "APStatus", id = BaseColumns._ID)
public class APStatus extends PlusModel {

    private static boolean patched = false;
    private final static String TAG = APStatus.class.getSimpleName();
    private final static boolean d = false;

    private static final String[] schema = {
            "CREATE TABLE APStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE APStatus ADD COLUMN timestamp INTEGER;",
            "ALTER TABLE APStatus ADD COLUMN basal_percent INTEGER;",
            "ALTER TABLE APStatus ADD COLUMN basal_absolute REAL default -1;",
            "CREATE UNIQUE INDEX index_APStatus_timestamp on APStatus(timestamp);"};


    @Expose
    @Column(name = "timestamp", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long timestamp;

    @Expose
    @Column(name = "basal_percent")
    public int basal_percent;

    @Expose
    @Column(name = "basal_absolute")
    public double basal_absolute;


    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    // static methods

    public static APStatus createEfficientRecord(long timestamp_ms, int basal_percent, double basal_absolute) {
        final APStatus existing = last();
        if (existing == null || (existing.basal_percent != basal_percent) || (existing.basal_absolute != basal_absolute)) {

            if (existing != null && existing.timestamp > timestamp_ms) {
                UserError.Log.e(TAG, "Refusing to create record older than current: " + JoH.dateTimeText(timestamp_ms) + " vs " + JoH.dateTimeText(existing.timestamp));
                return null;
            }

            final APStatus fresh = APStatus.builder()
                    .timestamp(timestamp_ms)
                    .basal_absolute(basal_absolute)
                    .basal_percent(basal_percent)
                    .build();

            UserError.Log.d(TAG, "New record created: " + fresh.toS());

            fresh.save();
            return fresh;
        } else {
            return existing;
        }

    }

    public static APStatus createEfficientRecord(long timestamp_ms, int basal_percent) {
        val basal_absolute = Profile.getBasalRateAbsoluteFromPercent(timestamp_ms, basal_percent);
        return createEfficientRecord(timestamp_ms, basal_percent,  basal_absolute);
    }

    public static APStatus createEfficientRecord(long timestamp_ms, double basal_absolute) {
        val basal_percent = Profile.getBasalRatePercentFromAbsolute(timestamp_ms, basal_absolute);
        return createEfficientRecord(timestamp_ms, basal_percent,  basal_absolute);
    }

    // TODO use persistent store?
    public static APStatus last() {
        try {
            return new Select()
                    .from(APStatus.class)
                    .orderBy("timestamp desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            updateDB();
            return null;
        }
    }

    public static List<APStatus> latestForGraph(int number, double startTime) {
        return latestForGraph(number, (long) startTime, Long.MAX_VALUE);
    }

    public static List<APStatus> latestForGraph(int number, long startTime) {
        return latestForGraph(number, startTime, Long.MAX_VALUE);
    }

    public static List<APStatus> latestForGraph(int number, long startTime, long endTime) {
        return latestForGraph(number, startTime, endTime, true);
    }

    public static List<APStatus> latestForGraph(int number, long startTime, long endTime, boolean extensionRecord) {
        try {
            final List<APStatus> results = new Select()
                    .from(APStatus.class)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp asc") // warn asc!
                    .limit(number)
                    .execute();

            if (extensionRecord) {
                // extend line to now if we have current data but it is continuation of last record
                // so not generating a new efficient record.
                if (results != null && (results.size() > 0)) {
                    final APStatus last = results.get(results.size() - 1);
                    final long last_raw_record_timestamp = ExternalStatusService.getLastStatusLineTime();
                    // check are not already using the latest.
                    if (last_raw_record_timestamp > last.timestamp) {
                        Double last_recorded_absolute = ExternalStatusService.getAbsoluteBRDouble();
                        final Integer last_recorded_tbr;
                        if (last_recorded_absolute == null) {
                            last_recorded_tbr = ExternalStatusService.getTBRInt();
                        } else {
                            last_recorded_tbr = Profile.getBasalRatePercentFromAbsolute(last_raw_record_timestamp, last_recorded_absolute);
                        }

                        if (last_recorded_tbr != null) {
                            if ((last.basal_percent == last_recorded_tbr)
                                    && (JoH.msSince(last.timestamp) < Constants.HOUR_IN_MS * 3)
                                    && (JoH.msSince(ExternalStatusService.getLastStatusLineTime()) < Constants.MINUTE_IN_MS * 20)) {
                                if (last_recorded_absolute == null) {
                                    last_recorded_absolute = Profile.getBasalRateAbsoluteFromPercent(last_raw_record_timestamp, last_recorded_tbr);
                                }
                                results.add(new APStatus(JoH.tsl(), last_recorded_tbr, last_recorded_absolute));
                                UserError.Log.d(TAG, "Adding extension record");
                            }
                        }
                    }
                }
            }
            return results;
        } catch (android.database.sqlite.SQLiteException e) {
            updateDB();
            return new ArrayList<>();
        }
    }


    public static List<APStatus> cleanup(int retention_days) {
        return new Delete()
                .from(APStatus.class)
                .where("timestamp < ?", JoH.tsl() - (retention_days * 86400000L))
                .execute();
    }


    // create the table ourselves without worrying about model versioning and downgrading
    public static void updateDB() {
        patched = fixUpTable(schema, patched);
    }
}



