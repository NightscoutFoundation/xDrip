package com.eveningoutpost.dexdrip;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the units preference {@link GcmActivity#pushCalibration2} reads.
 * <p>
 * A follower sends a calibration to its master in mg/dL, so a value typed in mmol/L is converted
 * first. Out-of-range values are rejected with a toast that shows the converted number, which
 * makes the units read visible without sending anything.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class GcmActivityCalibrationUnitsTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        storeString("dex_collection_method", "Follower");
        Home.resetFollowerCacheForTests();
    }

    /** Clears the store, then caches "not a follower" from it, so later tests in the shared JVM never see one. */
    @After
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        Home.resetFollowerCacheForTests();
        Home.get_follower();
    }

    // ===== Units preference drives the range check ===================================================================

    /** In mg/dL, a value of 30 is below the 40 mg/dL floor and is rejected as 30. */
    @Test
    public void lowValueIsRejectedUnconvertedWhenUnitsAreMgdl() {
        // :: Setup
        storeString("units", "mgdl");

        // :: Act
        String toast = pushCalibrationAndReadToast(30);

        // :: Verify
        assertThat(toast).isEqualTo("Calibration out of range: 30.0 mg/dl");
    }

    /**
     * In mmol/L, the same 30 is converted to about 540 mg/dL and rejected as too high. mgdl is the
     * default, so this is the test that proves the preference is read at all.
     */
    @Test
    public void valueIsConvertedBeforeTheRangeCheckWhenUnitsAreMmol() {
        // :: Setup
        storeString("units", "mmol");

        // :: Act
        String toast = pushCalibrationAndReadToast(30);

        // :: Verify
        assertThat(toast).isEqualTo("Calibration out of range: " + (30 * Constants.MMOLL_TO_MGDL) + " mg/dl");
    }

    /** With no units stored the value is treated as mg/dL. */
    @Test
    public void valueIsTreatedAsMgdlWhenNothingIsStored() {
        // :: Act
        String toast = pushCalibrationAndReadToast(30);

        // :: Verify
        assertThat(toast).isEqualTo("Calibration out of range: 30.0 mg/dl");
    }

    // ===== Helpers ===================================================================================================

    private String pushCalibrationAndReadToast(double value) {
        GcmActivity.pushCalibration2(value, "batch7-calibration", 0);
        ShadowLooper.idleMainLooper();
        return ShadowToast.getTextOfLatestToast();
    }

    private void storeString(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString(key, value).commit();
    }
}
