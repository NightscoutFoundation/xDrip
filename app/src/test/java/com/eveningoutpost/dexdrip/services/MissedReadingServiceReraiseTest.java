package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link MissedReadingService#getOtherAlertReraiseSec}.
 * <p>
 * An "other alert" is raised again after a delay. With re-raise switched on for the alert, the delay is its own
 * re-raise setting in seconds. Otherwise it is the alert's snooze in minutes, falling back to the shared
 * "other alerts" snooze when the alert has none of its own.
 * <p>
 * Every test uses its own alert name, so values stored by one test cannot reach another.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class MissedReadingServiceReraiseTest extends RobolectricTestWithConfig {

    private static final String OTHER_ALERTS_SNOOZE_KEY = "other_alerts_snooze";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        Pref.removeItem(OTHER_ALERTS_SNOOZE_KEY);                // shared key; keep each test's fallback explicit
    }

    @After
    public void tearDown() {
        Pref.removeItem(OTHER_ALERTS_SNOOZE_KEY);                // Pref's cache outlives this class; don't leak the 25
    }

    // ===== Re-raise on ===============================================================================================

    /** With re-raise on, the delay is the alert's own re-raise setting, in seconds. */
    @Test
    public void reraiseOn_usesReraiseSeconds() {
        // :: Setup
        final String alert = "reraise_on_alert";
        Pref.setBoolean(alert + "_enable_alerts_reraise", true);
        Pref.setString(alert + "_reraise_sec", "90");
        Pref.setString(alert + "_snooze", "15");

        // :: Act
        final long seconds = MissedReadingService.getOtherAlertReraiseSec(RuntimeEnvironment.application, alert);

        // :: Verify
        assertThat(seconds).isEqualTo(90);
    }

    // ===== Re-raise off ==============================================================================================

    /** With re-raise off, the delay is the alert's own snooze, converted from minutes to seconds. */
    @Test
    public void reraiseOff_usesAlertSnoozeMinutes() {
        // :: Setup
        final String alert = "reraise_off_alert";
        Pref.setBoolean(alert + "_enable_alerts_reraise", false);
        Pref.setString(alert + "_reraise_sec", "90");
        Pref.setString(alert + "_snooze", "15");

        // :: Act
        final long seconds = MissedReadingService.getOtherAlertReraiseSec(RuntimeEnvironment.application, alert);

        // :: Verify
        assertThat(seconds).isEqualTo(15 * 60);
    }

    /** An alert with no snooze of its own falls back to the shared "other alerts" snooze. */
    @Test
    public void reraiseOff_noAlertSnooze_usesOtherAlertsSnooze() {
        // :: Setup
        final String alert = "no_snooze_alert";
        Pref.setString(OTHER_ALERTS_SNOOZE_KEY, "25");

        // :: Act
        final long seconds = MissedReadingService.getOtherAlertReraiseSec(RuntimeEnvironment.application, alert);

        // :: Verify
        assertThat(seconds).isEqualTo(25 * 60);
    }

    /** With nothing stored at all, the delay is the 20-minute default. */
    @Test
    public void reraiseOff_nothingStored_usesTwentyMinutes() {
        // :: Setup
        final String alert = "unset_alert";

        // :: Act
        final long seconds = MissedReadingService.getOtherAlertReraiseSec(RuntimeEnvironment.application, alert);

        // :: Verify
        assertThat(seconds).isEqualTo(20 * 60);
    }
}
