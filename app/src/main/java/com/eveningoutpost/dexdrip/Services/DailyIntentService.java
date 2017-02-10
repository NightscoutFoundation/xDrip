package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.RollCall;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.utils.Telemetry;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity.checkForAnUpdate;

public class DailyIntentService extends IntentService {
    private final static String TAG = DailyIntentService.class.getSimpleName();
    //private SharedPreferences mPrefs;
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
                // prune old database records
                //mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                try {
                    startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_DB, TAG);
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on watch clear DB ", e);
                }
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
                try {
                    if (Home.get_master_or_follower()) RollCall.pruneOld(0);
                } catch (Exception e) {
                    Log.e(TAG, "exception on RollCall prune " + e);
                }
                try {
                    Telemetry.sendCaptureReport();
                } catch (Exception e) {
                    Log.e(TAG, "Exception in Telemetry: " + e);
                }
                Log.i(TAG, "DailyIntentService onHandleIntent exiting after " + ((JoH.tsl() - start) / 1000) + " seconds");
                //} else {
                // Log.e(TAG, "DailyIntentService exceeding rate limit");
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

}
