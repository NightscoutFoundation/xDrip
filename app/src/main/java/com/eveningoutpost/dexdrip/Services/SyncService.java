package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.MongoSendTask;
import com.eveningoutpost.dexdrip.UtilityModels.SensorSendQueue;

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
        if (enableRESTUpload || enableMongoUpload) { syncToMogoDb(); }
        setRetryTimer();
    }

    public void setRetryTimer() {
        if (enableRESTUpload || enableMongoUpload) { //Check for any upload type being enabled
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            long wakeTime = calendar.getTimeInMillis() + (1000 * 60 * 6);
            // make it up on the next BG reading or retry if the reading doesn't materialize
            PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, SyncService.class), PendingIntent.FLAG_CANCEL_CURRENT);
            alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        }
    }

    public void syncToMogoDb() {
        MongoSendTask task = new MongoSendTask(getApplicationContext());
        task.execute();
    }
}
