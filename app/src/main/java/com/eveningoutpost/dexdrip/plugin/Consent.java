package com.eveningoutpost.dexdrip.plugin;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * JamOrHam
 *
 * Plugin consent data storage
 */

public class Consent {
    private static final String PREFIX = "plugin-consent-";

    public static void setGiven(final PluginDef pluginDef) {
        Pref.setBoolean(PREFIX + pluginDef.name, true);
    }

    public static boolean isGiven(final PluginDef pluginDef) {
        return Pref.getBooleanDefaultFalse(PREFIX + pluginDef.name);
    }
}
