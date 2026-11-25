package com.eveningoutpost.dexdrip.models;

import static com.google.common.truth.Truth.assertThat;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link StepCounter}
 */
public class StepCounterTest extends RobolectricTestWithConfig {

    @Before
    public void clearRecords() {
        // The database is kept between the tests of this class.
        new Delete().from(StepCounter.class).execute();
    }

    @Test
    public void dailyTotalCountsOnlyRecordsSinceLocalMidnight() {
        final long midnight = StepCounter.startOfToday();
        StepCounter.createUniqueRecord(midnight - 1, 1000, true); // yesterday
        StepCounter.createUniqueRecord(midnight, 200, true);
        StepCounter.createUniqueRecord(midnight + 1, 300, true);

        assertThat(StepCounter.getDailyTotal()).isEqualTo(500);
    }

    @Test
    public void dailyTotalIsZeroRightAfterMidnight() {
        final long midnight = StepCounter.startOfToday();
        StepCounter.createUniqueRecord(midnight - 2, 1000, true); // yesterday, absolute
        StepCounter.createEfficientRecord(midnight - 1, 4000); // yesterday, device counter

        assertThat(StepCounter.getDailyTotal()).isEqualTo(0);
    }

    @Test
    public void dailyTotalFallsBackToDeviceCounterFromToday() {
        final long midnight = StepCounter.startOfToday();
        StepCounter.createEfficientRecord(midnight - 1, 4000); // yesterday
        StepCounter.createEfficientRecord(midnight + 1, 700); // today

        assertThat(StepCounter.getDailyTotal()).isEqualTo(700);
    }
}
