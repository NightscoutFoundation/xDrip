package com.eveningoutpost.dexdrip.Models;

import android.R.bool;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.text.DateFormat;
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

    @Column(name = "minutes_between") //??? what is the differance between minutes_between and default_snooze ???
    public int minutes_between;

    @Column(name = "default_snooze")
    public int default_snooze;

    @Column(name = "text") // ??? what's that? is it different from name?
    public String text;
    
    @Column(name = "mp3_file")
    public String mp3_file;
    
    @Column(name = "uuid", index = true)
    public String uuid;

    private final static String TAG = Notifications.class.getSimpleName();
    
    public static AlertType get_alert(String uuid) {

        return new Select()
        .from(AlertType.class)
        .where("uuid = ? ", uuid)
        .executeSingle();
    }
    
    // bg_minute is the estimatin of the bg change rate
    public static AlertType get_highest_active_alert(double bg, double bg_minute) {
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
            .orderBy("threshold asc")
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
    }
    
    public static void add_alert(String name, boolean above, double threshold, boolean all_day, int minutes_between) {
        AlertType at = new AlertType();
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = UUID.randomUUID().toString();
        at.active = true;
        at.save();
    }
    
    public static void update_alert(String uuid, String name, boolean above, double threshold, boolean all_day, int minutes_between) {
        AlertType at = get_alert(uuid);
        at.name = name;
        at.above = above;
        at.threshold = threshold;
        at.all_day = all_day;
        at.minutes_between = minutes_between;
        at.uuid = uuid;
        at.active = true;
        at.save();
    }
    public static void remove_alert(String uuid) {
        AlertType alert = get_alert(uuid);
        alert.delete();
    }
    
    public String toString() {
        
        String name = "name: " + this.name;
        String above = "above: " + this.above;
        String threshold = "threshold: " + this.threshold;
        String all_day = "all_day: " + this.all_day;
        String minutes_between = "minutes_between: " + this.minutes_between; 
        String uuid = "uuid: " + this.uuid; 

        return name + " " + above + " " + threshold + " "+ all_day + " " + minutes_between + " uuid" + uuid;
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
        List<AlertType> alerts  = new Select()
            .from(AlertType.class)
            .where("above = ?", above)
            .execute();

        return alerts;
    }
    
    
    // This function is a replacment for the UI. It will make sure that there are exactly two alerts
    // based on what the user has set as high and low. Will be replaced by a UI.
    public static void CreateStaticAlerts(Context context) {
        // If there are two alerts already, we are done...
        List<AlertType> Alerts  = new Select()
            .from(AlertType.class)
            .execute();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Double highValue = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowValue = Double.parseDouble(prefs.getString("lowValue", "70"));
        if (Alerts.size() == 2) {
            if(Alerts.get(0).threshold == highValue && Alerts.get(0).above == true &&
                    Alerts.get(1).threshold == lowValue && Alerts.get(1).above == false) {
                Log.e(TAG, "CreateStaticAlerts we have our alerts ok...");
                return;
            }
            if(Alerts.get(1).threshold == highValue && Alerts.get(1).above == true &&
                    Alerts.get(0).threshold == lowValue && Alerts.get(0).above == false) {
                Log.e(TAG, "CreateStaticAlerts we have our alerts ok...");
                return;
            }
            
        }
        Log.e(TAG, "CreateStaticAlerts re-creating all our alerts again");
        remove_all();
        add_alert("high alert", true, highValue, true, 1);
        add_alert("low alert", false, lowValue, true, 1);
        print_all();
    }
    
   
    public static void testAll() {
        
        remove_all();
        add_alert("high alert 1", true, 180, true, 10);
        add_alert("high alert 2", true, 200, true, 10);
        add_alert("high alert 3", true, 220, true, 10);
        print_all();
        AlertType a1 = get_highest_active_alert(190, 0);
        Log.e(TAG, "a1 = " + a1.toString());
        AlertType a2 = get_highest_active_alert(210, 0);
        Log.e(TAG, "a2 = " + a2.toString());        

        
        AlertType a3 = get_alert(a1.uuid);
        Log.e(TAG, "a1 == a3 ? need to see true " + (a1==a3) + a1 + " " + a3);
        
        add_alert("low alert 1", false, 80, true, 10);
        add_alert("low alert 2", false, 60, true, 10);
        
        AlertType al1 = get_highest_active_alert(90, 0);
        Log.e(TAG, "al1 should be null  " + al1);
        al1 = get_highest_active_alert(80, 0);
        Log.e(TAG, "al1 = " + al1.toString());
        AlertType al2 = get_highest_active_alert(50, 0);
        Log.e(TAG, "al2 = " + al2.toString());

        Log.e(TAG, "HigherAlert(a1, a2) = a1?" +  (HigherAlert(a1,a2) == a2));
        Log.e(TAG, "HigherAlert(al1, al2) = al1?" +  (HigherAlert(al1,al2) == al2));
        Log.e(TAG, "HigherAlert(a1, al1) = al1?" +  (HigherAlert(a1,al1) == al1));
        Log.e(TAG, "HigherAlert(al1, a2) = al1?" +  (HigherAlert(al1,a2) == al1));
        
        // Make sure we do not influance on real data...
        remove_all();
        
    }
 
 
    private boolean in_time_frame() {
        int time_now = 0; //TODO: Get the actual time of day as a double this WILL NOT WORK without that
        if (all_day) {
            //Log.e(TAG, "in_time_frame returning true " );
            return true; 
        }

        if(start_time_minutes < end_time_minutes) {
            if (time_now > start_time_minutes && time_now < end_time_minutes) {
                return true;
            }
        } else {
            if (time_now < start_time_minutes || time_now > end_time_minutes) {
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
}
