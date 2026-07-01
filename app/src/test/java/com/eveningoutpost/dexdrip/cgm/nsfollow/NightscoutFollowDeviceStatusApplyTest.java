package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies that {@link NightscoutFollow#applyDeviceStatus} maps device status fields
 * to {@link PumpStatus} and routes uploader status to {@link NightscoutFollowService}.
 *
 * Pump battery priority: pump's own battery ({@code pump.battery.percent}) is preferred
 * over uploader battery for {@link PumpStatus}. Uploader battery and charging status
 * are always routed to {@link NightscoutFollowService} regardless.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowDeviceStatusApplyTest extends RobolectricTestWithConfig {

    @Before
    public void setUp() {
        super.setUp();
        PumpStatus.setBattery(-1);
        PumpStatus.setReservoir(-1);
        NightscoutFollowService.clearUploaderStatus();
    }

    // ===== Pump battery priority =================================================================

    @Test
    public void applyDeviceStatus_prefersPumpBatteryOverUploaderBattery() {
        // :: Setup — pump battery present alongside uploader battery; pump should win
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 60;
        ds.pump = new DeviceStatus.Pump();
        ds.pump.battery = new DeviceStatus.Pump.Battery();
        ds.pump.battery.percent = 45;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify — pump battery stored in PumpStatus
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(45.0);
    }

    @Test
    public void applyDeviceStatus_fallsBackToUploaderBattery_whenNoPumpBattery() {
        // :: Setup — no pump, only uploader battery
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 72;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify — uploader battery used as fallback for PumpStatus
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(72.0);
    }

    @Test
    public void applyDeviceStatus_fallsBackToUploaderBattery_whenPumpBatteryIsNull() {
        // :: Setup — pump present but no battery object
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 55;
        ds.pump = new DeviceStatus.Pump();
        // pump.battery intentionally null

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(55.0);
    }

    @Test
    public void applyDeviceStatus_fallsBackToNestedUploaderBattery_whenNoPumpBattery() {
        // :: Setup — modern REST format with nested uploader.battery, no pump battery
        DeviceStatus ds = new DeviceStatus();
        ds.uploader = new DeviceStatus.Uploader();
        ds.uploader.battery = 68;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(68.0);
    }

    @Test
    public void applyDeviceStatus_skipsSettingBatteryInPumpStatus_whenNoBatteryAvailable() {
        // :: Setup — no battery anywhere
        DeviceStatus ds = new DeviceStatus();

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getBattery()).isLessThan(0.0);
    }

    // ===== Uploader status routing ===============================================================

    @Test
    public void applyDeviceStatus_routesUploaderBatteryToService() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 84;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify — uploader battery always reaches the service (independent of pump battery)
        assertThat(NightscoutFollowService.uploaderBattery).isEqualTo(84);
    }

    @Test
    public void applyDeviceStatus_routesUploaderBatteryToService_evenWhenPumpBatteryPresent() {
        // :: Setup — both pump battery and uploader battery available
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 80;
        ds.pump = new DeviceStatus.Pump();
        ds.pump.battery = new DeviceStatus.Pump.Battery();
        ds.pump.battery.percent = 45;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify — uploader battery still reaches service even though pump battery won PumpStatus
        assertThat(NightscoutFollowService.uploaderBattery).isEqualTo(80);
        // Pump battery went to PumpStatus
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(45.0);
    }

    @Test
    public void applyDeviceStatus_routesNestedUploaderBatteryToService() {
        // :: Setup — nested format
        DeviceStatus ds = new DeviceStatus();
        ds.uploader = new DeviceStatus.Uploader();
        ds.uploader.battery = 72;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(NightscoutFollowService.uploaderBattery).isEqualTo(72);
    }

    @Test
    public void applyDeviceStatus_routesChargingStatusToService() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 84;
        ds.isCharging = true;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(NightscoutFollowService.uploaderCharging).isTrue();
    }

    @Test
    public void applyDeviceStatus_routesNullUploaderBatteryToService_whenNotPresent() {
        // :: Setup — pump battery only, no uploader battery
        DeviceStatus ds = new DeviceStatus();
        ds.pump = new DeviceStatus.Pump();
        ds.pump.battery = new DeviceStatus.Pump.Battery();
        ds.pump.battery.percent = 45;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify — service uploader battery is null (not set)
        assertThat(NightscoutFollowService.uploaderBattery).isNull();
    }

    // ===== Reservoir =============================================================================

    @Test
    public void applyDeviceStatus_populatesReservoirInPumpStatus() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.pump = new DeviceStatus.Pump();
        ds.pump.reservoir = 11.5;

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getReservoirString()).contains("11.5");
    }

    @Test
    public void applyDeviceStatus_skipsSettingReservoirWhenPumpIsNull() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.uploaderBattery = 75;

        // :: Act — should not NPE
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getReservoirString()).isEmpty();
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(75.0);
    }

    @Test
    public void applyDeviceStatus_skipsSettingReservoirWhenReservoirIsNull() {
        // :: Setup
        DeviceStatus ds = new DeviceStatus();
        ds.pump = new DeviceStatus.Pump();
        // reservoir not set — remains null

        // :: Act
        NightscoutFollow.applyDeviceStatus(ds);

        // :: Verify
        assertThat(PumpStatus.getReservoirString()).isEmpty();
    }

    // ===== Reset clears everything ===============================================================

    @Test
    public void resetInstance_clearsBatteryAndReservoirFromPumpStatus() {
        // :: Setup
        PumpStatus.setBattery(84);
        PumpStatus.setReservoir(11.5);

        // :: Act
        NightscoutFollow.resetInstance();

        // :: Verify
        assertThat(PumpStatus.getBatteryString()).isEmpty();
        assertThat(PumpStatus.getReservoirString()).isEmpty();
    }

    @Test
    public void resetInstance_clearsUploaderStatusFromService() {
        // :: Setup
        NightscoutFollowService.updateUploaderStatus(80, true);

        // :: Act
        NightscoutFollow.resetInstance();

        // :: Verify
        assertThat(NightscoutFollowService.uploaderBattery).isNull();
        assertThat(NightscoutFollowService.uploaderCharging).isNull();
    }
}
