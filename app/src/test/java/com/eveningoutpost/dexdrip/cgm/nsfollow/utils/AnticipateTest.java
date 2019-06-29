package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Anticipate}
 *
 * @author Asbjørn Aarrestad - 2019.06 - asbjorn@aarrestad.com
 */
public class AnticipateTest {

    @Test
    public void next_simpleMode() {
        // :: Setup
        long now = 10000;
        long last = now - 1000;
        int period = 1000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(now + 5);
    }

    @Test
    public void next_lateCall() {
        // :: Setup
        long now = 10000;
        long last = now - 5000;
        int period = 1000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(now + 5);
    }

    @Test
    public void next_waitPeriod() {
        // :: Setup
        long now = 10000;
        long last = now - 5;
        int period = 1000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(last + period - grace);
    }

    @Test
    public void next_waitPeriod2() {
        // :: Setup
        long now = 10000;
        long last = now - 15;
        int period = 1000;
        int grace = 5;

        // :: Act
        long next = Anticipate.next(now, last, period, grace);

        // :: Verify
        assertThat(next).isEqualTo(last + period - grace);
    }
}