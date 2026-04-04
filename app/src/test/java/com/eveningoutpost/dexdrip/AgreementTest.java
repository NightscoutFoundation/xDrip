package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

public class AgreementTest extends RobolectricTestWithConfig {

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- onCreate ---

    /** Characterization: IUnderstand defaults to false when preference is not set */
    @Test
    public void onCreate_iUnderstandDefaultsFalse() {
        Agreement activity = Robolectric.buildActivity(Agreement.class).create().get();

        assertThat(activity.IUnderstand).isFalse();
    }

    /** Characterization: IUnderstand is true when preference has been set */
    @Test
    public void onCreate_iUnderstandTrueWhenPreferenceSet() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                xdrip.getAppContext());
        prefs.edit().putBoolean(Agreement.prefmarker, true).commit();

        Agreement activity = Robolectric.buildActivity(Agreement.class).create().get();

        assertThat(activity.IUnderstand).isTrue();
    }
}
