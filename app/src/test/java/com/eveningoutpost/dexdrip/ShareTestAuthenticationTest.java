package com.eveningoutpost.dexdrip;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.utilitymodels.DexShareAttributes;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowBluetoothGatt;

import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for {@link ShareTest#authenticateConnection}, which writes the Share receiver's serial number,
 * read from the settings, to the receiver's authentication characteristic.
 * <p>
 * The receiver is a Robolectric GATT that offers the Share cradle service, so the bytes sent to it can be read back.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class ShareTestAuthenticationTest extends RobolectricTestWithConfig {

    private static final String KEY = "share_key";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
    }

    // ===== Authentication ============================================================================================

    /** The stored serial number is written upper-cased, followed by six zeros. */
    @Test
    public void writesTheStoredSerialNumber() {
        // :: Setup
        store().edit().putString(KEY, "sm12345678").commit();
        final BluetoothGatt gatt = shareReceiver();

        // :: Act
        shareTest().authenticateConnection(gatt);

        // :: Verify
        assertThat(sentTo(gatt)).isEqualTo("SM12345678000000");
    }

    /** With no serial number stored, the placeholder serial is written. */
    @Test
    public void writesThePlaceholderWithoutAStoredSerialNumber() {
        // :: Setup
        final BluetoothGatt gatt = shareReceiver();

        // :: Act
        shareTest().authenticateConnection(gatt);

        // :: Verify
        assertThat(sentTo(gatt)).isEqualTo("SM00000000000000");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    private static ShareTest shareTest() {
        return Robolectric.buildActivity(ShareTest.class).create().get();
    }

    /** What was last written to the receiver, as text. */
    private static String sentTo(BluetoothGatt gatt) {
        return new String(shadowOf(gatt).getLatestWrittenBytes(), StandardCharsets.US_ASCII);
    }

    /** A connected receiver whose discovered services hold the Share cradle service and its authentication code. */
    private static BluetoothGatt shareReceiver() {
        final BluetoothGattService cradle =
                new BluetoothGattService(DexShareAttributes.CradleService, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        cradle.addCharacteristic(new BluetoothGattCharacteristic(DexShareAttributes.AuthenticationCode,
                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE));
        final BluetoothGatt gatt = ShadowBluetoothGatt.newInstance(
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:11:22:33:AA:BB"));
        shadowOf(gatt).setGattCallback(new BluetoothGattCallback() {});
        shadowOf(gatt).addDiscoverableService(cradle);
        gatt.discoverServices();
        return gatt;
    }
}
