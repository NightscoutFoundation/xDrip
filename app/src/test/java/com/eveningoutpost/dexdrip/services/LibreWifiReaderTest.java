package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class LibreWifiReaderTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        // Reset static httpClient field to null so each test gets fresh state
        Field httpClientField = LibreWifiReader.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void readHttpJson_sendsCorrectHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));
        String url = server.url("").toString();
        // Remove trailing slash
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        // :: Act
        Method readHttpJson = LibreWifiReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);
        readHttpJson.invoke(null, url, 1);
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
        assertThat(recorded.getPath()).contains("/libre/1");
    }

    @Test
    public void httpClient_hasCorrectTimeouts() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));
        String url = server.url("").toString();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        // :: Act — trigger client creation
        Method readHttpJson = LibreWifiReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);
        readHttpJson.invoke(null, url, 1);

        // :: Verify — check the static httpClient field
        Field httpClientField = LibreWifiReader.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) httpClientField.get(null);
        assertThat(client).isNotNull();
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(20000);
    }
}
