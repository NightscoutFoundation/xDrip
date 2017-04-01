package com.eveningoutpost.dexdrip.utils;

import android.app.PendingIntent;
import android.content.Intent;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.Models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.Models.JoH.showNotification;

/**
 * Created by jamorham on 26/01/2017.
 */

public class CheckBridgeBattery {

    private static final String TAG = CheckBridgeBattery.class.getSimpleName();
    private static final String PREFS_ITEM = "bridge_battery";
    private static final int NOTIFICATION_ITEM = 541;
    private static int last_level = -1;
    private static boolean notification_showing = false;
    private static int threshold = 20;
    private static final int repeat_seconds = 1200;

    public static boolean checkBridgeBattery() {

        boolean lowbattery = false;

        if (!Home.getPreferencesBooleanDefaultFalse("bridge_battery_alerts")) return false;

        try {
            threshold = Integer.parseInt(Home.getPreferencesStringWithDefault("bridge_battery_alert_level", "30"));
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Got error parsing alert level");
        }

        final int this_level = Home.getPreferencesInt("bridge_battery", -1);
        if ((this_level > 0) && (threshold > 0)) {
            if ((this_level < threshold) && (this_level < last_level)) {
                if (JoH.pratelimit("bridge-battery-warning", repeat_seconds)) {
                    notification_showing = true;
                    lowbattery = true;
                    final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                    showNotification(xdrip.getAppContext().getString(R.string.low_bridge_battery), xdrip.getAppContext().getString(R.string.bridge_battery_dropped_to, this_level), pendingIntent, NOTIFICATION_ITEM, true, true, false);
                }
            } else {
                if (notification_showing) {
                    cancelNotification(NOTIFICATION_ITEM);
                    notification_showing = false;
                }
            }
            last_level = this_level;
        }
        return lowbattery;
    }


    public static void testHarness() {
        if (Home.getPreferencesInt(PREFS_ITEM, -1) < 1)
            Home.setPreferencesInt(PREFS_ITEM, 60);
        Home.setPreferencesInt(PREFS_ITEM, Home.getPreferencesInt(PREFS_ITEM, 0) - (int) (JoH.tsl() % 15));
        UserError.Log.d(TAG, "Bridge battery: " + Home.getPreferencesInt(PREFS_ITEM, 0));
        checkBridgeBattery();
    }


}
