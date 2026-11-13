package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Behavioural tests for how {@link AlertPlayer} reads the user's glucose alert profile.
 * <p>
 * The profile decides whether a glucose alarm starts quietly and gets louder ("ascending") or plays at a fixed level.
 * The tests write the profile to the store the settings screen writes to and ask {@link AlertPlayer#isAscendingMode}
 * what the player will do.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class AlertPlayerAlertProfileTest extends RobolectricTestWithConfig {

    private static final String KEY = "bg_alert_profile";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
    }

    // ===== Alert profile =============================================================================================

    /** With no profile chosen, the alarm starts quietly and gets louder. */
    @Test
    public void noStoredProfileMeansAscending() {
        // :: Act
        final boolean ascending = AlertPlayer.isAscendingMode(xdrip.getAppContext());

        // :: Verify
        assertWithMessage("ascending mode with no stored profile").that(ascending).isTrue();
    }

    /** A user who chose "medium" gets an alarm at a fixed level, not the ascending default. */
    @Test
    public void storedMediumProfileIsNotAscending() {
        // :: Setup
        store().edit().putString(KEY, "medium").commit();

        // :: Act
        final boolean ascending = AlertPlayer.isAscendingMode(xdrip.getAppContext());

        // :: Verify
        assertWithMessage("ascending mode with the medium profile stored").that(ascending).isFalse();
    }

    /** A user who chose "Silent" does not get the ascending default. */
    @Test
    public void storedSilentProfileIsNotAscending() {
        // :: Setup
        store().edit().putString(KEY, "Silent").commit();

        // :: Act
        final boolean ascending = AlertPlayer.isAscendingMode(xdrip.getAppContext());

        // :: Verify
        assertWithMessage("ascending mode with the silent profile stored").that(ascending).isFalse();
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
