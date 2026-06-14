package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Characterization tests for {@link InfoInterceptor} on okhttp 3.12.13.
 *
 * It logs body size only; the request and response must pass through unchanged.
 *
 * @author Asbjørn Aarrestad
 */
public class InfoInterceptorTest extends RobolectricTestWithConfig {

    private static final MediaType JSON = MediaType.parse("application/json");

    private MockWebServer server;
    private OkHttpClient client;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new OkHttpClient.Builder()
                .addInterceptor(new InfoInterceptor("test-tag"))
                .build();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) server.shutdown();
    }

    @Test
    public void postWithBody_passesRequestAndResponseThroughUnchanged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(201).setBody("ok"));
        final Request request = new Request.Builder()
                .url(server.url("/v1/data"))
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        final String responseBody;
        final int code;
        try (Response response = client.newCall(request).execute()) {
            code = response.code();
            assertThat(response.body()).isNotNull();
            responseBody = response.body().string();
        }

        // :: Verify
        assertThat(code).isEqualTo(201);
        assertThat(responseBody).isEqualTo("ok");

        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/data");
        assertThat(recorded.getHeader("Content-Encoding")).isNull();
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"a\":1}");
    }
}
