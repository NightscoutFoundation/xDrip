package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link Anticipate}
 *
 * @author Asbjørn Aarrestad - 2019.06 - asbjorn@aarrestad.com
 */
public class AnticipateTest {

    @Test
    public void next_simpleMode() {
        // :: Setup
        long now = 10_000;
        int period = 1_000;
        long last = now - 1_000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(now + 5);
    }

    @Test
    public void next_lateCall() {
        // :: Setup
        long now = 10_000;
        long last = now - 5_000;
        int period = 1_000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(now + 5);
    }

    @Test
    public void next_waitPeriod() {
        // :: Setup
        long now = 10_000;
        long last = now - 5;
        int period = 1_000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(last + period - grace);
    }

    @Test
    public void next_waitPeriod2() {
        // :: Setup
        long now = 10_000;
        long last = now - 15;
        int period = 1_000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(last + period - grace);
    }

    // ===== Batch-verifications ===================================================================
    @Test
    public void next_betweenGraceAndAPeriodSinceLastReading() {
        // :: Setup
        long now = 10_000;
        long period = 1_000;
        int grace = 5;

        int testCount = 0;

        // :: Act
        for (long last = now - 3 * grace; last >= now - period; last -= grace) {
            long next = Anticipate.next(now, last, period, grace);

            // :: Verify
            assertWithMessage("last: " + last)
                    .that(next)
                    .isEqualTo(Math.max(last + period - grace, now + grace));

            testCount++;
        }

        assertThat(testCount).isEqualTo(198);
    }

    @Test
    public void next_moreThanTwoPeriodsSinceLastReading() {
        // :: Setup
        long now = 10_000;
        long period = 1_000;
        long grace = 5;

        int testCount = 0;

        // :: Act
        for (long last = now - 3 * grace; last >= now - 5 * period; last -= (grace * 3)) {
            long next = Anticipate.next(now, last, period, grace);

            // :: Verify
            assertWithMessage("last: " + last + " next is at least")
                    .that(next)
                    .isAtLeast(now + grace);
            assertWithMessage("last: " + last + " next is at most")
                    .that(next)
                    .isAtMost(now + period);

            long restOfWholePeriod = Math.abs((next - last) % period);
            if (restOfWholePeriod > period / 2) {
                restOfWholePeriod -= period;
            }
            assertWithMessage("last: " + last + " is around modulus grace")
                    .that(restOfWholePeriod)
                    .isAtMost(2 * grace);

            testCount++;
        }

        assertThat(testCount).isEqualTo(333);
    }

    // ===== Real world examples ===================================================================
    @Test
    public void next_realWorld1() {
        // :: Setup
        // 2019-06-38 11:18:49
        long now = 1_561_893_529_622L;
        // 2019-06-30 11:13:29
        long last = 1_561_893_209_000L;
        // 5 minutes
        long period = 300_000L;
        // 10 seconds
        long grace = 10_000;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        // 2019-06-30 11:18:59 (now + grace)
        assertThat(next).isEqualTo(1_561_893_799_000L);
    }
}