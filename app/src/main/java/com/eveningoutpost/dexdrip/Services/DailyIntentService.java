package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.DesertSync;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.RollCall;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.IncompatibleApps;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.UploaderQueue;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
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
        // TODO background thread
        final PowerManager.WakeLock wl = JoH.getWakeLock("DailyIntentService", 120000);
        try {
            if (JoH.pratelimit("daily-intent-service", 60000)) {
                Log.i(TAG, "DailyIntentService onHandleIntent Starting");
                Long start = JoH.tsl();

                // @TecMunky -- save database before pruning - allows daily capture of database
                if (Pref.getBooleanDefaultFalse("save_db_ondemand")) {
                    try {
                        String export = DatabaseUtil.saveSql(getBaseContext(), "daily");
                    } catch (Exception e) {
                        Log.e(TAG, "DailyIntentService exception on Daily Save Database - ", e);
                    }
                }

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
                    BgSendQueue.cleanQueue(); // no longer used

                } catch (Exception e) {
                    Log.d(TAG, "DailyIntentService exception on BgSendQueue "+ e);
                }
                try {
                    CalibrationSendQueue.cleanQueue();
                } catch (Exception e) {
                    Log.d(TAG, "DailyIntentService exception on CalibrationSendQueue "+ e);
                }
                try {
                    UploaderQueue.cleanQueue();
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on UploaderQueue ", e);
                }
                try {
                    StepCounter.cleanup(Pref.getInt("retention_pebble_movement", 180));
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on PebbleMovement ", e);
                }

                try {
                    final int bg_retention_days = Pref.getStringToInt("retention_days_bg_reading", 0);
                    if (bg_retention_days > 0) {
                        BgReading.cleanup(bg_retention_days);
                    }
                } catch (Exception e) {
                    Log.e(TAG,"DailyIntentService exception on BgReadings cleanup ",e);
                }

                try {
                    BluetoothGlucoseMeter.startIfNoRecentData();
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on BluetoothGlucoseMeter");
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
                    DesertSync.cleanup();
                } catch (Exception e) {
                    Log.e(TAG,"Exception cleaning up DesertSync");
                }
                try {
                    Telemetry.sendFirmwareReport();
                    Telemetry.sendCaptureReport();
                } catch (Exception e) {
                    Log.e(TAG, "Exception in Telemetry: " + e);
                }

                try {
                    IncompatibleApps.notifyAboutIncompatibleApps();
                } catch (Exception e) {
                    //
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
