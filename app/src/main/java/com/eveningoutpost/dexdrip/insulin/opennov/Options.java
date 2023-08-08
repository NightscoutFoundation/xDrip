package com.eveningoutpost.dexdrip.insulin.opennov;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * JamOrHam
 * OpenNov preference handling
 */

public class Options {

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("opennov_enabled");
    }

    public static boolean playSounds() {
        return Pref.getBooleanDefaultFalse("opennov_play_sounds");
    }

    public static boolean extraDebug() {
        return Pref.getBooleanDefaultFalse("opennov_debug");
    }

    public static boolean loadEverything() {
        return Pref.getBooleanDefaultFalse("opennov_load_everything");
    }

    public static boolean removePrimingDoses() {
        return Pref.getBooleanDefaultFalse("opennov_remove_priming")
                && primingUnits() > 0.0d
                && primingMinutes() > 0.0d;
    }
    public static boolean hidePrimingDoses() {
        return Pref.getBooleanDefaultFalse("opennov_hide_priming");
    }

    public static double primingUnits() {
        return Pref.getStringToDouble("opennov_prime_units", 0.0);
    }
    public static double primingMinutes() {
        return Pref.getStringToDouble("opennov_prime_minutes", 0.0);
    }

}
