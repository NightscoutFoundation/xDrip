package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class IdempotentMigrationsTest extends RobolectricTestWithConfig {

    private static final String oldPref = "calibrate_external_libre_2_algorithm";
    private static final String newPref = "calibrate_external_libre_2_algorithm_type";

    private Map<String, ?> preferencesBefore;

    @Before
    public void before() {
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        preferencesBefore = new HashMap<>(Pref.getInstance().getAll());
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        cleanup();
    }

    @After
    public void after() {
        restorePreferences();
    }

    private void cleanup() {
        Pref.removeItem(oldPref);
        Pref.removeItem(newPref);
    }

    /**
     * performAll() writes around three dozen preferences that have nothing to do with what is
     * asserted here, and the whole module shares one preference store for the lifetime of the JVM,
     * so anything left behind is visible to every later test class. This puts the store back
     * exactly as it was found. The two AlertType rows performAll() inserts are not undone.
     */
    private void restorePreferences() {
        final SharedPreferences.Editor editor = Pref.getInstance().edit();
        for (final String key : new HashSet<>(Pref.getInstance().getAll().keySet())) {
            if (!preferencesBefore.containsKey(key)) {
                editor.remove(key);
            }
        }
        for (final Map.Entry<String, ?> entry : preferencesBefore.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof Boolean) {
                editor.putBoolean(entry.getKey(), (Boolean) value);
            } else if (value instanceof String) {
                editor.putString(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                editor.putInt(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(entry.getKey(), (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(entry.getKey(), (Float) value);
            }
        }
        editor.commit();
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

    // ===== legacySettingsFix =====================================================================

    /**
     * The migration must keep forcing bridge battery alerts off. The preference is not
     * Parakeet-specific — it also gates the alerts for every other bridge — so removing that line
     * along with the Parakeet ones would switch low battery alerts back on for every user.
     */
    @Test
    public void performAllForcesBridgeBatteryAlertsOff() {
        // :: Setup
        Pref.setBoolean("bridge_battery_alerts", true);
        Pref.setString("bridge_battery_alert_level", "5");

        // :: Act
        new IdempotentMigrations(RuntimeEnvironment.getApplication().getApplicationContext()).performAll();

        // :: Verify
        assertWithMessage("bridge battery alerts stay forced off")
                .that(Pref.getBooleanDefaultFalse("bridge_battery_alerts")).isFalse();
        assertWithMessage("the alert level stays pinned to its migrated default")
                .that(Pref.getString("bridge_battery_alert_level", "")).isEqualTo("30");
    }

    // ===== Legacy REST URI migration =============================================================

    /** A credential-prefixed legacy URL is folded into the URI authority and gains a trailing slash. */
    @Test
    public void legacyCredentialPrefixedUrlIsRewritten() {
        // :: Setup
        storeBaseUrl("user:pass@http://ns.example.com/api/v1");

        // :: Act
        new IdempotentMigrations(xdrip.getAppContext()).performAll();

        // :: Verify
        assertThat(storedBaseUrl()).isEqualTo("http://user:pass@ns.example.com/api/v1/");
    }

    /** An already-modern URL is left alone apart from the trailing slash the migration guarantees. */
    @Test
    public void modernUrlIsLeftAlone() {
        // :: Setup
        storeBaseUrl("http://user:pass@ns.example.com/api/v1/");

        // :: Act
        new IdempotentMigrations(xdrip.getAppContext()).performAll();

        // :: Verify
        assertThat(storedBaseUrl()).isEqualTo("http://user:pass@ns.example.com/api/v1/");
    }

    // ===== Helpers ===============================================================================

    private void storeBaseUrl(String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("cloud_storage_api_base", value).commit();
    }

    private String storedBaseUrl() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .getString("cloud_storage_api_base", "");
    }
}
