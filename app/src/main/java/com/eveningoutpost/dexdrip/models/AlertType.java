package com.eveningoutpost.dexdrip.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.activeandroid.util.SQLiteUtils;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import lombok.val;

/**
 * Created by Emma Black on 1/14/15.
 */
@Table(name = "AlertType", id = BaseColumns._ID)
public class AlertType extends Model {

    @Expose
    @Column(name = "name")
    public String name;

    @Expose
    @Column(name = "active")
    public boolean active;

    @Expose
    @Column(name = "volume")
    public int volume;

    @Expose
    @Column(name = "vibrate")
    public boolean vibrate;

    @Expose
    @Column(name = "light")
    public boolean light;

    @Expose
    @Column(name = "override_silent_mode")
    public boolean override_silent_mode;

    @Expose
    @Column(name = "force_speaker")
    public boolean force_speaker;

    @Expose
    @Column(name = "predictive")
    public boolean predictive;

    @Expose
    @Column(name = "time_until_threshold_crossed")
    public double time_until_threshold_crossed;

    // If it is not above, then it must be below.
    @Expose
    @Column(name = "above")
    public boolean above;

    @Expose
    @Column(name = "threshold")
    public double threshold;

    @Expose
    @Column(name = "all_day")
    public boolean all_day;

    @Expose
    @Column(name = "start_time_minutes")
    public int start_time_minutes;  // This have probable be in minutes from start of day. this is not time...

    @Expose
    @Column(name = "end_time_minutes")
    public int end_time_minutes;

    @Expose
    @Column(name = "minutes_between") //??? what is the difference between minutes_between and default_snooze ???
    public int minutes_between; // The idea here was if ignored it will go off again each x minutes, snooze would be if it was aknowledged and dismissed it will go off again in y minutes
    // that said, Im okay with doing away with the minutes between and just doing it at a set 5 mins like dex

    @Expose
    @Column(name = "default_snooze")
    public int default_snooze;

    @Expose
    @Column(name = "text") // ??? what's that? is it different from name?
    public String text; // I figured if we wanted some special text, Its

    @Expose
    @Column(name = "mp3_file")
    public String mp3_file;

    @Expose
    @Column(name = "uuid", index = true)
    public String uuid;

    public final static String LOW_ALERT_55 = "c5f1999c-4ec5-449e-adad-3980b172b920";
    private final static String TAG = Notifications.class.getSimpleName();
    private final static String TAG_ALERT = "AlertBg";
    private static boolean patched = false;

