package com.eveningoutpost.dexdrip.Models;

import android.util.Log;
import android.provider.BaseColumns;

import com.activeandroid.annotation.Column;
import com.activeandroid.Model;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by stephenblack on 1/14/15.
 */
@Table(name = "ActiveBgAlert", id = BaseColumns._ID)
public class ActiveBgAlert extends Model {
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    
    @Column(name = "alert_uuid")
    public String alert_uuid;

    @Column(name = "is_snoozed") //??? Do we need this
    public boolean is_snoozed;

    @Column(name = "last_alerted_at") // Do we need this
    public Long last_alerted_at;

    @Column(name = "next_alert_at")
    public Long next_alert_at;

    public boolean ready_to_alarm() {
        if(new Date().getTime() > next_alert_at) {
            return true;
        }
        return false;
    }

    public void snooze(int minutes) {
        next_alert_at = new Date().getTime() + minutes * 60000;
        save();
    }
    
    public String toString() {
        
        String alert_uuid = "alert_uuid: " + this.alert_uuid;
        String is_snoozed = "is_snoozed: " + this.is_snoozed;
        String last_alerted_at = "last_alerted_at: " + DateFormat.getDateTimeInstance(
                DateFormat.LONG, DateFormat.LONG).format(new Date(this.last_alerted_at));
        String next_alert_at = "next_alert_at: " + DateFormat.getDateTimeInstance(
                DateFormat.LONG, DateFormat.LONG).format(new Date(this.next_alert_at)); 

        return alert_uuid + " " + is_snoozed + " " + last_alerted_at + " "+ next_alert_at;
        
        
    }
    
    // We should only have at most one active alert at any given time.
    // This means that we will only have one of this objects at the database at any given time.
    // so we have the following static functions: getOnly, saveData, ClearData
    
    
    public static ActiveBgAlert getOnly() {
        ActiveBgAlert aba = new Select()
                .from(ActiveBgAlert.class)
                .orderBy("_ID asc")
                .executeSingle();
        
        if (aba != null) {
            Log.v(TAG, "ActiveBgAlert getOnly aba = " + aba.toString());
        } else {
            Log.v(TAG, "ActiveBgAlert getOnly returning null");
        }
        
        return aba;
    }
    
    public static void Create(String alert_uuid, boolean is_snoozed, Long next_alert_at) {
        Log.e(TAG, "ActiveBgAlert Create called");
        ActiveBgAlert aba = getOnly();
        if (aba == null) {
            aba = new ActiveBgAlert();
        }
        aba.alert_uuid = alert_uuid;
        aba.is_snoozed = is_snoozed;
        aba.last_alerted_at = 0L;
        aba.next_alert_at = next_alert_at;
        aba.save();
    }
    
    public static void ClearData() {
        Log.e(TAG, "ActiveBgAlert ClearData called");
        ActiveBgAlert aba = getOnly();
        if (aba != null) {
            aba.delete();
        }
    }
        
}

