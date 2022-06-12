package com.eveningoutpost.dexdrip.watch.lefun;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

// jamorham

// very lightweight entry point class to avoid loader overhead when not in use

public class LeFunEntry {

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("lefun_enabled");
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("lefun_send_alarms");
    }

    public static boolean areCallAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("lefun_option_call_notifications");
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("le-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    startWithRefresh();
                }
            });
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
       // android.util.Log.d("lefun", "Preference key: " + key);
        if (key.startsWith("lefun")) {
            startWithRefresh();
        }
    };

    static void startWithRefresh() {
        Inevitable.task("lefun-preference-changed", 1000, () -> JoH.startService(LeFunService.class, "function", "refresh"));
    }
}
