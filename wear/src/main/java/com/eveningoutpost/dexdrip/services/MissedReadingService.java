package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
//import com.eveningoutpost.dexdrip.Models.Reminder;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
//import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleUtil;
//import com.eveningoutpost.dexdrip.UtilityModels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
//import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.Date;

import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getLocalServiceCollectingState;

//import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

public class MissedReadingService extends IntentService {
    int otherAlertSnooze;
    private final static String TAG = MissedReadingService.class.getSimpleName();
    private static int aggressive_backoff_timer = 120;
    public MissedReadingService() {
        super("MissedReadingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final SharedPreferences prefs;
        final boolean bg_missed_alerts;
        final Context context;
        final int bg_missed_minutes;


        context = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Log.d(TAG, "MissedReadingService onHandleIntent");

        final long stale_millis = Home.stale_data_millis();

        // send to pebble
        /*if (prefs.getBoolean("broadcast_to_pebble", false) && (PebbleUtil.getCurrentPebbleSyncType(prefs) != 1) && !BgReading.last_within_millis(stale_millis)) {
            if (JoH.ratelimit("peb-miss",120)) context.startService(new Intent(context, PebbleWatchSync.class));
            // update pebble even when we don't have data to ensure missed readings show
        }*/

        /*if ((Home.get_forced_wear()) && prefs.getBoolean("disable_wearG5_on_missedreadings", false)) {
            int bg_wear_missed_minutes = readPerfsInt(prefs, "disable_wearG5_on_missedreadings_level", 30);
            if (BgReading.getTimeSinceLastReading() >= (bg_wear_missed_minutes * 1000 * 60)) {
                Log.d(TAG, "Request WatchUpdaterService to disable force_wearG5 when wear is connected");
                startWatchUpdaterService(context, WatchUpdaterService.ACTION_DISABLE_FORCE_WEAR, TAG);
            }
        }*/

        if ((prefs.getBoolean("aggressive_service_restart", false) || DexCollectionType.isFlakey())) {//!Home.get_enable_wear() &&
            if (!BgReading.last_within_millis(stale_millis) && Sensor.isActive() && (!getLocalServiceCollectingState())) {
                if (JoH.ratelimit("aggressive-restart", aggressive_backoff_timer)) {
                    Log.e(TAG, "Aggressively restarting collector service due to lack of reception: backoff: "+aggressive_backoff_timer);
                    if (aggressive_backoff_timer < 1200) aggressive_backoff_timer+=60;
                    CollectionServiceStarter.startBtService(context);
                } else {
                    aggressive_backoff_timer = 120; // reset
                }
            }
        }
        //Reminder.processAnyDueReminders();
        //BluetoothGlucoseMeter.immortality();

        bg_missed_alerts =  prefs.getBoolean("bg_missed_alerts", false);//KS TODO bg_missed_alerts pref not supported
        if (!bg_missed_alerts || !Pref.getBoolean("bg_notifications", false)) {
            // we should not do anything in this case. if the ui, changes will be called again
            return;
        }

        bg_missed_minutes =  readPerfsInt(prefs, "bg_missed_minutes", 30);
        final long now = new Date().getTime();

        if (BgReading.getTimeSinceLastReading() >= (bg_missed_minutes * 1000 * 60) &&
                prefs.getLong("alerts_disabled_until", 0) <= now &&
                inTimeFrame(prefs)) {
            Notifications.bgMissedAlert(context);
            checkBackAfterSnoozeTime(context, now);
        } else  {

            long disabletime = prefs.getLong("alerts_disabled_until", 0) - now;

            long missedTime = bg_missed_minutes* 1000 * 60 - BgReading.getTimeSinceLastReading();
            long alarmIn = Math.max(disabletime, missedTime);
            checkBackAfterMissedTime(alarmIn);
        }
    }

    private boolean inTimeFrame(SharedPreferences prefs) {
        
        int startMinutes = prefs.getInt("missed_readings_start", 0);
        int endMinutes = prefs.getInt("missed_readings_end", 0);
        boolean allDay = prefs.getBoolean("missed_readings_all_day", true);

        return AlertType.s_in_time_frame(allDay, startMinutes, endMinutes);
    }

    private void checkBackAfterSnoozeTime(Context context, long now) {
    	// This is not 100% acurate, need to take in account also the time of when this alert was snoozed.
        UserNotification userNotification = UserNotification.GetNotificationByType("bg_missed_alerts");
        if(userNotification == null) {
            // No active alert exists, should not happen, we have just created it.
        	Log.wtf(TAG, "No active alert exists.");
            setAlarm(getOtherAlertReraiseSec(context, "bg_missed_alerts") * 1000, false);
        } else {
            // we have an alert that should be re-raised on userNotification.timestamp
        	long alarmIn = (long)userNotification.timestamp - now;
        	if(alarmIn < 0) {
        		alarmIn = 0;
        	}
            setAlarm(alarmIn, true);
        }
    }

    private void checkBackAfterMissedTime(long alarmIn) {
        setAlarm(alarmIn, false);
    }
    
    // alarmIn is relative time ms
    public void setAlarm(long alarmIn, boolean force) {
        if(!force && (alarmIn < 5 * 60 * 1000)) {
            // No need to check more than once every 5 minutes
            alarmIn = 5 * 60 * 1000;
        }
    	Log.d(TAG, "Setting timer to  " + alarmIn / 60000 + " minutes from now" );
        //Calendar calendar = Calendar.getInstance();
        //AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        //long wakeTime = calendar.getTimeInMillis() + alarmIn;
        PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        JoH.wakeUpIntent(this, alarmIn, serviceIntent);

       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);*/
    }
    
    static public int readPerfsInt(SharedPreferences prefs, String name, int defaultValue) {
        try {
            return Integer.parseInt(prefs.getString(name, "" + defaultValue));
             
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    static public long getOtherAlertReraiseSec(Context context, String alertName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableAlertsReraise = prefs.getBoolean(alertName + "_enable_alerts_reraise" , false);
        if(enableAlertsReraise) {
            return readPerfsInt(prefs, alertName + "_reraise_sec", 60);
        } else {
            return 60 * getOtherAlertSnoozeMinutes(prefs, alertName);
        }

    }
    
    static public long getOtherAlertSnoozeMinutes(SharedPreferences prefs, String alertName) {
        int defaultSnooze = readPerfsInt(prefs, "other_alerts_snooze", 20);
        return readPerfsInt(prefs, alertName + "_snooze", defaultSnooze);
    }
    
}
