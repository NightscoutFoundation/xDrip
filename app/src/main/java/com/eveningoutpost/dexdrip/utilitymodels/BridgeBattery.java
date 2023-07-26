package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

// jamorham

public class BridgeBattery {

    // This should be refactored throughout the code to use something like PersistentStore instead
    // of prefs.
    private static final String PREFS_ITEM = "bridge_battery";


    public static int getBestBridgeBattery() {
        if (DexCollectionType.hasBattery()) {
            return Pref.getInt(PREFS_ITEM, -1);
        } else {
            return -1;
        }

    }
}
