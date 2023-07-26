package com.eveningoutpost.dexdrip.calibrations;

import android.util.Log;

import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.Forecast.PolyTrendLine;
import com.eveningoutpost.dexdrip.models.Forecast.TrendLine;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an example calibration algorithm which takes the last 7 calibrations
 * and produces a best fit line through them (Linear regression)
 * <p>
 * It has no weighting but uses the age adjusted raw value for certain data sources
 * <p>
 * If there is insufficient calibration data then it defaults to the xDrip-Original
 * algorithm. So you will only see a divergence with 4 or more calibrations.
 * <p>
 * This algorithm has no sanity checks and so could easily produce wildly inaccurate slopes.
 * <p>
 * Created by jamorham on 04/10/2016.
 * <p>
 * Maintained by jamorham
 * <p>
 * If you would like to modify a calibration plugin,
 * please create a new one and make the modifications there
 */

public class LastSevenUnweightedA extends CalibrationAbstract {

    private static final String TAG = "Last7Ua";

    @Override
    public String getAlgorithmName() {
        return TAG;
    }

    @Override
    public String getAlgorithmDescription() {
        return "Unweighted last 7 calibrations. Example only";
    }

    @Override
    public CalibrationData getCalibrationData(long until) {

        // TODO cache must understand until
        //CalibrationData cd = loadDataFromCache(TAG);
        CalibrationData cd = null;
        if (cd == null) {

            // first is most recent
            final List<Calibration> calibrations = Calibration.latestValid(7, until);
            if ((calibrations == null) || (calibrations.size() == 0)) return null;
            // have we got enough data to have a go
            if (calibrations.size() < 4) {
                // just use whatever xDrip original would have come up with at this point
                Log.d(TAG, "Falling back to xDrip-original values");
                cd = new CalibrationData(calibrations.get(0).slope, calibrations.get(0).intercept);
            } else {
                // TODO sanity checks
                final TrendLine bg_to_raw = new PolyTrendLine(1);

                final List<Double> raws = new ArrayList<>();
                final List<Double> bgs = new ArrayList<>();
                final boolean adjust_raw = !DexCollectionType.hasLibre();
                for (Calibration calibration : calibrations) {
                    // sanity check?
                    // weighting!
                    final double raw = adjust_raw ? calibration.adjusted_raw_value : calibration.raw_value;
                    Log.d(TAG, "Calibration: " + raw + " -> " + calibration.bg);
                    raws.add(raw);
                    bgs.add(calibration.bg);
                }

                bg_to_raw.setValues(PolyTrendLine.toPrimitiveFromList(bgs), PolyTrendLine.toPrimitiveFromList(raws));
                Log.d(TAG, "Error Variance: " + bg_to_raw.errorVarience());
                final double intercept = bg_to_raw.predict(0);
                Log.d(TAG, "Intercept: " + intercept);
                final double one = bg_to_raw.predict(1);
                Log.d(TAG, "One: " + one);
                final double slope = one - intercept;
                Log.d(TAG, "Slope: " + slope);
                cd = new CalibrationData(slope, intercept);
            }
        }
        return cd; // null if invalid
    }

    @Override
    public boolean invalidateCache() {
        return saveDataToCache(TAG, null);
    }

    // this means we invalidate the cache close to a calibration if our alg
    // makes use of this updated data as the xDrip original algorithm does
    @Override
    public boolean newCloseSensorData() {
        return invalidateCache();
    }
}
