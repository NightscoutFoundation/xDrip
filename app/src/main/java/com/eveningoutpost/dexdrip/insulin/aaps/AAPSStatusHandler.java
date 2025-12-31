package com.eveningoutpost.dexdrip.insulin.aaps;

import com.eveningoutpost.dexdrip.alert.Persist;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import info.nightscout.sdk.localmodel.devicestatus.NSDeviceStatus;
import lombok.val;

/**
 * JamOrHam
 * <p>
 * Handle processing of AAPS device status updates
 */

public class AAPSStatusHandler {

    private static final String TAG = AAPSStatusHandler.class.getSimpleName();
    private static final Gson gson = new GsonBuilder().create();
    private static final Persist.StringTimeout store =
            new Persist.StringTimeout("AAPS_DEVICE_STATUS", Constants.MINUTE_IN_MS * 21);
    private static volatile NSDeviceStatus last;

    // process and store received json in to object and maintain persistent time limited cache
    public static void processDeviceStatus(final String json) {
        synchronized (AAPSStatusHandler.class) {
            try {
                last = gson.fromJson(json, NSDeviceStatus.class);
                Log.d(TAG, "DEBUG: got device status: " + last.toString());
                if (last != null) {
                    store.set(json);
                    val pump = last.getPump();
                    if (pump != null) {
                        val r = pump.getReservoir();
                        if (r != null) {
                            PumpStatus.setReservoir(r);
                        }
                        val b = pump.getBattery();
                        if (b != null) {
                            val pc = b.getPercent();
                            if (pc != null) {
                                PumpStatus.setBattery(pc);
                            }
                        }
                    }
                    PumpStatus.syncUpdate();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing device status: " + e + " json: " + json);
            }
        }
    }

    // get instance either from cache or persistent store if still valid
    public static NSDeviceStatus get() {
        synchronized (AAPSStatusHandler.class) {
            val json = store.get(); // local copy
            if (json != null) {
                if (last == null) {
                    // needs reconstructing
                    try {
                        last = gson.fromJson(json, NSDeviceStatus.class);
                    } catch (Exception e) {
                        Log.wtf(TAG, "Unusual problem reconstructing device status: " + e);
                    }
                }
                return last;
            }
            return null;
        }
    }

}
