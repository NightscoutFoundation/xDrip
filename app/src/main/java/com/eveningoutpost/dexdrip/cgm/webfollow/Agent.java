package com.eveningoutpost.dexdrip.cgm.webfollow;

import android.os.Build;

import com.eveningoutpost.dexdrip.xdrip;

/**
 * JamOrHam
 */

public class Agent {

    private static final String specimen = "Dalvik/2.1.0 (Linux; U; Android 12; Pixel 6 Build/SQ3A.220605.009.B1)";

    public static String get(final String fmt) {
        if (xdrip.isRunningTest()) {
            return specimen;
        } else {
            return String.format(fmt, Build.VERSION.RELEASE, Build.MODEL, Build.ID);
        }
    }

}
