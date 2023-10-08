package com.eveningoutpost.dexdrip.processing;

import com.eveningoutpost.dexdrip.models.BgReading;

import java.util.List;

/**
 * The interface for xDrip data smoothers to implement
 *
 * Simply extend the BaseSmoother class to ensure default implementations of any extension of this
 * interface.
 */

public interface JSmoother {

    /**
     * Smooth bg readings list. Modifications are made in place
     *
     * @param readings the readings
     * @return the list
     */

    List<BgReading> smoothBgReadings(final List<BgReading> readings);
}
