package com.eveningoutpost.dexdrip.models;

import android.provider.BaseColumns;

import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Emma Black on 1/14/15.
 */
@Table(name = "ActiveBgAlert", id = BaseColumns._ID)
public class ActiveBgAlert extends Model {

    private final static String TAG = AlertPlayer.class.getSimpleName();
    private static boolean patched = false;

    @Column(name = "alert_uuid")
    public String alert_uuid;

    @Column(name = "is_snoozed")
    public boolean is_snoozed;

    @Column(name = "last_alerted_at") // Do we need this
    public Long last_alerted_at;

    @Column(name = "next_alert_at")
    public Long next_alert_at;

    // This is needed in order to have ascending alerts
    // we set the real value of it when is_snoozed is being turned to false
    @Column(name = "alert_started_at")
    public Long alert_started_at;


    public boolean ready_to_alarm() {
        if(new Date().getTime() > next_alert_at) {
            return true;
        }
        return false;
    }

    public static boolean currentlyAlerting() {
        final ActiveBgAlert activeBgAlert = getOnly();
        return activeBgAlert != null && !activeBgAlert.is_snoozed;
    }

    public static boolean alertSnoozeOver() {
        ActiveBgAlert activeBgAlert = getOnly();
        if (activeBgAlert == null) {
            // no alert exists, so snoozing is over... (this should not happen)
            Log.wtf(TAG, "ActiveBgAlert getOnly returning null (we have just checked it)");
            return true;
        }
        return activeBgAlert.ready_to_alarm();
    }

    public void snooze(int minutes) {
        next_alert_at = new Date().getTime() + minutes * 60000;
        is_snoozed = true;
        Log.ueh("Snoozed Alert","Snoozed until: "+JoH.dateTimeText(next_alert_at));
        save();
    }

    public String toString() {

        try {
            String alert_uuid = "alert_uuid: " + this.alert_uuid;
            String is_snoozed = "is_snoozed: " + this.is_snoozed;
            String last_alerted_at = "last_alerted_at: " + DateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.LONG).format(new Date(this.last_alerted_at));
            String next_alert_at = "next_alert_at: " + DateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.LONG).format(new Date(this.next_alert_at));

            String alert_started_at = "alert_started_at: " + DateFormat.getDateTimeInstance(
                    DateFormat.LONG, DateFormat.LONG).format(new Date(this.alert_started_at));

            return alert_uuid + " " + is_snoozed + " " + last_alerted_at + " " + next_alert_at + " " + alert_started_at;

        } catch (NullPointerException e) {
            Log.e(TAG, "Got Nullpointer exception in toString()! " + e);
            return "Nullpointer exception in toString!";
        }
    }

    // We should only have at most one active alert at any given time.
    // This means that we will only have one of this objects at the database at any given time.
    // so we have the following static functions: getOnly, saveData, ClearData

    public static ActiveBgAlert getOnly() {
        try {
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
        } catch (android.database.sqlite.SQLiteException e) {
            Log.d(TAG,"ActiveBgAlert rebuilding table strcuture");
            fixUpTable();
            return null;
        }
    }

    public static AlertType alertTypegetOnly() {
        ActiveBgAlert aba = getOnly();

        if (aba == null) {
            Log.v(TAG, "ActiveBgAlert: alertTypegetOnly returning null");
            return null;
        }

        AlertType alert = AlertType.get_alert(aba.alert_uuid);
        if(alert == null) {
            Log.d(TAG, "alertTypegetOnly did not find the active alert as part of existing alerts. returning null");
            // removing the alert to be in a better state.
            ClearData();
            return null;
        }
        if(!alert.uuid.equals(aba.alert_uuid)) {
            Log.wtf(TAG, "AlertType.get_alert did not return the correct alert");
        }
        return alert;
    }

    public static void Create(String alert_uuid, boolean is_snoozed, Long next_alert_at) {
        Log.d(TAG, "ActiveBgAlert Create called");
        fixUpTable();
        ActiveBgAlert aba = getOnly();
        if (aba == null) {
            aba = new ActiveBgAlert();
        }
        aba.alert_uuid = alert_uuid;
        aba.is_snoozed = is_snoozed;
        aba.last_alerted_at = 0L;
        aba.next_alert_at = next_alert_at;
        aba.alert_started_at = new Date().getTime();
        aba.save();
    }

    public static void ClearData() {
        Log.d(TAG, "ActiveBgAlert ClearData called");
        ActiveBgAlert aba = getOnly();
        if (aba != null) {
            aba.delete();
        }
    }

    public static void ClearIfSnoozeFinished() {
        Log.d(TAG, "ActiveBgAlert ClearIfSnoozeFinished called");
        ActiveBgAlert aba = getOnly();
        if (aba != null) {
            if(new Date().getTime() > aba.next_alert_at) {
                Log.d(TAG, "ActiveBgAlert ClearIfSnoozeFinished deleting allert");
                aba.delete();
            }
        }
    }

    // This function is called from ClockTick, when we play
    // If we were snoozed, we update the snooze to false, and update the start time.
    // return the time in minutes from the time playing the alert has started
    public int getUpdatePlayTime() {
        if(is_snoozed) {
            is_snoozed = false;
            alert_started_at = new Date().getTime();
            save();
        }
        Long timeSeconds =  (new Date().getTime() - alert_started_at) / 1000;
        return (int)Math.round(timeSeconds / 60.0);
    }

    public void updateNextAlertAt(long nextAlertTime){
        next_alert_at = nextAlertTime;
        save();
    }

    private static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "CREATE TABLE ActiveBgAlert (_id INTEGER PRIMARY KEY AUTOINCREMENT;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN alert_started_at INTEGER;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN alert_uuid TEXT;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN is_snoozed INTEGER;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN last_alerted_at INTEGER;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN next_alert_at INTEGER;",
                "ALTER TABLE ActiveBgAlert ADD COLUMN alert_started_at INTEGER;"
        };

        for (String patch : patchup) {
            try {
                SQLiteUtils.execSql(patch);
                Log.e(TAG, "Processed patch should not have succeeded!!: " + patch);
            } catch (Exception e) {
                // Log.d(TAG, "Patch: " + patch + " generated exception as it should: " + e.toString());
            }
        }
        patched = true;
    }
}

