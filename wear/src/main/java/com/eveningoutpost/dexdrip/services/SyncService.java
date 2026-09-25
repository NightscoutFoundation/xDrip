package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderTask;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SYNC_QUEUE_RETRY_ID;

public class SyncService extends IntentService {
    private static final String TAG = SyncService.class.getSimpleName();

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "STARTING INTENT SERVICE");
        attemptSend();
    }

    private void attemptSend() {
        if (Pref.getBooleanDefaultFalse("cloud_storage_api_enable")
                || Pref.getBooleanDefaultFalse("wear_sync")
                || Pref.getBooleanDefaultFalse("cloud_storage_mongodb_enable")
                || Pref.getBooleanDefaultFalse("cloud_storage_influxdb_enable")) {
            synctoCloudDatabases(); // attempt to sync queues
            startSyncService(6 * Constants.MINUTE_IN_MS); // set retry timer
        }
    }

    private void synctoCloudDatabases() {
        final UploaderTask task = new UploaderTask();
        task.executeOnExecutor(xdrip.executor);
    }

    public static void executeService(long delay) {
        Log.d(TAG, "static starting Sync service delay: " + delay);
        if (delay == 0) {
            xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), SyncService.class));
        } else {
            final PendingIntent serviceIntent = PendingIntent.getService(xdrip.getAppContext(), SYNC_QUEUE_RETRY_ID, new Intent(xdrip.getAppContext(), SyncService.class), PendingIntent.FLAG_CANCEL_CURRENT);
            JoH.wakeUpIntent(xdrip.getAppContext(), delay, serviceIntent);
        }
    }

    // First check if a network is available
    // If no network is a available then perform selfping workaround
    // to establish connectivity via an urgent message over google wear data api
    // If a network is available proceed with executing the service
    public static void startSyncService(long delay) {
        // If no network is connected then enforce cellular/wifi usage
        if (!JoH.isAnyNetworkConnected()) {
            final long timeout = JoH.tsl() + Constants.SECOND_IN_MS * 15;
            while (!JoH.isAnyNetworkConnected() && JoH.tsl() < timeout) {
                if (JoH.quietratelimit("cellularWifi-reconnect", 15)) {
                    JoH.forceCellularOrWifiUsage();
                }
                Log.d(TAG, "Sleeping for connect, remaining: " + JoH.niceTimeScalar(JoH.msTill(timeout)));
                JoH.threadSleep(1000);
            }
            // Perform ping-myself via datalayer to wakeup cellular connection via wearable api workaroud
            Log.wtf(TAG, "No network is available! Start Pingmyself workaround!");
            WatchUpdaterService.startServiceAndPingMyself();
            //if (!JoH.isAnyNetworkConnected()) {
            //    Log.wtf(TAG, "No network is available! - ORLY?!");
            //    return;
            //}
        } else {
            executeService(delay);
        }
    }
}
