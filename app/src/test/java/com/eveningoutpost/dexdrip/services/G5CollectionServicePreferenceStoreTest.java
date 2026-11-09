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
 * Behavioural tests for the settings {@link G5CollectionService} reads from the default preference store: the
 * foreground listener it registers when created, the transmitter ID and the four Bluetooth workaround switches.
 * <p>
 * Each test writes to the store the settings screen writes to, and reads the service's log to see what it found.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class G5CollectionServicePreferenceStoreTest extends RobolectricTestWithConfig {

    private ServiceController<G5CollectionService> service;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        G5CollectionService.keep_running = true; // stopping the collector elsewhere in the suite clears it
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        if (service != null) service.destroy();
        Inevitable.kill("foreground-status"); // ForegroundServiceStarter.start() schedules it on its own thread
        G5CollectionService.keep_running = true; // a start without the G5 collector selected clears it
    }

    // ===== Foreground listener =======================================================================================

    /** Turning the setting off while the service runs reaches the service's listener. */
    @Test
    public void turningForegroundOffReachesTheG5Collector() {
        // :: Setup
        service = Robolectric.buildService(G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Removing from foreground");
    }

    // ===== Transmitter ID ============================================================================================

    /** The transmitter ID the service works with is the one stored in the settings. */
    @Test
    public void transmitterIdComesFromTheSettings() {
        // :: Setup
        store().edit().putString("dex_txid", "4G7H8J").commit();
        service = Robolectric.buildService(G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        service.get().evaluateG6Settings(); // logs the ID, stops at the null adapter; no firmware details, no G6 switch

        // :: Verify
        assertThat(serviceLog()).contains("Transmitter: 4G7H8J");
    }

    /** A stopped service no longer hears the foreground setting, even after it has read the transmitter ID. */
    @Test
    public void stoppedServiceNoLongerListensAfterReadingTheTransmitterId() {
        // :: Setup
        service = Robolectric.buildService(G5CollectionService.class).create();
        service.get().evaluateG6Settings(); // re-reads the store the listener is removed from
        service.destroy();
        service = null; // already destroyed
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Verify
        assertThat(serviceLog()).doesNotContain("Removing from foreground");
    }

    // ===== Workaround switches =======================================================================================

    /** A start logs the four Bluetooth workaround switches the user turned on. */
    @Test
    public void startReportsTheWorkaroundSwitchesTurnedOn() {
        // :: Setup
        store().edit()
                .putBoolean("run_ble_scan_constantly", true)
                .putBoolean("always_unbond_G5", true)
                .putBoolean("always_get_new_keys", true)
                .putBoolean("run_G5_ble_tasks_on_uithread", true)
                .commit();
        service = Robolectric.buildService(G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        service.startCommand(0, 1);

        // :: Verify
        assertThat(serviceLog()).contains("settingsToString: scanConstantly alwaysUnbond alwaysAuthenticate "
                + "enforceMainThread delayOn133Errors tryOnDemandBondWithDelay "); // last two are fixed defaults
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag("G5CollectionService").stream()
                .map(l -> l.msg)
                .collect(Collectors.toList());
    }
}
