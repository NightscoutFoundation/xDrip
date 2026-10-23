package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the buttons on {@link NewSensorLocation}.
 * <p>
 * Save reports the chosen location in a toast and returns to the home screen. Cancel returns to the home screen
 * without a toast. Either way the screen closes.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class NewSensorLocationTest extends RobolectricTestWithConfig {

    private static final int PRIVATE_ID = 200;
    private static final int OTHER_ID = 201;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        ShadowToast.reset();                                     // toasts from earlier tests would satisfy the check
    }

    // ===== Save ======================================================================================================

    /** Saving with the default choice reports that the user does not wish to share, then goes home. */
    @Test
    public void save_defaultChoice_reportsPrivateAndGoesHome() {
        // :: Setup
        final NewSensorLocation activity = Robolectric.buildActivity(NewSensorLocation.class).create().get();

        // :: Act
        activity.findViewById(R.id.saveSensorLocation).performClick();

        // :: Verify
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Sensor locaton is I don't wish to share");
        assertGoesHomeAndCloses(activity);
    }

    /** Saving with "Other" reports the text the user typed. */
    @Test
    public void save_otherChoice_reportsTypedLocation() {
        // :: Setup
        final NewSensorLocation activity = Robolectric.buildActivity(NewSensorLocation.class).create().get();
        ((RadioGroup) activity.findViewById(R.id.myRadioGroup)).check(OTHER_ID);
        ((EditText) activity.findViewById(R.id.edit_sensor_location)).setText("Calf");

        // :: Act
        activity.findViewById(R.id.saveSensorLocation).performClick();

        // :: Verify
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo("Sensor locaton is Calf");
        assertGoesHomeAndCloses(activity);
    }

    // ===== Cancel ====================================================================================================

    /** Cancel goes home without reporting a location. */
    @Test
    public void cancel_goesHomeWithoutToast() {
        // :: Setup
        final NewSensorLocation activity = Robolectric.buildActivity(NewSensorLocation.class).create().get();
        assertThat(((RadioGroup) activity.findViewById(R.id.myRadioGroup)).getCheckedRadioButtonId())
                .isEqualTo(PRIVATE_ID);

        // :: Act
        activity.findViewById(R.id.saveSensorLocationCancel).performClick();

        // :: Verify
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        assertGoesHomeAndCloses(activity);
    }

    // ===== Helpers ===================================================================================================

    private static void assertGoesHomeAndCloses(final NewSensorLocation activity) {
        final Intent started = shadowOf(activity).getNextStartedActivity();
        assertThat(started).isNotNull();
        assertThat(started.getComponent().getClassName()).isEqualTo(Home.class.getName());
        assertThat(activity.isFinishing()).isTrue();
    }
}
