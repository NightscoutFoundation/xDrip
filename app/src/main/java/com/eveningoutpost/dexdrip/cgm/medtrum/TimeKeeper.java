package com.eveningoutpost.dexdrip.cgm.medtrum;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;

/**
 * JamOrHam
 *
 * Manage Medtrum transmitter reference times
 */

public class TimeKeeper {

    private static final String TAG = "MedtrumTime";
    private static final String PREF_TAG = "medtrum-time-";

    public static int secondsSinceReferenceTime(long serial, long time) {
        final long stored_time = getTime(serial);
        if (stored_time == 0) return -1;
        return (int) ((time - stored_time) / 1000);
    }

    public static long timeStampFromTickCounter(long serial, int tick) {
        final long stored_time = getTime(serial);
        if (stored_time == 0) return -1;
        return stored_time + (tick * MINUTE_IN_MS * 2);
    }

    public static int tickCounterFromTimeStamp(long serial, long timestmap) {
        final long stored_time = getTime(serial);
        if (stored_time == 0) return -1;
        return (int) ((timestmap - stored_time) / (MINUTE_IN_MS * 2));
    }

    public static void setTime(long serial, long time) {
        if (serial != 0) {
            if (time >= 1530198145000L) {
                PersistentStore.setLong(getPrefTag(serial), time);
                UserError.Log.d(TAG, "Reference Time updated: " + JoH.dateTimeText(time));
            } else {
                UserError.Log.e(TAG, "Time is too far in past: " + JoH.dateText(time));
            }
        } else {
            UserError.Log.e(TAG, "Serial is not set");
        }
    }

    private static long getTime(long serial) {
        return PersistentStore.getLong(getPrefTag(serial));
    }

    private static String getPrefTag(long serial) {
        return PREF_TAG + serial;
    }

}
