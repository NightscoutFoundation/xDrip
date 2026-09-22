package com.eveningoutpost.dexdrip.localeTasker.receiver;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.localeTasker.bundle.PluginBundleManager;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for {@link FireReceiver}.
 * <p>
 * A Tasker SNOOZE_LOW message must land in the same preference store the rest of the app reads its
 * snooze state from. The receiver resolves that store itself and passes it on, so the stored
 * snooze deadline is the observable proof of which store it resolved.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class FireReceiverTest extends RobolectricTestWithConfig {

    private static final String LOW_SNOOZE_KEY = SnoozeActivity.SnoozeType.LOW_ALERTS.getPrefKey();
    private static final String HIGH_SNOOZE_KEY = SnoozeActivity.SnoozeType.HIGH_ALERTS.getPrefKey();

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().clear().putLong(LOW_SNOOZE_KEY, 0).putLong(HIGH_SNOOZE_KEY, 0).commit();
    }

    // ===== Tasker snooze messages ================================================================

    /** SNOOZE_LOW 30 sets a low-alert deadline roughly thirty minutes out and leaves high alerts alone. */
    @Test
    public void snoozeLowStoresDeadlineForLowAlertsOnly() {
        // :: Setup
        long before = JoH.tsl();

        // :: Act
        new FireReceiver().onReceive(xdrip.getAppContext(), taskerIntent("SNOOZE_LOW 30"));

        // :: Verify
        long deadline = storedDeadline(LOW_SNOOZE_KEY);
        assertThat(deadline).isAtLeast(before + 29 * 60 * 1000L);
        assertThat(deadline).isAtMost(JoH.tsl() + 31 * 60 * 1000L);
        assertThat(storedDeadline(HIGH_SNOOZE_KEY)).isEqualTo(0);
    }

    /** SNOOZE_HIGH 45 sets a high-alert deadline and leaves low alerts alone. */
    @Test
    public void snoozeHighStoresDeadlineForHighAlertsOnly() {
        // :: Setup
        long before = JoH.tsl();

        // :: Act
        new FireReceiver().onReceive(xdrip.getAppContext(), taskerIntent("SNOOZE_HIGH 45"));

        // :: Verify
        long deadline = storedDeadline(HIGH_SNOOZE_KEY);
        assertThat(deadline).isAtLeast(before + 44 * 60 * 1000L);
        assertThat(deadline).isAtMost(JoH.tsl() + 46 * 60 * 1000L);
        assertThat(storedDeadline(LOW_SNOOZE_KEY)).isEqualTo(0);
    }

    /**
     * An intent with a foreign action is ignored outright. This is the negative half of the pair
     * above: on its own it would pass with the preference read deleted, but together with
     * snoozeLowStoresDeadlineForLowAlertsOnly it pins that the receiver acts on Tasker's action
     * and nothing else.
     */
    @Test
    public void foreignActionStoresNothing() {
        // :: Act
        new FireReceiver().onReceive(xdrip.getAppContext(),
                new Intent("com.example.SOMETHING_ELSE"));

        // :: Verify
        assertThat(storedDeadline(LOW_SNOOZE_KEY)).isEqualTo(0);
        assertThat(storedDeadline(HIGH_SNOOZE_KEY)).isEqualTo(0);
    }

    // ===== Helpers ===============================================================================

    private Intent taskerIntent(String message) {
        Bundle bundle = new Bundle();
        bundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE, message);
        bundle.putInt(PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE, 1);
        return new Intent(com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING)
                .putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, bundle);
    }

    private long storedDeadline(String key) {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getLong(key, 0);
    }
}
