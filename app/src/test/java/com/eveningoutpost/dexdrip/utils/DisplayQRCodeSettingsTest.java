package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Looper;
import android.widget.ImageView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.time.Duration;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the settings {@link DisplayQRCode} puts in a QR code for another phone to scan.
 * <p>
 * The tests decode the QR picture the activity shows and read it the way the scanning phone does.
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
        final DisplayQRCode activity = Robolectric.buildActivity(DisplayQRCode.class).setup().get();

        // :: Act
        activity.xdripPlusSyncSettings(null);

        // :: Verify
        final Map<String, String> encoded = shownSettings(activity);
        assertThat(encoded.get("custom_sync_key")).isEqualTo("abcdef0123456789abcd");
        assertThat(encoded.get("use_custom_sync_key")).isEqualTo("true");
    }

    /** Showing the sync QR code writes neither the sync key nor the encoded payload to the log. */
    @Test
    public void syncSettingsKeepTheSyncKeyOutOfTheLog() throws Exception {
        // :: Setup
        store().edit()
                .putString("custom_sync_key", "abcdef0123456789abcd")
                .putBoolean("use_custom_sync_key", true)
                .commit();
        final DisplayQRCode activity = Robolectric.buildActivity(DisplayQRCode.class).setup().get();

        // :: Act
        activity.xdripPlusSyncSettings(null);

        // :: Verify
        final String payload = scannedText(activity).substring(QRcodeUtils.qrmarker.length());
        for (final ShadowLog.LogItem item : ShadowLog.getLogs()) {
            assertThat(item.msg).doesNotContain("abcdef0123456789abcd");
            assertThat(item.msg).doesNotContain(payload.substring(0, 40));
        }
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
        final DisplayQRCode activity = Robolectric.buildActivity(DisplayQRCode.class).setup().get();

        // :: Act
        activity.connectionSettings(null);

        // :: Verify
        final Map<String, String> encoded = shownSettings(activity);
        assertThat(encoded.get("dex_collection_method")).isEqualTo("WifiWixel");
        assertThat(encoded.get("wifi_recievers_addresses")).isEqualTo("192.168.1.5:50005");
        assertThat(encoded.get("highValue")).isEqualTo("10");
        assertThat(encoded.get("lowValue")).isEqualTo("4");
        assertThat(encoded.get("units")).isEqualTo("mmol");
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** The settings in the QR code the activity shows, decoded the way the scanning phone decodes them. */
    private static Map<String, String> shownSettings(final DisplayQRCode activity) throws Exception {
        final Map<String, String> settings = QRcodeUtils.decodeString(scannedText(activity));
        assertThat(settings).isNotNull();
        return settings;
    }

    /** The text in the QR code the activity shows, as a scanner reads it. */
    private static String scannedText(final DisplayQRCode activity) throws Exception {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        final ImageView image = activity.findViewById(R.id.qrcodeimage1);
        final Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        final BinaryBitmap binary = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(width, height, pixels)));
        return new QRCodeReader().decode(binary).getText();
    }
}
