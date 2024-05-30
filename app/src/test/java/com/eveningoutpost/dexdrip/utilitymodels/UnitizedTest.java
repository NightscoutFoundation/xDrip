package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

public class UnitizedTest {

    @Test
    public void mmolConvertTest() {
        assertWithMessage("to mmol").that(Unitized.mmolConvert(99.1)).isWithin(0.001).of(5.5);
    }

    @Test
    public void mgdlConvertTest() {
        assertWithMessage("to mgdl").that(Unitized.mgdlConvert(5.5)).isWithin(0.001).of(99.1);
    }
}