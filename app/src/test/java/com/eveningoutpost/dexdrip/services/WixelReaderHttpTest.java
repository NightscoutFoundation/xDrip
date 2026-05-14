package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class WixelReaderHttpTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDownServer() throws Exception {
        server.shutdown();
        // Reset static httpClient field so tests are independent
        Field httpClientField = WixelReader.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);
    }

    @Test
    public void httpClient_usesSharedClient() throws Exception {
        // :: Setup
        Field httpClientField = WixelReader.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);

        // :: Act — trigger lazy init by invoking readHttpJson
        server.enqueue(new MockResponse().setBody("[]"));
        Method readHttpJson = WixelReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);
        readHttpJson.invoke(null, server.url("/").toString(), 1);

        OkHttpClient client = (OkHttpClient) httpClientField.get(null);

        // :: Verify — same instance as shared client (not just newBuilder copy)
        assertThat(client).isSameInstanceAs(OkHttpWrapper.getClient());
    }

    @Test
    public void httpClient_sharesConnectionPool() throws Exception {
        // :: Setup
        Field httpClientField = WixelReader.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);

        // :: Act
        server.enqueue(new MockResponse().setBody("[]"));
        Method readHttpJson = WixelReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);
        readHttpJson.invoke(null, server.url("/").toString(), 1);

        OkHttpClient client = (OkHttpClient) httpClientField.get(null);

        // :: Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void readHttpJson_sendsCorrectHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));
        Method readHttpJson = WixelReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);

        // :: Act
        readHttpJson.invoke(null, server.url("/test").toString(), 1);
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    public void readHttpJson_sendsQueryParams() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));
        Method readHttpJson = WixelReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);

        // :: Act
        readHttpJson.invoke(null, server.url("/test").toString(), 3);
        RecordedRequest recorded = server.takeRequest();

        // :: Verify — numberOfRecords is incremented by 1 internally
        assertThat(recorded.getPath()).contains("n=4");
        assertThat(recorded.getPath()).contains("r=");
    }

    @Test
    public void readHttpJson_returnsEmptyListOnEmptyResponse() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody(""));
        Method readHttpJson = WixelReader.class.getDeclaredMethod("readHttpJson", String.class, int.class);
        readHttpJson.setAccessible(true);

        // :: Act
        @SuppressWarnings("unchecked")
        List<?> result = (List<?>) readHttpJson.invoke(null, server.url("/test").toString(), 1);

        // :: Verify
        assertThat(result).isEmpty();
    }
}
