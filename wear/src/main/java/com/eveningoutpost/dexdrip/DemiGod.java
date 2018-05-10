package com.eveningoutpost.dexdrip;

// jamorham

// detect jamorham custom rom with demigod mode

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import com.eveningoutpost.dexdrip.Models.UserError;

public class DemiGod {

    private static final String TAG = DemiGod.class.getSimpleName();

    private static final String DEVICE_POWER = "android.permission.DEVICE_POWER";

    private static Boolean enabled = null;

    public static boolean isPresent() {
        if (enabled == null) {
            enabled = determineDemiGodMode();
            if (enabled) UserError.Log.uel(TAG, "DemiGod mode reports as: " + enabled);
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
