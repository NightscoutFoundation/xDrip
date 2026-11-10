package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

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
 * Behavioural tests for the settings the watch's {@link DexCollectionService} reads from the default preference
 * store: the foreground listener it registers when created, and the transmitter ID a bridge beacon is checked against.
 * <p>
 * Each test writes to the store the settings screen writes to, and reads the service's log to see what it found.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class DexCollectionServicePreferenceStoreTest extends RobolectricTestWithConfig {

    /** The transmitter ID "6ABCD" as the service encodes it: five characters, five bits each. */
    private static final int ENCODED_6ABCD = 6630797;

    private ServiceController<DexCollectionService> service;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        store().edit().clear().commit();
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        if (service != null) service.destroy();
    }

    // ===== Foreground listener =======================================================================================

    /** Turning the setting off while the service runs reaches the service's listener. */
    @Test
    public void turningForegroundOffReachesTheBridgeCollector() {
        // :: Setup
        service = Robolectric.buildService(DexCollectionService.class).create();
        ShadowLog.clear();

        // :: Act
        store().edit().putBoolean("run_service_in_foreground", false).commit();

        // :: Verify
        assertThat(serviceLog()).contains("Removing from foreground");
    }

    // ===== Bridge beacon =============================================================================================

    /** A beacon from another transmitter is checked against the transmitter ID stored in the settings. */
    @Test
    public void beaconFromAnotherTransmitterIsCheckedAgainstTheStoredId() {
        // :: Setup
        store().edit().putString("dex_txid", "6ABCD").commit();
        service = Robolectric.buildService(DexCollectionService.class).create();
        final byte[] beacon = {0x07, (byte) 0xF1, 0, 0, 0, 0}; // source 0 in bytes 2-5
        ShadowLog.clear();

        // :: Act
        service.get().setSerialDataToTransmitterRawData(beacon, beacon.length);

        // :: Verify
        assertThat(serviceLog())
                .contains("setSerialDataToTransmitterRawData: TXID wrong.  Expected " + ENCODED_6ABCD + " but got 0");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static List<String> serviceLog() {
        return ShadowLog.getLogsForTag("DexCollectionService").stream()
                .map(l -> l.msg)
                .collect(Collectors.toList());
    }
}
