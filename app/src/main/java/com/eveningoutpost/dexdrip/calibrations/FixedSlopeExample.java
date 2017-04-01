package com.eveningoutpost.dexdrip.calibrations;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by jamorham on 04/10/2016.
 * <p>
 * Just an example dummy calibration algorithm to illustrate a basic starting point
 * This will be extremely inaccurate if used to interpret real data as there is
 * no actual calibration occurring.
 */

public class FixedSlopeExample extends CalibrationAbstract {

    private static final String TAG = xdrip.getAppContext().getString(R.string.fixed_slope_tag);

    @Override
    public String getAlgorithmName() {
        return TAG;
    }

    @Override
    public String getAlgorithmDescription() {
        return xdrip.getAppContext().getString(R.string.fixed_slope_description);
    }

    @Override
    public CalibrationData getCalibrationData(long until) {
        return new CalibrationData(1.08d, -5.0d);
    }
}
