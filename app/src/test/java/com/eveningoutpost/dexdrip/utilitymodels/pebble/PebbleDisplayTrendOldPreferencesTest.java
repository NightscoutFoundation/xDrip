package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the special-message preferences the Pebble watchface receives.
 * <p>
 * {@code buildDictionary} compares the glucose it is about to send against
 * {@code pebble_special_value}, and on a match replaces the message with {@code pebble_special_text}.
 * The dictionary it returns is the watch's payload, so the tests read the message back out of it.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class PebbleDisplayTrendOldPreferencesTest extends RobolectricTestWithConfig {

    private PebbleDisplayTrendOld display;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        publishBatteryLevel();                                  // the dictionary carries a battery status

        long end = JoH.tsl();
        display = new PebbleDisplayTrendOld();
        display.initDisplay(xdrip.getAppContext(), null,
                new BgGraphBuilder(xdrip.getAppContext(), end - Constants.DAY_IN_MS, end));
        display.dg = displayGlucose("5.5");
    }

    // ===== Special message =======================================================================

    /** A glucose matching the special value swaps the message for the stored special text. */
    @Test
    public void glucoseMatchingTheSpecialValue_sendsTheSpecialText() {
        // :: Setup
        prefs().edit()
                .putString("pebble_special_value", "5.5")
                .putString("pebble_special_text", "steady as she goes")
                .commit();

        // :: Act
        final String sent = message();

        // :: Verify
        assertThat(sent).isEqualTo("steady as she goes");
    }

    /** With no special text stored the match still fires, on the built-in text. */
    @Test
    public void glucoseMatchingTheSpecialValue_withNoStoredText_sendsTheDefault() {
        // :: Setup
        prefs().edit().putString("pebble_special_value", "5.5").commit();

        // :: Act
        final String sent = message();

        // :: Verify
        assertThat(sent).isEqualTo("BAZINGA!");
    }

    /** A glucose that does not match leaves the message empty, whatever the special text says. */
    @Test
    public void glucoseNotMatchingTheSpecialValue_sendsNoMessage() {
        // :: Setup
        prefs().edit()
                .putString("pebble_special_value", "9.9")
                .putString("pebble_special_text", "steady as she goes")
                .commit();

        // :: Act
        final String sent = message();

        // :: Verify
        assertThat(sent).isEmpty();
    }

    /** With no special value stored nothing can match, so the message stays empty. */
    @Test
    public void noSpecialValueStored_sendsNoMessage() {
        // :: Act
        final String sent = message();

        // :: Verify
        assertThat(sent).isEmpty();
    }

    // ===== Helpers ===============================================================================

    private String message() {
        PebbleDictionary dictionary = display.buildDictionary();
        return dictionary.getString(PebbleDisplayTrendOld.MESSAGE_KEY);
    }

    private static BestGlucose.DisplayGlucose displayGlucose(String unitized) {
        BestGlucose.DisplayGlucose dg = new BestGlucose.DisplayGlucose();
        dg.unitized = unitized;
        dg.mgdl = 99;
        dg.timestamp = JoH.tsl();
        dg.mssince = 0;         // fresh, so the no-signal branch stays out of the way
        dg.delta_name = "Flat";
        return dg;
    }

    /** The battery status is read from the sticky broadcast, which the test environment has none of. */
    private static void publishBatteryLevel() {
        Intent battery = new Intent(Intent.ACTION_BATTERY_CHANGED);
        battery.putExtra(BatteryManager.EXTRA_LEVEL, 50);
        battery.putExtra(BatteryManager.EXTRA_SCALE, 100);
        RuntimeEnvironment.application.sendStickyBroadcast(battery);
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
