package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
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
            if (!BgReading.isRawMarkerValue(raw_value) || hard_check) {
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


    /**
     * Check Libre serial for unexpected changes. Stop xDrip sensor session if there is a mismatch.
     *
     * Same sensor session with different sensor serial number results in error response
     *
     * boolean return of true indicates problem
     */

    private static final String PREF_LIBRE_SN = "SensorSanity-LibreSN";
    private static final String PREF_LIBRE_SENSOR_UUID = "SensorSanity-LibreSensor";

    public static boolean checkLibreSensorChangeIfEnabled(final String sn) {
        return Pref.getBoolean("detect_libre_sn_changes", true) && checkLibreSensorChange(sn);
    }

    public synchronized static boolean checkLibreSensorChange(final String currentSerial) {
        if ((currentSerial == null) || currentSerial.length() < 4) return false;
        final String lastSn = PersistentStore.getString(PREF_LIBRE_SN);
        if (!currentSerial.equals(lastSn)) {
            final Sensor this_sensor = Sensor.currentSensor();
            if ((lastSn.length() > 3) && (this_sensor != null)) {

                final String last_uuid = PersistentStore.getString(PREF_LIBRE_SENSOR_UUID);

                if (last_uuid.equals(this_sensor.uuid)) {
                    if (last_uuid.length() > 3) {
                        UserError.Log.wtf(TAG, String.format("Different sensor serial number for same sensor uuid: %s :: %s vs %s", last_uuid, lastSn, currentSerial));
                        Sensor.stopSensor();
                        JoH.static_toast_long("Stopping sensor due to serial number change");
                        Sensor.stopSensor();
                        return true;
                    }
                } else {
                    PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, this_sensor.uuid);
                }
            }
            PersistentStore.setString(PREF_LIBRE_SN, currentSerial);
        }
        return false;
    }

}
