package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

public class DailyIntentService extends IntentService {
    private final static String TAG = DailyIntentService.class.getSimpleName();
    private final String ACTION_RESET_COUNTERS = "RESETCOUNTERS";
    // DAILY TASKS CAN GO IN HERE!

    public DailyIntentService() {
        super("DailyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("DailyIntentService", 120000);
        try {
            if (JoH.pratelimit("daily-intent-service", 60000)) {
                Log.i(TAG, "DailyIntentService onHandleIntent Starting");
                Long start = JoH.tsl();
                Context context = getApplicationContext();
                try {
                    Log.d(TAG, "start SensorService with " + ACTION_RESET_COUNTERS);
                    context.startService(new Intent(context, SensorService.class).setAction(ACTION_RESET_COUNTERS));
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on watch clear DB ", e);
                }
                try {
                    PebbleMovement.cleanup(Home.getPreferencesInt("retention_pebble_movement", 7));//180
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on PebbleMovement ", e);
                }
                Log.i(TAG, "DailyIntentService onHandleIntent exiting after " + ((JoH.tsl() - start) / 1000) + " seconds");
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

}
