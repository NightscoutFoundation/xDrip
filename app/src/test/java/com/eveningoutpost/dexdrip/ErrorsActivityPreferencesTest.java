package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the log-sync preferences {@link ErrorsActivity} reads when it opens.
 * <p>
 * Opening the error log pushes the phone's logs to a paired watch, but only when both wear sync and
 * watch log sync are switched on. The push is a started service, so the tests open the screen and
 * look for it.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class ErrorsActivityPreferencesTest extends RobolectricTestWithConfig {

    private static final String WEAR_SYNC_KEY = "wear_sync";
    private static final String SYNC_WEAR_LOGS_KEY = "sync_wear_logs";

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        Pref.getInstance().edit().clear().commit();
    }

    // ===== Both switches on pushes the logs ======================================================

    /** With wear sync and log sync both stored on, opening the screen starts the log-sync push. */
    @Test
    public void bothSwitchesOn_startTheLogSync() {
        // :: Setup
        store(WEAR_SYNC_KEY, true);
        store(SYNC_WEAR_LOGS_KEY, true);

        // :: Act
        final Intent started = openActivityAndCollectStartedService();

        // :: Verify
        assertThat(started).isNotNull();
        assertThat(started.getComponent().getClassName()).isEqualTo(WatchUpdaterService.class.getName());
        assertThat(started.getAction()).isEqualTo(WatchUpdaterService.ACTION_SYNC_LOGS);
    }

    // ===== Either switch off keeps them home =====================================================

    /**
     * Log sync stored off keeps the push away even though wear sync is on. This is the case that
     * pins the activity's own read: nothing else in the start path consults
     * {@code sync_wear_logs}.
     */
    @Test
    public void logSyncOff_startsNothing() {
        // :: Setup
        store(WEAR_SYNC_KEY, true);
        store(SYNC_WEAR_LOGS_KEY, false);

        // :: Act
        final Intent started = openActivityAndCollectStartedService();

        // :: Verify
        assertThat(started).isNull();
    }

    /** Wear sync stored off keeps the push away even though log sync is on. */
    @Test
    public void wearSyncOff_startsNothing() {
        // :: Setup
        store(WEAR_SYNC_KEY, false);
        store(SYNC_WEAR_LOGS_KEY, true);

        // :: Act
        final Intent started = openActivityAndCollectStartedService();

        // :: Verify
        assertThat(started).isNull();
    }

    /** With neither stored, an unconfigured phone opens the error log without touching the watch. */
    @Test
    public void nothingStored_startsNothing() {
        // :: Act
        final Intent started = openActivityAndCollectStartedService();

        // :: Verify
        assertThat(started).isNull();
    }

    // ===== Helpers ===============================================================================

    private Intent openActivityAndCollectStartedService() {
        Robolectric.buildActivity(ErrorsActivity.class).create();
        return shadowOf(RuntimeEnvironment.application).getNextStartedService();
    }

    /**
     * Writes through both stores. {@link Pref} caches one instance for the whole JVM, bound to
     * whichever application object first asked it for one, so a class running after another in the
     * shared suite reads a different store than the activity's own
     * {@code getDefaultSharedPreferences} call returns.
     */
    private void store(String key, boolean value) {
        prefs().edit().putBoolean(key, value).commit();
        Pref.setBoolean(key, value);
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
