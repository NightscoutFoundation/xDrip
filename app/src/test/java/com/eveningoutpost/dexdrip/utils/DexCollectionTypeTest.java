package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DexCollectionTypeTest extends RobolectricTestWithConfig {

    final String opt = "calibrate_external_libre_2_algorithm_type";
    final String opt2 = "external_blukon_algorithm";

    private void cleanup() {
        Pref.removeItem(opt);
        Pref.removeItem(opt2);
    }

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    @Test
    public void isLibreOOPNonCalibratebleAlgorithmTest() {

        Pref.setString(opt,"no_calibration");
        assertWithMessage("no calibration matches").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isTrue();
        Pref.setString(opt,"calibrate_raw");
        assertWithMessage("calibrate raw matches").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isFalse();
        Pref.setString(opt,"calibrate_glucose");
        assertWithMessage("calibrate glucose matches").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isFalse();

        Pref.setBoolean(opt2,false);
        Pref.setString(opt,"no_calibration");
        assertWithMessage("no calibration matches 1").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isTrue();
        Pref.setString(opt,"calibrate_raw");
        assertWithMessage("calibrate raw matches 1").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isFalse();
        Pref.setString(opt,"calibrate_glucose");
        assertWithMessage("calibrate glucose matches 1").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isFalse();

        Pref.setBoolean(opt2,true);
        Pref.setString(opt,"no_calibration");
        assertWithMessage("no calibration matches 2 ").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isTrue();
        Pref.setString(opt,"calibrate_raw");
        assertWithMessage("calibrate raw matches 2").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isTrue();
        Pref.setString(opt,"calibrate_glucose");
        assertWithMessage("calibrate glucose matches 2").that(DexCollectionType.isLibreOOPNonCalibratebleAlgorithm(DexCollectionType.LimiTTer)).isTrue();

    }
}