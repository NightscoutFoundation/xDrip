package com.eveningoutpost.dexdrip.services;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the re-raise preference {@link SnoozeOnNotificationDismissService} reads
 * when the user swipes away a missed-reading alert.
 * <p>
 * With re-raise switched on, swiping the alert away snoozes it; with it off, the alert is left alone
 * so it can fire again. The snooze is visible as the alert's stored time moving into the future.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class SnoozeOnNotificationDismissServicePreferencesTest extends RobolectricTestWithConfig {

    private static final String MISSED = "bg_missed_alerts";
    private static final String RERAISE_KEY = MISSED + "_enable_alerts_reraise";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        deleteAllMissedAlerts();                                // the ActiveAndroid DB leaks between tests
    }

    @After
    public void tearDown() {
        deleteAllMissedAlerts();
    }

    // ===== Re-raise on snoozes =======================================================================================

    /** With re-raise on, swiping away a missed-reading alert snoozes it into the future. */
    @Test
    public void reraiseOn_snoozesTheAlert() {
        // :: Setup
        prefs().edit().putBoolean(RERAISE_KEY, true).commit();
        final long raised = seedAlert();

        // :: Act
        dismiss(raised);

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(MISSED).timestamp).isGreaterThan((double) JoH.tsl());
    }

    // ===== Re-raise off leaves it alone ==============================================================================

    /** With re-raise off, the alert keeps its original time so it can fire again. */
    @Test
    public void reraiseOff_leavesTheAlert() {
        // :: Setup
        prefs().edit().putBoolean(RERAISE_KEY, false).commit();
        final long raised = seedAlert();

        // :: Act
        dismiss(raised);

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(MISSED).timestamp).isEqualTo((double) raised);
    }

    /**
     * A swipe within two seconds of the alert is ignored even with re-raise on. A regression pin,
     * not swap evidence: this gate does not read the preference. The raise time is a minute in the
     * future so a slow first class load can never push it past the two-second limit.
     */
    @Test
    public void reraiseOn_tooSoon_leavesTheAlert() {
        // :: Setup
        prefs().edit().putBoolean(RERAISE_KEY, true).commit();
        final long raised = JoH.tsl() + Constants.MINUTE_IN_MS;
        UserNotification.create("missed", MISSED, raised);

        // :: Act
        dismiss(raised);

        // :: Verify
        assertThat(UserNotification.GetNotificationByType(MISSED).timestamp).isEqualTo((double) raised);
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** {@code DeleteNotificationByType} removes only the newest row, so loop until none is left. */
    private static void deleteAllMissedAlerts() {
        while (UserNotification.GetNotificationByType(MISSED) != null) {
            UserNotification.DeleteNotificationByType(MISSED);
        }
    }

    /** Stores a missed-reading alert raised ten minutes ago and returns its time. */
    private static long seedAlert() {
        final long raised = JoH.tsl() - 10 * Constants.MINUTE_IN_MS;
        UserNotification.create("missed", MISSED, raised);
        return raised;
    }

    private static void dismiss(long raised) {
        final SnoozeOnNotificationDismissService service =
                Robolectric.buildService(SnoozeOnNotificationDismissService.class).create().get();
        service.onHandleIntent(new Intent()
                .putExtra("alertType", MISSED)
                .putExtra("raisedTimeStamp", raised));
    }
}
