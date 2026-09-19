package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;

public class DexCollectionTypeTest extends RobolectricTestWithConfig {

    final String opt = "calibrate_external_libre_2_algorithm_type";
    final String opt2 = "external_blukon_algorithm";

    private void cleanup() {
        Pref.removeItem(opt);
        Pref.removeItem(opt2);
        Pref.removeItem("bridge_battery");
        Pref.removeItem("parakeet_battery");
        Pref.removeItem("dex_collection_method");
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

    // ===== getBestBridgeBatteryPercent ==================================================================================

    /**
     * The wifi uploader battery is the only battery readout WifiWixel and Mock have. Renaming the
     * feature must not change which preference each collection type reads.
     */
    @Test
    public void getBestBridgeBatteryPercentReadsTheUploaderBatteryForWifiOnlyTypes() {
        // :: Setup
        Pref.setInt("bridge_battery", 71);
        Pref.setInt("parakeet_battery", 42);

        // :: Act & Verify
        for (val type : new DexCollectionType[]{DexCollectionType.WifiWixel, DexCollectionType.Mock}) {
            DexCollectionType.setDexCollectionType(type);
            assertWithMessage(type + " reads the uploader battery")
                    .that(DexCollectionType.getBestBridgeBatteryPercent()).isEqualTo(42);
        }

        for (val type : new DexCollectionType[]{DexCollectionType.WifiBlueToothWixel,
                DexCollectionType.WifiDexBridgeWixel, DexCollectionType.LimiTTerWifi, DexCollectionType.LibreWifi}) {
            DexCollectionType.setDexCollectionType(type);
            assertWithMessage(type + " reads the bridge battery")
                    .that(DexCollectionType.getBestBridgeBatteryPercent()).isEqualTo(71);
        }

        DexCollectionType.setDexCollectionType(DexCollectionType.NSFollow);
        assertWithMessage("a type with neither wifi nor a bridge battery reports -2")
                .that(DexCollectionType.getBestBridgeBatteryPercent()).isEqualTo(-2);
    }
}
