package com.eveningoutpost.dexdrip.processing;

import com.eveningoutpost.dexdrip.Models.BgReading;

import java.util.List;

/**
 * JamOrHam
 *
 * BaseSmoother class, simply extend this class and override methods you want to support
 *
 */
public abstract class BaseSmoother implements JSmoother {

    public List<BgReading> smoothBgReadings(final List<BgReading> readings) {
        return readings; // Default implementation does nothing
    }

}
