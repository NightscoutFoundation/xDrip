package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by jamorham on 17/02/2018.
 */
@RunWith(RobolectricTestRunner.class)
//@Config(manifest = Config.NONE)
//@Config(constants = BuildConfig.class, manifest = "../../../../app/src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml") // use this config inside android studio 3 or set Android JUnit default working directory to $MODULE_DIR$
@Config(constants = BuildConfig.class, manifest = "../../../../../src/test/java/com/eveningoutpost/dexdrip/TestingManifest.xml")
public class SensorSanityTest {


    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void sanity_raw_test_1() throws Exception {

        assertWithMessage("Unrestricted passes").that(SensorSanity.isRawValueSane(1, DexCollectionType.Disabled)).isTrue();

        assertWithMessage("G5 typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.DexcomG5)).isTrue();
        assertWithMessage("G4 typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.DexbridgeWixel)).isTrue();
        assertWithMessage("Libre typical OK").that(SensorSanity.isRawValueSane(100, DexCollectionType.LimiTTer)).isTrue();


        assertWithMessage("G5 low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.DexcomG5)).isFalse();
        assertWithMessage("G4 low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.DexbridgeWixel)).isFalse();
        assertWithMessage("Libre low FAIL").that(SensorSanity.isRawValueSane(1, DexCollectionType.LimiTTer)).isFalse();

    }

    @Test
    public void sanity_raw_test_2() throws Exception {

        assertWithMessage("Unrestricted passes").that(SensorSanity.isRawValueSane(10000, DexCollectionType.Disabled)).isTrue();

        assertWithMessage("G5 typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.DexcomG5)).isTrue();
        assertWithMessage("G4 typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.DexbridgeWixel)).isTrue();
        assertWithMessage("Libre typical OK").that(SensorSanity.isRawValueSane(300, DexCollectionType.LimiTTer)).isTrue();


        assertWithMessage("G5 high FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.DexcomG5)).isFalse();
        assertWithMessage("G4 high FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.DexbridgeWixel)).isFalse();
   //     assertWithMessage("Libre low FAIL").that(SensorSanity.isRawValueSane(1001, DexCollectionType.LimiTTer)).isFalse();

    }
}