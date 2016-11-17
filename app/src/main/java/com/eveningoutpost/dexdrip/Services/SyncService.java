package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.MongoSendTask;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Calendar;

public class SyncService extends IntentService {
    private Context mContext;
    private Boolean enableRESTUpload;
    private Boolean enableMongoUpload;
    private SharedPreferences prefs;

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("SYNC SERVICE:", "STARTING INTENT SERVICE");
        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        enableRESTUpload = prefs.getBoolean("cloud_storage_api_enable", false);
        enableMongoUpload = prefs.getBoolean("cloud_storage_mongodb_enable", false);
        attemptSend();
    }

    public void attemptSend() {
        if (enableRESTUpload || enableMongoUpload) { syncToMongoDb(); }
        setRetryTimer();
    }

    public void setRetryTimer() {
        if (enableRESTUpload || enableMongoUpload) { //Check for any upload type being enabled
            final PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, SyncService.class), PendingIntent.FLAG_CANCEL_CURRENT);
            JoH.wakeUpIntent(this,(1000 * 60 * 6),serviceIntent); // TODO use static method below instead
        }
    }

    private void syncToMongoDb() {
        // TODO does this need locking?
        MongoSendTask task = new MongoSendTask(getApplicationContext());
        task.executeOnExecutor(xdrip.executor);
    }

    public static void startSyncService(long delay) {
        Log.d("SyncService", "static starting Sync service delay: " + delay);
        if (delay == 0) {
            xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), SyncService.class));
        } else {
            final PendingIntent serviceIntent = PendingIntent.getService(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), SyncService.class), PendingIntent.FLAG_CANCEL_CURRENT);
            JoH.wakeUpIntent(xdrip.getAppContext(), delay, serviceIntent);
        }
    }
}
