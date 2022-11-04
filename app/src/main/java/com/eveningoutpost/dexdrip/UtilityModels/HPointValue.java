package com.eveningoutpost.dexdrip.UtilityModels;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.FUZZER;

import lecho.lib.hellocharts.model.PointValue;
import lombok.AllArgsConstructor;

/**
 * JamOrHam
 * <p>
 * Handle de-fuzzing and legacy calls
 */

@AllArgsConstructor
public class HPointValue extends PointValue {

    public HPointValue set(double x, double y) {
        super.set(x, y);
        return this;
    }

    public HPointValue(double x, float y) {
        super(x, y);
    }

    public HPointValue(double x, double y) {
        super(x, y);
    }

    public long getTimeStamp() {
        return (long) (getX() * FUZZER);
    }

}
