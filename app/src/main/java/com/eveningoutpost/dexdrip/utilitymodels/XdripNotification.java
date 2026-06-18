package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Notification;

import com.eveningoutpost.dexdrip.models.UserError;

/*
 * Created by jwoglom on 5/17/2018
 * <p>
 * Wrapper for android.app.Notification.Builder that adds the necessary notification
 * channel ID if enabled. Identical functionality-wise to XdripNotificationCompat.
 */

public class XdripNotification {

    private final static String TAG = XdripNotification.class.getSimpleName();

    public static Notification build(Notification.Builder builder) {
        try {
            builder.setChannelId(NotificationChannels.getChan(builder).getId());
        } catch (Exception e) {
            builder.setChannelId(NotificationChannels.ONGOING_CHANNEL);
        }
        // No setGroup call here.
        UserError.Log.d(TAG, "Channel: " + builder.build().getChannelId() + " | Group: " + builder.build().getGroup());
        return builder.build();
    }
}
