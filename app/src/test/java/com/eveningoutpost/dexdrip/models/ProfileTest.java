package com.eveningoutpost.dexdrip.models;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.profileeditor.BasalRepository;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class ProfileTest extends RobolectricTestWithConfig {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    @Before
    public void before() {
        BasalRepository.dummyRatesForTesting();
    }

    @Before
    public void after() {
        BasalRepository.clearRates();
    }


    @Test
    public void getBasalRateTest() {
    }

    @Test
    public void getBasalRateAbsoluteFromPercentTest() {
        assertWithMessage("25% == 0.5 of 2.0").that(Profile.getBasalRatePercentFromAbsolute(1651949922000L, 0.5d)).isEqualTo(25);
    }

    @Test
    public void getBasalRatePercentFromAbsoluteTest() {
       assertWithMessage("50% == 1.0U").that(Profile.getBasalRateAbsoluteFromPercent(1651949922000L, 50)).isEqualTo(1.0d);
    }

    /**
     * Characterization: the no-arg reloadPreferences() reads defaults through
     * PreferenceManager.getDefaultSharedPreferences() and applies them (here the insulin action
     * time). Locks in behaviour across the android.preference → androidx.preference import swap.
     */
    @Test
    public void reloadPreferences_appliesInsulinActionTimeFromDefaultPrefs() {
        // :: Setup
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        prefs.edit().putString("xplus_insulin_dia", "5.5").commit();

        // :: Act
        Profile.reloadPreferences();

        // :: Verify
        assertThat(Profile.insulinActionTime(1651949922000L)).isEqualTo(5.5d);
    }
}