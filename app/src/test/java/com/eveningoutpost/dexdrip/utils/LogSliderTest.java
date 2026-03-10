package com.eveningoutpost.dexdrip.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class LogSliderTest {

    private static final double TOLERANCE = 0.01;

    // -- Boundary positions return boundary values --

    @Test
    public void atSliderStart_returnsValueStart() {
        double result = LogSlider.calc(0, 100, 1, 100, 0);
        assertWithMessage("slider at start position")
                .that(result).isWithin(TOLERANCE).of(1.0);
    }

    @Test
    public void atSliderEnd_returnsValueEnd() {
        double result = LogSlider.calc(0, 100, 1, 100, 100);
        assertWithMessage("slider at end position")
                .that(result).isWithin(TOLERANCE).of(100.0);
    }

    // -- Midpoint is geometric mean (log-interpolation property) --

    @Test
    public void atMidpoint_returnsGeometricMean() {
        // Geometric mean of 1 and 100 = sqrt(1*100) = 10
        double result = LogSlider.calc(0, 100, 1, 100, 50);
        assertWithMessage("midpoint should be geometric mean")
                .that(result).isWithin(0.5).of(10.0);
    }

    // -- Monotonically increasing --

    @Test
    public void higherPosition_givesHigherValue() {
        double low = LogSlider.calc(0, 100, 1, 1000, 25);
        double mid = LogSlider.calc(0, 100, 1, 1000, 50);
        double high = LogSlider.calc(0, 100, 1, 1000, 75);
        assertThat(low).isLessThan(mid);
        assertThat(mid).isLessThan(high);
    }

    // -- Works with non-zero slider start --

    @Test
    public void nonZeroSliderStart_mapsCorrectly() {
        double atStart = LogSlider.calc(10, 50, 1, 100, 10);
        double atEnd = LogSlider.calc(10, 50, 1, 100, 50);
        assertWithMessage("at start").that(atStart).isWithin(TOLERANCE).of(1.0);
        assertWithMessage("at end").that(atEnd).isWithin(TOLERANCE).of(100.0);
    }

    // -- Small value range --

    @Test
    public void smallValueRange_interpolatesCorrectly() {
        double result = LogSlider.calc(0, 10, 5, 10, 0);
        assertWithMessage("start of small range")
                .that(result).isWithin(TOLERANCE).of(5.0);
    }
}
