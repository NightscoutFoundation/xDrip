package com.eveningoutpost.dexdrip.processing;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.processing.sgfilter.ContinuousPadder;
import com.eveningoutpost.dexdrip.processing.sgfilter.EnvelopeProcessor;
import com.eveningoutpost.dexdrip.processing.sgfilter.LowPreserver;
import com.eveningoutpost.dexdrip.processing.sgfilter.SGFilter;

import java.util.Collections;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * The Goto Smoother which implements a Savitzky-Golay filter with contiguous sequence splitting,
 * continuous edge padding, low value preservation and decay envelope post processing.
 */

public class GotoSmoother extends BaseSmoother {

    private static final String TAG = GotoSmoother.class.getSimpleName();
    private static final boolean D = false;

    private SGFilter filter;
    private double[] coefficients;
    private int multiplier = 1;
    private final long period;

    /**
     * Sg filter double [ ].
     *
     * @param data the data
     * @return the double [ ]
     */
    public double[] sgFilter(final double[] data) {
        val padding = filter.getNl();
        val empty = new double[padding];
        if (D) UserError.Log.d(TAG, "Padding size on filter: " + padding);
        return filter.smooth(data, empty, empty, 0, new double[][]{coefficients});
    }

    /**
     * Instantiates a new Goto smoother.
     *
     * @param typicalSamplePeriod the typical sample period
     */
    public GotoSmoother(final long typicalSamplePeriod) {
        this.period = typicalSamplePeriod;
        multiplier = getMultiplier(typicalSamplePeriod);
        setFilter(5 * multiplier);
    }

    private GotoSmoother() {
        throw new RuntimeException("Don't use default constructor");
    }

    private static int getMultiplier(final long period) {
        return (int) (BgGraphBuilder.DEXCOM_PERIOD / period);
    }

    /**
     * Sets filter.
     *
     * @param distance the distance
     */
    void setFilter(final int distance) {
        coefficients = SGFilter.computeSGCoefficients(distance, distance, 2);
        filter = new SGFilter(distance, distance);
        filter.appendPreprocessor(new ContinuousPadder());
        filter.appendPostprocessor(new LowPreserver(this.multiplier, getLowMarkInMgDl()));
        filter.appendPostprocessor(new EnvelopeProcessor(this.multiplier));
    }

    /**
     * Extract double array double [ ].
     *
     * @param readings the readings
     * @param start    the start
     * @param length   the length
     * @return the double [ ]
     */
    static double[] extractDoubleArray(final List<BgReading> readings, final int start, final int length) {
        if (readings == null) return null;
        val data = new double[length];
        for (int i = 0; i < length; i++) {
            data[i] = (float) readings.get(i + start).calculated_value;
        }
        return data;
    }

    /**
     * Repatriate doubles.
     *
     * @param readings the readings
     * @param start    the start
     * @param length   the length
     * @param data     the data
     */
    static void repatriateDoubles(final List<BgReading> readings, final int start, final int length, final double[] data) {
        if (readings == null) return;
        if (length != data.length) {
            throw new ArrayIndexOutOfBoundsException("repatriate data length doesn't match specified length");
        }
        for (int i = 0; i < length; i++) {
            readings.get(start + i).calculated_value = data[i];
        }
    }

    /**
     * Smooth partial reading list.
     *
     * @param readings the readings
     * @param start    the start
     * @param length   the length
     */
    void smoothPartialReadingList(final List<BgReading> readings, final int start, final int length) {
        if (readings == null) return;
        if (D) UserError.Log.d(TAG, "Got smooth partial input length " + length + " @ " + start);
        if (length <= (3 * multiplier)) return;
        repatriateDoubles(readings, start, length, sgFilter(extractDoubleArray(readings, start, length)));
    }

    /**
     * Find contiguous count of sequential data elements.
     *
     * @param readings the readings
     * @param start    the start
     * @return the count
     */
    int findContiguousCount(final List<BgReading> readings, final int start) {
        final long triggerPeriod = period + Constants.MINUTE_IN_MS;
        long lastGoodTimestamp = -1;
        int count = 0;
        for (int i = start; i < readings.size(); i++) {
            val r = readings.get(i);
            if ((lastGoodTimestamp != -1) && (Math.abs(r.timestamp - lastGoodTimestamp) > triggerPeriod)) {
                break;
            }
            lastGoodTimestamp = r.timestamp;
            count++;
        }
        if (D)
            UserError.Log.d(TAG, "Find cont from " + start + "/" + readings.size() + " yielded " + count);
        return count;
    }

    @Override
    public List<BgReading> smoothBgReadings(final List<BgReading> readings) {
        if (readings == null) return null;
        if (readings.size() == 0) return readings;
        // descending timestamps, eg newest first.
        Collections.sort(readings, (o1, o2) -> (Long.compare(o2.timestamp, o1.timestamp)));

        int start = 0;
        while (start < readings.size()) {
            int length = findContiguousCount(readings, start);
            smoothPartialReadingList(readings, start, length);
            start += length;
        }
        return readings; // modifications are in place
    }

}
