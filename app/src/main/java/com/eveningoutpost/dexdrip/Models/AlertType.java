package com.eveningoutpost.dexdrip.Models;

import android.R.bool;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by stephenblack on 1/14/15.
 */
@Table(name = "AlertType", id = BaseColumns._ID)
public class AlertType extends Model {

    @Column(name = "name")
    public String name;

    @Column(name = "active")
    public boolean active;

    @Column(name = "volume")
    public int volume;

    @Column(name = "vibrate")
    public boolean vibrate;

    @Column(name = "light")
    public boolean light;

    @Column(name = "override_silent_mode")
    public boolean override_silent_mode;

    @Column(name = "predictive")
    public boolean predictive;

    @Column(name = "time_until_threshold_crossed")
    public double time_until_threshold_crossed;

    // If it is not above, then it must be below.
    @Column(name = "above")
    public boolean above;

    @Column(name = "threshold")
    public double threshold;

    @Column(name = "all_day")
    public boolean all_day;

    @Column(name = "start_time_minutes")
    public int start_time_minutes;  // This have probable be in minutes from start of day. this is not time...

    @Column(name = "end_time_minutes")
    public int end_time_minutes;

    @Column(name = "minutes_between") //??? what is the difference between minutes_between and default_snooze ???
    public int minutes_between; // The idea here was if ignored it will go off again each x minutes, snooze would be if it was aknowledged and dismissed it will go off again in y minutes
    // that said, Im okay with doing away with the minutes between and just doing it at a set 5 mins like dex

    @Column(name = "default_snooze")
    public int default_snooze;

    @Column(name = "text") // ??? what's that? is it different from name?
    public String text; // I figured if we wanted some special text, Its

    @Column(name = "mp3_file")
    public String mp3_file;

    @Column(name = "uuid", index = true)
    public String uuid;

