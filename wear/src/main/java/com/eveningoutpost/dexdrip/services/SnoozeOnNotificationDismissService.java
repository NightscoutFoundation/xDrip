package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;

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
        final String alertType = (intent != null) ? intent.getStringExtra("alertType") : "null intent"; // Replace by constant
        Log.e(TAG, "SnoozeOnNotificationDismissService called source = " + alertType);
        if(alertType.equals("bg_alerts")  ) {
            snoozeBgAlert();
            return;
        }
        if(alertType.equals("bg_unclear_readings_alert") || 
           alertType.equals("bg_missed_alerts")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean enableAlertsReraise = prefs.getBoolean(alertType + "_enable_alerts_reraise", false);
            if(enableAlertsReraise) {
                // Only snooze these alert if it the reraise function is enabled. 
                snoozeOtherAlert(alertType);
            }
            return;
        }
        
        if(alertType.equals("bg_predict_alert") ||
                alertType.equals("persistent_high_alert")) {
            Log.wtf(TAG, "SnoozeOnNotificationDismissService called for unsupported type!!! source = " + alertType);
            
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
    
    private void snoozeOtherAlert(String alertType) {//KS TODO not implemented
        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long snoozeMinutes = MissedReadingService.getOtherAlertSnoozeMinutes(prefs, alertType);
        Log.i(TAG, "snoozeOtherAlert calling snooze alert alert = " + alertType + " snoozeMinutes = " + snoozeMinutes);
        UserNotification.snoozeAlert(alertType, snoozeMinutes);*/
    }
}
