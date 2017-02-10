package com.eveningoutpost.dexdrip.calibrations;

/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * Just an example dummy calibration algorithm to illustrate a basic starting point
 * This will be extremely inaccurate if used to interpret real data as there is
 * no actual calibration occurring.
 */

public class FixedSlopeExample extends CalibrationAbstract {

    private static final String TAG = "Fixed Slope Example";

    @Override
    public String getAlgorithmName() {
        return TAG;
    }

    @Override
    public String getAlgorithmDescription() {
        return "never use for real data";
    }

    @Override
    public CalibrationData getCalibrationData(long until) {
        return new CalibrationData(1.08d, -5.0d);
    }
}
