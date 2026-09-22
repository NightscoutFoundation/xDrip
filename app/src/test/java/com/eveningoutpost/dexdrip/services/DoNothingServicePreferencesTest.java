package com.eveningoutpost.dexdrip.services;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the foreground switch the follower service listens on.
 * <p>
 * {@code onCreate} registers a listener on the default shared preferences, and that registration is
 * the file's only use of them: turning {@code run_service_in_foreground} off has to take the service
 * out of the foreground, and turning it back on has to raise the ongoing notification again. A
 * listener registered on the wrong store would leave the switch dead, so the tests drive the switch
 * and watch the service move.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class DoNothingServicePreferencesTest extends RobolectricTestWithConfig {

    private DoNothingService service;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        service = Robolectric.buildService(DoNothingService.class).create().get();
    }

    // ===== The foreground switch =================================================================

    /** A freshly created service runs in the foreground. */
    @Test
    public void onCreate_leavesTheServiceInTheForeground() {
        // :: Act
        final boolean foregroundStopped = shadowOf(service).isForegroundStopped();

        // :: Verify
        assertThat(foregroundStopped).isFalse();
    }

    /** Switching the preference off takes the service out of the foreground. */
    @Test
    public void switchingTheForegroundPreferenceOff_stopsTheForeground() {
        // :: Act
        prefs().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Verify
        assertThat(shadowOf(service).isForegroundStopped()).isTrue();
    }

    /** Switching it back on raises the ongoing notification again. */
    @Test
    public void switchingTheForegroundPreferenceOn_raisesTheOngoingNotification() {
        // :: Setup
        prefs().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Act
        prefs().edit().putBoolean("run_service_in_foreground", true).commit();

        // :: Verify
        assertThat(shadowOf(service).getLastForegroundNotification()).isNotNull();
    }

    /** An unrelated preference leaves the service where it is. */
    @Test
    public void anUnrelatedPreferenceChange_leavesTheForegroundAlone() {
        // :: Act
        prefs().edit().putString("units", "mmol").commit();

        // :: Verify
        assertThat(shadowOf(service).isForegroundStopped()).isFalse();
    }

    // ===== Helpers ===============================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
