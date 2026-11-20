package com.eveningoutpost.dexdrip.utils;

import android.util.Base64;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for reading a scanned xDrip settings QR code with {@link QRcodeUtils#decodeString}.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class QRcodeUtilsTest extends RobolectricTestWithConfig {

    private static final String SYNC_KEY = "abcdef0123456789abcd";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        ShadowLog.clear();
    }

    // ===== Decoding ==================================================================================================

    /** A scanned settings code gives back the settings it carries. */
    @Test
    public void scannedCodeGivesBackItsSettings() {
        // :: Act
        final Map<String, String> settings = QRcodeUtils.decodeString(scannedCode());

        // :: Verify
        assertThat(settings).containsEntry("custom_sync_key", SYNC_KEY);
    }

    /** Reading a scanned settings code writes neither the sync key nor the scanned text to the log. */
    @Test
    public void scannedCodeKeepsTheSyncKeyOutOfTheLog() {
        // :: Setup
        final String scanned = scannedCode();

        // :: Act
        final Map<String, String> settings = QRcodeUtils.decodeString(scanned);

        // :: Verify
        assertThat(settings).containsEntry("custom_sync_key", SYNC_KEY);
        final String payload = scanned.substring(QRcodeUtils.qrmarker.length(), QRcodeUtils.qrmarker.length() + 40);
        for (final ShadowLog.LogItem item : ShadowLog.getLogs()) {
            assertThat(item.msg).doesNotContain(SYNC_KEY);
            assertThat(item.msg).doesNotContain(payload);
        }
    }

    /** Reading a scanned compact settings code writes neither the sync key nor the scanned text to the log. */
    @Test
    public void scannedCompactCodeKeepsTheSyncKeyOutOfTheLog() {
        // :: Setup
        final String scanned = scannedCompactCode();

        // :: Act
        final Map<String, String> settings = QRcodeUtils.decodeString(scanned);

        // :: Verify
        assertThat(settings).containsEntry("custom_sync_key", SYNC_KEY);
        final String payload = scanned.substring(QRcodeUtils.qrmarker2.length(), QRcodeUtils.qrmarker2.length() + 40);
        for (final ShadowLog.LogItem item : ShadowLog.getLogs()) {
            assertThat(item.msg).doesNotContain(SYNC_KEY);
            assertThat(item.msg).doesNotContain(payload);
        }
    }

    // =================================================================================================================

    /** The text a scanner reads from the sync settings code of another phone. */
    private static String scannedCode() {
        final Map<String, String> settings = new HashMap<>();
        settings.put("custom_sync_key", SYNC_KEY);
        settings.put("use_custom_sync_key", "true");
        return QRcodeUtils.qrmarker + JoH.compressString(new JSONObject(settings).toString());
    }

    /** The text a scanner reads from a compact (binary) settings code carrying the sync key. */
    private static String scannedCompactCode() {
        final Map<String, byte[]> settings = new HashMap<>();
        settings.put("custom_sync_key", SYNC_KEY.getBytes(StandardCharsets.UTF_8));
        settings.put("use_custom_sync_key", "true".getBytes(StandardCharsets.UTF_8));
        final byte[] compressed = JoH.compressBytesToBytes(QRcodeUtils.serializeBinaryPrefsMap(settings));
        return QRcodeUtils.qrmarker2 + Base64.encodeToString(compressed, Base64.NO_PADDING | Base64.NO_WRAP);
    }
}
