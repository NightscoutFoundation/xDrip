package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * Various tests for {@link Calibration} class
 *
 * @author asbjorn aarrestad - asbjorn@aarrestad.com 2018.03.
 */
public class CalibrationTest extends RobolectricTestWithConfig {

    @Test
    public void initialCalibration_raisingBg_OK() {
        // :: Setup
        // Add mock sensor
        Sensor mockSensor = new Sensor();
        mockSensor.started_at = System.currentTimeMillis() - (1000 * 60 * 20);
        mockSensor.uuid = UUID.randomUUID().toString();
        mockSensor.save();

        // Add mock bg readings
        addMockBgReading(125, 11, mockSensor);
        addMockBgReading(130, 6, mockSensor);
        addMockBgReading(135, 1, mockSensor);

        // :: Act
        Calibration.initialCalibration(140, 145, RuntimeEnvironment.application.getApplicationContext());

        // :: Verify
        List<Calibration> calibrations = Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3);
        assertThat(calibrations).hasSize(2);
        Calibration calibration1 = calibrations.get(0);
        assertThat(calibration1.bg).isWithin(0.01).of(145);
        assertThat(calibration1.raw_value).isWithin(0.01).of(135);
        assertThat(calibration1.slope).isWithin(0.001).of(1);
        assertThat(calibration1.intercept).isWithin(0.001).of(9.9);

        Calibration calibration2 = calibrations.get(1);
        assertThat(calibration2.bg).isWithin(0.01).of(140);
        assertThat(calibration2.raw_value).isWithin(0.01).of(130);
        assertThat(calibration2.slope).isWithin(0.001).of(1);
        assertThat(calibration2.intercept).isWithin(0.001).of(10);
    }

    @Test
    public void initialCalibration_fallingBg_OK() {
        // :: Setup
        // Add mock sensor
        Sensor mockSensor = new Sensor();
        mockSensor.started_at = System.currentTimeMillis() - (1000 * 60 * 20);
        mockSensor.uuid = UUID.randomUUID().toString();
        mockSensor.save();

        // Add mock bg readings
        addMockBgReading(135, 11, mockSensor);
        addMockBgReading(130, 6, mockSensor);
        addMockBgReading(125, 1, mockSensor);

        // :: Act
        Calibration.initialCalibration(145, 140, RuntimeEnvironment.application);

        // :: Verify
        List<Calibration> calibrations = Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3);
        assertThat(calibrations).hasSize(2);
        Calibration calibration1 = calibrations.get(0);
        assertThat(calibration1.bg).isWithin(0.01).of(145);
        assertThat(calibration1.raw_value).isWithin(0.01).of(130);
        assertThat(calibration1.slope).isWithin(0.001).of(1);
        assertThat(calibration1.intercept).isWithin(0.001).of(14.9);

        Calibration calibration2 = calibrations.get(1);
        assertThat(calibration2.bg).isWithin(0.01).of(140);
        assertThat(calibration2.raw_value).isWithin(0.01).of(125);
        assertThat(calibration2.slope).isWithin(0.001).of(1);
        assertThat(calibration2.intercept).isWithin(0.001).of(15);
    }

    // ===== Internal Helpers ======================================================================

    private void addMockBgReading(int raw_data, int minutes, Sensor sensor) {
        BgReading mockReading = new BgReading();
        mockReading.raw_data = raw_data;
        mockReading.timestamp = System.currentTimeMillis() - (1000 * 60 * minutes);
        mockReading.sensor = sensor;
        mockReading.age_adjusted_raw_value = raw_data + 0.1;
        mockReading.save();
    }
}
