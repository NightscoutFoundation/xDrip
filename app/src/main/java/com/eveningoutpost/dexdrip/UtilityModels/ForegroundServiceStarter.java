package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import java.util.Date;

/**
 * Created by Emma Black on 12/25/14.
 */
public class ForegroundServiceStarter {
    private Service mService;
    private Context mContext;
    private boolean run_service_in_foreground = false;
    private Handler mHandler;


    public ForegroundServiceStarter(Context context, Service service) {
        mContext = context;
        mService = service;
        mHandler = new Handler(Looper.getMainLooper());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        run_service_in_foreground = prefs.getBoolean("run_service_in_foreground", false);
    }

    public void start() {
        if (run_service_in_foreground) {
            Log.d("FOREGROUND", "should be moving to foreground");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    long end = System.currentTimeMillis() + (60000 * 5);
                    long start = end - (60000 * 60*3) -  (60000 * 10);
                    mService.startForeground(new Notifications().ongoingNotificationId, new Notifications().createOngoingNotification(new BgGraphBuilder(mContext, start, end), mContext));
                }
            });
        }
    }

    public void stop() {
        if (run_service_in_foreground) {
            Log.d("FOREGROUND", "should be moving out of foreground");
            mService.stopForeground(true);
        }
    }

}
