package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import android.content.SharedPreferences;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the settings {@link DisplayQRCode} puts in a QR code for another phone to scan.
 * <p>
 * The activity logs the settings it is about to encode, so the tests read them back from that line rather than
 * decoding the picture.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class DisplayQRCodeSettingsTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        ShadowLog.clear();
    }

    // ===== Sync settings =============================================================================================

    /** The sync QR code carries the stored sync key and whether it is in use. */
    @Test
    public void syncSettingsCarryTheStoredSyncKey() throws Exception {
        // :: Setup
        store().edit()
                .putString("custom_sync_key", "abcdef0123456789abcd")
                .putBoolean("use_custom_sync_key", true)
                .commit();
        final DisplayQRCode activity = Robolectric.buildActivity(DisplayQRCode.class).create().get();

        // :: Act
        activity.xdripPlusSyncSettings(null);

        // :: Verify
        final JSONObject encoded = lastEncodedSettings();
        assertThat(encoded.getString("custom_sync_key")).isEqualTo("abcdef0123456789abcd");
        assertThat(encoded.getString("use_custom_sync_key")).isEqualTo("true");
    }

    // ===== Connection settings =======================================================================================

    /** The connection QR code carries the stored collection method, levels and units. */
    @Test
    public void connectionSettingsCarryTheStoredValues() throws Exception {
        // :: Setup
        store().edit()
                .putString("dex_collection_method", "WifiWixel")
                .putString("wifi_recievers_addresses", "192.168.1.5:50005")
                .putString("highValue", "10")
                .putString("lowValue", "4")
                .putString("units", "mmol")
                .commit();
        final DisplayQRCode activity = Robolectric.buildActivity(DisplayQRCode.class).create().get();

        // :: Act
        activity.connectionSettings(null);

        // :: Verify
        final JSONObject encoded = lastEncodedSettings();
        assertThat(encoded.getString("dex_collection_method")).isEqualTo("WifiWixel");
        assertThat(encoded.getString("wifi_recievers_addresses")).isEqualTo("192.168.1.5:50005");
        assertThat(encoded.getString("highValue")).isEqualTo("10");
        assertThat(encoded.getString("lowValue")).isEqualTo("4");
        assertThat(encoded.getString("units")).isEqualTo("mmol");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** The settings from the last "Serialized: " line the activity logged. */
    private static JSONObject lastEncodedSettings() throws Exception {
        String last = null;
        for (final ShadowLog.LogItem item : ShadowLog.getLogsForTag("jamorham qr")) {
            if (item.msg.startsWith("Serialized: ")) last = item.msg.substring("Serialized: ".length());
        }
        assertThat(last).isNotNull();
        return new JSONObject(last);
    }
}
