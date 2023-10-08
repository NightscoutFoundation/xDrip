package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

import android.content.pm.PackageManager;
import android.os.Build;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

public class WholeHouse {

    private static final String PREF = "plus_whole_house";
    private static final String TAG = WholeHouse.class.getSimpleName();

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF);
    }

    public static boolean isLive() {
        return isEnabled() && (isRpi() || isAndroidTV());
    }

    public static boolean isRpi() {
        return Build.MODEL.startsWith("Raspberry Pi");
    }

    private static boolean isAndroidTV() {
        if (xdrip.getAppContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_ETHERNET)) {
            UserError.Log.d(TAG, "Android TV Detected");
            return true;
        }
        return false;
    }

}