    // This shouldn't be needed but it seems it is
    public static void fixUpTable() {
        if (patched) return;
        String[] patchup = {
                "ALTER TABLE AlertType ADD COLUMN volume INTEGER;",
                "ALTER TABLE AlertType ADD COLUMN light INTEGER;",
                "ALTER TABLE AlertType ADD COLUMN predictive INTEGER;",
                "ALTER TABLE AlertType ADD COLUMN text TEXT;",
                "ALTER TABLE AlertType ADD COLUMN force_speaker INTEGER;",
                "ALTER TABLE AlertType ADD COLUMN time_until_threshold_crossed REAL;"
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



    public static AlertType get_alert(String uuid) {

        return new Select()
        .from(AlertType.class)
        .where("uuid = ? ", uuid)
        .executeSingle();
    }

    /*
     * This function has 3 needs. In the case of "unclear state" return null
     * In the case of "unclear state" for more than predefined time, return the "55" alert
     * In case that alerts are turned off, only return the 55.
     */
    public static AlertType get_highest_active_alert(Context context, double bg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.d("NOTIFICATIONS", "Notifications are currently disabled!!");
            return null;
        }

        if (bg <= 14) { // Special dexcom codes should not set off low alarms
            return null;
        }

        AlertType at;
        at = get_highest_active_alert_helper(bg, prefs);
        if (at != null) {
            Log.d(TAG_ALERT, "get_highest_active_alert_helper returned alert uuid = " + at.uuid + " alert name = " + at.name);
        } else {
            Log.d(TAG_ALERT, "get_highest_active_alert_helper returned NULL");
        }
        return at;
    }

    private static AlertType filter_alert_on_stale(AlertType alert, SharedPreferences prefs)
    {
        // this should already be happening in notifications.java but it doesn't seem to work so adding here as well
        if (prefs.getBoolean("disable_alerts_stale_data", false)) {
            final int stale_minutes = Math.max(6, Integer.parseInt(prefs.getString("disable_alerts_stale_data_minutes", "15")) + 2);
            if (!BgReading.last_within_minutes(stale_minutes)) {
                Log.w(TAG, "Blocking alarm raise as data older than: " + stale_minutes);
                return null; // block
            }
        }
        return alert; // allow
    }

    // bg_minute is the estimatin of the bg change rate
    private static AlertType get_highest_active_alert_helper(double bg, SharedPreferences prefs) {
        // Chcek the low alerts

        final double offset = ActivityRecognizedService.raise_limit_due_to_vehicle_mode() ? ActivityRecognizedService.getVehicle_mode_adjust_mgdl() : 0;

        if(prefs.getLong("low_alerts_disabled_until", 0) > new Date().getTime()){
            Log.i("NOTIFICATIONS", "get_highest_active_alert_helper: Low alerts are currently disabled!! Skipping low alerts");

        } else {
            List<AlertType> lowAlerts  = new Select()
                    .from(AlertType.class)
                    .where("threshold >= ?", bg-offset)
                    .where("above = ?", false)
                    .orderBy("threshold asc")
                    .execute();

            for (AlertType lowAlert : lowAlerts) {
                if(lowAlert.should_alarm(bg-offset)) {
                    return filter_alert_on_stale(lowAlert,prefs);
                }
            }
        }


        // If no low alert found or low alerts disabled, check higher alert.
        if(prefs.getLong("high_alerts_disabled_until", 0) > new Date().getTime()){
            Log.i("NOTIFICATIONS", "get_highest_active_alert_helper: High alerts are currently disabled!! Skipping high alerts");
            ;
        } else {
            List<AlertType> HighAlerts  = new Select()
                    .from(AlertType.class)
                    .where("threshold <= ?", bg)
                    .where("above = ?", true)
                    .orderBy("threshold desc")
                    .execute();

            for (AlertType HighAlert : HighAlerts) {
                //Log.e(TAG, "Testing high alert " + HighAlert.toString());
                if(HighAlert.should_alarm(bg)) {
                    return filter_alert_on_stale(HighAlert,prefs);
                }
            }
        }
        // no alert found
        return null;
    }

    // returns true, if one allert is up and the second is down
    public static boolean OpositeDirection(AlertType a1, AlertType a2) {
        if (a1.above != a2.above) {
            return true;
        }
        return false;
    }

    // Checks if a1 is more important than a2. returns the higher one
    public static AlertType HigherAlert(AlertType a1, AlertType a2) {
        if (a1.above && !a2.above) {
            return a2;
        }
        if (!a1.above && a2.above) {
            return a1;
        }
        if (a1.above && a2.above) {
            // both are high, the higher the better
            if (a1.threshold > a2.threshold) {
                return a1;
            } else {
                return a2;
            }
        }
        if (a1.above || a2.above) {
            Log.wtf(TAG, "a1.above and a2.above must be false");
        }
        // both are low, the lower the better
        if (a1.threshold < a2.threshold) {
            return a1;
        } else {
            return a2;
        }
    }

    public static void remove_all() {
        List<AlertType> Alerts  = new Select()
        .from(AlertType.class)
        .execute();

        for (AlertType alert : Alerts) {
            alert.delete();
        }
        ActiveBgAlert.ClearData();
    }

    public static void add_alert(
            String uuid,
            String name,
            boolean above,
            double threshold,
            boolean all_day,
            int minutes_between,
            String mp3_file,
            int start_time_minutes,
            int end_time_minutes,
            boolean override_silent_mode,
            boolean force_speaker,
            int snooze,
            boolean vibrate,
            boolean active) {
        AlertType at = new AlertType();
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid != null? uuid : UUID.randomUUID().toString();
        at.active = active;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.force_speaker = force_speaker;
        at.default_snooze = snooze;
        at.vibrate = vibrate;
        at.save();
    }

    public static void update_alert(
            String uuid,
            String name,
            boolean above,
            double threshold,
            boolean all_day,
            int minutes_between,
            String mp3_file,
            int start_time_minutes,
            int end_time_minutes,
            boolean override_silent_mode,
            boolean force_speaker,
            int snooze,
            boolean vibrate,
            boolean active) {

        fixUpTable();

        final AlertType at = get_alert(uuid);
        if (at == null) {
            Log.e(TAG, "Alert Type null during update");
            return;
        }
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid;
        at.active = active;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.force_speaker = force_speaker;
        at.default_snooze = snooze;
        at.vibrate = vibrate;
        at.save();
    }
    public static void remove_alert(String uuid) {
        AlertType alert = get_alert(uuid);
		if(alert != null) {
	        alert.delete();
        }
    }

    public String toString() {

        String name = "name: " + this.name;
        String above = "above: " + this.above;
        String threshold = "threshold: " + this.threshold;
        String all_day = "all_day: " + this.all_day;
        String time = "Start time: " + this.start_time_minutes + " end time: "+ this.end_time_minutes;
        String minutes_between = "minutes_between: " + this.minutes_between;
        String uuid = "uuid: " + this.uuid;

        return name + " " + above + " " + threshold + " "+ all_day + " " +time +" " + minutes_between + " uuid" + uuid;
    }

    public String toS() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        return gson.toJson(this);
    }

