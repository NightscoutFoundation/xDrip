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
 * Behavioural tests for the "run service in foreground" listener that {@link WifiCollectionService} registers on the
 * default preference store when it is created.
 * <p>
 * The tests flip the setting in the store the settings screen writes to, and read the service's log to see whether
 * the listener heard it.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class WifiCollectionServiceForegroundPreferenceTest extends RobolectricTestWithConfig {

    private static final String KEY = "run_service_in_foreground";

    private ServiceController<WifiCollectionService> service;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        ShadowLog.clear();
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        if (service != null) service.destroy();
        Inevitable.kill("foreground-status"); // ForegroundServiceStarter.start() schedules it on its own thread
    }

    // ===== Foreground listener =======================================================================================

    /** Turning the setting on while the service runs moves the service to the foreground. */
    @Test
    public void turningTheSettingOnMovesTheServiceToTheForeground() {
        // :: Setup
        startService();

        // :: Act
        store().edit().putBoolean(KEY, true).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Moving to foreground");
    }

    /** Turning the setting off while the service runs takes the service out of the foreground. */
    @Test
    public void turningTheSettingOffTakesTheServiceOutOfTheForeground() {
        // :: Setup
        store().edit().putBoolean(KEY, true).commit();
        startService();

        // :: Act
        store().edit().putBoolean(KEY, false).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Removing from foreground");
    }

    /** Once the service is destroyed it stops listening, so a later change does nothing. */
    @Test
    public void aStoppedServiceNoLongerListens() {
        // :: Setup
        startService();
        stopService();
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean(KEY, true).commit();

        // :: Verify
        assertThat(serviceLog()).doesNotContain("Moving to foreground");
    }

    // =================================================================================================================

    private void startService() {
        service = Robolectric.buildService(WifiCollectionService.class).create();
    }

    private void stopService() {
        service.destroy();
        service = null;
    }

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag("WifiCollectionService").stream().map(l -> l.msg).collect(Collectors.toList());
    }
}
