package com.eveningoutpost.dexdrip.utils.framework;

import android.os.Build;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.models.JoH.buggy_samsung;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;

/**
 * jamorham
 *
 * Samsung have made modifications to the Android framework which breaks compatibility with the
 * published reference documentation. This diminishes the user experience and the required features
 * available to developers. Until they fix these bugs we attempt to work-around them...
 */

@RequiredArgsConstructor
public class BuggySamsung {

    // TODO this overlaps with ob1 implementation
    // TODO eventually individual implementations of this should consolidate here.
    private static final String BUGGY_SAMSUNG_ENABLED = "buggy-samsung-enabled";
    private static final long TOLERABLE_JITTER = 10000;

    private final String TAG;
    @Getter
    private long max_wakeup_jitter;

    public long evaluate(final long wakeup_time) {
        if (wakeup_time > 0) {
            final long wakeup_jitter = msSince(wakeup_time);
            //UserError.Log.e(TAG, "debug jitter: " + wakeup_jitter);
            if (wakeup_jitter < 0) {
                UserError.Log.d(TAG, "Woke up Early..");
            } else {
                if (wakeup_jitter > 1000) {
                    UserError.Log.d(TAG, "Wake up, time jitter: " + JoH.niceTimeScalar(wakeup_jitter));
                    if ((wakeup_jitter > TOLERABLE_JITTER) && (!buggy_samsung) && isSamsung()) {
                        UserError.Log.wtf(TAG, "Enabled wake workaround due to jitter of: " + JoH.niceTimeScalar(wakeup_jitter));
                        buggy_samsung = true;
                        PersistentStore.incrementLong(BUGGY_SAMSUNG_ENABLED);
                        max_wakeup_jitter = 0;
                    } else {
                        max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
                        checkWasBuggy();
                    }
                }
            }
            return wakeup_jitter;
        }
        return 0; // no wakeup time specified

    }

    // enable if we have historic markers showing previous enabling
    public void checkWasBuggy() {
        if (!buggy_samsung && isSamsung() && PersistentStore.getLong(BUGGY_SAMSUNG_ENABLED) > 4) {
            UserError.Log.e(TAG, "Enabling wake workaround due to persistent metric");
            buggy_samsung = true;
        }
    }

    public static boolean isSamsung() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung")
                || Build.MANUFACTURER.toLowerCase().contains("xiaomi")
                || Build.MANUFACTURER.toLowerCase().contains("oneplus")    // experimental test
                || Build.MANUFACTURER.toLowerCase().contains("oppo");      // experimental test
    }

}
