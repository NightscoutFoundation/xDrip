package com.eveningoutpost.dexdrip.UtilityModels;




import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationsReceiver extends BroadcastReceiver {

    private final static String TAG = Notifications.class.getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e(TAG, "New onRecieve called Threadid " + Thread.currentThread().getId());

        Notifications notifications = Notifications.getInstance(context);
        notifications.periodicTimer(context);

    }
}