package com.eveningoutpost.dexdrip.stats;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the range counts in {@link DBSearchUtil}.
 * <p>
 * The counts classify stored readings against the highValue/lowValue preferences, interpreting
 * them in whichever unit the units preference names. Asserting the same seeded readings under both
 * mgdl and equivalent mmol thresholds pins the preference read and its rescaling, so the tests
 * survive the android.preference -> androidx PreferenceManager swap only if that is unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class DBSearchUtilTest extends RobolectricTestWithConfig {

    private int originalStatsState;

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app

        // The ActiveAndroid database is process-wide and is not reset between test methods or
        // classes, so start from a known-empty table and leave it that way (see tearDown).
        BgReading.deleteALL();

        originalStatsState = StatsActivity.state;
        StatsActivity.state = StatsActivity.D7;                 // deterministic 7-day window

        seedReadings();
    }

    @After
    public void tearDown() {
        BgReading.deleteALL();
        StatsActivity.state = originalStatsState; // mutable public static shared with other tests
    }

    // --- Classification in mgdl ---

    /** With high=170 / low=70 mgdl the seeded 50/120/300 readings split one to each bucket. */
    @Test
    public void classifiesSeededReadings_mgdl() {
        // :: Setup
        setThresholds("mgdl", "170", "70");

        // :: Act
        int below = below();
        int inRange = inRange();
        int above = above();

        // :: Verify
        assertThat(below).isEqualTo(1);
        assertThat(inRange).isEqualTo(1);
        assertThat(above).isEqualTo(1);
    }

    // --- Same classification in mmol ---

    /**
     * The equivalent mmol thresholds (9.4 mmol ~ 169.4 mgdl, 3.9 mmol ~ 70.3 mgdl) must reproduce
     * the mgdl split exactly. This is what proves the units preference is read and applied, rather
     * than the stored numbers being used raw as mgdl.
     */
    @Test
    public void classifiesSeededReadings_mmolMatchesMgdl() {
        // :: Setup
        setThresholds("mmol", "9.4", "3.9");

        // :: Act
        int below = below();
        int inRange = inRange();
        int above = above();

        // :: Verify
        assertThat(below).isEqualTo(1);
        assertThat(inRange).isEqualTo(1);
        assertThat(above).isEqualTo(1);
    }

    // --- Thresholds actually move the boundary ---

    /**
     * Raising the high threshold above every reading empties the above-range bucket and moves
     * that reading into range. A count that ignored the highValue preference would not change.
     */
    @Test
    public void raisingHighThresholdEmptiesAboveRange() {
        // :: Setup
        setThresholds("mgdl", "400", "70");

        // :: Act
        int above = above();
        int inRange = inRange();

        // :: Verify
        assertThat(above).isEqualTo(0);
        assertThat(inRange).isEqualTo(2);
    }

    /**
     * Lowering the low threshold below every reading empties the below-range bucket and moves
     * that reading into range.
     */
    @Test
    public void loweringLowThresholdEmptiesBelowRange() {
        // :: Setup
        setThresholds("mgdl", "170", "40");

        // :: Act
        int below = below();
        int inRange = inRange();

        // :: Verify
        assertThat(below).isEqualTo(0);
        assertThat(inRange).isEqualTo(2);
    }

    // --- Helpers ---

    private int below() {
        return DBSearchUtil.noReadingsBelowRange(xdrip.getAppContext());
    }

    private int inRange() {
        return DBSearchUtil.noReadingsInRange(xdrip.getAppContext());
    }

    private int above() {
        return DBSearchUtil.noReadingsAboveRange(xdrip.getAppContext());
    }

    private void setThresholds(String units, String highValue, String lowValue) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit()
                .putString("units", units)
                .putString("highValue", highValue)
                .putString("lowValue", lowValue)
                .commit();
    }

    /**
     * Seeds three readings inside the 7-day window: one clearly low, one mid-range, one high. All
     * are above the CUTOFF of 38 and unsynced, which the counting queries require.
     */
    private void seedReadings() {
        insertReading(50.0);
        insertReading(120.0);
        insertReading(300.0);
    }

    private void insertReading(final double calculatedValue) {
        final BgReading bg = new BgReading();
        bg.calculated_value = calculatedValue;
        bg.raw_data = calculatedValue;
        bg.timestamp = JoH.tsl() - Constants.HOUR_IN_MS;
        bg.save();
    }
}
