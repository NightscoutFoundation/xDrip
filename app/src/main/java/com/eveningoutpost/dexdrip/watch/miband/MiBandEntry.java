package com.eveningoutpost.dexdrip.watch.miband;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.watch.miband.MiBandService;

// jamorham

// very lightweight entry point class to avoid loader overhead when not in use

public class MiBandEntry {
    private static final String PREF_MIBAND_ENABLED = "miband_enabled";
    private static final String PREF_SEND_REDINGS = "miband_send_readings";
    private static final String PREF_SEND_ALARMS = "miband_send_alarms";
    private static final String PREF_CALL_ALERTS = "miband_option_call_notifications";



    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_ENABLED);
    }
    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS);
    }

    public static boolean isNeedSendReading() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_REDINGS);
    }

    public static boolean areCallAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_CALL_ALERTS);
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("mb-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    startWithRefresh();
                }
            });
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.startsWith("miband")) {
                android.util.Log.d("miband", "Preference key: " + key);
                startWithRefresh();
            }
        }
    };

    static void startWithRefresh() {
        Inevitable.task("miband-preference-changed", 1000, () -> JoH.startService(MiBandService.class, "function", "refresh"));
    }
}
