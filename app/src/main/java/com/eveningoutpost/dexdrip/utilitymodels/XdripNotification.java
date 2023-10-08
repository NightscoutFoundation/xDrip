package com.eveningoutpost.dexdrip.utilitymodels;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;

/*
 * Created by jwoglom on 5/17/2018
 * <p>
 * Wrapper for android.app.Notification.Builder that adds the necessary notification
 * channel ID if enabled. Identical functionality-wise to XdripNotificationCompat.
 */

public class XdripNotification {

    @TargetApi(Build.VERSION_CODES.O)
    public static Notification build(Notification.Builder builder) {
        if ((Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)) {
            if (Pref.getBooleanDefaultFalse("use_notification_channels")) {
                // get dynamic channel based on contents of the builder
                try {
                    final String id = NotificationChannels.getChan(builder).getId();
                    builder.setChannelId(id);
                } catch (NullPointerException e) {
                    //noinspection ConstantConditions
                    builder.setChannelId(null);
                }
            } else {
                //noinspection ConstantConditions
                builder.setChannelId(null);
            }
            return builder.build();
        } else {
            return builder.build(); // standard pre-oreo behaviour
        }
    }
}
