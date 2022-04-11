package com.eveningoutpost.dexdrip.insulin.opennov;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;

/**
 * JamOrHam
 * OpenNov preference handling
 */

public class Options {

    public static boolean playSounds() {
        return Pref.getBooleanDefaultFalse("opennov_play_sounds");
    }

    public static boolean extraDebug() {
        return Pref.getBooleanDefaultFalse("opennov_debug");
    }

    public static boolean loadEverything() {
        return Pref.getBooleanDefaultFalse("opennov_load_everything");
    }

}
