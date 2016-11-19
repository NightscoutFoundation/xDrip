package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import static com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity.checkForAnUpdate;

public class DailyIntentService extends IntentService {
    private final static String TAG = DailyIntentService.class.getSimpleName();
    // DAILY TASKS CAN GO IN HERE!

    public DailyIntentService() {
        super("DailyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("DailyIntentService", 120000);
        Log.i(TAG, "DailyIntentService onHandleIntent Starting");
        Long start = JoH.tsl();
        // prune old database records
        try {
            UserError.cleanup();
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on UserError ", e);
        }
        try {
            BgSendQueue.cleanQueue();
            
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on BgSendQueue ", e);
        }
        try {
            CalibrationSendQueue.cleanQueue();
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on CalibrationSendQueue ", e);
        }
        try {
            UploaderQueue.cleanQueue();
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on UploaderQueue ", e);
        }
        try {
            PebbleMovement.cleanup(Home.getPreferencesInt("retention_pebble_movement", 180));
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on PebbleMovement ", e);
        }
        try {
            checkForAnUpdate(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "DailyIntentService exception on checkForAnUpdate ", e);
        }
        JoH.releaseWakeLock(wl);
        Log.i(TAG, "DailyIntentService onHandleIntent exiting after " + ((JoH.tsl() - start) / 1000) + " seconds");
    }

}
