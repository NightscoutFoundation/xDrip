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
}