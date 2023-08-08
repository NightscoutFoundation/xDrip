package com.eveningoutpost.dexdrip.processing.sgfilter;

import com.eveningoutpost.dexdrip.models.UserError;

import java.security.InvalidParameterException;


/**
 * JamOrHam
 * <p>
 * Low Preserving Post Processor
 */
public class LowPreserver implements Postprocessor {

    private static final String TAG = LowPreserver.class.getSimpleName();
    private static final double BASE_FALLOFF = 0.2;
    private static final double LOW_VALUE_IN_MGDL = 72;
    private static final boolean D = false;

    /**
     * Decay Falloff.
     */
    double falloff;

    /**
     * The Low Threshold.
     */
    double threshold;

    /**
     * Where low starts
     */

    int lStart;

    /**
     * Current index position (accessed via synchronized)
     */
    int i;

    /**
     * Instantiates a new Low Preserving processor.
     *
     * @param multiplier the data size multiplier
     * @param lowCutOff  the low cut off
     */
    public LowPreserver(final double multiplier, final double lowCutOff) {
        falloff = BASE_FALLOFF / multiplier;
        threshold = lowCutOff;
    }

    /**
     * Instantiates a new Low preserver with specified multiplier and default threshold
     *
     * @param multiplier the multiplier
     */
    public LowPreserver(final double multiplier) {
        this(multiplier, LOW_VALUE_IN_MGDL);
    }

    /**
     * Instantiates a new Low Preserver processor with default multiplier
     */
    public LowPreserver() {
        this(1);
    }

    @Override
    public synchronized void apply(final double[] data, final double[] alt_data) {
        if (data == null || alt_data == null || alt_data.length == 0) return;
        if (data.length != alt_data.length) {
            throw new InvalidParameterException("Data and Alt Data must be the same size");
        }

        lStart = -1;
        for (i = 0; i < alt_data.length; i++) {
            if (alt_data[i] <= threshold) {
                if (lStart == -1) {
                    if (D) UserError.Log.d(TAG, "Low start at " + i);
                    lStart = i;
                }
            } else {
                evaluate(data, alt_data, i);
            }
        }
        evaluate(data, alt_data, i);
    }

    /**
     * Evaluate.
     *
     * @param data     the data
     * @param alt_data the alt data
     * @param i        the
     */
    void evaluate(final double[] data, final double[] alt_data, final int i) {
        if (lStart != -1) {
            if (D) UserError.Log.d(TAG, "Low end at " + i);
            ripple(data, alt_data, lStart + (i - 1 - lStart) / 2);
            lStart = -1;
        }
    }

    /**
     * Ripple.
     *
     * @param data     the data
     * @param alt_data the alt data
     * @param target   the target
     */
    void ripple(final double[] data, final double[] alt_data, int target) {
        if (D) UserError.Log.d(TAG, "Ripple target " + target);
        double epsilon = 1d;
        for (int i = target; i < data.length; i++) {
            data[i] = (alt_data[i] * epsilon + data[i] * (1d - epsilon));
            epsilon -= falloff;
            if (epsilon < 0d) break;
        }
        epsilon = 1d;
        for (int i = target; i >= 0; i--) {
            data[i] = (alt_data[i] * epsilon + data[i] * (1d - epsilon));
            epsilon -= falloff;
            if (epsilon < 0d) break;
        }
    }
}
