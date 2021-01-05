package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Dex_ConstantsTest extends Dex_Constants {

    @Test
    public void trendTest () {
        assertEquals(TREND_ARROW_VALUES.NONE, TREND_ARROW_VALUES.getTrend(43));
        assertEquals(TREND_ARROW_VALUES.DOUBLE_UP, TREND_ARROW_VALUES.getTrend(38));
        assertEquals(TREND_ARROW_VALUES.FLAT, TREND_ARROW_VALUES.getTrend(0.9));
        assertEquals(TREND_ARROW_VALUES.DOUBLE_DOWN, TREND_ARROW_VALUES.getTrend(-3.5));
    }

}