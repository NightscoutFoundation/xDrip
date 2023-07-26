package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.profileeditor.BasalRepository;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

public class ProfileTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        BasalRepository.dummyRatesForTesting();
    }

    @Before
    public void after() {
        BasalRepository.clearRates();
    }


    @Test
    public void getBasalRateTest() {
    }

    @Test
    public void getBasalRateAbsoluteFromPercentTest() {
        assertWithMessage("25% == 0.5 of 2.0").that(Profile.getBasalRatePercentFromAbsolute(1651949922000L, 0.5d)).isEqualTo(25);
    }

    @Test
    public void getBasalRatePercentFromAbsoluteTest() {
       assertWithMessage("50% == 1.0U").that(Profile.getBasalRateAbsoluteFromPercent(1651949922000L, 50)).isEqualTo(1.0d);
    }
}