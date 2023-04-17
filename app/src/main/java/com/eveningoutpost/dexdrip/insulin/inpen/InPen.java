package com.eveningoutpost.dexdrip.insulin.inpen;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * jamorham
 */

public class InPen {

    private static final String PREF_INPEN_MAC = "inpen_mac";

    static final String STORE_INPEN_ADVERT = "InPen-advert-";
    static final String STORE_INPEN_INFOS = "InPen-infos-";
    static final String STORE_INPEN_BATTERY = "InPen-battery-";

    static final float DEFAULT_BOND_UNITS = 2.0f;

    static String getMac() {
        return Pref.getString(PREF_INPEN_MAC, null);
    }

    static void setMac(final String mac) {
        Pref.setString(PREF_INPEN_MAC, mac);
    }

    static boolean soundsEnabled() {
        return Pref.getBooleanDefaultFalse("inpen_sounds");
    }

}
