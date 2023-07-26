package com.eveningoutpost.dexdrip.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Mdns;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by tzachi dar on 10/14/15.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class WifiCollectionService extends Service {
    private final static String TAG = WifiCollectionService.class.getSimpleName();
    private static final long TOLERABLE_JITTER = 10000;
    private static final String WIFI_COLLECTION_WAKEUP = "WifiCollectionWakeupTime";
    private static String lastState = "Not Running";
    private static long requested_wake_time = 0;
    private static long max_wakeup_jitter = 0;
    public WifiCollectionService dexCollectionService;
    private SharedPreferences prefs;
    private ForegroundServiceStarter foregroundServiceStarter;

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

    // For DexCollectionType probe
    public static boolean isRunning() {
        return !(lastState.equals("Not Running") || lastState.startsWith("Stopping", 0));
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus(Context context) {
        final List<StatusItem> l = new ArrayList<>();
        l.add(new StatusItem("IP Collector Service", lastState));
        l.add(new StatusItem("Next poll", JoH.niceTimeTill(PersistentStore.getLong(WIFI_COLLECTION_WAKEUP))));
        if (max_wakeup_jitter > 2000) {
            l.add(new StatusItem("Wakeup jitter", JoH.niceTimeScalar(max_wakeup_jitter), max_wakeup_jitter > TOLERABLE_JITTER ? StatusItem.Highlight.BAD : StatusItem.Highlight.NORMAL));
        }
        if (JoH.buggy_samsung) {
            l.add(new StatusItem("Buggy handset", "Using workaround", max_wakeup_jitter < TOLERABLE_JITTER ? StatusItem.Highlight.GOOD : StatusItem.Highlight.BAD));
        }
        if(DexCollectionType.hasLibre()) {
            l.addAll(LibreWifiReader.megaStatus());
        } else {
            l.addAll(WixelReader.megaStatus());
        }
        
        final int bridgeBattery = Pref.getInt("parakeet_battery", 0);
        if (bridgeBattery > 0) {
            l.add(new StatusItem("Parakeet Battery", bridgeBattery + "%", bridgeBattery < 50 ? bridgeBattery < 40 ? StatusItem.Highlight.BAD : StatusItem.Highlight.NOTICE : StatusItem.Highlight.GOOD));
        }
        l.addAll(Mdns.megaStatus(context));
        return l;
    }

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

        if (requested_wake_time > 0) {
            JoH.persistentBuggySamsungCheck();
            final long wakeup_jitter = JoH.msSince(requested_wake_time);
            if (wakeup_jitter > 2000) {
                Log.d(TAG, "Wake up jitter: " + JoH.niceTimeScalar(wakeup_jitter));
            }
            if ((wakeup_jitter > TOLERABLE_JITTER) && (!JoH.buggy_samsung) && (JoH.isSamsung())) {
                UserError.Log.wtf(TAG, "Enabled wake workaround due to jitter of: " + JoH.niceTimeScalar(wakeup_jitter));
                JoH.setBuggySamsungEnabled();
                max_wakeup_jitter = 0;
            } else {
                max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
            }
        }

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

    public void setFailoverTimer() {
        if (DexCollectionType.hasWifi()) {
            long retry_in;
            if(DexCollectionType.hasLibre()) {
                retry_in = LibreWifiReader.timeForNextRead();
            } else {
                retry_in = WixelReader.timeForNextRead();
            }
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            //requested_wake_time = JoH.wakeUpIntent(this, retry_in, PendingIntent.getService(this, Constants.WIFI_COLLECTION_SERVICE_ID, new Intent(this, this.getClass()), 0));
            requested_wake_time = JoH.wakeUpIntent(this, retry_in, WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.WIFI_COLLECTION_SERVICE_ID));
            PersistentStore.setLong(WIFI_COLLECTION_WAKEUP, requested_wake_time);
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
        AsyncTask<String, Void, Void> task;
        if(DexCollectionType.hasLibre()) {
            task = new LibreWifiReader(getApplicationContext());
        } else {
            task = new WixelReader(getApplicationContext());
        }
        task.executeOnExecutor(xdrip.executor);
    }

    // data for NanoStatus
    public static SpannableString nanoStatus() {
        return null;
    }
}
