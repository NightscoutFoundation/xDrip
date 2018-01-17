package com.eveningoutpost.dexdrip.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by jamorham on 04/06/2016.
 */
public class PowerStateReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham power";
    // TODO move this all to a generic reusable state storage class
    private static final String PREFS_POWER_INTERNAL = "general_internal";
    private static final String PREFS_POWER_STATE = "power_state";
    private static SharedPreferences prefs;

    private static void init_prefs() {
        if (prefs == null) prefs = xdrip.getAppContext()
                .getSharedPreferences(PREFS_POWER_INTERNAL, Context.MODE_PRIVATE);
    }

    private static boolean getInternalPrefsBoolean(String name) {
        init_prefs();
        return prefs.getBoolean(name, false);
    }

    private static void setInternalPrefsBoolean(String name, boolean value) {
        init_prefs();
        prefs.edit().putBoolean(name, value).apply();
    }

    public static boolean is_power_connected() {
        return getInternalPrefsBoolean(PREFS_POWER_STATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            setInternalPrefsBoolean(PREFS_POWER_STATE, true);
            Log.d(TAG, "Power connected");
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            setInternalPrefsBoolean(PREFS_POWER_STATE, false);
            Log.d(TAG, "Power disconnected ");
        }
    }
}
