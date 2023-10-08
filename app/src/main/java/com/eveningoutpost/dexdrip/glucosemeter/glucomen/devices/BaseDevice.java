package com.eveningoutpost.dexdrip.glucosemeter.glucomen.devices;

import lombok.Getter;

/**
 * JamOrHam
 * GlucoMen device parameters base class
 */

public abstract class BaseDevice {

    @Getter
    protected int serialOffset = -1;
    @Getter
    protected int serialSize = 2;
    @Getter
    protected int indexOffset = -1;
    @Getter
    protected int indexSize = 1;
    @Getter
    protected int glucoseStart = -1;
    @Getter
    protected int glucoseSize = -1;
    @Getter
    protected int ketoneStart = -1;
    @Getter
    protected int ketoneSize = -1;

    @Getter
    protected boolean known = false;

    public int getGlucoseRecordOffset(final int record) {
        return glucoseStart + record * glucoseSize;
    }

    public int getKetoneRecordOffset(final int record) {
        return ketoneStart + record * ketoneSize;
    }

}
