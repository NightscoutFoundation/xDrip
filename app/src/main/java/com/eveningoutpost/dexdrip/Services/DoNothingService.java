package com.eveningoutpost.dexdrip.Services;

import android.annotation.TargetApi;
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
    private static long nextWakeUpTime = -1;
    private static int wakeUpErrors = 0;
    private static boolean wakeUpFailsafe = false;


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

        if (nextWakeUpTime > 0) {
            final long wake_time_difference = Calendar.getInstance().getTimeInMillis() - nextWakeUpTime;
            if (wake_time_difference > 10000) {
                UserError.Log.e(TAG, "Slow Wake up! time difference in ms: " + wake_time_difference);
                wakeUpErrors = wakeUpErrors + 3;
                if (wakeUpErrors > 7) {
                    wakeUpFailsafe = true;
                    UserError.Log.e(TAG, "Wake up FailSafe engaged!!!");
                }

            } else {
                if (wakeUpErrors > 0) wakeUpErrors--;
            }
        }

        if (CollectionServiceStarter.isFollower(getApplicationContext())) {
            new Thread(new Runnable() {
                public void run() {
                    int minsago = GcmListenerSvc.lastMessageMinutesAgo();
                    //Log.d(TAG, "Tick: minutes ago: " + minsago);
                    int sleep_time = 1000;

                    if ((minsago > 60) && (minsago < 70)) {
                        if (JoH.ratelimit("slow-service-restart", 60)) {
                            UserError.Log.e(TAG, "Restarting collection service + full wakeup due to minsago: " + minsago + " !!!");
                            Home.startHomeWithExtra(getApplicationContext(), Home.HOME_FULL_WAKEUP, "1");
                            CollectionServiceStarter.restartCollectionService(getApplicationContext());
                        }
                    }

                    if (minsago > 6) {
                        GcmActivity.requestPing();
                        sleep_time = (minsago < 60) ? ((minsago / 6) * 1000) : 1000; // increase sleep time up to 10s for first hour or revert
                    }

                    try {
                        Thread.sleep(sleep_time);
                    } catch (
                            InterruptedException e) {
                    }

                    setFailOverTimer();
                    JoH.releaseWakeLock(wl);
                }
            }).start();
        } else {
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }
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
            final long retry_in = (5 * 60 * 1000);
            UserError.Log.d(TAG, "setFailoverTimer: Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            final Calendar calendar = Calendar.getInstance();
            final AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            final long wakeUpTime = calendar.getTimeInMillis() + retry_in;
            nextWakeUpTime = wakeUpTime;

            final PendingIntent wakeIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeUpTime, wakeIntent);
            } else {
                if ((wakeUpFailsafe) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                    alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(wakeUpTime, wakeIntent), wakeIntent);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarm.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, wakeIntent);
                    } else {
                        alarm.set(AlarmManager.RTC_WAKEUP, wakeUpTime, wakeIntent);
                    }
                }
            }
        } else {
            stopSelf();
        }
    }

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

}