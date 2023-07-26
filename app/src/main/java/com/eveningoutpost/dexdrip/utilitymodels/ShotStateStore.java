package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * Created by jamorham on 20/06/2016.
 */

import android.content.Context;

import com.eveningoutpost.dexdrip.xdrip;

public class ShotStateStore {

    private static final String PREFS_SHOWCASE_INTERNAL = "showcase_internal";
    private static final int INVALID_SHOT_ID = -1;

    long shotId = INVALID_SHOT_ID;

    public static boolean hasShot(int shotId) {
        return isSingleShot(shotId) && xdrip.getAppContext()
                .getSharedPreferences(PREFS_SHOWCASE_INTERNAL, Context.MODE_PRIVATE)
                .getBoolean("hasShot" + shotId, false);
    }

    private static boolean isSingleShot(int shotId) {
        return shotId != INVALID_SHOT_ID;
    }

    public static void resetAllShots() {
        xdrip.getAppContext().getSharedPreferences(PREFS_SHOWCASE_INTERNAL, Context.MODE_PRIVATE).edit().clear().apply();
    }
}