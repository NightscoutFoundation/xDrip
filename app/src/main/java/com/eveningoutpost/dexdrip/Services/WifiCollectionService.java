
package com.eveningoutpost.dexdrip.Services;

import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utils.BgToSpeech;

/**
 * Created by tzachi dar on 10/14/15.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WifiCollectionService extends Service {
    private final static String TAG = WifiCollectionService.class.getSimpleName();
    private SharedPreferences prefs;
    private BgToSpeech bgToSpeech;
    public WifiCollectionService dexCollectionService;

    private ForegroundServiceStarter foregroundServiceStarter;
    private Context mContext;

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
        bgToSpeech = BgToSpeech.setupTTS(mContext); //keep reference to not being garbage collected
        Log.i(TAG, "onCreate: STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1){
            stopSelf();
            return START_NOT_STICKY;
        }
        if (CollectionServiceStarter.isWifiWixel(getApplicationContext()) ) {
            runWixelReader();
            // For simplicity done here, would better happen once we know if we have a packet or not...
            setFailoverTimer();
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy entered");
        foregroundServiceStarter.stop();
        BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
        // ???? What will realy stop me, or am I already stopped???
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.compareTo("run_service_in_foreground") == 0) {
                Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                    foregroundServiceStarter.start();
                    Log.d(TAG, "Moving to foreground");
                } else {
                    dexCollectionService.stopForeground(true);
                    Log.d(TAG, "Removing from foreground");
                }
            }
        }
    };


    public void setFailoverTimer() {
        if (CollectionServiceStarter.isWifiWixel(getApplicationContext())) {
            long retry_in = WixelReader.timeForNextRead();
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                alarm.setExact(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, WifiCollectionService.class), 0));
            } else {
                alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, WifiCollectionService.class), 0));
            }
        } else {
            stopSelf();
        }
    }

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }
    
    private void runWixelReader() {
        // Theoretically can create more than one task. Should not be a problem since android runs them
        // on the same thread.
        WixelReader task = new WixelReader(getApplicationContext());
        // Assume here that task will execute, otheirwise we leak a wake lock...
        task.execute();
    }
}
