package com.eveningoutpost.dexdrip.UtilityModels;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.FUZZER;

import com.eveningoutpost.dexdrip.BuildConfig;

import lecho.lib.hellocharts.model.PointValue;
import lombok.AllArgsConstructor;

/**
 * JamOrHam
 * <p>
 * Due to range issues with float we need to wrap around a friendly origin timestamp expressing
 * but storing real value in a bigger primitive type while maintaining compatibility with libraries
 */

@AllArgsConstructor
public class HPointValue extends PointValue {

    private static final long OFFSET = (BuildConfig.buildTimestamp - Constants.MONTH_IN_MS) / FUZZER;

    private double x;
    private float y;

    @Override
    public float getX() {
        return convert(this.x);
    }

    @Override
    public float getY() {
        return this.y;
    }

    public HPointValue set(double x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public long getTimeStamp() {
        return (long) (x * FUZZER);
    }

    public static float convert(final double x) {
        return (float) (x - OFFSET);
    }

    public static double unconvert(final float x) {
        return x + OFFSET;
    }

}
