package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Notification;

import com.eveningoutpost.dexdrip.models.UserError;

/*
 * Created by jwoglom on 5/17/2018
 * <p>
 * Wrapper for android.app.Notification.Builder that adds the necessary notification
 * channel ID if enabled. Identical functionality-wise to XdripNotificationCompat.

 * This is now dedicated to the ongoing notification
 * No other notification should use this class.
 */

public class XdripNotification {

    private final static String TAG = XdripNotification.class.getSimpleName();

    public static Notification build(Notification.Builder builder) {
        String id;
        try {
            // This calls the simplified getChan in NotificationChannels.java
            id = NotificationChannels.getChan(builder).getId();
        } catch (Exception e) {
            id = NotificationChannels.ONGOING_CHANNEL;
        }
        builder.setChannelId(id);

        builder.setGroup(null);
        builder.setGroupSummary(false);
        builder.setCategory(Notification.CATEGORY_STATUS);
        builder.setWhen(0);
        builder.setShowWhen(false);
        builder.setOnlyAlertOnce(true);

        final Notification n = builder.build();
        UserError.Log.d(TAG, "Notif: chan=" + n.getChannelId() + " group=" + n.getGroup() + " summary=" + ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0));
        return n;
    }
}
