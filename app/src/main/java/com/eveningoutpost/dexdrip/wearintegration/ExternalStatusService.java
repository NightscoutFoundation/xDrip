package com.eveningoutpost.dexdrip.wearintegration;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

/**
 * Created by adrian on 14/02/16.
 */
public class ExternalStatusService extends IntentService{
    //constants
    public static final String EXTRA_STATUSLINE = "com.eveningoutpost.dexdrip.Extras.Statusline";
    public static final String ACTION_NEW_EXTERNAL_STATUSLINE = "com.eveningoutpost.dexdrip.ExternalStatusline";
    public static final String RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_EXTERNAL_STATUSLINE";
    public static final int MAX_LEN = 40;
    private final static String TAG = ExternalStatusService.class.getSimpleName();

    public ExternalStatusService() {
        super("ExternalStatusService");
        setIntentRedelivery(true);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null)
            return;

        try {
            final String action = intent.getAction();
            if (action == null) return;

            if (ACTION_NEW_EXTERNAL_STATUSLINE.equals(action)) {
                String statusline = intent.getStringExtra(EXTRA_STATUSLINE);
                if (statusline != null) {

                    if (statusline.length() > MAX_LEN) {
                        statusline = statusline.substring(0, MAX_LEN);
                    }

                    // send to wear
                    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("wear_sync", false)) {
                        startWatchUpdaterService(this, WatchUpdaterService.ACTION_SEND_STATUS, TAG, "externalStatusString", "" + statusline);
                        /*By integrating the watch part of Nightwatch we inherited the same wakelock
                         problems NW had - so adding the same quick fix for now.
                         TODO: properly "wakelock" the wear (and probably pebble) services
                        */
                        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "externalStatusService").acquire(15000);
                    }
                }
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}