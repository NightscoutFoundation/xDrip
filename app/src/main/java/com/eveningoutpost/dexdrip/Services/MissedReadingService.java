package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.ReadDataShare;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;

import java.util.Calendar;
import java.util.Date;

public class MissedReadingService extends IntentService {
    int otherAlertSnooze;
    private final static String TAG = AlertPlayer.class.getSimpleName();

    public MissedReadingService() {
        super("MissedReadingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs;
        boolean bg_missed_alerts;
        Context context;
        int bg_missed_minutes;
        
        
        context = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        bg_missed_alerts =  prefs.getBoolean("bg_missed_alerts", false);
        bg_missed_minutes =  Integer.parseInt(prefs.getString("bg_missed_minutes", "30"));
        otherAlertSnooze =  Integer.parseInt(prefs.getString("other_alerts_snooze", "20"));

        long now = new Date().getTime();

        if (bg_missed_alerts && 
                BgReading.getTimeSinceLastReading() >= (bg_missed_minutes * 1000 * 60) &&
                prefs.getLong("alerts_disabled_until", 0) <= now) {
            Notifications.bgMissedAlert(context);
            checkBackAfterSnoozeTime();
        } else  {
            long alarmIn = prefs.getLong("alerts_disabled_until", 0) - now;
            if (alarmIn <= 0) {
                alarmIn = Long.parseLong(prefs.getString("bg_missed_minutes", "30"))* 1000 * 60 - BgReading.getTimeSinceLastReading();
            }
            Log.d(TAG, "Setting timer to  " + alarmIn / 60000 + " minutes from now" );
            checkBackAfterMissedTime(alarmIn);
        }
    }

    public void checkBackAfterSnoozeTime() {
        setAlarm(otherAlertSnooze * 1000 * 60);
    }

    public void checkBackAfterMissedTime(long alarmIn) {
        setAlarm(alarmIn);
    }

    public void setAlarm(long alarmIn) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        long wakeTime = calendar.getTimeInMillis() + alarmIn;
        PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
    }
}
