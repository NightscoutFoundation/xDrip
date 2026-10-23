package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

/**
 * Pins what the wifi uploader reports to Nightscout across its rename from PARAKEET to
 * WIFI_UPLOADER - the battery level and the device name that appears in the devicestatus feed.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class NightscoutBatteryDeviceTest extends RobolectricTestWithConfig {

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem("parakeet_battery");
    }

    // ===== uploader json ================================================================================================

    /** The stored uploader battery level is what reaches Nightscout. */
    @Test
    public void wifiUploaderReportsTheStoredBatteryLevel() throws Exception {
        // :: Setup
        Pref.setInt("parakeet_battery", 42);

        // :: Act
        final JSONObject json = NightscoutBatteryDevice.WIFI_UPLOADER
                .getUploaderJson(RuntimeEnvironment.getApplication().getApplicationContext());

        // :: Verify
        assertWithMessage("battery level reaches Nightscout").that(json.getInt("battery")).isEqualTo(42);
    }

    /** The type field is wire-visible too, and it is the enum name rather than the device name. */
    @Test
    public void wifiUploaderReportsItsTypeAsWifiUploader() throws Exception {
        // :: Setup
        Pref.setInt("parakeet_battery", 42);

        // :: Act
        final JSONObject json = NightscoutBatteryDevice.WIFI_UPLOADER
                .getUploaderJson(RuntimeEnvironment.getApplication().getApplicationContext());

        // :: Verify
        assertWithMessage("the type shown in Nightscout").that(json.getString("type")).isEqualTo("WIFI_UPLOADER");
    }

    /** The device name is wire-visible in the Nightscout devicestatus feed. */
    @Test
    public void wifiUploaderReportsItsDeviceName() {
        // :: Act
        final String deviceName = NightscoutBatteryDevice.WIFI_UPLOADER.getDeviceName();

        // :: Verify
        assertWithMessage("the device name shown in Nightscout").that(deviceName).isEqualTo("Wifi Uploader");
    }
}
