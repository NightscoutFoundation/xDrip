package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService;
import com.google.gson.annotations.Expose;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


/*
 * This class is used to hold basal values that are loaded from openaps.
 * A typical record will look like:
 
 {
    "_id": {
        "$oid": "5cbf8cdc2a32d151f3489b96"
    },
    "duration": 90,
    "raw_duration": {
        "timestamp": "2019-04-24T01:07:30+03:00",
        "_type": "TempBasalDuration",
        "id": "FgNeBwFYEw==",
        "duration (min)": 90
    },
    "timestamp": "2019-04-24T01:07:30+03:00",
    "absolute": 0,
    "rate": 0,
    "raw_rate": {
        "timestamp": "2019-04-24T01:07:30+03:00",
        "_type": "TempBasal",
        "id": "MwBeBwFYEwA=",
        "temp": "absolute",
        "rate": 0
    },
    "eventType": "Temp Basal",
    "medtronic": "mm://openaps/mm-format-ns-treatments/Temp Basal",
    "created_at": "2019-04-24T01:07:30+03:00",
    "enteredBy": "openaps://medtronic/754"
}
 
 On cgm-remote-monitor the code that is handling this can be found at:
 https://github.com/nightscout/cgm-remote-monitor/blob/dc7ea1bf471b5043355806dc24ae317a1412ea5f/lib/data/dataloader.js#L156
 
  
  The fields that we will take from there are:
  treatment.created_at
  treatment.duration
  treatment.rate
 
 */



@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "NSBasal", id = BaseColumns._ID)
public class NSBasal extends PlusModel {

    private static boolean patched = false;
    private final static String TAG = NSBasal.class.getSimpleName();
    private final static boolean d = false;

    private static final String[] schema = {
            "CREATE TABLE NSBasal (_id INTEGER PRIMARY KEY AUTOINCREMENT);",
            "ALTER TABLE NSBasal ADD COLUMN created_at INTEGER;", // ms since epoch
            "ALTER TABLE NSBasal ADD COLUMN duration INTEGER;",   // ms
            "ALTER TABLE NSBasal ADD COLUMN rate REAL;",          // units per hour
            "CREATE UNIQUE INDEX index_NSBasal_created_at on NSBasal(created_at);"};


    @Expose
    @Column(name = "created_at", unique = true, onUniqueConflicts = Column.ConflictAction.IGNORE)
    public long created_at;

    @Expose
    @Column(name = "rate")
    public double rate;
    
    @Expose
    @Column(name = "duration")
    public long duration;


