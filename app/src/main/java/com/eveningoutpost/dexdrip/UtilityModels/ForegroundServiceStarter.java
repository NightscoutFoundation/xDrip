package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.DexShareCollectionService;

/**
 * Created by stephenblack on 12/25/14.
 */
public class ForegroundServiceStarter {
    private Service mService;
    private Context mContext;
    private boolean run_service_in_foreground = false;
    private int FOREGROUND_ID = 8811;

    public ForegroundServiceStarter(Context context, Service service) {
        mContext = context;
        mService = service;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        run_service_in_foreground = prefs.getBoolean("run_service_in_foreground", false);
    }

    private Notification notification() {
        Intent intent = new Intent(mContext, Home.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(Home.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder b=new NotificationCompat.Builder(mService);
        b.setOngoing(true);
        b.setCategory(Notification.CATEGORY_SERVICE);
        // Hide this notification "below the fold" on L+
        b.setPriority(Notification.PRIORITY_MIN);
        // Don't show this notification on the lock screen on L+
        b.setVisibility(Notification.VISIBILITY_SECRET);
        b.setContentTitle("xDrip is Running")
                .setContentText("xDrip Data collection service is running.")
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on);
        b.setContentIntent(resultPendingIntent);
        return(b.build());
    }

    public void start() {
        if (run_service_in_foreground) {
            Log.e("FOREGROUND", "should be moving to foreground");
            mService.startForeground(FOREGROUND_ID, notification());
        }
    }

    public void stop() {
        if (run_service_in_foreground) {
            Log.e("FOREGROUND", "should be moving out of foreground");
            mService.stopForeground(true);
        }
    }
}
