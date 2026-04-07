package com.eveningoutpost.dexdrip.influxdb;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Caller-level integration tests for {@link InfluxDBUploader}.
 * <p>
 * Uses MockWebServer to intercept real HTTP traffic from the InfluxDB client,
 * verifying the line protocol data written by the uploader.
 *
 * @author Asbjørn Aarrestad
 */
public class InfluxDBUploaderCallerTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private SharedPreferences prefs;

    @Before
    public void setUp() {
        super.setUp();
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void upload_sendsGlucoseMeasurementWithCorrectFields() throws Exception {
        // :: Setup
        startServer();
        server.enqueue(new MockResponse().setResponseCode(204));

        InfluxDBUploader uploader = createUploader();

        BgReading bg = new BgReading();
        bg.timestamp = 1000000L;
        bg.calculated_value = 180.0;
        bg.calculated_value_slope = 0.0;
        bg.raw_data = 200.0;
        bg.age_adjusted_raw_value = 200.0;
        bg.filtered_data = 190.0;
        bg.noise = "1";
        bg.hide_slope = false;

        // :: Act
        boolean result = uploader.upload(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isTrue();

        RecordedRequest request = server.takeRequest();
        String body = decompressIfNeeded(request);

        assertThat(body).contains("glucose");
        assertThat(body).contains("value_mgdl=180i");
    }

    @Test
    public void upload_sendsMeterMeasurementWithDeviceTag() throws Exception {
        // :: Setup
        startServer();
        server.enqueue(new MockResponse().setResponseCode(204));

        InfluxDBUploader uploader = createUploader();

        Calibration meter = new Calibration();
        meter.timestamp = 2000000L;
        meter.bg = 120.0;

        // :: Act
        boolean result = uploader.upload(
                Collections.emptyList(),
                Collections.singletonList(meter),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isTrue();

        RecordedRequest request = server.takeRequest();
        String body = decompressIfNeeded(request);

        assertThat(body).contains("meter");
        assertThat(body).contains("mbg=120.0");
    }

    @Test
    public void upload_skipsCalibrationWithZeroSlope() throws Exception {
        // :: Setup
        startServer();
        server.enqueue(new MockResponse().setResponseCode(204));

        InfluxDBUploader uploader = createUploader();

        Calibration cal = new Calibration();
        cal.timestamp = 3000000L;
        cal.slope = 0d;
        cal.intercept = 10.0;
        cal.check_in = false;

        // :: Act
        boolean result = uploader.upload(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(cal));

        // :: Verify
        assertThat(result).isTrue();

        RecordedRequest request = server.takeRequest();
        String body = decompressIfNeeded(request);

        assertThat(body).doesNotContain("calibration");
    }

    @Test
    public void upload_returnsFalseOnConnectionError() throws Exception {
        // :: Setup
        startServer();
        server.shutdown();

        InfluxDBUploader uploader = createUploader();

        BgReading bg = new BgReading();
        bg.timestamp = 4000000L;
        bg.calculated_value = 100.0;
        bg.calculated_value_slope = 0.0;
        bg.raw_data = 100.0;
        bg.age_adjusted_raw_value = 100.0;
        bg.filtered_data = 100.0;
        bg.noise = "1";
        bg.hide_slope = false;

        // :: Act
        boolean result = uploader.upload(
                Collections.singletonList(bg),
                Collections.emptyList(),
                Collections.emptyList());

        // :: Verify
        assertThat(result).isFalse();
    }

    // --- Helpers ---

    private void startServer() throws IOException {
        server.start();
    }

    private InfluxDBUploader createUploader() {
        prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
        String serverUrl = server.url("/").toString();
        // Remove trailing slash to avoid double-slash in path construction
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        prefs.edit()
                .putString("cloud_storage_influxdb_uri", serverUrl)
                .putString("cloud_storage_influxdb_database", "testdb")
                .putString("cloud_storage_influxdb_username", "testuser")
                .putString("cloud_storage_influxdb_password", "testpass")
                .apply();
        return new InfluxDBUploader(xdrip.getAppContext());
    }

    private String decompressIfNeeded(RecordedRequest request) throws IOException {
        String contentEncoding = request.getHeader("Content-Encoding");
        byte[] bodyBytes = request.getBody().readByteArray();

        if ("gzip".equals(contentEncoding)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);
            GZIPInputStream gzis = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
        }

        return new String(bodyBytes, "UTF-8");
    }
}
