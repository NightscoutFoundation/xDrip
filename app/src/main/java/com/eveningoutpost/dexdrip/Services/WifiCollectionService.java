
package com.eveningoutpost.dexdrip.Services;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Mdns;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by tzachi dar on 10/14/15.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WifiCollectionService extends Service {
    private final static String TAG = WifiCollectionService.class.getSimpleName();
    private SharedPreferences prefs;
    //private BgToSpeech bgToSpeech;
    public WifiCollectionService dexCollectionService;

    private ForegroundServiceStarter foregroundServiceStarter;
    //private Context mContext;

    private static String lastState = "Not Running";

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        //mContext = getApplicationContext();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        // bgToSpeech = BgToSpeech.setupTTS(mContext); //keep reference to not being garbage collected
        Log.i(TAG, "onCreate: STARTING SERVICE");
        lastState = "Starting up " + JoH.hourMinuteString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("xdrip-wificolsvc-onStart", 60000);

      /*  if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            stopSelf();
            if (wl.isHeld()) wl.release();
            return START_NOT_STICKY;
        }*/

        if (DexCollectionType.hasWifi()) {
            runWixelReader();
            // For simplicity done here, would better happen once we know if we have a packet or not...
            setFailoverTimer();
        } else {
            lastState = "Stopping " + JoH.hourMinuteString();
            stopSelf();
            if (wl.isHeld()) wl.release();
            return START_NOT_STICKY;
        }
        lastState = "Started " + JoH.hourMinuteString();
        if (wl.isHeld()) wl.release();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy entered");
        foregroundServiceStarter.stop();
        //BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
        // ???? What will realy stop me, or am I already stopped???
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        } catch (Exception e) {
            Log.e(TAG, "Exception unregistering prefListener");
        }
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

    private static final String WIFI_COLLECTION_WAKEUP = "WifiCollectionWakeupTime";

    public void setFailoverTimer() {
        if (DexCollectionType.hasWifi()) {
            final long retry_in = WixelReader.timeForNextRead();
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            final Calendar calendar = Calendar.getInstance();
            PersistentStore.setLong(WIFI_COLLECTION_WAKEUP, calendar.getTimeInMillis() + retry_in);
            JoH.wakeUpIntent(this, retry_in, PendingIntent.getService(this, Constants.WIFI_COLLECTION_SERVICE_ID, new Intent(this, this.getClass()), 0));
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
        task.executeOnExecutor(xdrip.executor);
    }

    // For DexCollectionType probe
    public static boolean isRunning() {
        return lastState.equals("Not Running") || lastState.startsWith("Stopping", 0) ? false : true;
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus(Context context) {
        final List<StatusItem> l = new ArrayList<>();
        l.add(new StatusItem("IP Collector Service", lastState));
        l.add(new StatusItem("Next poll", JoH.niceTimeTill(PersistentStore.getLong(WIFI_COLLECTION_WAKEUP))));
        l.addAll(WixelReader.megaStatus());
        final int bridgeBattery = Pref.getInt("parakeet_battery", 0);
        if (bridgeBattery > 0) {
            l.add(new StatusItem("Parakeet Battery", bridgeBattery + "%", bridgeBattery < 50 ? bridgeBattery < 40 ? StatusItem.Highlight.BAD : StatusItem.Highlight.NOTICE : StatusItem.Highlight.GOOD));
        }
        l.addAll(Mdns.megaStatus(context));
        return l;
    }
}
