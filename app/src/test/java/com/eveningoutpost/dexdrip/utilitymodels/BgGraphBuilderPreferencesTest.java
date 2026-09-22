package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the preference-controlled fields of {@link BgGraphBuilder}.
 * <p>
 * The constructor reads four display preferences and assigns them to public fields, which makes this
 * the one file in the batch whose preference read is assertable with nothing but a constructor call.
 * The fields are the graph's glucose scale and its high/low markers, so a silent change here would
 * move every threshold line the user sees.
 * <p>
 * {@code forecastLowMark} is only asserted in its default shape. Its other branch is gated on
 * {@code Pref.getBoolean("low_value_is_forecast_low_threshold")}, and {@code Pref} freezes on the
 * first store it ever sees, so no test can flip it.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class BgGraphBuilderPreferencesTest extends RobolectricTestWithConfig {

    private static final double TOLERANCE = 0.0001;

    private long start;
    private long end;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        end = JoH.tsl();
        start = end - Constants.DAY_IN_MS;
    }

    // ===== Glucose units =========================================================================

    /** mg/dL is the stored value the graph scales against directly. */
    @Test
    public void unitsMgdl_makesTheBuilderMgdl() {
        // :: Setup
        prefs().edit().putString("units", "mgdl").commit();

        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.doMgdl).isTrue();
    }

    /** Anything other than the literal "mgdl" is treated as mmol/L. */
    @Test
    public void unitsMmol_makesTheBuilderMmol() {
        // :: Setup
        prefs().edit().putString("units", "mmol").commit();

        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.doMgdl).isFalse();
    }

    /** With no stored unit at all the graph falls back to mg/dL. */
    @Test
    public void noStoredUnits_defaultsToMgdl() {
        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.doMgdl).isTrue();
    }

    // ===== High and low markers ==================================================================

    /** The high and low markers come straight from the stored strings. */
    @Test
    public void storedThresholds_becomeTheHighAndLowMarks() {
        // :: Setup
        prefs().edit()
                .putString("highValue", "180")
                .putString("lowValue", "65")
                .commit();

        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.highMark).isWithin(TOLERANCE).of(180d);
        assertThat(builder.lowMark).isWithin(TOLERANCE).of(65d);
    }

    /** With nothing stored the markers fall back to 170 and 70. */
    @Test
    public void noStoredThresholds_fallBackTo170And70() {
        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.highMark).isWithin(TOLERANCE).of(170d);
        assertThat(builder.lowMark).isWithin(TOLERANCE).of(70d);
    }

    /** An unparseable threshold falls back to the same default rather than throwing. */
    @Test
    public void unparseableThreshold_fallsBackToTheDefault() {
        // :: Setup
        prefs().edit().putString("highValue", "not a number").commit();

        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.highMark).isWithin(TOLERANCE).of(170d);
    }

    /** By default the forecast low marker mirrors the low marker. */
    @Test
    public void forecastLowMark_mirrorsTheLowMarkByDefault() {
        // :: Setup
        prefs().edit().putString("lowValue", "72").commit();

        // :: Act
        final BgGraphBuilder builder = build();

        // :: Verify
        assertThat(builder.forecastLowMark).isWithin(TOLERANCE).of(builder.lowMark);
    }

    // ===== Helpers ===============================================================================

    private static android.content.SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private BgGraphBuilder build() {
        return new BgGraphBuilder(xdrip.getAppContext(), start, end);
    }
}
