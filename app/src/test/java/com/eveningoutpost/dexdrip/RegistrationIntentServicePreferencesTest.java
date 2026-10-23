package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests which preference store {@link RegistrationIntentService} records the token state in.
 * <p>
 * When the push token cannot be fetched, the service records that it was not sent, so a later run
 * tries again. Only this failure branch runs here: without Firebase set up, fetching the token throws.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class RegistrationIntentServicePreferencesTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
    }

    // ===== Token not sent ============================================================================================

    /** A failed token fetch clears the sent flag in the default preference store. */
    @Test
    public void tokenFetchFails_recordsTokenNotSent() {
        // :: Setup
        prefs().edit().putBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, true).commit();
        final RegistrationIntentService service =
                Robolectric.buildService(RegistrationIntentService.class).create().get();

        // :: Act
        service.onHandleIntent(new Intent());

        // :: Verify
        assertThat(prefs().getBoolean(PreferencesNames.SENT_TOKEN_TO_SERVER, true)).isFalse();
    }

    // ===== Helpers ===================================================================================================

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
