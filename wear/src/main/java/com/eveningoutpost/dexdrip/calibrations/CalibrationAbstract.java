package com.eveningoutpost.dexdrip.calibrations;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
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

    private static final double HIGHEST_SANE_INTERCEPT = 39; // max value that intercept can be with positive slope

    /* Overridable methods */

    // boolean responses typically indicate if anything received and processed the call
    // null return values mean unsupported or invalid

    // get the calibration data (caching is handled internally)

    public synchronized CalibrationData getCalibrationData() {
        return getCalibrationData(JoH.tsl() + Constants.HOUR_IN_MS);
    }

    public synchronized CalibrationData getCalibrationData(long until) {
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
    // by default this invalidates the caches
    public boolean newFingerStickData() {
        return PluggableCalibration.invalidateAllCaches();
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
        if (!isCalibrationSane(data)) return -1;
        return raw * data.slope + data.intercept;
    }

    // faster method when CalibrationData is passed - could be overridden for non-linear algs

    public double getGlucoseFromBgReading(BgReading bgReading, CalibrationData data) {
        if (data == null) return -1;
        if (bgReading == null) return -1;
        if (!isCalibrationSane(data)) {
            recordSanityFailure(data);
            return -1;
        }
        // algorithm can override to decide whether or not to be using age_adjusted_raw
        return bgReading.age_adjusted_raw_value * data.slope + data.intercept;
    }

    public BgReading getBgReadingFromBgReading(BgReading bgReading, CalibrationData data) {
        if (data == null) return null;
        if (bgReading == null) return null;
        // do we need deep clone?
        final BgReading new_bg = (BgReading) JoH.cloneObject(bgReading);
        if (new_bg == null) return null;
        // algorithm can override to decide whether or not to be using age_adjusted_raw
        new_bg.calculated_value = getGlucoseFromBgReading(bgReading, data);
        new_bg.filtered_calculated_value = getGlucoseFromFilteredBgReading(bgReading, data);
        return new_bg;
    }

    public double getGlucoseFromFilteredBgReading(BgReading bgReading, CalibrationData data) {
        if (data == null) return -1;
        if (bgReading == null) return -1;
        if (!isCalibrationSane(data)) {
            recordSanityFailure(data);
            return -1;
        }
        // algorithm can override to decide whether or not to be using age_adjusted_raw
        return bgReading.ageAdjustedFiltered_fast() * data.slope + data.intercept;
    }

    public boolean isCalibrationSane() {
        return isCalibrationSane(JoH.tsl());
    }

    public boolean isCalibrationSane(final long until) {
        final CalibrationData data = getCalibrationData(until);
        return isCalibrationSane(data);
    }

    // Check that intercept is less than the highest allowed value.
    // This does not cater for negative slopes but they are not used by any algorithm at this point.
    public boolean isCalibrationSane(final CalibrationData data) {
        return data != null && !(data.intercept > HIGHEST_SANE_INTERCEPT);
    }

    private void recordSanityFailure(final CalibrationData pcalibration) {
        if (JoH.pratelimit("best-sanity-failure", 600)) {
            UserError.Log.wtf(getAlgorithmName(), "Unable to produce data due to plugin failing sanity check: " + pcalibration.toS());
        }
    }

    // TODO there currently is no way to opt out of this sanity check and for performance reasons
    // TODO we have to be careful about how one is implemented if it is needed.
    public static double getHighestSaneIntercept() {
        return HIGHEST_SANE_INTERCEPT;
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

    // persistent old style cache
    protected static boolean saveDataToCache(String tag, CalibrationData data) {
        final String lookup_tag = "CalibrationDataCache-" + tag;
        memory_cache.put(lookup_tag, data);
        PersistentStore.setString(lookup_tag, dataToJsonString(data));
        return true;
    }

    // memory only cache
    protected static boolean clearMemoryCache() {
        memory_cache.clear();
        return true;
    }

    // memory only cache - TODO possible room for improvement using timestamp as well
    protected static boolean saveDataToCache(String tag, CalibrationData data, long timestamp, long last_calibration) {
        final String lookup_tag = tag + last_calibration;
        memory_cache.put(lookup_tag, data);
        return true;
    }

    // memory only cache
    protected static CalibrationData loadDataFromCache(String tag, long timestamp) {
        final String lookup_tag = tag + timestamp;
        return memory_cache.get(lookup_tag);
    }

    // persistent old style cache
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

        public String toS() {
            return JoH.defaultGsonInstance().toJson(this);
        }
    }
}
