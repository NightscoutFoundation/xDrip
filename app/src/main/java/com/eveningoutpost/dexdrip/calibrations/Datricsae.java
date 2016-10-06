package com.eveningoutpost.dexdrip.calibrations;

/**
 * Created by jamorham on 04/10/2016.
 *
 * Maintained by jamorham
 *
 * If you would like to modify a calibration plugin,
 * please create a new one and make the modifications there
 *
 */

public class Datricsae extends CalibrationAbstract {

    private static final String TAG = "Datricsae";

    @Override
    public String getAlgorithmName() {
        return TAG;
    }

    @Override
    public String getAlgorithmDescription() {
        return "pronounced: da-trix-ee - place holder only - do not use";
    }

    @Override
    public CalibrationData getCalibrationData() {
        // TODO an actual algorithm
        return new CalibrationData(1.6d, -100d);
    }
}
