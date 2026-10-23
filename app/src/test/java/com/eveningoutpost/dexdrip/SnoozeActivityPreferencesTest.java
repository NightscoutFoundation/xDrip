package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the disabled-alert deadlines {@link SnoozeActivity} reads when it opens.
 * <p>
 * Each alert group is switched off by storing the time it comes back. The snooze screen reads those
 * three timestamps and turns them into the status line the user is shown, so the rendered text is
 * where the reads become visible.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class SnoozeActivityPreferencesTest extends RobolectricTestWithConfig {

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    // ===== A stored deadline in the future disables its group ====================================

    /** An all-alerts deadline an hour out puts the all-alerts warning on screen. */
    @Test
    public void storedAllAlertsDeadline_isReportedAsDisabled() {
        // :: Setup
        disableUntil(SnoozeActivity.SnoozeType.ALL_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS);

        // :: Act
        String status = openScreenAndReadStatus();

        // :: Verify
        assertThat(status).contains(string(R.string.all_alerts_disabled_until));
    }

    /** A low-alerts deadline an hour out puts the low-alerts warning on screen. */
    @Test
    public void storedLowAlertsDeadline_isReportedAsDisabled() {
        // :: Setup
        disableUntil(SnoozeActivity.SnoozeType.LOW_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS);

        // :: Act
        String status = openScreenAndReadStatus();

        // :: Verify
        assertThat(status).contains(string(R.string.low_alerts_disabled_until));
        assertThat(status).doesNotContain(string(R.string.high_alerts_disabled_until));
    }

    /** A high-alerts deadline an hour out puts the high-alerts warning on screen. */
    @Test
    public void storedHighAlertsDeadline_isReportedAsDisabled() {
        // :: Setup
        disableUntil(SnoozeActivity.SnoozeType.HIGH_ALERTS, JoH.tsl() + Constants.HOUR_IN_MS);

        // :: Act
        String status = openScreenAndReadStatus();

        // :: Verify
        assertThat(status).contains(string(R.string.high_alerts_disabled_until));
        assertThat(status).doesNotContain(string(R.string.low_alerts_disabled_until));
    }

    // ===== An expired or absent deadline disables nothing ========================================

    /** A deadline that has already passed is not a live snooze, so no warning is shown. */
    @Test
    public void expiredDeadline_isNotReportedAsDisabled() {
        // :: Setup
        disableUntil(SnoozeActivity.SnoozeType.ALL_ALERTS, JoH.tsl() - Constants.HOUR_IN_MS);

        // :: Act
        String status = openScreenAndReadStatus();

        // :: Verify
        assertThat(status).doesNotContain(string(R.string.all_alerts_disabled_until));
    }

    /** With nothing stored the screen opens without any disabled-alert warning. */
    @Test
    public void nothingStored_reportsNoDisabledAlerts() {
        // :: Act
        String status = openScreenAndReadStatus();

        // :: Verify
        assertThat(status).doesNotContain(string(R.string.all_alerts_disabled_until));
        assertThat(status).doesNotContain(string(R.string.low_alerts_disabled_until));
        assertThat(status).doesNotContain(string(R.string.high_alerts_disabled_until));
    }

    // ===== Helpers ===============================================================================

    private String openScreenAndReadStatus() {
        SnoozeActivity activity = Robolectric.buildActivity(SnoozeActivity.class).create().get();
        TextView alertStatus = activity.findViewById(R.id.alert_status);
        assertThat(alertStatus).isNotNull();
        return alertStatus.getText().toString();
    }

    private void disableUntil(SnoozeActivity.SnoozeType type, long until) {
        prefs().edit().putLong(type.getPrefKey(), until).commit();
    }

    private static String string(int resourceId) {
        return xdrip.getAppContext().getString(resourceId);
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
