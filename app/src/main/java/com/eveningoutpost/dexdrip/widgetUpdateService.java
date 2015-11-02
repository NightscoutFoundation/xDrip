package com.eveningoutpost.dexdrip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import com.eveningoutpost.dexdrip.Models.UserError.Log;

import java.util.Calendar;

public class widgetUpdateService extends Service {
    public String TAG = "widgetUpdateService";
    BroadcastReceiver _broadcastReceiver;
    public widgetUpdateService() {}

    @Override
    public IBinder onBind(Intent intent) { throw new UnsupportedOperationException("Not yet implemented"); }

    @Override
    public void onCreate() {
        super.onCreate();
        setFailoverTimer();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo();
                }
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setFailoverTimer();
        updateCurrentBgInfo();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_broadcastReceiver != null) {
            unregisterReceiver(_broadcastReceiver);
        }
    }

    public void setFailoverTimer() { //Keep it alive!
        if (AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), xDripWidget.class)).length > 0) {
            long retry_in = (1000 * 60 * 5);
            Log.d(TAG, "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            long wakeTime = calendar.getTimeInMillis() + retry_in;
            PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        } else {
            stopSelf();
        }
    }

    public void updateCurrentBgInfo() {
        Log.d(TAG, "Sending update flag to widget");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), xDripWidget.class));
        Log.d(TAG, "Updating " + ids.length + " widgets");
        Intent intent = new Intent(this,xDripWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);
    }
}
