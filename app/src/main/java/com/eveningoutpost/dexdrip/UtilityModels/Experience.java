package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

/**
 * Created by jamorham on 22/01/2017.
 * <p>
 * Gauge a user's experience level
 */

public class Experience {

    private static final String TAG = "xdrip-Experience";
    private static final String marker = "xdrip-plus-installed-time";

    // caches
    private static boolean got_data = false;
    private static boolean not_newbie = false;

    public static boolean isNewbie() {
        if (not_newbie) return false;
        final long installed_time = Home.getPreferencesLong(marker, -1);
        if (installed_time > 0) {
            UserError.Log.d(TAG, "First Installed " + JoH.niceTimeSince(installed_time) + " ago");
            not_newbie = true;
            return false;
        } else {
            // probably newbie
            Home.setPreferencesLong(marker, JoH.tsl());
            if (gotData()) return false;
            UserError.Log.d(TAG, "Looks like a Newbie");
            return true;
        }
    }

    public static boolean gotData() {
        if (got_data) return true;
        if (BgReading.last(true) != null) {
            got_data = true;
            return true;
        } else {
            return false;
        }
    }

}
