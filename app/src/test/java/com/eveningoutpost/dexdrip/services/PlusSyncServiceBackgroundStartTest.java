package com.eveningoutpost.dexdrip.services;

import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.RefusingContext;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Covers {@link PlusSyncService#startSyncService} when Android refuses the start.
 * <p>
 * This is the first service {@code xdrip.onCreate} starts on its own stack, so in a process Android
 * started only to deliver a broadcast it is the first thing to hit the target SDK 26 background
 * restriction: that process counts as being in the background, {@code Context.startService} throws,
 * and the process died before the receiver ever ran. The sync service simply does not start on such
 * a process; it starts as usual the next time the app or a collector brings the process up itself.
 *
 * @author Asbjørn Aarrestad
 */
public class PlusSyncServiceBackgroundStartTest extends RobolectricTestWithConfig {

    private RefusingContext refusing;
    private boolean createdBefore;
    private String tokenBefore;
    private boolean ceaseBefore;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);

        createdBefore = PlusSyncService.created;
        tokenBefore = GcmActivity.token;
        ceaseBefore = GcmActivity.cease_all_activity;
        PlusSyncService.created = false;
        GcmActivity.token = null;
        GcmActivity.cease_all_activity = false;

        refusing = new RefusingContext(RuntimeEnvironment.application);
        PreferenceManager.getDefaultSharedPreferences(refusing).edit().clear().commit();
    }

    @After
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application).edit().clear().commit();
        PlusSyncService.created = createdBefore;
        GcmActivity.token = tokenBefore;
        GcmActivity.cease_all_activity = ceaseBefore;
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    // ========================================== startSyncService ============================================

    /** The start is attempted against the sync service and the refusal stays inside the method. */
    @Test
    public void startSyncServiceAttemptsTheStartAndSwallowsTheRefusal() {
        // :: Act
        PlusSyncService.startSyncService(refusing, "test");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(1);
        assertWithMessage("target service")
                .that(RefusingContext.targetClassOf(refusing.attempted().get(0)))
                .isEqualTo(PlusSyncService.class.getName());
    }

    /** A refused start must leave nothing latched behind that would block the attempt after it. */
    @Test
    public void aRefusedStartDoesNotStopTheNextOne() {
        // :: Act
        PlusSyncService.startSyncService(refusing, "first");
        PlusSyncService.startSyncService(refusing, "second");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).hasSize(2);
        assertWithMessage("service marked as created").that(PlusSyncService.created).isFalse();
    }

    /** With sync switched off nothing is attempted at all - the guard must not change that. */
    @Test
    public void startSyncServiceAttemptsNothingWhenSyncIsDisabled() {
        // :: Setup
        PreferenceManager.getDefaultSharedPreferences(refusing)
                .edit().putBoolean("disable_all_sync", true).commit();

        // :: Act
        PlusSyncService.startSyncService(refusing, "test");

        // :: Verify
        assertWithMessage("attempted starts").that(refusing.attempted()).isEmpty();
        assertWithMessage("all activity ceased").that(GcmActivity.cease_all_activity).isTrue();
    }

}
