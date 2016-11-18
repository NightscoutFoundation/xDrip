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

import static com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity.checkForAnUpdate;

public class DailyIntentService extends IntentService {
    // DAILY TASKS CAN GO IN HERE!

    public DailyIntentService() {
        super("DailyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("DailyIntentService", 120000);

        // prune old database records
        try {
            UserError.cleanup();
        } catch (Exception e) {
            //
        }
        try {
            BgSendQueue.cleanQueue();
        } catch (Exception e) {
            //
        }
        try {
            CalibrationSendQueue.cleanQueue();
        } catch (Exception e) {
            //
        }
        try {
            UploaderQueue.cleanQueue();
        } catch (Exception e) {
            //
        }
        try {
            PebbleMovement.cleanup(Home.getPreferencesInt("retention_pebble_movement", 180));
        } catch (Exception e) {
            //
        }
        try {
            checkForAnUpdate(getApplicationContext());
        } catch (Exception e) {
            //
        }
        JoH.releaseWakeLock(wl);
    }

}
