package com.eveningoutpost.dexdrip.utils;

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
public class WebAppHelperTest extends RobolectricTestWithConfig {

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
        server.enqueue(new MockResponse().setBody("hello"));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (jamorham)")
                .header("Connection", "close")
                .url(server.url("/test"))
                .build();

        // :: Act
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).isEqualTo("Mozilla/5.0 (jamorham)");
        assertThat(recorded.getHeader("Connection")).isEqualTo("close");
    }

    @Test
    public void request_successfulResponse_returnsBodyBytes() throws Exception {
        // :: Setup
        byte[] expected = new byte[]{0x01, 0x02, 0x03};
        server.enqueue(new MockResponse().setBody(new okio.Buffer().write(expected)));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .build();

        // :: Act
        Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().bytes()).isEqualTo(expected);
    }

    @Test
    public void request_failedResponse_isNotSuccessful() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .build();

        // :: Act
        Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isFalse();
    }
}
