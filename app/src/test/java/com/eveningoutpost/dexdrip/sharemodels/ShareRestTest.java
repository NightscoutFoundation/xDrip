package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies ShareRest network interceptor adds required headers (User-Agent, Content-Type, Accept).
 *
 * @author Asbjørn Aarrestad
 */
public class ShareRestTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getOkHttpClient_addsRequiredHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"ok\""));

        // Use reflection to call the private getOkHttpClient method
        java.lang.reflect.Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        OkHttpClient client = (OkHttpClient) method.invoke(new ShareRest(null, new OkHttpClient()));

        // :: Act
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/test"))
                .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), "{}"))
                .build();
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).contains("CGM-Store-1.2");
        assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void getOkHttpClient_returnsOkHttp3Client() throws Exception {
        // :: Setup & Act
        java.lang.reflect.Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        Object client = method.invoke(new ShareRest(null, new OkHttpClient()));

        // :: Verify
        assertThat(client).isInstanceOf(OkHttpClient.class);
    }
}
