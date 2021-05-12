package com.eveningoutpost.dexdrip.utils.math;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class RollingAverageTest {

    @Test
    public void instantTest() {

        final String[] results = {
                "Rolling Average: Size: 0/3 Average: 0.0    0:0.0 1:0.0 2:0.0 ",
                "Rolling Average: Size: 0/10 Average: 0.0    0:0.0 1:0.0 2:0.0 3:0.0 4:0.0 5:0.0 6:0.0 7:0.0 8:0.0 9:0.0 "
        };

        final RollingAverage avg1 = new RollingAverage(3);
        assertWithMessage("inst 0").that(avg1.toS()).isEqualTo(results[0]);
        final RollingAverage avg2 = new RollingAverage(10);
        assertWithMessage("inst 1").that(avg2.toS()).isEqualTo(results[1]);

    }

    @Test
    public void putTest() {

        final String results[] = {
                "Rolling Average: Size: 1/3 Average: 1.0    0:1.0 1:0.0 2:0.0 ",
                "Rolling Average: Size: 2/3 Average: 1.5    0:1.0 1:2.0 2:0.0 ",
                "Rolling Average: Size: 3/3 Average: 2.0    0:1.0 1:2.0 2:3.0 ",
                "Rolling Average: Size: 3/3 Average: 3.0    0:4.0 1:2.0 2:3.0 ",
                "Rolling Average: Size: 3/3 Average: 4.0    0:4.0 1:5.0 2:3.0 ",
                "Rolling Average: Size: 3/3 Average: 5.0    0:4.0 1:5.0 2:6.0 ",
                "Rolling Average: Size: 3/3 Average: 6.0    0:7.0 1:5.0 2:6.0 ",
                "Rolling Average: Size: 3/3 Average: 7.0    0:7.0 1:8.0 2:6.0 ",
                "Rolling Average: Size: 3/3 Average: 8.0    0:7.0 1:8.0 2:9.0 ",
                "Rolling Average: Size: 3/3 Average: 9.0    0:10.0 1:8.0 2:9.0 "
        };

        final RollingAverage avg1 = new RollingAverage(3);
        for (int i = 1; i <= 10; i++) {
            avg1.put(i);
            assertWithMessage("avg " + i).that(avg1.toS()).isEqualTo(results[i - 1]);
        }
    }

    @Test
    public void peakTest() {
        RollingAverage avg1 = new RollingAverage(2);
        assertWithMessage("peak 2").that(avg1.getPeak()).isEqualTo(0.5d);
        avg1 = new RollingAverage(3);
        assertWithMessage("peak 3").that(avg1.getPeak()).isEqualTo(1d);
        avg1 = new RollingAverage(4);
        assertWithMessage("peak 4").that(avg1.getPeak()).isEqualTo(1.5d);
        avg1 = new RollingAverage(5);
        assertWithMessage("peak 5").that(avg1.getPeak()).isEqualTo(2d);
    }

    @Test
    public void averageTest() {

        // TODO more comprehensive tests

    }
}