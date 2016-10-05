package com.eveningoutpost.dexdrip.calibrations;

import com.eveningoutpost.dexdrip.Models.Calibration;

/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * This mirrors the standard xDrip calibration algorithm
 * in the plugin format. Beware it doesn't reimplement it,
 * only pulls the values from the standard database.
 */

public class XDripOriginal extends CalibrationAbstract {

    private static final String TAG = "xDrip Original";

    @Override
    public String getAlgorithmName() {
        return TAG;
    }

    @Override
    public String getAlgorithmDescription() {
        return " classic algorithm";
    }

    @Override
    public CalibrationData getCalibrationData() {

        final Calibration calibration = Calibration.lastValid();
        if (calibration == null) return null;
        return new CalibrationData(calibration.slope, calibration.intercept);
    }
}
