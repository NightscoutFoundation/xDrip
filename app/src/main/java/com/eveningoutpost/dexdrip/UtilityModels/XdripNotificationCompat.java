package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Notification;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Created by jamorham on 18/10/2017.
 */

public class XdripNotificationCompat extends NotificationCompat {

    public static Notification build(NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // get dynamic channel based on contents of the builder
            final String id = NotificationChannels.getChan(builder).getId();
            builder.setChannelId(id);
            return builder.build();
        } else {
            return builder.build(); // standard pre-oreo behaviour
        }
    }
}

