package com.eveningoutpost.dexdrip.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SnoozeOnNotificationDismissService extends IntentService {
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private final static long MINIMUM_CANCEL_DELAY = 2 * Constants.SECOND_IN_MS;

    public SnoozeOnNotificationDismissService() {
        super("SnoozeOnNotificationDismissService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String alertType = (intent != null) ? intent.getStringExtra("alertType") : "null intent"; // Replace by constant
        if (alertType == null) alertType = "null";
        final long time_showing = (intent != null) ? JoH.msSince(intent.getLongExtra("raisedTimeStamp", JoH.tsl() - 10 * Constants.MINUTE_IN_MS)) : 10 * Constants.MINUTE_IN_MS;
        if (time_showing <= MINIMUM_CANCEL_DELAY) {
            UserError.Log.wtf(TAG, "Attempt to cancel alert (" + alertType + ") within minimum limit of: " + JoH.niceTimeScalar(MINIMUM_CANCEL_DELAY));
            Home.startHomeWithExtra(xdrip.getAppContext(),"confirmsnooze","simpleconfirm");
        }
        Log.e(TAG, "SnoozeOnNotificationDismissService called source = " + alertType + " shown for: " + JoH.niceTimeScalar(time_showing));
        if (alertType.equals("bg_alerts") && (time_showing > MINIMUM_CANCEL_DELAY)) {
            snoozeBgAlert();
            return;
        }
        if (alertType.equals("bg_unclear_readings_alert") ||
                alertType.equals("bg_missed_alerts")) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean enableAlertsReraise = prefs.getBoolean(alertType + "_enable_alerts_reraise", false);
            if (enableAlertsReraise && (time_showing > MINIMUM_CANCEL_DELAY)) {
                // Only snooze these alert if it the reraise function is enabled. 
                snoozeOtherAlert(alertType);
            }
            return;
        }

        if (alertType.equals("bg_predict_alert") ||
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
    
    private void snoozeOtherAlert(String alertType) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long snoozeMinutes = MissedReadingService.getOtherAlertSnoozeMinutes(prefs, alertType);
        Log.i(TAG, "snoozeOtherAlert calling snooze alert alert = " + alertType + " snoozeMinutes = " + snoozeMinutes);
        UserNotification.snoozeAlert(alertType, snoozeMinutes);

        if (Pref.getBooleanDefaultFalse("pref_amazfit_enable_key")
                && Pref.getBooleanDefaultFalse("pref_amazfit_BG_alert_enable_key")) {
            Amazfitservice.start("xDrip_AlarmCancel");
        }
    }
}
