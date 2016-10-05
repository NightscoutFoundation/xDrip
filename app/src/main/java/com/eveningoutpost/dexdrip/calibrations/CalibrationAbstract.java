package com.eveningoutpost.dexdrip.calibrations;

/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * The idea here is for a standard class format which you can
 * extend to implement your own pluggable calibration algorithms
 * <p>
 * See FixedSlopeExample or Datricsae for examples on doing this
 */

public abstract class CalibrationAbstract {

    /* Overridable methods */

    // get the calibration data (caching is handled internally)

    public CalibrationData getCalibrationData() {
        // default no implementation
        return null;
    }


    // indicate that the cache should be invalidated as BG sample data has changed
    // or time has passed in a way we want to invalidate any existing cache

    public boolean invalidateCache() {
        // default no implementation
        return true;
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

    // TODO caching helper


    /* Data Exchange Class */

    // for returning data to xDrip

    public class CalibrationData {
        public double slope;
        public double intercept;

        public CalibrationData(double slope, double intercept) {
            this.slope = slope;
            this.intercept = intercept;
        }
    }
}
