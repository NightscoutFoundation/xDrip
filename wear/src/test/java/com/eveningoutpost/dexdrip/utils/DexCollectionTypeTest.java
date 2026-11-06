package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;

/**
 * Pins the wear copy of {@link DexCollectionType#getBestBridgeBatteryPercent()} so it keeps
 * matching the app copy through the wifi uploader battery rename.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class DexCollectionTypeTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem("bridge_battery");
        Pref.removeItem("parakeet_battery");
        Pref.removeItem("dex_collection_method");
    }

    // ===== getBestBridgeBatteryPercent ==================================================================================

    /** WifiWixel and Mock read the uploader battery; the other wifi types read the bridge battery. */
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
