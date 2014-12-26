package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.eveningoutpost.dexdrip.R;

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
        NotificationCompat.Builder b=new NotificationCompat.Builder(mService);
        b.setOngoing(true);
        b.setContentTitle("DexDrip is Running")
                .setContentText("DexDrip Data collection service is running.")
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on);
        return(b.build());
    }

    public void start() {
        if (run_service_in_foreground) { mService.startForeground(FOREGROUND_ID, notification()); }
    }

    public void stop() {
        if (run_service_in_foreground) { mService.stopForeground(true); }
    }
}
