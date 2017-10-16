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
