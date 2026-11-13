package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the settings the watch's {@link G5CollectionService} reads from the default preference store:
 * the listener it registers when created, the transmitter ID and the five Bluetooth workaround switches.
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
        store().edit().clear().commit();
        G5CollectionService.keep_running = true; // a start without the G5 collector selected clears it
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        G5CollectionService.keep_running = false; // a scan its timer thread starts after this returns at once
        if (service != null) service.destroy();
        PersistentStore.setBoolean(CollectionServiceStarter.pref_run_wear_collector, false);
    }

    // ===== Settings listener =========================================================================================

    /** Changing a Bluetooth workaround switch while the service runs reaches the service's listener. */
    @Test
    public void changingAWorkaroundSwitchReachesTheG5Collector() {
        // :: Setup
        service = Robolectric.buildService(G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean("always_unbond_G5", true).commit();

        // :: Verify
        assertThat(serviceLog()).contains("G5 Setting Change");
    }

    // ===== Transmitter ID ============================================================================================

    /** The transmitter ID the service works with is the one stored in the settings. */
    @Test
    public void transmitterIdComesFromTheSettings() {
        // :: Setup
        store().edit()
                .putString("dex_collection_method", "DexcomG5")
                .putString("dex_txid", "4G7H8J")
                .commit();
        PersistentStore.setBoolean(CollectionServiceStarter.pref_run_wear_collector, true);
        BluetoothAdapter.getDefaultAdapter().enable(); // otherwise it schedules retries on its own timer threads
        service = Robolectric.buildService(G5CollectionService.class).create().startCommand(0, 1); // gets the adapter
        ShadowLog.clear();

        // :: Act
        service.get().setupBluetooth();

        // :: Verify
        assertThat(serviceLog()).contains("Transmitter: 4G7H8J");
    }

    // ===== Workaround switches =======================================================================================

    /** A start logs the five Bluetooth workaround switches the user turned on. */
    @Test
    public void startReportsTheWorkaroundSwitchesTurnedOn() {
        // :: Setup
        store().edit()
                .putBoolean("run_ble_scan_constantly", true)
                .putBoolean("always_unbond_G5", true)
                .putBoolean("always_get_new_keys", true)
                .putBoolean("run_G5_ble_tasks_on_uithread", true)
                .putBoolean("always-on-screen", true)
                .commit();
        service = Robolectric.buildService(G5CollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        service.startCommand(0, 1);

        // :: Verify
        assertThat(serviceLog()).contains("settingsToString: scanConstantly alwaysUnbond alwaysAuthenticate "
                + "enforceMainThread delayOn133Errors tryOnDemandBondWithDelay " // these two are fixed defaults
                + "alwaysOnScreem ");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag(G5CollectionService.TAG).stream()
                .map(l -> l.msg)
                .collect(Collectors.toList());
    }
}
