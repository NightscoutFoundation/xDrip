package com.eveningoutpost.dexdrip.wearintegration;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowBluetoothDevice;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the collector the Amazfit watch is told about.
 * <p>
 * {@code getCurrentDevice} reports the paired transmitter only when the stored collection method is
 * the G5 one, and matches the paired device against the stored transmitter id. The reported name is
 * what the watch face shows, so the tests read it back.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class AmazfitservicePreferencesTest extends RobolectricTestWithConfig {

    private Amazfitservice service;

    // ===== Setup =================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        prefs().edit().clear().commit();                        // the preference file leaks between test methods
        pairTransmitterNamed("Dexcom EF");
        service = Robolectric.buildService(Amazfitservice.class).create().get();
    }

    // ===== Which collector the watch is told about ===============================================

    /** With the G5 collector stored, the paired transmitter's id is what the watch is told. */
    @Test
    public void g5CollectionMethod_reportsThePairedTransmitter() {
        // :: Setup
        prefs().edit()
                .putString("dex_collection_method", "DexcomG5")
                .putString("dex_txid", "ABCDEF")
                .commit();

        // :: Act
        final String device = service.getCurrentDevice();

        // :: Verify
        assertThat(device).isEqualTo("ABCDEF");
    }

    /** A transmitter id that does not match the paired device is not reported. */
    @Test
    public void g5CollectionMethod_withAnotherTransmitterId_reportsNoDevice() {
        // :: Setup
        prefs().edit()
                .putString("dex_collection_method", "DexcomG5")
                .putString("dex_txid", "ABCD99")
                .commit();

        // :: Act
        final String device = service.getCurrentDevice();

        // :: Verify
        assertThat(device).isEqualTo("None Set");
    }

    /** Any other collection method never looks at the paired transmitter at all. */
    @Test
    public void otherCollectionMethod_reportsNoDevice() {
        // :: Setup
        prefs().edit()
                .putString("dex_collection_method", "BluetoothWixel")
                .putString("dex_txid", "ABCDEF")
                .commit();

        // :: Act
        final String device = service.getCurrentDevice();

        // :: Verify
        assertThat(device).isEqualTo("None Set");
    }

    /** With nothing stored the default collection method applies, and it is not the G5 one. */
    @Test
    public void noStoredCollectionMethod_reportsNoDevice() {
        // :: Act
        final String device = service.getCurrentDevice();

        // :: Verify
        assertThat(device).isEqualTo("None Set");
    }

    // ===== Helpers ===============================================================================

    /** The match is made on the last two characters of the device name. */
    private static void pairTransmitterNamed(String name) {
        BluetoothDevice device = ShadowBluetoothDevice.newInstance("11:22:33:44:55:66");
        shadowOf(device).setName(name);
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setBondedDevices(Collections.singleton(device));
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
