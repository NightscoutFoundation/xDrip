package com.eveningoutpost.dexdrip.G5Model;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class RawScalingTest {

    @Test
    public void scaleTest() {
        assertWithMessage("G5 1").that((int) RawScaling.scale(10000, RawScaling.DType.G5, false)).isEqualTo(10000);

        assertWithMessage("G6v1 1").that((int) RawScaling.scale(294, RawScaling.DType.G6v1, false)).isEqualTo(9996);

        assertWithMessage("G6v2 1").that((int) RawScaling.scale(1168582904, RawScaling.DType.G6v2, false)).isEqualTo(155299);
    }

    @Test
    public void scale1Test() {

        // TODO lookup using preferences

    }
}