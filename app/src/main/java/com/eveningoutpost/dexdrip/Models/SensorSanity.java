package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.utils.DexCollectionType;

/**
 * Created by jamorham on 02/03/2018.
 *
 * Checks for whether sensor data is within a sane range
 *
 */

public class SensorSanity {

    public static final double DEXCOM_MIN_RAW = 5; // raw values below this will be treated as error
    public static final double DEXCOM_MAX_RAW = 1000; // raw values above this will be treated as error

    public static final double LIBRE_MIN_RAW = 5; // raw values below this will be treated as error

    private static final String TAG = "SensorSanity";

    public static boolean isRawValueSane(double raw_value) {
        return isRawValueSane(raw_value, DexCollectionType.getDexCollectionType());
    }

    public static boolean isRawValueSane(double raw_value, DexCollectionType type) {
        // passes by default!
        boolean state = true;

        // checks for each type of data source

        if (DexCollectionType.hasDexcomRaw(type)) {
            if (raw_value < DEXCOM_MIN_RAW) state = false;
            else if (raw_value > DEXCOM_MAX_RAW) state = false;

        } else if (DexCollectionType.hasLibre(type)) {
            if (raw_value < LIBRE_MIN_RAW) state = false;
        }

        if (!state) {
            if (JoH.ratelimit("sanity-failure", 20)) {
                final String msg = "Sensor Raw Data Sanity Failure: " + raw_value;
                UserError.Log.e(TAG, msg);
                JoH.static_toast_long(msg);
            }
        }

        return state;
    }

}
