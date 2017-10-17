package com.eveningoutpost.dexdrip.UtilityModels;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwoglom on 10/15/2017.
 *
 * Contains setup for creation of notification channels, and constants for
 * channelId values used when creating notifications.
 */

public class NotificationChannels extends ContextWrapper {
    public static final String TAG = NotificationChannels.class.getSimpleName();
    private NotificationManager notifManager;

    public static final String LOW_BRIDGE_BATTERY_CHANNEL = "lowBridgeBattery";
    public static final String LOW_TRANSMITTER_BATTERY_CHANNEL = "lowTransmitterBattery";
    public static final String NIGHTSCOUT_UPLOADER_CHANNEL = "nightscoutUploaderChannel";
    public static final String PARAKEET_STATUS_CHANNEL = "parakeetStatusChannel";
    public static final String REMINDER_CHANNEL = "reminderChannel";
    public static final String BG_ALERT_CHANNEL = "bgAlertChannel";
    public static final String BG_MISSED_ALERT_CHANNEL = "bgMissedAlertChannel";
    public static final String BG_RISE_DROP_CHANNEL = "bgRiseDropChannel";
    public static final String BG_PREDICTED_LOW_CHANNEL = "bgPredictedLowChannel";
    public static final String BG_PERSISTENT_HIGH_CHANNEL = "bgPersistentHighChannel";
    public static final String CALIBRATION_CHANNEL = "calibrationChannel";
    public static final String ONGOING_CHANNEL = "ongoingChannel";


    @TargetApi(Build.VERSION_CODES.O)
    public NotificationChannels(Context ctx) {
        super(ctx);

        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Log.d(TAG, "Notification channels not available on this sdk version. ("+Build.VERSION.SDK_INT+")");
            return;
        }

        List<NotificationChannel> notifChannels = new ArrayList<>();
        notifChannels.add(new NotificationChannel(
                LOW_BRIDGE_BATTERY_CHANNEL,
                "Low bridge battery",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                LOW_TRANSMITTER_BATTERY_CHANNEL,
                "Low transmitter battery",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                NIGHTSCOUT_UPLOADER_CHANNEL,
                "Nightscout Uploader",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                PARAKEET_STATUS_CHANNEL,
                "Parakeet Status",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                REMINDER_CHANNEL,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                BG_ALERT_CHANNEL,
                "Blood Glucose Alert",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                BG_MISSED_ALERT_CHANNEL,
                "BG Missed Readings",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                BG_RISE_DROP_CHANNEL,
                "BG Rise/Drop",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                BG_PREDICTED_LOW_CHANNEL,
                "BG Predicted Low",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                BG_PERSISTENT_HIGH_CHANNEL,
                "BG Persistent High",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                CALIBRATION_CHANNEL,
                "Calibration",
                NotificationManager.IMPORTANCE_DEFAULT));
        notifChannels.add(new NotificationChannel(
                ONGOING_CHANNEL,
                "Ongoing",
                NotificationManager.IMPORTANCE_DEFAULT));

        getNotifManager().createNotificationChannels(notifChannels);
        Log.d(TAG, "Notification channels created.");
    }

    private NotificationManager getNotifManager() {
        if (notifManager == null) {
            notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notifManager;
    }
}
