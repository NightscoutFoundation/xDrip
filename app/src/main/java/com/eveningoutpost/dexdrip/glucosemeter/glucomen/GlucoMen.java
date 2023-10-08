package com.eveningoutpost.dexdrip.glucosemeter.glucomen;

import android.nfc.Tag;

import com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices.Identify;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * JamOrHam
 * GlucoMen Entry class
 */

public class GlucoMen {

    private static final String TAG = GlucoMen.class.getSimpleName();

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("nfc_meter_enabled");
    }

    public static boolean playSounds() {
        return Pref.getBooleanDefaultFalse("bluetooth_meter_play_sounds");
    }

    public static boolean wantThis(final Tag tag) {
        if (!isEnabled()) return false;
        return Identify.getDevice(tag).isKnown();
    }

}
