package com.eveningoutpost.dexdrip.services;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjorn Aarrestad
 */
public class WixelReaderHttpTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void request_hasCorrectHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("{\"status\":\"ok\"}"));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("User-Agent", "Mozilla/5.0")
                .header("Connection", "close")
                .url(server.url("/test?n=1&r=123"))
                .build();

        // :: Act
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
        assertThat(recorded.getPath()).contains("n=1");
    }

    @Test
    public void request_successfulResponse_returnsBody() throws Exception {
        // :: Setup
        String json = "[{\"_id\":1}]";
        server.enqueue(new MockResponse().setBody(json));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .build();

        // :: Act
        Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().string()).isEqualTo(json);
    }
}
