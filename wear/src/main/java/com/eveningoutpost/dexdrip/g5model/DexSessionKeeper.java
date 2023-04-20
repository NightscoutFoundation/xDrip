package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

// jamorham

// track active session time

public class DexSessionKeeper {

    private static final String PREF_SESSION_START = "OB1-SESSION-START";
    private static final long WARMUP_PERIOD = Constants.HOUR_IN_MS * 2;

    public static void clearStart() {
        PersistentStore.setLong(PREF_SESSION_START, 0);
    }

    public static void setStart(long when) {
        // TODO sanity check
        PersistentStore.setLong(PREF_SESSION_START, when);
    }

    public static long getStart() {
        // value 0 == not started
        return PersistentStore.getLong(PREF_SESSION_START);
    }

    public static boolean isStarted() {
        return getStart() != 0;
    }

    public static String prettyTime() {
        if (isStarted()) {
            final long elapsed = JoH.msSince(getStart());
            if (elapsed < WARMUP_PERIOD) {
                return JoH.niceTimeScalar((double) WARMUP_PERIOD - elapsed, 1);
            } else {
                return JoH.niceTimeScalar((double) elapsed, 1);
            }
        } else {
            return "";
        }
    }

    // check for mismatch between sensor state currency and transmitter time
    public static boolean warmUpTimeValid() {
        return (isStarted() && (JoH.msSince(getStart()) <= WARMUP_PERIOD));
    }

}
