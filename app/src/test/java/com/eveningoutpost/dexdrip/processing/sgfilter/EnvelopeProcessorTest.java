package com.eveningoutpost.dexdrip.processing.sgfilter;

import org.junit.Test;

import lombok.val;

import static com.google.common.truth.Truth.assertWithMessage;

public class EnvelopeProcessorTest {

    @Test
    public void applyTest1() {

        val x = new EnvelopeProcessor();

        val original = new double[]{5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d};
        val current = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        val expected = new double[]{5.0, 4.6, 4.2, 3.8000000000000003, 3.4000000000000004, 3.0000000000000004, 2.6000000000000005, 2.2000000000000006, 1.8000000000000005, 1.4000000000000006, 1.4000000000000006, 1.8000000000000005, 2.2000000000000006, 2.6000000000000005, 3.0000000000000004, 3.4000000000000004, 3.8000000000000003, 4.2, 4.6, 5.0};

        x.apply(current, original);

        assertWithMessage("env test 1").that(current).isEqualTo(expected);
    }

    @Test
    public void applyTest2() {

        val x = new EnvelopeProcessor(0.5);

        val original = new double[]{5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d, 5d};
        val current = new double[]{1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d};
        val expected = new double[]{5.0, 4.2, 3.4000000000000004, 2.6000000000000005, 1.8000000000000003, 1.0000000000000002, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0000000000000002, 1.8000000000000003, 2.6000000000000005, 3.4000000000000004, 4.2, 5.0};

        x.apply(current, original);

        assertWithMessage("env test 2").that(current).isEqualTo(expected);
    }

    @Test
    public void applyTest3() {

        val x = new EnvelopeProcessor(0.5);

        val original = new double[]{5d, 5d, 5d};
        val current = new double[]{1d, 1d, 1d};
        val expected = new double[]{5.0, 4.2, 5.0};

        x.apply(current, original);

        assertWithMessage("env test 3").that(current).isEqualTo(expected);
    }

    @Test
    public void applyTest4() {

        val x = new EnvelopeProcessor(0.5);

        val original = new double[]{5d, 5d};
        val current = new double[]{1d, 1d};
        val expected = new double[]{5.0, 5.0};

        x.apply(current, original);

        assertWithMessage("env test 4").that(current).isEqualTo(expected);
    }

    @Test
    public void applyTest5() {

        val x = new EnvelopeProcessor(0.5);

        val original = new double[]{5d};
        val current = new double[]{1d};
        val expected = new double[]{5.0};

        x.apply(current, original);

        assertWithMessage("env test 5").that(current).isEqualTo(expected);
    }

}