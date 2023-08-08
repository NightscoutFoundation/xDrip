package com.eveningoutpost.dexdrip.cgm.webfollow;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import lombok.val;

/**
 * JamOrHam
 * Preference handling repository
 */

public class Cpref {

    private static final String DEFAULT_PORT = "8080";

    public static String get(final String which) {

        switch (which) {
            case "U":
                return Pref.getString("webfollow_username", null);
            case "P":
                return Pref.getString("webfollow_password", null);
            case "CK":
                return Pref.getStringTrimmed("webfollow_master_domain", null);
            case "PU":
                return Pref.getString("webfollow_proxy_username", null);
            case "PP":
                return Pref.getString("webfollow_proxy_password", null);
            case "HA":
                return Pref.getStringTrimmed("webfollow_proxy_address", null);
            case "HP":
                val p = Pref.getStringTrimmed("webfollow_proxy_port", null);
                return (validatePort(p) ? p : DEFAULT_PORT);

        }
        return null;
    }

    public static boolean getB(final String which) {

        switch (which) {
            case "UP":
                return Pref.getBooleanDefaultFalse("webfollow_use_proxy");
            case "HP":
                return Pref.getBooleanDefaultFalse("webfollow_proxy_type_http");
        }
        return false;
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        android.util.Log.d("WebFollow", "Preference key: " + key);
        if (key.startsWith("webfollow_")) {
            startWithRefresh(key.startsWith("webfollow_master"));
        }
    };

    static void startWithRefresh(boolean full) {
        Inevitable.task("webfollow-preference-changed", 2000, () -> JoH.startService(WebFollowService.class, "function", full ? "fullrefresh" : "refresh"));
    }

    private static boolean validatePort(final String p) {
        try {
            val i = Integer.parseInt(p);
            return i >= 1 && i <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