    public String toS() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
       return "created_at: " + sdf.format(new Date(created_at)) + " duration: " + duration + " rate: " + rate; 
    }
    public String toJson() {
        return JoH.defaultGsonInstance().toJson(this);
    }
    
    public long endTimestamp() {
        return created_at + duration * 60000;
    }
    
    public static NSBasal getForTimestamp(double timestamp) {
        NSBasal nsBasal = null;
        try {
            nsBasal = new Select()
                .from(NSBasal.class)
                .where("created_at =? ", timestamp )
                .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(TAG, "Error in NSBasal getForTimestamp");
            updateDB();
            return null;
        }
        Log.d(TAG, "getForTimestamp: returning " + nsBasal);
        return nsBasal;
    }

    // created_at is in ms.
    // rate is in units/hour
    // duration is in ms
    public static NSBasal createEfficientRecord(long created_at, double rate, long duration) {
        long start = JoH.tsl();
        NSBasal old = getForTimestamp(created_at);
        //Log.e(TAG, "getForTimestamp took" + (JoH.tsl() - start) + " ms");
        if(old != null) {
            // Is there a change in any of the values?
            if(old.duration == duration && old.rate == rate) {
                // This is a duplicate object, nothing to do.
                return null;
            }
            // update object and return it
            old.duration = duration;
            old.rate = rate;
            old.save();
            return old;
        }
        
        final NSBasal fresh = NSBasal.builder()
                .created_at(created_at)
                .rate(rate)
                .duration(duration)
                .build();

        UserError.Log.d(TAG, "New NSBasal record created: " + fresh.toS());
        try {
            fresh.save();
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(TAG, "Error in NSBasal last");
            updateDB();
            return null;
        }
        GcmActivity.pushNsBasal(fresh);
        return fresh;

    }

    public static NSBasal last() {
        try {
            return new Select()
                    .from(NSBasal.class)
                    .orderBy("created_at desc")
                    .executeSingle();
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(TAG, "Error in NSBasal last");
            updateDB();
            return null;
        }
    }
    
    public static void addFromJson(String json) {
        if (json == null) {
            return;
        }
        NSBasal fresh;
        try {
            fresh = JoH.defaultGsonInstance().fromJson(json, NSBasal.class);
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e );
            return;
        }
        try {
            fresh.save();
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(TAG, "Error in NSBasal createFromJson");
            updateDB();
            return;
        }
        Log.e(TAG, "Successfuly created NSBasal value " + json);
    }

    public static List<NSBasal> latestForGraph(int number, long startTime, long endTime) {
        try {
            final List<NSBasal> results = new Select()
                    .from(NSBasal.class)
                    .where("created_at >= " + Math.max(startTime, 0))
                    .where("created_at <= " + endTime)
                    .orderBy("created_at asc") // warn asc!
                    .limit(number)
                    .execute();
            
            if (results == null || (results.size() == 0)) {
                return new ArrayList<NSBasal>();
            }
            return results;
        } catch (android.database.sqlite.SQLiteException e) {
            updateDB();
            return new ArrayList<NSBasal>();
        }
    }
    
    public void trimStart(long loaded_start) {
        duration += (created_at - loaded_start) / 60000;
        if(duration < 0) {
            Log.e(TAG, "xxxxx errror duration is negative");
        }
        created_at = loaded_start;
    }
    
    public void setEnd(long new_end) {
        duration = (new_end - created_at) / 60000;
        if(duration < 0) {
            Log.e(TAG, "xxxxx errror duration is negative");
        }
    }
    
    // Merge two objects with the same rate, where the later comes directly 
    // after the first.
    public void merge(NSBasal nextBasal) {
      if (rate != nextBasal.rate) {
        Log.e(TAG, "ERROR rate should be the same " + rate + " " + nextBasal.rate);
        return;
      }
      if(endTimestamp() < nextBasal.created_at || 
          nextBasal.created_at < created_at) {
        Log.e(TAG, "ERROR start time of two objects is wrong " + toS() + " " + nextBasal.toS());
        return;
      }
      
      setEnd(nextBasal.endTimestamp());
      
    }

    // static methods
/*
    public static NSBasal createEfficientRecord(long created_at, double rate) {
        final APStatus existing = last();
        if (existing == null || (existing.basal_percent != basal_percent)) {

            if (existing != null && existing.timestamp > timestamp_ms) {
                UserError.Log.e(TAG, "Refusing to create record older than current: " + JoH.dateTimeText(timestamp_ms) + " vs " + JoH.dateTimeText(existing.timestamp));
                return null;
            }

            final APStatus fresh = APStatus.builder()
                    .timestamp(timestamp_ms)
                    .basal_percent(basal_percent)
                    .build();

            UserError.Log.d(TAG, "New record created: " + fresh.toS());

            fresh.save();
            return fresh;
        } else {
            return existing;
        }

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
        try {
            final List<APStatus> results = new Select()
                    .from(APStatus.class)
                    .where("timestamp >= " + Math.max(startTime, 0))
                    .where("timestamp <= " + endTime)
                    .orderBy("timestamp asc") // warn asc!
                    .limit(number)
                    .execute();
            // extend line to now if we have current data but it is continuation of last record
            // so not generating a new efficient record.
            if (results != null && (results.size() > 0)) {
                final APStatus last = results.get(results.size() - 1);
                final long last_raw_record_timestamp = ExternalStatusService.getLastStatusLineTime();
                // check are not already using the latest.
                if (last_raw_record_timestamp > last.timestamp) {
                    final Integer last_recorded_tbr = ExternalStatusService.getTBRInt();
                    if (last_recorded_tbr != null) {
                        if ((last.basal_percent == last_recorded_tbr)
                                && (JoH.msSince(last.timestamp) < Constants.HOUR_IN_MS * 3)
                                && (JoH.msSince(ExternalStatusService.getLastStatusLineTime()) < Constants.MINUTE_IN_MS * 20)) {
                            results.add(new APStatus(JoH.tsl(), last_recorded_tbr));
                            UserError.Log.d(TAG, "Adding extension record");
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

*/
    // create the table ourselves without worrying about model versioning and downgrading
    public static void updateDB() {
        patched = fixUpTable(schema, patched);
    }
}
