package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.UserNotification;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SnoozeOnNotificationDismissService extends IntentService {
    private final static String TAG = AlertPlayer.class.getSimpleName();

    public SnoozeOnNotificationDismissService() {
        super("SnoozeOnNotificationDismissService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String alertType = intent.getStringExtra("alertType"); // Replace by constant
        Log.e(TAG, "SnoozeOnNotificationDismissService called source = " + alertType);
        if(alertType.equals("bg_alerts")  ) {
            snoozeBgAlert();
            return;
        }
        if(alertType.equals("bg_unclear_readings_alert") || 
           alertType.equals("bg_missed_alerts") ||
           alertType.equals("bg_predict_alert") ||
           alertType.equals("persistent_high_alert")) {
            snoozeOtherAlert(alertType);
            return;
        }
        Log.e(TAG, "SnoozeOnNotificationDismissService called for unknown source = " + alertType);
    }
    
    private void snoozeBgAlert() {
        AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();

        int snooze = 30;
        if(activeBgAlert != null) {
            if(activeBgAlert.default_snooze != 0) {
                snooze = activeBgAlert.default_snooze;
            } else {
                snooze = SnoozeActivity.getDefaultSnooze(activeBgAlert.above);
            }
        }

        AlertPlayer.getPlayer().Snooze(getApplicationContext(), snooze);
    }
    
    private void snoozeOtherAlert(String alertType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int snoozeMinutes = MissedReadingService.readPerfsInt(prefs, "other_alerts_snooze", 20);
        UserNotification.snoozeAlert(alertType, snoozeMinutes);
    }
}
