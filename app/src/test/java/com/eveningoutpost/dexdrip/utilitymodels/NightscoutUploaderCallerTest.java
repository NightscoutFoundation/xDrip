package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Caller-level integration tests for {@link NightscoutUploader} using MockWebServer.
 * Exercises the actual production code paths including secret hashing,
 * payload building, error handling, and the disabled-upload guard.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutUploaderCallerTest extends RobolectricTestWithConfig {

    private static final String SECRET = "my-api-secret";
    private static final String EXPECTED_HASHED_SECRET =
            Hashing.sha1().hashBytes(SECRET.getBytes(Charsets.UTF_8)).toString();

    private MockWebServer server;
    private SharedPreferences prefs;

    @Before
    public void setUpServer() throws IOException {
        super.setUp();
        server = new MockWebServer();
        server.start();
        prefs = PreferenceManager.getDefaultSharedPreferences(
                org.robolectric.RuntimeEnvironment.application);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void uploadRest_hashesSecretWithSha1_andSendsAsApiSecretHeader() throws Exception {
        // :: Setup
        final String baseUrl = "http://" + SECRET + "@" + server.getHostName()
                + ":" + server.getPort() + "/api/v1/";
        prefs.edit()
                .putBoolean("cloud_storage_api_enable", true)
                .putString("cloud_storage_api_base", baseUrl)
                .putBoolean("cloud_storage_api_download_enable", false)
                .apply();

        final BgReading bg = createBgReading(120.0, System.currentTimeMillis());

        // Enqueue enough responses: status + entries + devicestatus (battery)
        enqueueSuccessResponses(5);

        final NightscoutUploader uploader = new NightscoutUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        final boolean result = uploader.uploadRest(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isTrue();

        // Find the entries POST request among all requests
        RecordedRequest entriesRequest = findRequestByPath("/api/v1/entries");
        assertThat(entriesRequest).isNotNull();
        assertThat(entriesRequest.getHeader("api-secret")).isEqualTo(EXPECTED_HASHED_SECRET);
    }

    @Test
    public void uploadRest_buildsBgEntryPayloadWithExpectedFields() throws Exception {
        // :: Setup
        final long timestamp = 1700000000000L;
        final double bgValue = 185.0;
        final String baseUrl = "http://" + SECRET + "@" + server.getHostName()
                + ":" + server.getPort() + "/api/v1/";
        prefs.edit()
                .putBoolean("cloud_storage_api_enable", true)
                .putString("cloud_storage_api_base", baseUrl)
                .putBoolean("cloud_storage_api_download_enable", false)
                .apply();

        final BgReading bg = createBgReading(bgValue, timestamp);

        enqueueSuccessResponses(5);

        final NightscoutUploader uploader = new NightscoutUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        uploader.uploadRest(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        final RecordedRequest entriesRequest = findRequestByPath("/api/v1/entries");
        assertThat(entriesRequest).isNotNull();

        final String body = decompressIfNeeded(entriesRequest);
        final JSONArray arr = new JSONArray(body);
        assertThat(arr.length()).isEqualTo(1);

        final JSONObject entry = arr.getJSONObject(0);
        assertThat(entry.getInt("sgv")).isEqualTo((int) bgValue);
        assertThat(entry.getLong("date")).isEqualTo(timestamp);
        assertThat(entry.getString("type")).isEqualTo("sgv");
        assertThat(entry.getInt("rssi")).isEqualTo(100);
        assertThat(entry.has("direction")).isTrue();
        assertThat(entry.has("dateString")).isTrue();
        assertThat(entry.has("sysTime")).isTrue();
        assertThat(entry.getString("device")).startsWith("xDrip-");
    }

    @Test
    public void uploadRest_whenServerReturns500_returnsFalse() throws Exception {
        // :: Setup
        final String baseUrl = "http://" + SECRET + "@" + server.getHostName()
                + ":" + server.getPort() + "/api/v1/";
        prefs.edit()
                .putBoolean("cloud_storage_api_enable", true)
                .putString("cloud_storage_api_base", baseUrl)
                .putBoolean("cloud_storage_api_download_enable", false)
                .apply();

        final BgReading bg = createBgReading(120.0, System.currentTimeMillis());

        // Status may succeed, but entries upload fails with 500
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"status\":\"ok\",\"version\":\"14.0\"}"));
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        final NightscoutUploader uploader = new NightscoutUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        final boolean result = uploader.uploadRest(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isFalse();
    }

    @Test
    public void uploadRest_whenDisabled_sendsNoHttpRequests() throws Exception {
        // :: Setup
        prefs.edit()
                .putBoolean("cloud_storage_api_enable", false)
                .putString("cloud_storage_api_base", "http://secret@localhost/api/v1/")
                .apply();

        final BgReading bg = createBgReading(120.0, System.currentTimeMillis());

        final NightscoutUploader uploader = new NightscoutUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        final boolean result = uploader.uploadRest(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static BgReading createBgReading(double calculatedValue, long timestamp) {
        final BgReading bg = new BgReading();
        bg.calculated_value = calculatedValue;
        bg.filtered_calculated_value = calculatedValue;
        bg.timestamp = timestamp;
        bg.raw_data = calculatedValue;
        bg.age_adjusted_raw_value = calculatedValue;
        bg.filtered_data = calculatedValue;
        bg.noise = "1";
        bg.calculated_value_slope = 0;
        bg.hide_slope = false;
        return bg;
    }

    private void enqueueSuccessResponses(int count) {
        for (int i = 0; i < count; i++) {
            server.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"status\":\"ok\",\"version\":\"14.0\"}"));
        }
    }

    /**
     * Finds the first request whose path matches the given prefix.
     * Drains up to 10 requests from the server within a short timeout.
     */
    private RecordedRequest findRequestByPath(String pathPrefix) throws InterruptedException {
        RecordedRequest match = null;
        for (int i = 0; i < 10; i++) {
            final RecordedRequest req = server.takeRequest(2, TimeUnit.SECONDS);
            if (req == null) break;
            if (req.getPath() != null && req.getPath().startsWith(pathPrefix) && "POST".equals(req.getMethod())) {
                if (match == null) {
                    match = req;
                }
            }
        }
        return match;
    }

    /**
     * Decompresses the request body if gzip-encoded, otherwise returns it as-is.
     */
    private static String decompressIfNeeded(RecordedRequest request) throws IOException {
        final String encoding = request.getHeader("Content-Encoding");
        if ("gzip".equals(encoding)) {
            return decompress(request.getBody().readByteArray());
        }
        return request.getBody().readUtf8();
    }

    private static String decompress(byte[] compressed) throws IOException {
        try (final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             final Reader reader = new InputStreamReader(gis, StandardCharsets.UTF_8)) {
            final StringBuilder sb = new StringBuilder();
            final char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }
}