    public final static String LOW_ALERT_55 = "c5f1999c-4ec5-449e-adad-3980b172b920";
    private final static String TAG = Notifications.class.getSimpleName();
    private final static String TAG_ALERT = "AlertBg";

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
            Log.w("NOTIFICATIONS", "Notifications are currently disabled!!");
            return null;
        }

        checkIfMissedReadingAlert(context);

        if (bg <= 14) { // Special dexcom codes should not set off low alarms
            return null;
        }

        Boolean bg_unclear_readings_alerts = prefs.getBoolean("bg_unclear_readings_alerts", false);
        Long UnclearTimeSetting = Long.parseLong(prefs.getString("bg_unclear_readings_minutes", "90")) * 60000;

        Long UnclearTime = BgReading.getUnclearTime(UnclearTimeSetting);

        AlertType at;
        if (UnclearTime >= UnclearTimeSetting && bg_unclear_readings_alerts ) {
            Log.w("NOTIFICATIONS", "Readings have been unclear for too long!!");
            Notifications.bgUnclearAlert(context);
        }
        if ((UnclearTime > 0 ) && bg_unclear_readings_alerts) {
            Log.e(TAG_ALERT, "We are in an clear state, but not for too long. Alerts are disabled");
            return null;
        }
        at = get_highest_active_alert_helper(bg);
        if (at != null) {
            Log.e(TAG_ALERT, "get_highest_active_alert_helper returned alert uuid = " + at.uuid + " alert name = " + at.name);
        } else {
            Log.e(TAG_ALERT, "get_highest_active_alert_helper returned NULL");
        }
        return at;
    }

    public static void checkIfMissedReadingAlert(Context context){
        context.startService(new Intent(context, MissedReadingService.class));
    }



    // bg_minute is the estimatin of the bg change rate
    private static AlertType get_highest_active_alert_helper(double bg) {
        // Chcek the low alerts

        List<AlertType> lowAlerts  = new Select()
            .from(AlertType.class)
            .where("threshold >= ?", bg)
            .where("above = ?", false)
            .orderBy("threshold asc")
            .execute();

        for (AlertType lowAlert : lowAlerts) {
            if(lowAlert.should_alarm(bg)) {
                return lowAlert;
            }
        }

        // If no low alert found, check higher alert.
        List<AlertType> HighAlerts  = new Select()
            .from(AlertType.class)
            .where("threshold <= ?", bg)
            .where("above = ?", true)
            .orderBy("threshold desc")
            .execute();

        for (AlertType HighAlert : HighAlerts) {
            //Log.e(TAG, "Testing high alert " + HighAlert.toString());
            if(HighAlert.should_alarm(bg)) {
                return HighAlert;
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
            int snooze) {
        AlertType at = new AlertType();
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid != null? uuid : UUID.randomUUID().toString();
        at.active = true;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.default_snooze = snooze;
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
            int snooze) {

        if(uuid.equals(LOW_ALERT_55)) {
            // This alert can not be removed/updated
            return;
        }

        AlertType at = get_alert(uuid);
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid;
        at.active = true;
        at.mp3_file = mp3_file;
        at.start_time_minutes = start_time_minutes;
        at.end_time_minutes = end_time_minutes;
        at.override_silent_mode = override_silent_mode;
        at.default_snooze = snooze;
        at.save();
    }
    public static void remove_alert(String uuid) {
        if(uuid.equals(LOW_ALERT_55)) {
            // This alert can not be removed/updated
            return;
        }
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

    public static void print_all() {
        List<AlertType> Alerts  = new Select()
            .from(AlertType.class)
            .execute();

        Log.e(TAG,"List of all alerts");
        for (AlertType alert : Alerts) {
            Log.e(TAG, alert.toString());
        }
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



    // This function is used to make sure that we always have a static alert on 55 low.
    // This alert will not be editable/removable.
    public static void CreateStaticAlerts() {
        if(get_alert(LOW_ALERT_55) == null) {
            add_alert(LOW_ALERT_55, "low alert (unchangable)", false, 55, true, 1, null, 0, 0, true, 20);
        }
    }


    public static void testAll(Context context) {

        remove_all();
        add_alert(null, "high alert 1", true, 180, true, 10, null, 0, 0, true, 20);
        add_alert(null, "high alert 2", true, 200, true, 10, null, 0, 0, true, 20);
        add_alert(null, "high alert 3", true, 220, true, 10, null, 0, 0, true, 20);
        print_all();
        AlertType a1 = get_highest_active_alert(context, 190);
        Log.e(TAG, "a1 = " + a1.toString());
        AlertType a2 = get_highest_active_alert(context, 210);
        Log.e(TAG, "a2 = " + a2.toString());


        AlertType a3 = get_alert(a1.uuid);
        Log.e(TAG, "a1 == a3 ? need to see true " + (a1==a3) + a1 + " " + a3);

        add_alert(null, "low alert 1", false, 80, true, 10, null, 0, 0, true, 20);
        add_alert(null, "low alert 2", false, 60, true, 10, null, 0, 0, true, 20);

        AlertType al1 = get_highest_active_alert(context, 90);
        Log.e(TAG, "al1 should be null  " + al1);
        al1 = get_highest_active_alert(context, 80);
        Log.e(TAG, "al1 = " + al1.toString());
        AlertType al2 = get_highest_active_alert(context, 50);
        Log.e(TAG, "al2 = " + al2.toString());

        Log.e(TAG, "HigherAlert(a1, a2) = a1?" +  (HigherAlert(a1,a2) == a2));
        Log.e(TAG, "HigherAlert(al1, al2) = al1?" +  (HigherAlert(al1,al2) == al2));
        Log.e(TAG, "HigherAlert(a1, al1) = al1?" +  (HigherAlert(a1,al1) == al1));
        Log.e(TAG, "HigherAlert(al1, a2) = al1?" +  (HigherAlert(al1,a2) == al1));

        // Make sure we do not influance on real data...
        remove_all();

    }


    private boolean in_time_frame() {
        if (all_day) {
            //Log.e(TAG, "in_time_frame returning true " );
            return true;
        }
        // time_now is the number of minutes that have passed from the start of the day.
        Calendar rightNow = Calendar.getInstance();
        int time_now = toTime(rightNow.get(Calendar.HOUR_OF_DAY), rightNow.get(Calendar.MINUTE));
        Log.e(TAG, "time_now is " + time_now + " minutes" + " start_time " + start_time_minutes + " end_time " + end_time_minutes);
        if(start_time_minutes < end_time_minutes) {
            if (time_now >= start_time_minutes && time_now <= end_time_minutes) {
                return true;
            }
        } else {
            if (time_now >= start_time_minutes || time_now <= end_time_minutes) {
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
        int snooze,
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
            at.default_snooze = snooze;
            AlertPlayer.getPlayer().startAlert(context, false, at, "TEST");
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
}