    public static void print_all() {
        List<AlertType> Alerts  = new Select()
            .from(AlertType.class)
            .execute();

        Log.d(TAG,"List of all alerts");
        for (AlertType alert : Alerts) {
            Log.d(TAG, alert.toString());
        }
    }

    // get the first item in the alert list which is active for either high or low alert, sorted by when the threshold will be hit, eg the highest low alert or the lowest high alert
    public static double getFirstActiveAlertThreshold(final boolean highAlert) {
        final List<AlertType> list = getAll(highAlert);
        if (list != null) {
            for (final AlertType alert : list) {
                if (alert.active) return alert.threshold;
            }
        }
        return -1;
    }

    public static List<AlertType> getAllActive() {
        List<AlertType> alerts  = new Select()
                .from(AlertType.class)
                .where("active = ?", true)
                .execute();

        return alerts;
    }

    public static List<AlertType> getAll(boolean above) {
        String order;
        if (above) {
            order = "threshold asc";
        } else {
            order = "threshold desc";
        }
        List<AlertType> alerts  = new Select()
            .from(AlertType.class)
            .where("above = ?", above)
            .orderBy(order)
            .execute();

        return alerts;
    }

    public static boolean activeLowAlertExists() {
        List<AlertType> alerts = getAll(false);
        if(alerts == null) {
            return false;
        }
        for (AlertType alert : alerts) {
            if(alert.active) {
                return true;
            }
        }
        return false;
    }

    public static AlertType getMostExtremeAlert(boolean highAlerts) {
        val alerts = getAll(highAlerts);
        if (alerts == null) return null;
        val filtered = new ArrayList<AlertType>();
        for (val alert : alerts) {
            if (alert.active && alert.in_time_frame()) {    // remove alerts which are not live now
                filtered.add(alert);
            }
        }
        if (filtered.size() == 0) return null;
        return filtered.get(filtered.size()-1); // They are sorted with max last
    }

