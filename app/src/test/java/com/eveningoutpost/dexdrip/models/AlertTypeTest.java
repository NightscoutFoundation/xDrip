package com.eveningoutpost.dexdrip.models;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

/**
 * Characterization tests for {@link AlertType}, covering each method that reads from
 * {@code PreferenceManager.getDefaultSharedPreferences()}, so the android.preference →
 * androidx.preference import swap is proven behaviour-preserving.
 *
 * @author Asbjørn Aarrestad
 */
public class AlertTypeTest extends RobolectricTestWithConfig {

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- get_highest_active_alert ---

    /** Characterization: returns null while alerts are snoozed via alerts_disabled_until. */
    @Test
    public void get_highest_active_alert_returnsNullWhenAlertsDisabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        prefs.edit().putLong("alerts_disabled_until", new Date().getTime() + 60_000).commit();

        assertThat(AlertType.get_highest_active_alert(xdrip.getAppContext(), 100)).isNull();
    }

    // --- toSettings ---

    /** Characterization: toSettings serialises the alert table into the saved_alerts preference. */
    @Test
    public void toSettings_writesSavedAlertsPreference() {
        boolean result = AlertType.toSettings(xdrip.getAppContext());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        assertThat(result).isTrue();
        assertThat(prefs.contains("saved_alerts")).isTrue();
    }

    // --- fromSettings ---

    /** Characterization: fromSettings returns true (no-op) when no saved_alerts string is present. */
    @Test
    public void fromSettings_returnsTrueWhenNothingSaved() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        prefs.edit().putString("saved_alerts", "").commit();

        assertThat(AlertType.fromSettings(xdrip.getAppContext())).isTrue();
    }
}
