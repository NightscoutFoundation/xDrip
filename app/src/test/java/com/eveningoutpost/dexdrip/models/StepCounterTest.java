package com.eveningoutpost.dexdrip.models;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

/**
 * Tests for {@link StepCounter}
 */
public class StepCounterTest extends RobolectricTestWithConfig {

    @Test
    public void dailyTotalCountsOnlyRecordsSinceLocalMidnight() {
        final long midnight = StepCounter.startOfToday();
        StepCounter.createUniqueRecord(midnight - 1, 1000, true); // yesterday
        StepCounter.createUniqueRecord(midnight, 200, true);
        StepCounter.createUniqueRecord(midnight + 1, 300, true);

        assertThat(StepCounter.getDailyTotal()).isEqualTo(500);
    }
}
