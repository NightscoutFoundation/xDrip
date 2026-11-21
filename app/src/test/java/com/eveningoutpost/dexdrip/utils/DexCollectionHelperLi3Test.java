package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.li3.Li3Activity;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link DexCollectionHelper#assistance} with the Li3 collector: picking Li3 in the source wizard opens the
 * Li3 screen, where its setup steps are.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class DexCollectionHelperLi3Test extends RobolectricTestWithConfig {

    // =================================================== Assistance ==================================================

    /** Choosing Li3 opens the Li3 screen. */
    @Test
    public void assistance_li3_opensLi3Screen() {
        // :: Setup
        xdrip.setContextAlways(RuntimeEnvironment.application);
        Activity wizard = Robolectric.buildActivity(Activity.class).setup().get();

        // :: Act
        DexCollectionHelper.assistance(wizard, DexCollectionType.Li3);

        // :: Verify
        Intent opened = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertThat(opened).isNotNull();
        assertThat(opened.getComponent().getClassName()).isEqualTo(Li3Activity.class.getName());
    }

    /** Another collector without its own steps still opens nothing. */
    @Test
    public void assistance_gluPro_opensNothing() {
        // :: Setup
        xdrip.setContextAlways(RuntimeEnvironment.application);
        Activity wizard = Robolectric.buildActivity(Activity.class).setup().get();

        // :: Act
        DexCollectionHelper.assistance(wizard, DexCollectionType.GluPro);

        // :: Verify
        assertThat(shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isNull();
    }
}
