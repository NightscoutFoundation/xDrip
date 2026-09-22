package com.eveningoutpost.dexdrip.models;

import android.content.Intent;

import com.eveningoutpost.dexdrip.RefusingContext;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.services.PlusSyncService;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Covers {@link JoH#startService} when Android refuses the start.
 * <p>
 * From target SDK 26 a process that Android started to deliver a broadcast counts as being in the
 * background, so {@code Context.startService} throws instead of starting anything. Most callers
 * reach this method through {@code Inevitable}, which runs the task on a plain thread with nothing
 * catching what it throws, so the refusal took the whole process down from a worker thread. These
 * tests pin the caller-visible half of that: a refused start must not propagate, and it must not
 * stop whatever the caller does next.
 *
 * @author Asbjørn Aarrestad
 */
public class JoHStartServiceTest extends RobolectricTestWithConfig {

    private RefusingContext refusing;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        refusing = new RefusingContext(RuntimeEnvironment.application);
        xdrip.setContextAlways(refusing);
    }

    @After
    public void tearDown() {
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    // ============================================ startService ==============================================

    /** The start is attempted against the right service and the refusal stays inside JoH. */
    @Test
    public void startServiceAttemptsTheStartAndSwallowsTheRefusal() {
        // :: Act
        JoH.startService(MissedReadingService.class);

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        assertWithMessage("target service")
                .that(RefusingContext.targetClassOf(refusing.attempted().get(0)))
                .isEqualTo(MissedReadingService.class.getName());
    }

    /** A refused start must not stop the caller from getting to its next statement. */
    @Test
    public void aRefusedStartDoesNotStopTheNextOne() {
        // :: Act
        JoH.startService(PlusSyncService.class);
        JoH.startService(MissedReadingService.class);

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(2);
        assertWithMessage("first target")
                .that(RefusingContext.targetClassOf(refusing.attempted().get(0)))
                .isEqualTo(PlusSyncService.class.getName());
        assertWithMessage("second target")
                .that(RefusingContext.targetClassOf(refusing.attempted().get(1)))
                .isEqualTo(MissedReadingService.class.getName());
    }

    /** The argument overload builds its intent as usual and survives the refusal the same way. */
    @Test
    public void startServiceWithArgumentsStillCarriesThemWhenRefused() {
        // :: Act
        JoH.startService(MissedReadingService.class, "function", "test", "count", "2");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        final Intent attempted = refusing.attempted().get(0);
        assertWithMessage("function extra").that(attempted.getStringExtra("function")).isEqualTo("test");
        assertWithMessage("count extra").that(attempted.getStringExtra("count")).isEqualTo("2");
    }

    /** An odd argument count is a programming error and must still be reported, not swallowed. */
    @Test
    public void startServiceStillRejectsAnOddNumberOfArguments() {
        // :: Act
        try {
            JoH.startService(MissedReadingService.class, "function");
            assertWithMessage("odd argument count was accepted").fail();
        } catch (RuntimeException e) {
            // :: Verify
            assertWithMessage("reason").that(e.getMessage()).contains("Odd number of args");
        }
        assertWithMessage("no start attempted").that(refusing.attempted()).isEmpty();
    }

}
