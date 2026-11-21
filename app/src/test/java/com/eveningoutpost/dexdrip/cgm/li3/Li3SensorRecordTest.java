package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xrwd.li3.Li3SensorState;

/**
 * Tests for {@link Li3Store#ensureSensor}: readings must land on an xDrip {@link Sensor} that started when the Li3
 * did, and a new Li3 must never inherit the previous one's sensor.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3SensorRecordTest extends RobolectricTestWithConfig {

    private static final byte[] PIN = {0x32, 0x25, (byte) 0xec, 0x72};

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        clean();
    }

    @After
    public void tearDown() {
        clean();
    }

    // ================================================= ensureSensor ==================================================

    /** A new Li3 stops the sensor in use and starts one at the Li3's activation time. */
    @Test
    public void ensureSensor_newSerial_startsSensorAtActivation() {
        // :: Setup
        long now = tsl();
        Sensor old = Sensor.create(now - 20 * DAY_IN_MS);
        BgReading.bgReadingInsertFromGluPro(120, now - 3 * DAY_IN_MS, "test");
        long activationMs = now - 2 * DAY_IN_MS;

        // :: Act
        Li3Store.ensureSensor(state("0RRC989AQ", activationMs));

        // :: Verify
        Sensor current = Sensor.currentSensor();
        assertThat(current.uuid).isNotEqualTo(old.uuid);
        assertThat(current.started_at).isEqualTo(activationMs);
        assertThat(Sensor.getByUuid(old.uuid).stopped_at).isGreaterThan(0L);
    }

    /** The same Li3 seen again keeps the sensor it already has. */
    @Test
    public void ensureSensor_sameSerial_keepsSensor() {
        // :: Setup
        long activationMs = tsl() - DAY_IN_MS;
        Li3Store.ensureSensor(state("0RRC989AQ", activationMs));
        String uuid = Sensor.currentSensor().uuid;

        // :: Act
        Li3Store.ensureSensor(state("0RRC989AQ", activationMs));

        // :: Verify
        assertThat(Sensor.currentSensor().uuid).isEqualTo(uuid);
    }

    private static Li3SensorState state(String serial, long activationMs) {
        Li3SensorState state = new Li3SensorState(serial, "CC:22:DF:B8:F9:58", PIN);
        state.setActivationMs(activationMs);
        return state;
    }

    private static void clean() {
        BgReading.deleteALL();
        Sensor.deleteAll();
        Li3Store.forgetSensor();
    }
}
