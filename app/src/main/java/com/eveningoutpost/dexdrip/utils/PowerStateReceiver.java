package com.eveningoutpost.dexdrip.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by jamorham on 04/06/2016.
 */
public class PowerStateReceiver extends BroadcastReceiver {

    public static final String TAG = "jamorham power";
    public static boolean power_connected = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            power_connected = true;
            Log.d(TAG, "Power connected");
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            power_connected = false;
            Log.d(TAG, "Power disconnected ");
        }
    }
}
