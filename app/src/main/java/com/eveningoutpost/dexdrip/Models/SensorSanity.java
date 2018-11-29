package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.UtilityModels.Pref;
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

    public static final double DEXCOM_G6_MIN_RAW = 5; // raw values below this will be treated as error
    public static final double DEXCOM_G6_MAX_RAW = 1000; // raw values above this will be treated as error

    public static final double LIBRE_MIN_RAW = 5; // raw values below this will be treated as error

    private static final String TAG = "SensorSanity";

    public static boolean isRawValueSane(double raw_value) {
        return isRawValueSane(raw_value, DexCollectionType.getDexCollectionType(), false);
    }

    public static boolean isRawValueSane(double raw_value, boolean hard) {
        return isRawValueSane(raw_value, DexCollectionType.getDexCollectionType(), hard);
    }

    public static boolean isRawValueSane(double raw_value, DexCollectionType type) {
        return isRawValueSane(raw_value, type, false);

    }

    public static boolean isRawValueSane(double raw_value, DexCollectionType type, boolean hard_check) {

        // bypass checks if the allowing dead sensor engineering mode is enabled
        if (allowTestingWithDeadSensor()) {
            if (JoH.pratelimit("dead-sensor-sanity-passing", 3600)) {
                UserError.Log.e(TAG, "Allowing any value due to Allow Dead Sensor being enabled");
            }
            return true;
        }

        // passes by default!
        boolean state = true;

        // checks for each type of data source

        if (DexCollectionType.hasDexcomRaw(type)) {
            if ((raw_value != BgReading.SPECIAL_G5_PLACEHOLDER) || hard_check) {
                if (Pref.getBooleanDefaultFalse("using_g6")) {
                    if (raw_value < DEXCOM_G6_MIN_RAW) state = false;
                    else if (raw_value > DEXCOM_G6_MAX_RAW) state = false;
                } else {
                    if (raw_value < DEXCOM_MIN_RAW) state = false;
                    else if (raw_value > DEXCOM_MAX_RAW) state = false;
                }
            }

        } else if (DexCollectionType.hasLibre(type)) {
            if (raw_value < LIBRE_MIN_RAW) state = false;
        } else if (type == DexCollectionType.Medtrum) {
            if (raw_value < DEXCOM_MIN_RAW) state = false;
            else if (raw_value > DEXCOM_MAX_RAW) state = false;
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

    public static boolean allowTestingWithDeadSensor() {
        return Pref.getBooleanDefaultFalse("allow_testing_with_dead_sensor")
                && Pref.getBooleanDefaultFalse("engineering_mode");
    }

}
