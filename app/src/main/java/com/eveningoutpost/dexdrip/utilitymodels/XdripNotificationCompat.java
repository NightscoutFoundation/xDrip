package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Notification;
import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Created by jamorham on 18/10/2017.
 */

public class XdripNotificationCompat extends NotificationCompat {

    private final static String TAG = XdripNotificationCompat.class.getSimpleName();

    public static Notification build(NotificationCompat.Builder builder) {
        String id;
        try {
            id = NotificationChannels.getChan(builder).getId();
        } catch (Exception e) {
            // Guesser failed. Fall back to the general channel and make sure it exists,
            // otherwise the notification is silently dropped on API 26+.
            id = NotificationChannels.GENERAL_CHANNEL;
            NotificationChannels.setupGeneralChannel();
            UserError.Log.e(TAG, "getChan() failed, fell back to GENERAL_CHANNEL: " + e);
        }
        builder.setChannelId(id);

        // Ensure alerts are independent and not summaries
        builder.setGroup(null);
        builder.setGroupSummary(false);

        builder.setCategory(NotificationCompat.CATEGORY_ALARM);

        final Notification n = builder.build();

        UserError.Log.d(TAG, "NotifCompat: chan=" + id +
                " group=" + NotificationCompat.getGroup(n) +
                " summary=" + ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0));

        return n;
    }
}
