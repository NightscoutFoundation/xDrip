package com.eveningoutpost.dexdrip.UtilityModels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IdempotentMigrationsTest extends RobolectricTestWithConfig {

    private static final String oldPref = "calibrate_external_libre_2_algorithm";
    private static final String newPref = "calibrate_external_libre_2_algorithm_type";

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem(oldPref);
        Pref.removeItem(newPref);
    }

    @Test
    public void migrateOOP2CalibrationPreferencesTest() {

        assertWithMessage("null old 1").that(Pref.isPreferenceSet(oldPref)).isFalse();
        assertWithMessage("null new 1").that(Pref.isPreferenceSet(newPref)).isFalse();
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("null old 2").that(Pref.isPreferenceSet(oldPref)).isFalse();
        assertWithMessage("null new 2").that(Pref.isPreferenceSet(newPref)).isFalse();

        Pref.setBoolean(oldPref, true);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 3").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 3").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected 1").that(Pref.getString(newPref, "error")).isEqualTo("calibrate_raw");

        Pref.setBoolean(oldPref, false);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 4").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 4").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected no change 2").that(Pref.getString(newPref, "error")).isEqualTo("calibrate_raw");

        cleanup();
        Pref.setBoolean(oldPref, false);
        IdempotentMigrations.migrateOOP2CalibrationPreferences();
        assertWithMessage("set old 5").that(Pref.isPreferenceSet(oldPref)).isTrue();
        assertWithMessage("set new 5").that(Pref.isPreferenceSet(newPref)).isTrue();
        assertWithMessage("as expected 3").that(Pref.getString(newPref, "error")).isEqualTo("no_calibration");

    }

}