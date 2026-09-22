package com.eveningoutpost.dexdrip.services.broadcastservice;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.BroadcastModel;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the glucose unit that {@link BroadcastService} puts on the wire.
 * <p>
 * {@code prepareBgBundle} reads the stored unit and passes it on as {@code doMgdl}, which is what
 * every third-party app bound to the broadcast API scales its numbers by. The bundle is the
 * service's output, so the tests build one and read the flag back out.
 * <p>
 * The graph half of the bundle is left switched off: {@link Settings#isDisplayGraph()} defaults to
 * false, and the graph block reads the same preferences that
 * {@code BgGraphBuilderPreferencesTest} already covers.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class BroadcastServicePreferencesTest extends RobolectricTestWithConfig {

    private BroadcastService service;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        service = new BroadcastService();
    }

    // ===== Glucose units on the wire =============================================================

    /** mg/dL is passed on as the unit flag third-party apps scale by. */
    @Test
    public void unitsMgdl_isBroadcastAsMgdl() {
        // :: Setup
        prefs().edit().putString("units", "mgdl").commit();

        // :: Act
        final Bundle sent = bundle();

        // :: Verify
        assertThat(sent.getBoolean("doMgdl")).isTrue();
    }

    /** Anything other than the literal "mgdl" is broadcast as mmol/L. */
    @Test
    public void unitsMmol_isBroadcastAsMmol() {
        // :: Setup
        prefs().edit().putString("units", "mmol").commit();

        // :: Act
        final Bundle sent = bundle();

        // :: Verify
        assertThat(sent.getBoolean("doMgdl")).isFalse();
    }

    /** With no stored unit at all the broadcast falls back to mg/dL. */
    @Test
    public void noStoredUnits_isBroadcastAsMgdl() {
        // :: Act
        final Bundle sent = bundle();

        // :: Verify
        assertThat(sent.getBoolean("doMgdl")).isTrue();
    }

    // ===== Nothing to broadcast ==================================================================

    /** Without a model there is nothing to send, and the preferences are never reached. */
    @Test
    public void noModel_producesNoBundle() {
        // :: Act
        final Bundle sent = service.prepareBgBundle(null);

        // :: Verify
        assertThat(sent).isNull();
    }

    /** A model whose settings never arrived is equally unsendable. */
    @Test
    public void modelWithoutSettings_producesNoBundle() {
        // :: Act
        final Bundle sent = service.prepareBgBundle(new BroadcastModel(null));

        // :: Verify
        assertThat(sent).isNull();
    }

    // ===== Helpers ===============================================================================

    private Bundle bundle() {
        return service.prepareBgBundle(new BroadcastModel(new Settings()));
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
