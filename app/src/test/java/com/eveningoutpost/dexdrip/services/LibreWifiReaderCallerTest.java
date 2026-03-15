package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Caller-level integration tests for {@link LibreWifiReader#readHttpJson}.
 * <p>
 * Uses MockWebServer to verify HTTP interaction, JSON parsing, and error handling.
 *
 * @author Asbjørn Aarrestad
 */
public class LibreWifiReaderCallerTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUp() {
        super.setUp();
        resetHttpClient();
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        resetHttpClient();
    }

    @Test
    public void readHttpJson_parsesFieldsCorrectly() throws Exception {
        // :: Setup
        String json = "[{"
                + "\"BlockBytes\":\"AQID\","
                + "\"CaptureDateTime\":1000000,"
                + "\"ChecksumOk\":1,"
                + "\"TomatoBatteryLife\":85,"
                + "\"UploaderBatteryLife\":92,"
                + "\"SensorId\":\"SENSOR-ABC\""
                + "}]";
        server.enqueue(new MockResponse().setBody(json));
        server.start();
        String baseUrl = stripTrailingSlash(server.url("/").toString());

        // :: Act
        List<LibreWifiData> result = invokeReadHttpJson(baseUrl, 10);

        // :: Verify
        assertThat(result).hasSize(1);
        LibreWifiData data = result.get(0);
        assertThat(data.SensorId).isEqualTo("SENSOR-ABC");
        assertThat(data.CaptureDateTime).isEqualTo(1000000L);
        assertThat(data.ChecksumOk).isEqualTo(1);
        assertThat(data.TomatoBatteryLife).isEqualTo(85);
        assertThat(data.UploaderBatteryLife).isEqualTo(92);
        assertThat(data.BlockBytes).isEqualTo("AQID");
    }

    @Test
    public void readHttpJson_limitsResultsToNumberOfRecords() throws Exception {
        // :: Setup
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 5; i++) {
            if (i > 0) json.append(",");
            json.append("{\"SensorId\":\"S").append(i).append("\",\"CaptureDateTime\":").append(1000 + i).append("}");
        }
        json.append("]");
        server.enqueue(new MockResponse().setBody(json.toString()));
        server.start();
        String baseUrl = stripTrailingSlash(server.url("/").toString());

        // :: Act
        List<LibreWifiData> result = invokeReadHttpJson(baseUrl, 2);

        // :: Verify
        assertThat(result).hasSize(2);
    }

    @Test
    public void readHttpJson_returnsEmptyListOnServerError() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));
        server.start();
        String baseUrl = stripTrailingSlash(server.url("/").toString());

        // :: Act
        List<LibreWifiData> result = invokeReadHttpJson(baseUrl, 3);

        // :: Verify
        assertThat(result).isEmpty();
    }

    @Test
    public void readHttpJson_returnsEmptyListOnMalformedJson() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("not valid json {{{"));
        server.start();
        String baseUrl = stripTrailingSlash(server.url("/").toString());

        // :: Act
        List<LibreWifiData> result = invokeReadHttpJson(baseUrl, 3);

        // :: Verify
        assertThat(result).isEmpty();
    }

    @Test
    public void readHttpJson_buildsCorrectUrlAndHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));
        server.start();
        String baseUrl = stripTrailingSlash(server.url("/").toString());

        // :: Act
        invokeReadHttpJson(baseUrl, 3);

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/libre/3");
        assertThat(request.getHeader("User-Agent")).isEqualTo("Mozilla/5.0");
        assertThat(request.getHeader("Connection")).isEqualTo("close");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private List<LibreWifiData> invokeReadHttpJson(String url, int numberOfRecords) throws Exception {
        Method method = LibreWifiReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        method.setAccessible(true);
        return (List<LibreWifiData>) method.invoke(null, url, numberOfRecords);
    }

    private void resetHttpClient() {
        try {
            Field field = LibreWifiReader.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset httpClient", e);
        }
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
