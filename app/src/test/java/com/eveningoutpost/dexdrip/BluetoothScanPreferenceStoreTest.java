package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.google.zxing.integration.android.IntentIntegrator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for what {@link BluetoothScan} stores in the default preference store: a scanned Share serial
 * number, and the device the user picks from the scan list.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class BluetoothScanPreferenceStoreTest extends RobolectricTestWithConfig {

    private static final String ADDRESS = "00:11:22:33:44:55";

    private ActivityController<BluetoothScan> screen;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        new Delete().from(ActiveBluetoothDevice.class).execute();
    }

    /** Stops what a test left running, so it cannot reach into the next test. */
    @After
    public void tearDown() {
        Inevitable.kill("restart-collector"); // queued by returnToHome()
        if (screen != null) screen.pause().stop().destroy();
        new Delete().from(ActiveBluetoothDevice.class).execute();
    }

    // ===== Barcode result ============================================================================================

    /** A Share serial number read from a barcode is stored. */
    @Test
    public void scannedShareSerialIsStored() {
        // :: Setup
        screen = Robolectric.buildActivity(BluetoothScan.class).create();
        final Intent data = new Intent()
                .putExtra("SCAN_RESULT", "SM12345678")
                .putExtra("SCAN_RESULT_FORMAT", "CODE_128");

        // :: Act
        screen.get().onActivityResult(IntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, data);

        // :: Verify
        assertThat(store().getString("share_key", null)).isEqualTo("SM12345678");
    }

    // ===== Scan list =================================================================================================

    /** Picking a device from the scan list stores its address and the bridge flags. */
    @Test
    public void pickingADeviceStoresItsAddress() {
        // :: Setup
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        shadowOf(adapter).setEnabled(true);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        JoH.clearRatelimit("auto-start-bt-scan"); // gates the scan onResume starts
        screen = Robolectric.buildActivity(BluetoothScan.class).setup();
        final BluetoothDevice device = adapter.getRemoteDevice(ADDRESS);
        shadowOf(device).setName("xDrip");
        for (ScanCallback callback : shadowOf(adapter.getBluetoothLeScanner()).getScanCallbacks()) {
            callback.onBatchScanResults(Collections.singletonList(new ScanResult(device, null, -50, 0)));
        }
        ShadowLooper.idleMainLooper();

        // :: Act
        screen.get().getListView().performItemClick(null, 0, 0);

        // :: Verify
        assertThat(store().getString("last_connected_device_address", null)).isEqualTo(ADDRESS);
        assertThat(store().contains("use_transmiter_pl_bluetooth")).isTrue();
        assertThat(store().contains("use_rfduino_bluetooth")).isTrue();
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
