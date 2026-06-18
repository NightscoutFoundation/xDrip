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

