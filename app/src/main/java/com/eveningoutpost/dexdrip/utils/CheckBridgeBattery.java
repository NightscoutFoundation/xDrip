package com.eveningoutpost.dexdrip.utils;

import android.app.PendingIntent;
import android.content.Intent;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
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
    private static final String PARAKEET_PREFS_ITEM = "parakeet_battery";
    private static final String LAST_PARAKEET_PREFS_ITEM = "last-parakeet-battery";
    private static final int NOTIFICATION_ITEM = 541;
    private static final int PARAKEET_NOTIFICATION_ITEM = 542;
    private static final int repeat_seconds = 1200;
    private static final boolean d = false;
    private static int last_level = -1;
    private static int last_parakeet_level = -1;
    private static long last_parakeet_notification = -1;
    private static boolean notification_showing = false;
    private static boolean parakeet_notification_showing = false;
    private static int threshold = 20;


    public static boolean checkBridgeBattery() {

        boolean lowbattery = false;

        if (!Pref.getBooleanDefaultFalse("bridge_battery_alerts")) return false;

        try {
            threshold = Integer.parseInt(Pref.getString("bridge_battery_alert_level", "30"));
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Got error parsing alert level");
        }

        final int this_level = Pref.getInt("bridge_battery", -1);
        UserError.Log.d(TAG, "checkBridgeBattery threshold:" + threshold + " this_level:" + this_level + " last_level:" + last_level);
        if ((this_level > 0) && (threshold > 0)) {
            if ((this_level < threshold) && ((this_level < last_level) || (last_level == -1))) {
                if (JoH.pratelimit("bridge-battery-warning", repeat_seconds)) {
                    notification_showing = true;
                    lowbattery = true;
                    
                    boolean sound = true;
                    boolean vibrate = true;
                    if (Pref.getLong("alerts_disabled_until", 0) > JoH.tsl()) {
                        UserError.Log.d(TAG, "Not playing alert since Notifications are currently disabled!!");
                    	sound = false;
                    	vibrate = false;
                    }
                    
                    final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                    showNotification("Low bridge battery", "Bridge battery dropped to: " + this_level + "%",
                            pendingIntent, NOTIFICATION_ITEM, NotificationChannels.LOW_BRIDGE_BATTERY_CHANNEL, sound, vibrate, null, null, null);
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

    public static boolean checkForceWearBridgeBattery() {

        boolean lowbattery = false;

        if (!Pref.getBooleanDefaultFalse("bridge_battery_alerts")) return false;
        if (!Pref.getBooleanDefaultFalse("disable_wearG5_on_lowbattery")) return false;

        try {
            threshold = Integer.parseInt(Pref.getString("bridge_battery_alert_level", "30"));
            if (threshold > 5)//give user 5% leeway to begin charging wear device
                threshold = threshold - 5;
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Got error parsing alert level");
        }

        final int this_level = Pref.getInt("bridge_battery", -1);
        UserError.Log.d(TAG, "checkForceWearBridgeBattery threshold:" + threshold + " this_level:" + this_level);
        if ((this_level > 0) && (threshold > 0)) {
            if (this_level < threshold) {
                lowbattery = true;
            }
        }
        return lowbattery;
    }

    public static void checkParakeetBattery() {

        if (!Pref.getBooleanDefaultFalse("bridge_battery_alerts")) return;

        threshold = Pref.getStringToInt("bridge_battery_alert_level", 30);

        final int this_level = Pref.getInt(PARAKEET_PREFS_ITEM, -1);
        if (last_parakeet_level == -1) {
            last_parakeet_level = (int) PersistentStore.getLong(LAST_PARAKEET_PREFS_ITEM);
        }

        if (d) UserError.Log.e(TAG, "checkParakeetBattery threshold:" + threshold + " this_level:" + this_level + " last:" + last_parakeet_level);
        if ((this_level > 0) && (threshold > 0)) {
            if ((this_level < threshold) && (this_level < last_parakeet_level)) {
                if (JoH.pratelimit("parakeet-battery-warning", repeat_seconds)) {
                    parakeet_notification_showing = true;
                    final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                    cancelNotification(PARAKEET_NOTIFICATION_ITEM);
                    showNotification(xdrip.getAppContext().getString(R.string.low_parakeet_battery), "Parakeet battery dropped to: " + this_level + "%",
                            pendingIntent, PARAKEET_NOTIFICATION_ITEM, NotificationChannels.LOW_BRIDGE_BATTERY_CHANNEL, true, true, null, null, null);
                    last_parakeet_notification = JoH.tsl();
                    if (d) UserError.Log.e(TAG, "checkParakeetBattery RAISED ALERT threshold:" + threshold + " this_level:" + this_level + " last:" + last_parakeet_level);
                }
            } else {
                if ((parakeet_notification_showing) && (JoH.msSince(last_parakeet_level) > Constants.MINUTE_IN_MS * 25)) {
                    cancelNotification(PARAKEET_NOTIFICATION_ITEM);
                    parakeet_notification_showing = false;
                }
            }
            last_parakeet_level = this_level;
            PersistentStore.setLong(LAST_PARAKEET_PREFS_ITEM, this_level);
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
