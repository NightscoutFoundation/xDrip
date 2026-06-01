package com.eveningoutpost.dexdrip.utilitymodels;

import android.app.Service;
import android.content.Context;
import android.os.Build;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
import static com.eveningoutpost.dexdrip.utilitymodels.Notifications.ongoingNotificationId;

/**
 * Created by Emma Black on 12/25/14.
 */
public class ForegroundServiceStarter {

    private static final String TAG = "FOREGROUND";

    final private Service mService;
    final private Context mContext;
    //final private Handler mHandler;


    public ForegroundServiceStarter(Context context, Service service) {
        mContext = context;
        mService = service;
        //mHandler = new Handler(Looper.getMainLooper());

    }


    public void start() {
        if (mService == null) {
            Log.e(TAG, "SERVICE IS NULL - CANNOT START!");
            return;
        }
        Log.d(TAG, "should be moving to foreground");
        // mHandler.post(new Runnable() {
        //     @Override
        //     public void run() {
        // TODO use constants
        final long end = System.currentTimeMillis() + (60000 * 5);
        final long start = end - (60000 * 60 * 3) - (60000 * 10);
        foregroundStatus();
        Log.d(TAG, "CALLING START FOREGROUND: " + mService.getClass().getSimpleName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                /*
                  When is a foreground service not a foreground service?
                  When it's started from the background of course!
                  On android 11, even though the user explicitly grants us permission to use
                  background location, we still have to request to use it on a foreground
                  service, but only when it isn't re-started with the app open.
                  Even then the restrictions seem to be applied inconsistently!
                 */
            try {
                mService.startForeground(ongoingNotificationId, new Notifications().createOngoingNotification(new BgGraphBuilder(mContext, start, end), mContext), FOREGROUND_SERVICE_TYPE_MANIFEST);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "Got exception trying to use Android 10+ service starting for " + mService.getClass().getSimpleName() + " " + e);
                mService.startForeground(ongoingNotificationId, new Notifications().createOngoingNotification(new BgGraphBuilder(mContext, start, end), mContext));
            }
        } else {
            mService.startForeground(ongoingNotificationId, new Notifications().createOngoingNotification(new BgGraphBuilder(mContext, start, end), mContext));
        }

        //     }
        // });

    }

    public void stop() {
        Log.d(TAG, "should be moving out of foreground");
        mService.stopForeground(true);
    }

    protected void foregroundStatus() {
        Inevitable.task("foreground-status", 2000, () -> UserError.Log.d("XFOREGROUND", mService.getClass().getSimpleName() + (JoH.isServiceRunningInForeground(mService.getClass()) ? " is running in foreground" : " is not running in foreground")));
    }

}
