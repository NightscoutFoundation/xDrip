package com.eveningoutpost.dexdrip.insulin;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Tests for {@link InsulinManager} bolus-profile invariant.
 *
 * <p>Regression coverage for xDrip discussion #4617: in legacy (single-insulin) mode a null
 * bolus profile made the injection list come back empty, so IoB/COB/prediction silently vanished.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class InsulinManagerTest extends RobolectricTestWithConfig {

    /** Force a private static field, to simulate a partially-initialized manager. */
    private static void setStaticField(final String name, final Object value) throws Exception {
        final Field f = InsulinManager.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    public void getBolusProfile_fallsBackToFirstProfile_whenPointerNullButProfilesLoaded() throws Exception {
        // :: Setup - load profiles (this also sets bolusProfile), then simulate the broken state
        final ArrayList<Insulin> profiles = InsulinManager.getDefaultInstance();
        setStaticField("bolusProfile", null);

        // :: Act
        final Insulin result = InsulinManager.getBolusProfile();

        // :: Verify - never null; falls back to the first (default) profile
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(profiles.get(0).getName());
    }

    @Test
    public void getBolusProfile_keepsSelectedProfile_whenAlreadySet() {
        // :: Setup - select a specific, non-default profile
        final ArrayList<Insulin> profiles = InsulinManager.getDefaultInstance();
        InsulinManager.setBolusProfile(profiles.get(1));

        // :: Act
        final Insulin result = InsulinManager.getBolusProfile();

        // :: Verify - the fallback must not clobber a genuine selection
        assertThat(result.getName()).isEqualTo(profiles.get(1).getName());
    }
}
