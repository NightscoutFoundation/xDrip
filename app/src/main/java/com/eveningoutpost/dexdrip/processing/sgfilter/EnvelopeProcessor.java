package com.eveningoutpost.dexdrip.processing.sgfilter;

import java.security.InvalidParameterException;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Mirrored Decay Envelope Post Processor
 */

public class EnvelopeProcessor implements Postprocessor {

    private static final double BASE_FALLOFF = 0.1;

    /**
     * Decay Falloff.
     */
    double falloff;

    /**
     * Instantiates a new Envelope processor.
     *
     * @param multiplier the data size multiplier
     */
    public EnvelopeProcessor(final double multiplier) {
        falloff = BASE_FALLOFF / multiplier;
    }

    /**
     * Instantiates a new Envelope processor with default multiplier
     */
    public EnvelopeProcessor() {
        this(1);
    }

    @Override
    public void apply(final double[] data, final double[] alt_data) {
        if (data == null || alt_data == null || alt_data.length == 0) return;
        if (data.length != alt_data.length) {
            throw new InvalidParameterException("Data and Alt Data must be the same size");
        }
        val midpoint = alt_data.length / 2;
        double epsilon = 1d;
        for (int i = 0; i < midpoint; i++) {
            data[i] = (alt_data[i] * epsilon + data[i] * (1d - epsilon));
            epsilon -= falloff;
            if (epsilon < 0d) break;
        }
        epsilon = 1d;
        for (int i = data.length - 1; i >= midpoint; i--) {
            data[i] = (alt_data[i] * epsilon + data[i] * (1d - epsilon));
            epsilon -= falloff;
            if (epsilon < 0d) break;
        }
    }
}