    public static AlertType getLowestAlert() {
        return getMostExtremeAlert(false);
    }

    public static AlertType getHighestAlert() {
        return getMostExtremeAlert(true);
    }

    // This function is used to make sure that we always have a static alert on 55 low.
    // This alert will not be editable/removable.
    public static void CreateStaticAlerts() {
        if(get_alert(LOW_ALERT_55) == null) {
            add_alert(LOW_ALERT_55, "low alert ", false, 55, true, 1, null, 0, 0, true, true, 20, true, true);
        }
    }


    public static void testAll(Context context) {
        remove_all();
        add_alert(null, "high alert 1", true, 180, true, 10, null, 0, 0, true, true, 20, true, true);
        add_alert(null, "high alert 2", true, 200, true, 10, null, 0, 0, true, true,20, true, true);
        add_alert(null, "high alert 3", true, 220, true, 10, null, 0, 0, true, true,20, true, true);
        print_all();
        AlertType a1 = get_highest_active_alert(context, 190);
        Log.d(TAG, "a1 = " + a1.toString());
        AlertType a2 = get_highest_active_alert(context, 210);
        Log.d(TAG, "a2 = " + a2.toString());


        AlertType a3 = get_alert(a1.uuid);
        Log.d(TAG, "a1 == a3 ? need to see true " + (a1==a3) + a1 + " " + a3);

        add_alert(null, "low alert 1", false, 80, true, 10, null, 0, 0, true, true,20, true, true);
        add_alert(null, "low alert 2", false, 60, true, 10, null, 0, 0, true, true,20, true, true);

        AlertType al1 = get_highest_active_alert(context, 90);
        Log.d(TAG, "al1 should be null  " + al1);
        al1 = get_highest_active_alert(context, 80);
        Log.d(TAG, "al1 = " + al1.toString());
        AlertType al2 = get_highest_active_alert(context, 50);
        Log.d(TAG, "al2 = " + al2.toString());

        Log.d(TAG, "HigherAlert(a1, a2) = a1?" +  (HigherAlert(a1,a2) == a2));
        Log.d(TAG, "HigherAlert(al1, al2) = al1?" +  (HigherAlert(al1,al2) == al2));
        Log.d(TAG, "HigherAlert(a1, al1) = al1?" +  (HigherAlert(a1,al1) == al1));
        Log.d(TAG, "HigherAlert(al1, a2) = al1?" +  (HigherAlert(al1,a2) == al1));

        // Make sure we do not influance on real data...
        remove_all();

    }


    private boolean in_time_frame() {
        return s_in_time_frame(all_day, start_time_minutes, end_time_minutes);
    }
    
