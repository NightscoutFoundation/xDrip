package com.eveningoutpost.dexdrip;

import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Characterization test for {@link BGHistory}. Building the activity runs onCreate →
 * setupCharts → setupStatistics, which is where PreferenceManager.getDefaultSharedPreferences()
 * is called (feeding StatsResult). This locks in that the screen still builds before/after the
 * android.preference → androidx.preference import swap.
 *
 * @author Asbjørn Aarrestad
 */
public class BGHistoryTest extends RobolectricTestWithConfig {

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- onCreate ---

    /** Characterization: the activity builds, populating the statistics text view via prefs. */
    @Test
    public void onCreate_buildsAndPopulatesStatistics() {
        // :: Act
        BGHistory activity = Robolectric.buildActivity(BGHistory.class).create().get();

        // :: Verify
        assertThat(activity).isNotNull();
        TextView stats = activity.findViewById(R.id.historystats);
        assertThat(stats).isNotNull();
    }
}
