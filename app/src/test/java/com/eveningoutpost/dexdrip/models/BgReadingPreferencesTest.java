package com.eveningoutpost.dexdrip.models;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the three preference-driven alert gates in {@link BgReading}.
 * <p>
 * All three methods read their switches straight from the default shared preferences, so the tests
 * drive them through those preferences and observe the {@link UserNotification} row each path is
 * meant to leave behind or clear. The rise and drop gates are {@code void}, and the row is their
 * only observable: an alert that is switched off returns before it can touch the row, while an
 * alert that is switched on reaches {@code Notifications} and clears it.
 * <p>
 * {@code getAndRaiseUnclearReading}'s two preference gates both end in "false, and the row is
 * cleared", so a test can pin the read but not tell the two gates apart. Its live branch is out of
 * reach: it is guarded by {@code DexCollectionType.hasFiltered()} and the two
 * {@code Ob1G5CollectionService} checks, which all read through {@code Pref}, and {@code Pref}
 * freezes on the first store it ever sees.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class BgReadingPreferencesTest extends RobolectricTestWithConfig {

    private static final String RISE_ALERT = "bg_rise_alert";
    private static final String FALL_ALERT = "bg_fall_alert";
    private static final String UNCLEAR_ALERT = "bg_unclear_readings_alert";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        new Delete().from(UserNotification.class).execute();    // so do the rows
    }

    // ===== Rising alert ==========================================================================

    /** With the rising alert switched off the method returns before it can touch the notification. */
    @Test
    public void risingAlertOff_leavesTheNotificationAlone() {
        // :: Setup
        seedNotification(RISE_ALERT);
        prefs().edit().putBoolean("rising_alert", false).commit();

        // :: Act
        BgReading.checkForRisingAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(RISE_ALERT)).isNotNull();
    }

    /** All alerts snoozed until a future time stops the rising alert just as an off switch does. */
    @Test
    public void risingAlertOn_butAllAlertsDisabled_leavesTheNotificationAlone() {
        // :: Setup
        seedNotification(RISE_ALERT);
        prefs().edit()
                .putBoolean("rising_alert", true)
                .putLong("alerts_disabled_until", JoH.tsl() + Constants.HOUR_IN_MS)
                .commit();

        // :: Act
        BgReading.checkForRisingAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(RISE_ALERT)).isNotNull();
    }

    /** Switched on with no rise in the data, the method reaches the notification and clears it. */
    @Test
    public void risingAlertOn_andNoRiseInTheData_clearsTheNotification() {
        // :: Setup
        seedNotification(RISE_ALERT);
        prefs().edit().putBoolean("rising_alert", true).commit();

        // :: Act
        BgReading.checkForRisingAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(RISE_ALERT)).isNull();
    }

    // ===== Drop alert ============================================================================

    /** With the falling alert switched off the method returns before it can touch the notification. */
    @Test
    public void dropAlertOff_leavesTheNotificationAlone() {
        // :: Setup
        seedNotification(FALL_ALERT);
        prefs().edit().putBoolean("falling_alert", false).commit();

        // :: Act
        BgReading.checkForDropAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(FALL_ALERT)).isNotNull();
    }

    /** All alerts snoozed until a future time stops the drop alert just as an off switch does. */
    @Test
    public void dropAlertOn_butAllAlertsDisabled_leavesTheNotificationAlone() {
        // :: Setup
        seedNotification(FALL_ALERT);
        prefs().edit()
                .putBoolean("falling_alert", true)
                .putLong("alerts_disabled_until", JoH.tsl() + Constants.HOUR_IN_MS)
                .commit();

        // :: Act
        BgReading.checkForDropAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(FALL_ALERT)).isNotNull();
    }

    /** Switched on with no drop in the data, the method reaches the notification and clears it. */
    @Test
    public void dropAlertOn_andNoDropInTheData_clearsTheNotification() {
        // :: Setup
        seedNotification(FALL_ALERT);
        prefs().edit().putBoolean("falling_alert", true).commit();

        // :: Act
        BgReading.checkForDropAllert(xdrip.getAppContext());

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(FALL_ALERT)).isNull();
    }

    // ===== Unclear readings ======================================================================

    /** Alerts snoozed until a future time: no unclear reading is raised, and any standing one goes. */
    @Test
    public void unclearReadings_whileAllAlertsDisabled_isFalseAndClearsTheNotification() {
        // :: Setup
        seedNotification(UNCLEAR_ALERT);
        prefs().edit()
                .putBoolean("bg_unclear_readings_alerts", true)
                .putLong("alerts_disabled_until", JoH.tsl() + Constants.HOUR_IN_MS)
                .commit();

        // :: Act
        final boolean raised = BgReading.getAndRaiseUnclearReading(xdrip.getAppContext());

        // :: Verify
        assertThat(raised).isFalse();
        assertThat(UserNotification.GetNotificationByType(UNCLEAR_ALERT)).isNull();
    }

    /** With the feature switched off the same holds, and it is the default state. */
    @Test
    public void unclearReadings_withTheFeatureOff_isFalseAndClearsTheNotification() {
        // :: Setup
        seedNotification(UNCLEAR_ALERT);
        prefs().edit().putBoolean("bg_unclear_readings_alerts", false).commit();

        // :: Act
        final boolean raised = BgReading.getAndRaiseUnclearReading(xdrip.getAppContext());

        // :: Verify
        assertThat(raised).isFalse();
        assertThat(UserNotification.GetNotificationByType(UNCLEAR_ALERT)).isNull();
    }

    // ===== Helpers ===============================================================================

    private static void seedNotification(String type) {
        UserNotification.create("standing alert", type, JoH.tsl());
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
