package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.xdrip;

import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural test for the "run service in foreground" listener that {@link Ob1G5CollectionService} registers on
 * the default preference store when it is created.
 * <p>
 * The test flips the setting in the store the settings screen writes to, and reads the service's log to see whether
 * the listener heard it.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Ob1G5CollectionServicePreferenceStoreTest extends RobolectricTestWithConfig {

    private ServiceController<Ob1G5CollectionService> service;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        if (service != null) service.destroy();
        Inevitable.kill("foreground-status"); // ForegroundServiceStarter.start() schedules it on its own thread
    }

    // ===== Foreground listener =======================================================================================

    /** Turning the setting off while the service runs reaches the service's listener. */
    @Test
    public void turningForegroundOffReachesTheCollector() {
        // :: Setup
        service = Robolectric.buildService(Ob1G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Removing from foreground");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag("Ob1G5CollectionService").stream().map(l -> l.msg).collect(Collectors.toList());
    }
}
