package com.eveningoutpost.dexdrip;

// jamorham

// detect jamorham custom rom with demigod mode

import android.Manifest;
import android.content.pm.PackageManager;


import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

public class DemiGod {

    private static final String TAG = DemiGod.class.getSimpleName();
    private static final String PREF_MARKER = "has_demigod_mode";

    private static final String DEVICE_POWER = "android.permission.DEVICE_POWER";

    private static Boolean enabled = null;

    public static boolean isPresent() {
        if (enabled == null) {
            enabled = determineDemiGodMode();
            if (enabled) {
                UserError.Log.uel(TAG, "(restart) DemiGod mode reports as: " + enabled);
            }
            if (Pref.getBooleanDefaultFalse(PREF_MARKER) != enabled) {
                Pref.setBoolean(PREF_MARKER, enabled);
            }
        }
        return enabled;
    }

    private static boolean determineDemiGodMode() {
        return (ContextCompat.checkSelfPermission(xdrip.getAppContext(),
                Manifest.permission.BLUETOOTH_PRIVILEGED) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(xdrip.getAppContext(),
                        DEVICE_POWER) == PackageManager.PERMISSION_GRANTED);
    }
}