    static public boolean  s_in_time_frame(boolean s_all_day, int s_start_time_minutes, int s_end_time_minutes) {
        if (s_all_day) {
            //Log.e(TAG, "in_time_frame returning true " );
            return true;
        }
        // time_now is the number of minutes that have passed from the start of the day.
        Calendar rightNow = Calendar.getInstance();
        int time_now = toTime(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
        Log.d(TAG, "time_now is " + time_now + " minutes" + " start_time " + s_start_time_minutes + " end_time " + s_end_time_minutes);
        if(s_start_time_minutes < s_end_time_minutes) {
            if (time_now >= s_start_time_minutes && time_now <= s_end_time_minutes) {
                return true;
            }
        } else {
            if (time_now >= s_start_time_minutes || time_now <= s_end_time_minutes) {
                return true;
            }
        }
        return false;
    }

    private boolean beyond_threshold(double bg) {
        if (above && bg >= threshold) {
//            Log.e(TAG, "beyond_threshold returning true " );
            return true;
        } else if (!above && bg <= threshold) {
            return true;
        }
        return false;
    }

    private boolean trending_to_threshold(double bg) {
        if (!predictive) { return false; }
        if (above && bg >= threshold) {
            return true;
        } else if (!above && bg <= threshold) {
            return true;
        }
        return false;
    }
    
     public long getNextAlertTime(Context ctx) {
         int time = minutes_between;
         if (time < 1 || AlertPlayer.isAscendingMode(ctx)) {
             time = 1;
         }
         Calendar calendar = Calendar.getInstance();
         return calendar.getTimeInMillis() + (time * 60000);
     }

    public boolean should_alarm(double bg) {
//        Log.e(TAG, "should_alarm called active =  " + active );
        if(in_time_frame() && active && (beyond_threshold(bg) || trending_to_threshold(bg))) {
            return true;
        } else {
            return false;
        }
    }

    public static void testAlert(
        String name,
        boolean above,
        double threshold,
        boolean all_day,
        int minutes_between,
        String mp3_file,
        int start_time_minutes,
        int end_time_minutes,
        boolean override_silent_mode,
        boolean force_speaker,
        int snooze,
        boolean vibrate,
        Context context) {
            AlertType at = new AlertType();
            at.name = name;
            at.above = above;
            at.threshold = threshold;
            at.all_day = all_day;
            at.minutes_between = minutes_between;
            at.uuid = UUID.randomUUID().toString();
            at.active = true;
            at.mp3_file = mp3_file;
            at.start_time_minutes = start_time_minutes;
            at.end_time_minutes = end_time_minutes;
            at.override_silent_mode = override_silent_mode;
            at.force_speaker = force_speaker;
            at.default_snooze = snooze;
            at.vibrate = vibrate;
            AlertPlayer.getPlayer().startAlert(context, false, at, "TEST", false);
    }

    // Time is calculated in minutes. that is 01:20 means 80 minutes.

    // This functions are a bit tricky. We can only set time from 00:00 to 23:59 which leaves one minute out. this is because we ignore the
    // seconds. so if the user has set 23:59 we will consider this as 24:00
    // This will be done at the code that reads the time from the ui.



    // return the minutes part of the time
    public static int time2Minutes(int minutes) {
        return (minutes - 60*time2Hours(minutes)) ;
    }

 // return the hours part of the time
    public static int time2Hours(int minutes) {
        return minutes / 60;
    }

    // create the time from hours and minutes.
    public static int toTime(int hours, int minutes) {
        return hours * 60 + minutes;
    }
    
    // Convert all settings to a string and save it in the references. This is needed to allow it's backup. 
    public static boolean toSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<AlertType> alerts  = new Select()
            .from(AlertType.class)
            .execute();

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .serializeSpecialFloatingPointValues()
                .create();
        String output =  gson.toJson(alerts);
        Log.e(TAG, "Created the string " + output);
        prefs.edit().putString("saved_alerts", output).commit(); // always leave this as commit

        return true;

    }


    // Read all alerts from preference key and write them to db.
    public static boolean fromSettings(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedAlerts = prefs.getString("saved_alerts", "");
        if (savedAlerts.isEmpty()) {
            Log.i(TAG, "read saved_alerts string and it is empty");
            return true;
        }
        Log.i(TAG, "read alerts string " + savedAlerts);

        AlertType[] newAlerts = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(savedAlerts, AlertType[].class);
        if (newAlerts == null) {
            Log.e(TAG, "newAlerts is null");
            return true;
        }

        Log.i(TAG, "read successfuly " + newAlerts.length);
        // Now delete all existing alerts if we managed to unpack the json
        try {
            List<AlertType> alerts = new Select()
                    .from(AlertType.class)
                    .execute();
            for (AlertType alert : alerts) {
                alert.delete();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer exception: " + e);
        }

        try {
            for (AlertType alert : newAlerts) {
                Log.e(TAG, "Saving alert " + alert.name);
                alert.save();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got null pointer exception 2: " + e);
        }
        // Delete the string, so next time we will not load the data
        prefs.edit().putString("saved_alerts", "").apply();
        return true;

    }
    
}
