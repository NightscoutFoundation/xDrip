package com.eveningoutpost.dexdrip.watch.thinjam;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import lombok.val;

// jamorham

public class BlueJayEntry {

// very lightweight entry point class to avoid loader overhead when not in use

    // preference key groups to observe
    private static final String[] HUNT_PREFERENCES = {"bluejay", "units", "dex_txid"};

    private static final String PREF_ENABLED = "bluejay_enabled";
    private static final String PREF_PHONE_COLLECTOR_ENABLED = "bluejay_run_phone_collector";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }

    public static boolean isPhoneCollectorDisabled() {
        return isEnabled() && BlueJay.isCollector() && !Pref.getBoolean(PREF_PHONE_COLLECTOR_ENABLED, true);
    }

    public static void setEnabled() {
        Pref.setBoolean(PREF_ENABLED, true);
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("bluejay_send_alarms");
    }

    public static boolean areCallAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("bluejay_option_call_notifications");
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("bj-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    startWithRefresh();
                }
            });
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            for (val hunt : HUNT_PREFERENCES)
                if (key.startsWith(hunt)) {
                    android.util.Log.d("BlueJayPref", "Hit on Preference key: " + key);
                    if (key.equals(PREF_PHONE_COLLECTOR_ENABLED)) {
                        CollectionServiceStarter.restartCollectionServiceBackground();
                    }
                    startWithRefresh();
                    break;
                }
        }
    };

    public static void startWithRefreshIfEnabled() {
        if (isEnabled()) {
            startWithRefresh();
        }
    }

    public static void sendNotifyIfEnabled(final String msg) {
        if (isEnabled()) {
            final String fmsg = msg.replaceAll("^-", "").trim();
            if (!JoH.emptyString(msg)) {
                // TODO handle message types
                Inevitable.task("bluejay-send-notify-external", 200, () -> JoH.startService(BlueJayService.class, "function", "message", "message", fmsg));
            }
        }
    }

    static void startWithRefresh() {
        Inevitable.task("bluejay-preference-changed", 1000, () -> JoH.startService(BlueJayService.class, "function", "refresh"));
    }

}



