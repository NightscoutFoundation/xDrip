package com.eveningoutpost.dexdrip.utilitymodels;

import android.annotation.TargetApi;
import android.app.Notification;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Created by jamorham on 18/10/2017.
 */

public class XdripNotificationCompat extends NotificationCompat {

    @TargetApi(Build.VERSION_CODES.O)
    public static Notification build(NotificationCompat.Builder builder) {
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

