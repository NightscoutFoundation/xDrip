package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
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

    // This function is intended to be used by unit tests only.
    public static void clearEnviorment() {
        PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, "");
        PersistentStore.setString(PREF_LIBRE_SN, "");
    }
    
    public static boolean checkLibreSensorChangeIfEnabled(final String sn) {
        if( Home.get_is_libre_whole_house_collector() && Sensor.currentSensor() != null) {
            Log.e(TAG, "Stopping sensor because in libre whold house coverage sensor must be stopped.");
            Sensor.stopSensor();
        }
        return Pref.getBoolean("detect_libre_sn_changes", true) && checkLibreSensorChange(sn);
    }

    // returns true in the case of an error (had to stop the sensor)
    public synchronized static boolean checkLibreSensorChange(final String currentSerial) {
        Log.i(TAG, "checkLibreSensorChange called currentSerial = " + currentSerial);
        if ((currentSerial == null) || currentSerial.length() < 4) return false;
        final Sensor this_sensor = Sensor.currentSensor();
        if(this_sensor == null || this_sensor.uuid == null|| this_sensor.uuid.length() < 4) {
            Log.i(TAG, "no senosr open, deleting everything");
            PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, "");
            PersistentStore.setString(PREF_LIBRE_SN, "");
            return false;
        }
        final String lastSn = PersistentStore.getString(PREF_LIBRE_SN);
        final String last_uuid = PersistentStore.getString(PREF_LIBRE_SENSOR_UUID);
        Log.i(TAG, "checkLibreSensorChange Initial values: lastSn = " + lastSn + " last_uuid = " + last_uuid);
        if(lastSn.length() < 4 || last_uuid.length() < 4) {
            Log.i(TAG, "lastSn or last_uuid not valid, writing current values.");
            PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, this_sensor.uuid);
            PersistentStore.setString(PREF_LIBRE_SN, currentSerial);
            return false;
        }
        // Here we have the data that we need to start verifying.
        if(lastSn.equals(currentSerial)) {
            if(this_sensor.uuid.equals(last_uuid)) {
                // all well
                Log.i(TAG, "checkLibreSensorChange returning false 1");
                return false;
            } else {
                // This is the case that the user had a sensor, but he stopped it and started a new one in xDrip.
                // This is probably ok, since physical sensor has not changed.
                // we learn the new uuid and continue.
                Log.e(TAG, "A new xdrip sensor was found, updating state.");
                PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, this_sensor.uuid);
                return false;
            }
        } else {
            // We have a new sensorid. So physical sensors have changed.
            // verify uuid have also changed.
            if(this_sensor.uuid.equals(last_uuid)) {
                // We need to stop the sensor.
                Log.e(TAG, String.format("Different sensor serial number for same sensor uuid: %s :: %s vs %s", last_uuid, lastSn, currentSerial));
                Sensor.stopSensor();
                JoH.static_toast_long("Stopping sensor due to serial number change");
                // There is no open sensor now.
                PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, "");
                PersistentStore.setString(PREF_LIBRE_SN, "");
                return true;

            } else {
                // This is the first time we see this sensor, update our uuid and current serial.
                Log.i(TAG, "This is the first time we see this sensor uuid = " +  this_sensor.uuid);
                PersistentStore.setString(PREF_LIBRE_SENSOR_UUID, this_sensor.uuid);
                PersistentStore.setString(PREF_LIBRE_SN, currentSerial);
                return false;
            }
        }
    }

}
