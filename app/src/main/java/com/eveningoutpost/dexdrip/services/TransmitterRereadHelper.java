package com.eveningoutpost.dexdrip.services;

import static com.eveningoutpost.dexdrip.services.G5BaseService.G5_FIRMWARE_MARKER;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

/**
 * Navid200 - February 10, 2026
 *
 * Helper for rereading the details of the currently connected transmitter.
 *
 * Normally, xDrip captures transmitter details only once per device.
 * This class exists to handle the unusual case where we need to force
 * xDrip to reread the details from a transmitter.
 */
public class TransmitterRereadHelper {

    private static boolean txRereadActive = false;
    private static long txRereadEndTime = 0;

    private static final String TAG = "TransmitterRereadHelper";

    public static void requestTxReread() { // Clear cache for the current transmitter
        try {
            for (int t = 1; t <= 3; t++) {
                PersistentStore.removeItem(G5_FIRMWARE_MARKER + getTransmitterID() + "-" + t);
            }
            txRereadEndTime = JoH.tsl() + 16 * MINUTE_IN_MS;
            txRereadActive = true;
        } catch (Exception e) {
            UserError.Log.w(TAG, "Failed to clear transmitter details", e);
        }
    }

    public static boolean isTxRereadActive() {
        if (JoH.tsl() > txRereadEndTime) {
            txRereadActive = false;
        }
        return txRereadActive;
    }
}
