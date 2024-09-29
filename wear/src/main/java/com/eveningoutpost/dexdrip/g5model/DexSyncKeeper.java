package com.eveningoutpost.dexdrip.g5model;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;

public class DexSyncKeeper {

    private static final String TAG = DexTimeKeeper.class.getSimpleName();
    private static final String DEX_SYNC_STORE = "DEX_SYNC_STORE-";
    private static final long OLDEST_POSSIBLE = 1533839836123L;
    private static final long GRACE_TIME = 5000;
    private static final long VALIDITY_PERIOD = Constants.DAY_IN_MS;


    // store sync time as now
    public static void store(final String transmitterId) {
        store(transmitterId, JoH.tsl());
    }

    // store sync time
    public static void store(final String transmitterId, final long when) {

        if ((transmitterId == null) || (transmitterId.length() < 4)) {
            UserError.Log.e(TAG, "Invalid dex transmitter in store: " + transmitterId);
            return;
        }

        if (when < OLDEST_POSSIBLE) {
            UserError.Log.wtf(TAG, "Invalid timestamp to store: " + JoH.dateTimeText(when));
            return;
        }

        PersistentStore.cleanupOld(DEX_SYNC_STORE);
        PersistentStore.setLong(DEX_SYNC_STORE + transmitterId, when);
        UserError.Log.d(TAG, "Sync time updated to: " + JoH.dateTimeText(when));
    }

    public static void store(final String transmitterId, final long when, final long last_conection_time, final long last_glucose_processed) {
        final long latency = (last_glucose_processed - last_conection_time);
        UserError.Log.d(TAG, "Update time from glucose rx glucose: " + JoH.dateTimeText(when) + " latency:" + latency + " ms");
        if (latency < 8000) { // roughly half preempt
            store(transmitterId, when);
        } else {
            UserError.Log.e(TAG, "Refusing to update stored timestamp due to excessive processing latency: " + latency + "ms");
        }
    }

    public static void clear(final String transmitterId) {
        if (PersistentStore.getLong(DEX_SYNC_STORE + transmitterId) > 0) {
            UserError.Log.e(TAG, "Clearing stored timing sync information for: " + transmitterId);
            PersistentStore.setLong(DEX_SYNC_STORE + transmitterId, 0);
        }
    }

    // anticpiate next wake up from now
    public static long anticipate(final String transmitterId) {
        return anticipate(transmitterId, JoH.tsl());
    }

    // anticipate next wake up from time
    // -1 means we don't know anything
    static long anticipate(final String transmitterId, final long now) {
        final long last = PersistentStore.getLong(DEX_SYNC_STORE + transmitterId);
        if (last < OLDEST_POSSIBLE) {
            return -1;
        }
        if (last > now) {
            UserError.Log.e(TAG, "Anticipation time in the future! cannot use: " + JoH.dateTimeText(last));
            return -1; // can't be in the future
        }

        if (now - last > VALIDITY_PERIOD) {
            UserError.Log.e(TAG, "Anticipation time too old to use: " + JoH.dateTimeText(last));
            return -1;
        }

        final long modulo = (now - last) % DEXCOM_PERIOD;
        if ((modulo < GRACE_TIME) && ((now - last) > GRACE_TIME)) return now;
        final long next = now + (DEXCOM_PERIOD - modulo);
        return next;
    }

    public static boolean isReady(final String transmitterId) {
        return anticipate(transmitterId, JoH.tsl()) != -1;
    }

    // are we outside connection window?
    // TODO also handle waking up before window PRE_GRACE_TIME = 20seconds?
    public static boolean outsideWindow(final String transmitterId) {
        final long now = JoH.tsl();
        final long next = anticipate(transmitterId, now);
        return next != now;
    }

}

