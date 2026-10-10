package com.eveningoutpost.dexdrip;

import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.UploaderQueue;
import com.eveningoutpost.dexdrip.utils.jobs.BackgroundQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.time.Duration;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the treatment-acceptance preference {@link NSClientReceiver} reads.
 * <p>
 * The receiver caches its preference store in a static. Every test resets that cache, so each
 * broadcast reads the store this test wrote to.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class NSClientReceiverTreatmentPreferenceTest extends RobolectricTestWithConfig {

    private static final long TREATMENT_TIME = 1_700_000_000_000L;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        NSClientReceiver.resetPrefsCacheForTests();
        Home.resetFollowerCacheForTests();
        Home.get_follower(); // caches "not a follower", so the sync job below never pushes to a master
        Treatments.delete_all();
    }

    /**
     * A saved treatment queues a sync job one second later on the app-wide background queue. Runs it
     * now, so it can't fire inside a later test, then removes the rows and preferences this class wrote.
     */
    @After
    public void tearDown() {
        shadowOf(BackgroundQueue.getInstance().getLooper()).idleFor(Duration.ofSeconds(2));
        new Delete().from(UploaderQueue.class).execute();
        Treatments.delete_all();
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        NSClientReceiver.resetPrefsCacheForTests();
    }

    // ===== Treatment acceptance preference ===========================================================================

    /** With treatments accepted, a received carb treatment is saved. */
    @Test
    public void treatmentIsSavedWhenAccepted() {
        // :: Setup
        storeAcceptTreatments(true);

        // :: Act
        receiveCarbTreatment();

        // :: Verify
        assertThat(Treatments.byTimestamp(TREATMENT_TIME)).isNotNull();
    }

    /**
     * With treatments refused, the same treatment is dropped. Accepting is the default, so this is the
     * test that proves the preference is read at all.
     */
    @Test
    public void treatmentIsDroppedWhenRefused() {
        // :: Setup
        storeAcceptTreatments(false);

        // :: Act
        receiveCarbTreatment();

        // :: Verify
        assertThat(Treatments.byTimestamp(TREATMENT_TIME)).isNull();
    }

    /** With nothing stored, treatments are accepted. */
    @Test
    public void treatmentIsSavedWhenNothingIsStored() {
        // :: Act
        receiveCarbTreatment();

        // :: Verify
        assertThat(Treatments.byTimestamp(TREATMENT_TIME)).isNotNull();
    }

    // ===== Helpers ===================================================================================================

    private void receiveCarbTreatment() {
        String json = "{\"eventType\":\"Carb Correction\",\"carbs\":15,\"enteredBy\":\"batch7\","
                + "\"mills\":" + TREATMENT_TIME + "}";
        Intent intent = new Intent(Intents.ACTION_NEW_TREATMENT).putExtra("treatment", json);
        new NSClientReceiver().onReceive(xdrip.getAppContext(), intent);
    }

    private void storeAcceptTreatments(boolean accept) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean("accept_nsclient_treatments", accept).commit();
    }
}
