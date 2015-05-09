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
import android.util.Log;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.ReadDataShare;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;

import java.util.Calendar;

public class MissedReadingService extends IntentService {
    SharedPreferences prefs;
    boolean bg_missed_alerts;
    int bg_missed_minutes;
    int otherAlertSnooze;
    Context mContext;

    public MissedReadingService() {
        super("MissedReadingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        bg_missed_alerts =  prefs.getBoolean("bg_missed_alerts", false);
        bg_missed_minutes =  Integer.parseInt(prefs.getString("bg_missed_minutes", "30"));
        otherAlertSnooze =  Integer.parseInt(prefs.getString("other_alerts_snooze", "20"));

        if (bg_missed_alerts && BgReading.getTimeSinceLastReading() > (bg_missed_minutes * 1000 * 60)) {
            Notifications.bgMissedAlert(mContext);
            checkBackAfterSnoozeTime();
        } else {
            checkBackAfterMissedTime();
        }
    }

   public void checkBackAfterSnoozeTime() {
       setAlarm(otherAlertSnooze * 1000 * 60);
   }

    public void checkBackAfterMissedTime() {
        setAlarm(bg_missed_minutes * 1000 * 60);
    }

    public void setAlarm(long alarmIn) {
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            alarm.setExact(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + alarmIn, PendingIntent.getService(this, 0, new Intent(this, MissedReadingService.class), 0));
        } else {
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + alarmIn, PendingIntent.getService(this, 0, new Intent(this, MissedReadingService.class), 0));
        }
    }
}
