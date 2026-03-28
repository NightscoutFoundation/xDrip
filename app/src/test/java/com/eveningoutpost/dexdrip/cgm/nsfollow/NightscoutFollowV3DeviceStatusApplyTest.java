package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that {@link NightscoutFollowV3#applyDeviceStatus} maps device status fields
 * to {@link PumpStatus}, so NSFollow v3 data appears in the same status line block as
 * xDrip sync data.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3DeviceStatusApplyTest extends RobolectricTestWithConfig {

    @Before
    public void setUp() {
        super.setUp();
        // Reset PumpStatus to "not set" state before each test
        PumpStatus.setBattery(-1);
        PumpStatus.setReservoir(-1);
    }

    // ===== Battery ===============================================================================

    @Test
    public void applyDeviceStatus_populatesUploaderBatteryInPumpStatus() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 84;

        // :: Act
        NightscoutFollowV3.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(84.0);
    }

    @Test
    public void applyDeviceStatus_skipsSettingBatteryWhenUploaderBatteryIsNull() {
        // :: Setup — uploaderBattery intentionally null
        DeviceStatus ds = new DeviceStatus();

        // :: Act
        NightscoutFollowV3.applyDeviceStatus(ds);

        // :: Verify — battery unchanged (stays below zero = not set)
        assertThat(PumpStatus.getBattery()).isLessThan(0.0);
    }

    // ===== Reservoir =============================================================================

    @Test
    public void applyDeviceStatus_populatesReservoirInPumpStatus() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.pump = new DeviceStatus.Pump();
        ds.pump.reservoir = 11.5;

        // :: Act
        NightscoutFollowV3.applyDeviceStatus(ds);

        // :: Verify — PumpStatus formats reservoir with one decimal place
        assertThat(PumpStatus.getReservoirString()).contains("11.5");
    }

    // ===== Reset clears PumpStatus =======================================================

    @Test
    public void resetInstance_clearsBatteryAndReservoirFromPumpStatus() {
        // :: Setup — populate PumpStatus as if v3 was running
        PumpStatus.setBattery(84);
        PumpStatus.setReservoir(11.5);

        // :: Act — simulate user switching off v3
        NightscoutFollowV3.resetInstance();

        // :: Verify — stale device status no longer shown
        assertThat(PumpStatus.getBatteryString()).isEmpty();
        assertThat(PumpStatus.getReservoirString()).isEmpty();
    }

    @Test
    public void applyDeviceStatus_skipsSettingReservoirWhenPumpIsNull() {
        // :: Setup — pump intentionally null
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 75;

        // :: Act — should not NPE
        NightscoutFollowV3.applyDeviceStatus(ds);

        // :: Verify — reservoir not set, battery set correctly
        assertThat(PumpStatus.getReservoirString()).isEmpty();
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(75.0);
    }
}
