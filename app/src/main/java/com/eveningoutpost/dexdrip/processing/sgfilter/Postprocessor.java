package com.eveningoutpost.dexdrip.processing.sgfilter;

/**
 * The interface Postprocessor.
 */
public interface Postprocessor {

    /**
     * Data processing method. Called on Postprocessor instance when its
     * processing is needed
     *
     * @param data     the data
     * @param alt_data the alt data
     */
    void apply(double[] data, double[] alt_data);
}
