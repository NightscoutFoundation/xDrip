package com.eveningoutpost.dexdrip.calibrations;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * The idea here is for a standard class format which you can
 * extend to implement your own pluggable calibration algorithms
 * <p>
 * See FixedSlopeExample or Datricsae for examples on doing this
 */

public abstract class CalibrationAbstract {

    private static Map<String, CalibrationData> memory_cache = new HashMap<>();

    /* Overridable methods */

    // boolean responses typically indicate if anything received and processed the call
    // null return values mean unsupported or invalid

    // get the calibration data (caching is handled internally)

    public CalibrationData getCalibrationData() {
        // default no implementation
        return null;
    }


    // get calibration data at specific timestamp (more advanced)

    public CalibrationData getCalibrationDataAtTime(long timestamp) {
        // default no implementation
        return null;
    }

    // indicate that the cache should be invalidated as BG sample data has changed
    // or time has passed in a way we want to invalidate any existing cache

    public boolean invalidateCache() {
        // default no implementation
        return true;
    }


    // called when any new sensor data is available such as on every reading
    // this could be used to invalidate the cache if this extra data is used;
    public boolean newSensorData() {
        return false;
    }

    // called when any new sensor data is available within 20 minutes of last calibration
    // this could be used to invalidate the cache if this extra data is used;
    public boolean newCloseSensorData() {
        return false;
    }


    // called when new blood glucose data is available or there is a change in existing data
    // by default this invalidates the cache
    public boolean newFingerStickData() {
        return invalidateCache();
    }


    // the name of the alg - should be v.similar to its class name

    public String getAlgorithmName() {
        // default no implementation
        return null;
    }

    // a more detailed description of the basic idea behind the plugin

    public String getAlgorithmDescription() {
        // default no implementation
        return null;
    }


    /* Common utility methods */

    public String getNiceNameAndDescription() {
        String name = getAlgorithmName();
        String description = (name != null) ? getAlgorithmDescription() : "";
        return ((name != null) ? name : "None") + " - " + ((description != null) ? description : "");
    }


    // slower method but for ease of use when calculating a single value

    public double getGlucoseFromSensorValue(double raw) {
        final CalibrationData data = getCalibrationData();
        return getGlucoseFromSensorValue(raw, data);
    }


    // faster method when CalibrationData is passed - could be overridden for non-linear algs

    public double getGlucoseFromSensorValue(double raw, CalibrationData data) {
        if (data == null) return -1;
        return raw * data.slope + data.intercept;
    }

    // faster method when CalibrationData is passed - could be overridden for non-linear algs

    public double getGlucoseFromBgReading(BgReading bgReading, CalibrationData data) {
        if (data == null) return -1;
        if (bgReading == null) return -1;
        // algorithm can override to decide whether or not to be using age_adjusted_raw
        return bgReading.age_adjusted_raw_value * data.slope + data.intercept;
    }



    protected static CalibrationData jsonStringToData(String json) {
        try {
            return new Gson().fromJson(json, CalibrationData.class);
        } catch (Exception e) {
            return null;
        }
    }

    protected static String dataToJsonString(CalibrationData data) {
        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            return gson.toJson(data);
        } catch (NullPointerException e) {
            return "";
        }
    }

    protected static boolean saveDataToCache(String tag, CalibrationData data) {
        final String lookup_tag = "CalibrationDataCache-" + tag;
        memory_cache.put(lookup_tag, data);
        PersistentStore.setString(lookup_tag, dataToJsonString(data));
        return true;
    }

    protected static CalibrationData loadDataFromCache(String tag) {
        final String lookup_tag = "CalibrationDataCache-" + tag;
        if (!memory_cache.containsKey(lookup_tag)) {
            memory_cache.put(lookup_tag, jsonStringToData(PersistentStore.getString(lookup_tag)));
        }
        return memory_cache.get(lookup_tag);
    }

    /* Data Exchange Class */

    // for returning data to xDrip

    public class CalibrationData {
        @Expose
        public double slope;
        @Expose
        public double intercept;
        @Expose
        public long created;

        public CalibrationData(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
            this.created = JoH.tsl();
        }
    }
}
