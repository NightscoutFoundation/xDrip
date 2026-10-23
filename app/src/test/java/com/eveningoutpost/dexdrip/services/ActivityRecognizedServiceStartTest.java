package com.eveningoutpost.dexdrip.services;

import android.content.Intent;

import com.eveningoutpost.dexdrip.RefusingContext;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Covers {@link ActivityRecognizedService#startActivityRecogniser} when Android refuses the start.
 * <p>
 * This runs from {@code xdrip.onCreate} for anyone with motion tracking switched on, two lines after
 * the sync service. In a process Android started only to deliver a broadcast the start is refused at
 * target SDK 26, and an unguarded refusal here takes the process down just as surely as one further
 * up. Motion tracking simply stays off on such a process until the app is brought up in its own right.
 *
 * @author Asbjørn Aarrestad
 */
public class ActivityRecognizedServiceStartTest extends RobolectricTestWithConfig {

    private RefusingContext refusing;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        refusing = new RefusingContext(RuntimeEnvironment.application);
    }

    // ======================================= startActivityRecogniser ========================================

    /** The start is attempted against the recogniser, carrying its action, and the refusal stays inside. */
    @Test
    public void startActivityRecogniserAttemptsTheStartAndSwallowsTheRefusal() {
        // :: Act
        ActivityRecognizedService.startActivityRecogniser(refusing);

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        final Intent attempted = refusing.attempted().get(0);
        assertWithMessage("target service")
                .that(RefusingContext.targetClassOf(attempted))
                .isEqualTo(ActivityRecognizedService.class.getName());
        assertWithMessage("start action")
                .that(attempted.getStringExtra("START_ACTIVITY_ACTION"))
                .isEqualTo("START_ACTIVITY_ACTION");
    }

    /** A refused start must not stop the caller from getting to its next statement. */
    @Test
    public void aRefusedStartDoesNotStopTheNextOne() {
        // :: Act
        ActivityRecognizedService.startActivityRecogniser(refusing);
        ActivityRecognizedService.startActivityRecogniser(refusing);

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(2);
    }
}
