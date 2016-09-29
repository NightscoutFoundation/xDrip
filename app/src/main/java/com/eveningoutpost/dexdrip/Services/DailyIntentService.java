package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import static com.eveningoutpost.dexdrip.UtilityModels.UpdateActivity.checkForAnUpdate;

public class DailyIntentService extends IntentService {
    // DAILY TASKS CAN GO IN HERE!

    public DailyIntentService() {
        super("DailyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("DailyIntentService", 120000);
        try {
            UserError.cleanup();
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
