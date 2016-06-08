package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.GcmListenerSvc;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.google.android.gms.gcm.GcmReceiver;

import java.util.Calendar;

public class DoNothingService extends Service {
    private final static String TAG = DoNothingService.class.getSimpleName();
    public DoNothingService dexCollectionService;
    private SharedPreferences prefs;
    private ForegroundServiceStarter foregroundServiceStarter;
    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.compareTo("run_service_in_foreground") == 0) {
                UserError.Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                    foregroundServiceStarter.start();
                    UserError.Log.d(TAG, "Moving to foreground");
                } else {
                    dexCollectionService.stopForeground(true);
                    UserError.Log.d(TAG, "Removing from foreground");
                }
            }
        }
    };
    private Context mContext;


    public DoNothingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        mContext = getApplicationContext();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        UserError.Log.i(TAG, "onCreate: STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("donothing-follower", 60000);
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }
        if (CollectionServiceStarter.isFollower(getApplicationContext())) {
            int minsago = GcmListenerSvc.lastMessageMinutesAgo();
            Log.d(TAG, "Tick: minutes ago: " + minsago);
            if (minsago > 6) {
                GcmActivity.requestPing();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            setFailOverTimer();
        } else {
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }
        JoH.releaseWakeLock(wl);
        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UserError.Log.d(TAG, "onDestroy entered");
        foregroundServiceStarter.stop();
        UserError.Log.i(TAG, "SERVICE STOPPED");
    }

    private void setFailOverTimer() {
            if (Home.get_follower()) {
                final long retry_in = (5*60*1000);
                UserError.Log.d(TAG, "setFailoverTimer: Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
                final Calendar calendar = Calendar.getInstance();
                final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DoNothingService.class), 0));
                } else if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DoNothingService.class), 0));
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DoNothingService.class), 0));
                }
            } else {
                stopSelf();
            }
    }

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

}