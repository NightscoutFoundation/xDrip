package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.Test;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/02/2018.
 */
public class SensorSanityTest extends RobolectricTestWithConfig {

    @Test
    public void sanity_raw_test_1() {

        assertWithMessage("Unrestricted passes").that(SensorSanity.isRawValueSane(1, DexCollectionType.Disabled)).isTrue();

        assertWithMessage("G5 typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.DexcomG5)).isTrue();
        assertWithMessage("G4 typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.DexbridgeWixel)).isTrue();
        assertWithMessage("Libre typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.LimiTTer)).isTrue();


        assertWithMessage("G5 low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.DexcomG5)).isFalse();
        assertWithMessage("G4 low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.DexbridgeWixel)).isFalse();
        assertWithMessage("Libre low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.LimiTTer)).isFalse();

    }

    @Test
    public void sanity_raw_test_2() {

        assertWithMessage("Unrestricted passes").that(SensorSanity.isRawValueSane(10000, DexCollectionType.Disabled)).isTrue();

        assertWithMessage("G5 typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.DexcomG5)).isTrue();
        assertWithMessage("G4 typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.DexbridgeWixel)).isTrue();
        assertWithMessage("Libre typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.LimiTTer)).isTrue();


        assertWithMessage("G5 high FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.DexcomG5)).isFalse();
        assertWithMessage("G4 high FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.DexbridgeWixel)).isFalse();
        //     assertWithMessage("Libre low FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.LimiTTer)).isFalse();

    }

    @Test
    public void checkLibreSensorChangeTest() {

        final String[] testSerials = {"12345678", "12345678", "", "12345678", "12345678", "12345679", "", "12345679", "12345679", "12345680", "12345680", "12345680", "12345680", "12345680"};

        Sensor.shutdownAllSensors();
        JoH.clearCache();
        // everything null
        sensorCheck("all null check ", null, false, true);

        // Testing with null sensor
        for (int i = 0; i < testSerials.length; i++) {
            sensorCheck("Null sensor check " + i, testSerials[i], false, true);
        }

        // Testing for standard failure
        Sensor sensor = Sensor.create(JoH.tsl());
        for (int i = 0; i < 4; i++) {
            sensorCheck("Standard failure check (a) " + i, testSerials[i], false, false);
        }
        for (int i = 5; i < 6; i++) {
            sensorCheck("Standard failure check (b) " + i, testSerials[i], true, true);
        }
        for (int i = 6; i < testSerials.length; i++) {
            sensorCheck("Standard failure check (b) " + i, testSerials[i], false, true);
        }

        // Test sensor create
        final String[] testSerials2 = {"12345678", "12345678", "12345678", "12345678", null, "12345678", "12345678", "12345678", "12345678", "", "12345678", "12345678", "12345678", "12345678"};
        final long ts = JoH.tsl();

        Sensor.create(ts + 1);
        for (int i = 0; i < testSerials2.length; i++) {
            sensorCheck("new sensors (" + i + ") ", testSerials2[i], false, false);
        }
    }

    private void sensorCheck(final String checkName, final String sn, boolean shouldBeTrue, boolean shouldBeNull) {
        final boolean result = SensorSanity.checkLibreSensorChange(sn);
        final Sensor sensor = Sensor.currentSensor();

        //System.out.println("Test: " + checkName + " " + sn + " " + shouldBeTrue + " " + shouldBeNull + " -> " + result + " " + sensor);
        assertWithMessage(checkName + " boolean result").that(result).isEqualTo(shouldBeTrue);
        if (shouldBeNull) {
            assertWithMessage(checkName + " sensor result null").that(sensor).isNull();
        } else {
            assertWithMessage(checkName + " sensor result not null").that(sensor).isNotNull();
        }
    }
}