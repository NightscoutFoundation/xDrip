package com.eveningoutpost.dexdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Characterization tests for {@link BestGlucose}, locking in the behaviour that flows through
 * its {@code PreferenceManager.getDefaultSharedPreferences()} usage before/after the
 * android.preference → androidx.preference import swap.
 *
 * @author Asbjørn Aarrestad
 */
public class BestGlucoseTest extends RobolectricTestWithConfig {

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
    }

    // --- getDisplayGlucose ---

    /** Characterization: with no readings in the database, getDisplayGlucose returns null. */
    @Test
    public void getDisplayGlucose_returnsNullWhenNoReadings() {
        assertThat(BestGlucose.getDisplayGlucose()).isNull();
    }
}
