package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the settings {@link Notifications#ReadPerfs} copies into the fields the alert
 * code reads.
 * <p>
 * Whether glucose notifications are on, which sound they use, and whether values are shown in mg/dL all
 * come from the default preference store each time the alerts are checked.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class NotificationsPreferencesTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    /** The fields are public statics, so put them back to the defaults for later test classes. */
    @After
    public void tearDown() {
        prefs().edit().clear().commit();
        new Notifications().ReadPerfs(RuntimeEnvironment.application);
    }

    // ===== Stored values reach the alert code ========================================================================

    /** Stored non-default values reach the fields the alert code reads. */
    @Test
    public void readPerfs_copiesTheStoredValues() {
        // :: Setup
        prefs().edit()
                .putBoolean("bg_notifications", false)
                .putString("bg_notification_sound", "content://test/sound")
                .putString("units", "mmol")
                .commit();

        // :: Act
        new Notifications().ReadPerfs(RuntimeEnvironment.application);

        // :: Verify
        assertThat(Notifications.bg_notifications).isFalse();
        assertThat(Notifications.bg_notification_sound).isEqualTo("content://test/sound");
        assertThat(Notifications.doMgdl).isFalse();
    }

    // ===== Defaults ==================================================================================================

    /** With nothing stored, the defaults apply: notifications on, default sound, mg/dL. */
    @Test
    public void readPerfs_nothingStored_usesTheDefaults() {
        // :: Setup
        // nothing stored; setUp cleared the preference store

        // :: Act
        new Notifications().ReadPerfs(RuntimeEnvironment.application);

        // :: Verify
        assertThat(Notifications.bg_notifications).isTrue();
        assertThat(Notifications.bg_notification_sound).isEqualTo("default");
        assertThat(Notifications.doMgdl).isTrue();
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
