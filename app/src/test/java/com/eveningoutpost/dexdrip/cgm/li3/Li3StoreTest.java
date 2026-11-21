package com.eveningoutpost.dexdrip.cgm.li3;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xrwd.li3.Li3SensorState;

/**
 * Tests for {@link Li3Store}: what is stored about the current sensor.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Li3StoreTest extends RobolectricTestWithConfig {

    /** Where Li3Store keeps the sensor; a test can only make it unreadable by writing there. */
    private static final String STATE_KEY = "li3-sensor-state";

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        Li3Store.forgetSensor();
    }

    @After
    public void tearDown() {
        Li3Store.forgetSensor();
    }

    // =================================================== loadState ===================================================

    /** A stored sensor that can no longer be read, as after a format change, counts as no sensor instead of crashing. */
    @Test
    public void loadState_unreadable_returnsNull() {
        // :: Setup
        PersistentStore.setString(STATE_KEY, "{\"serialNumber\":\"0RRC989AQ\"}");

        // :: Act
        Li3SensorState state = Li3Store.loadState();

        // :: Verify
        assertThat(state).isNull();
    }
}
