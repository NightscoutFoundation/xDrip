package com.eveningoutpost.dexdrip.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.models.JoH.cancelNotification;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;

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

    public static void checkBridgeBattery() {

        if (!Pref.getBooleanDefaultFalse("bridge_battery_alerts")) return;

        try {
            threshold = Integer.parseInt(Pref.getString("bridge_battery_alert_level", "30"));
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Got error parsing alert level");
        }

        final int this_level = Pref.getInt("bridge_battery", -1);
        if ((this_level > 0) && (threshold > 0)) {
            if ((this_level < threshold) && (this_level < last_level)) {
                if (JoH.pratelimit("bridge-battery-warning", repeat_seconds)) {
                    notification_showing = true;
                    final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                    showNotification("Low bridge battery", "Bridge battery dropped to: " + this_level + "%", pendingIntent, NOTIFICATION_ITEM, true, true, false);
                }
            } else {
                if (notification_showing) {
                    cancelNotification(NOTIFICATION_ITEM);
                    notification_showing = false;
                }
            }
            last_level = this_level;
        }
    }

    public static int getBatteryLevel(Context context) {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) {
                return 50;
            }
            return (int) (((float) level / (float) scale) * 100.0f);
        } catch (NullPointerException e) {
            if (JoH.ratelimit("battery-read-error", 3600)) {
                UserError.Log.e(TAG, "Cannot read battery levels!");
            }
            return 50;
        }
    }

    public static void testHarness() {
        if (Pref.getInt(PREFS_ITEM, -1) < 1)
            Pref.setInt(PREFS_ITEM, 60);
        Pref.setInt(PREFS_ITEM, Pref.getInt(PREFS_ITEM, 0) - (int) (JoH.tsl() % 15));
        UserError.Log.d(TAG, "Bridge battery: " + Pref.getInt(PREFS_ITEM, 0));
        checkBridgeBattery();
    }


}
