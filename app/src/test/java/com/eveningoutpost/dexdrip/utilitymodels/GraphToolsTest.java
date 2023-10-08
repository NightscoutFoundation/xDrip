package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.utilitymodels.GraphTools.interpolateCalculatedValue;
import static com.google.common.truth.Truth.assertWithMessage;


public class GraphToolsTest extends RobolectricTestWithConfig {

    @Test
    public void bestYPositionTest() {
    // TODO
    }

    @Test
    public void interpolateCalculatedValueTest() {

        final long startTime = 1531124821000L;
        final long endTime = startTime + (Constants.MINUTE_IN_MS * 10);

        final BgReading first = new BgReading();
        final BgReading second = new BgReading();

        first.timestamp = startTime;
        first.calculated_value = 100;

        second.timestamp = endTime;
        second.calculated_value = 200;

        assertWithMessage("time at first matches first").that(interpolateCalculatedValue(first,second, startTime)).isEqualTo(100.0);
        assertWithMessage("time at second matches second").that(interpolateCalculatedValue(first,second, endTime)).isEqualTo(200.0);
        assertWithMessage("time point 1 matches expected").that(interpolateCalculatedValue(first,second, startTime+(Constants.SECOND_IN_MS * 150))).isEqualTo(125.0);
        assertWithMessage("time point 2 matches expected").that(interpolateCalculatedValue(first,second, startTime+(Constants.SECOND_IN_MS * 300))).isEqualTo(150.0);
        assertWithMessage("time point 3 matches expected").that(interpolateCalculatedValue(first,second, startTime+(Constants.SECOND_IN_MS * 400))).isWithin(0.1).of(166.6);



    }
}