package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
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
            "ALTER TABLE NSBasal ADD COLUMN duration REAL;",   // minutes
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
    public double duration;
    
    // This field can only be set to true if we create a fake object with value 0 to put on the screen, in the 
    // cast that we don't know what the real value is.
    private boolean fake_range;

    public String toS() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
       return "created_at: " + sdf.format(new Date(created_at)) + " duration: " + duration + " rate: " + rate; 
    }
    public String toJson() {
        return JoH.defaultGsonInstance().toJson(this);
    }
    
    public long endTimestamp() {
        return created_at + Math.round(duration * 60000.0);
    }
    
    // Please note, does not update the db.
    public NSBasal (long created_at, double rate, double duration) {
        this.created_at = created_at;
        this.rate = rate;
        this.duration = duration;
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
        duration += (created_at - loaded_start) / 60000.0;
        if(duration < 0) {
            Log.e(TAG, "Error duration is negative");
        }
        created_at = loaded_start;
    }
    
    public void setEnd(long new_end) {
        duration = (new_end - created_at) / 60000.0;
        if(duration < 0) {
            Log.e(TAG, "Error duration is negative");
        }
    }
    
    // Merge two objects with the same rate, where the later comes directly 
    // after the first.
    public boolean merge(NSBasal nextBasal) {
      if (rate != nextBasal.rate) {
        Log.e(TAG, "ERROR rate should be the same " + rate + " " + nextBasal.rate);
        return false;
      }
      if(endTimestamp() < nextBasal.created_at || 
          nextBasal.created_at < created_at) {
        Log.i(TAG, "ERROR start time of two objects is wrong " + toS() + " " + nextBasal.toS());
        return false;
      }
      
      setEnd(nextBasal.endTimestamp());
      return true;
      
    }
    
    public boolean isFakeRange() {
        return fake_range;
    }
    
    // The following functions are needed to prepare the lists to be drawn on the screen.
    static public void mergeIdenticalItems(List<NSBasal> basal_list) {
        // Go over the list and merge items with identical rate
        int list_size = basal_list.size();
        for(int i = 0; i < list_size - 1; i++) {
            for (int j=i+1; j < list_size; j++) {
                if(basal_list.get(i).rate != basal_list.get(j).rate) {
                    // Get out of the inner loop, objects are not the same
                    break;
                }
                // We have two objects with the same rate, merge them.
                boolean merged = basal_list.get(i).merge(basal_list.get(j));
                if(!merged) {
                    break;
                }
                basal_list.remove(j);
                list_size--;
                // Since we took one object out, we need to make sure that
                // next time we will continue from the correct place.
                j--;
            }
        }
    }
    static private void print_list(List<NSBasal> basallist, String extra) {
        Log.e("xxxx", "Starting print" + extra);
        for(NSBasal item : basallist)
        Log.e("xxxx", "adding item " + item.toS());
    }
    
    static public  void trimItems(List<NSBasal> basal_list) {
        // Go over the list and make sure that if a new basal was started before this one, this one is trimmed.
        // If we don't have data, use -1. On the prining function, we will not print it.
        //print_list(basal_list, "Before trimItems");
        int listSize = basal_list.size();
        for(int i = 0; i < listSize - 1; i++) {
            NSBasal current = basal_list.get(i);
            long next_start = basal_list.get(i+1).created_at;
            if (current.endTimestamp() > next_start) {
                // Trim this basal, if a new one did not allow it to finish.
                current.setEnd(next_start);
            } else if (next_start != current.endTimestamp()) {
                // We have a hole in our data. Default was used, but we don't know what it is.
                // So assuming zero. Creating a new value and adding it to the list.
                NSBasal newItem = new NSBasal(current.endTimestamp(), 0, (next_start - current.endTimestamp()) / 60000.0);
                newItem.fake_range = true;
                basal_list.add(i+1, newItem);
                listSize++;
            }
        }
        //print_list(basal_list, "After trimItems");
    }

    
    

    // create the table ourselves without worrying about model versioning and downgrading
    public static void updateDB() {
        patched = fixUpTable(schema, patched);
    }
}
