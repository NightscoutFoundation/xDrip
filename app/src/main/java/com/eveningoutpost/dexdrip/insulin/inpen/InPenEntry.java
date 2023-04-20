package com.eveningoutpost.dexdrip.insulin.inpen;


import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * jamorham
 *
 * InPen lightweight entry class
 */

public class InPenEntry {

    public static final String ID_INPEN = "InPen";
    public static long started_at = -1;

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("inpen_enabled");
    }

    private static void startWith(final String function) {
        Inevitable.task("inpen-changed-" + function, 1000, () -> JoH.startService(InPenService.class, "function", function));
    }

    public static void startWithRefresh() {
        startWith("refresh");
    }

    public static void startWithReset() {
        startWith("reset");
    }

    public static void startIfEnabled() {
        if (isEnabled()) {
            if (JoH.ratelimit("inpen-start", 40)) {
                startWithRefresh();
            }
        }
    }

    public static boolean isStarted() {
        return started_at != -1;
    }

    public static void immortality() {
        if (!isStarted()) {
            startIfEnabled();
        }
    }

}
