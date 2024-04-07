package com.eveningoutpost.dexdrip.alert;


import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;

import lombok.val;

@Config(instrumentedPackages = {"com.eveningoutpost.dexdrip.models.JoH"})
public class PersistTest extends RobolectricTestWithConfig {

    @Test
    public void testTimeoutString() {

        val PREF_NAME = "TEST_TIMEOUT_STRING";
        val testString = "Hello world";

        // setup
        PersistentStore.removeItem(PREF_NAME);
        ShadowSystemClock.advanceBy(Duration.ofHours(100));

        val store =
                new Persist.StringTimeout(PREF_NAME, Constants.MINUTE_IN_MS * 21);

        assertWithMessage("Time not zero").that(JoH.tsl()).isGreaterThan(Constants.HOUR_IN_MS);
        assertWithMessage("test empty null").that(store.get()).isNull();

        store.set(testString);
        assertWithMessage("test ok 1").that(store.get()).isEqualTo(testString);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));
        assertWithMessage("test ok 2").that(store.get()).isEqualTo(testString);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(10));
        assertWithMessage("test ok 3").that(store.get()).isEqualTo(testString);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(11));
        assertWithMessage("test expired 4").that(store.get()).isNull();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(11));
        assertWithMessage("test expired 5").that(store.get()).isNull();
        store.set(testString);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(10));
        assertWithMessage("test ok 4").that(store.get()).isEqualTo(testString);
    }

    @Test
    public void testTimeoutDouble() {

        val PREF_NAME = "TEST_TIMEOUT_DOUBLE";
        val testDouble = Double.valueOf(123.123);

        // setup
        PersistentStore.removeItem(PREF_NAME);
        ShadowSystemClock.advanceBy(Duration.ofHours(100));

        val store =
                new Persist.DoubleTimeout(PREF_NAME, Constants.MINUTE_IN_MS * 21);

        assertWithMessage("Time not zero").that(JoH.tsl()).isGreaterThan(Constants.HOUR_IN_MS);
        assertWithMessage("test empty null").that(store.get()).isNull();

        store.set(testDouble);
        assertWithMessage("test ok 1").that(store.get()).isEqualTo(testDouble);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));
        assertWithMessage("test ok 2").that(store.get()).isEqualTo(testDouble);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(10));
        assertWithMessage("test ok 3").that(store.get()).isEqualTo(testDouble);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(11));
        assertWithMessage("test expired 4").that(store.get()).isNull();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(11));
        assertWithMessage("test expired 5").that(store.get()).isNull();
        store.set(testDouble);
        ShadowSystemClock.advanceBy(Duration.ofMinutes(10));
        assertWithMessage("test ok 4").that(store.get()).isEqualTo(testDouble);
    }

}