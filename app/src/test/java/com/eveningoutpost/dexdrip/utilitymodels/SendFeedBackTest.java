package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that SendFeedBack's OkHttpClient configuration matches expectations
 * after migration from OkHttp2 to OkHttp3 shared client.
 *
 * @author Asbjørn Aarrestad
 */
public class SendFeedBackTest extends RobolectricTestWithConfig {

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
    public void client_sharesConnectionPool() {
        // :: Setup — reproduce the client construction from SendFeedBack.sendFeedback
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(new GzipRequestInterceptor())
                .build();

        // :: Act & Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void client_hasGzipInterceptor() {
        // :: Setup
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(new GzipRequestInterceptor())
                .build();

        // :: Act & Verify
        boolean hasGzip = client.interceptors().stream()
                .anyMatch(i -> i instanceof GzipRequestInterceptor);
        assertThat(hasGzip).isTrue();
    }

    @Test
    public void gzipInterceptor_addsContentEncodingHeader() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .addInterceptor(new GzipRequestInterceptor())
                .build();
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .post(new FormBody.Builder().add("key", "value").build())
                .build();

        // :: Act
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("gzip");
    }

    @Test
    public void gzipInterceptor_skipsIfAlreadyEncoded() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .addInterceptor(new GzipRequestInterceptor())
                .build();
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .header("Content-Encoding", "identity")
                .post(new FormBody.Builder().add("key", "value").build())
                .build();

        // :: Act
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("identity");
    }

    @Test
    public void formBody_containsFeedbackFields() {
        // :: Setup & Act
        FormBody formBody = new FormBody.Builder()
                .add("contact", "test@example.com")
                .add("body", "test feedback")
                .add("rating", "5.0")
                .add("type", "Bug Report")
                .build();

        // :: Verify
        assertThat(formBody.size()).isEqualTo(4);
        assertThat(formBody.name(0)).isEqualTo("contact");
        assertThat(formBody.value(0)).isEqualTo("test@example.com");
        assertThat(formBody.name(3)).isEqualTo("type");
        assertThat(formBody.value(3)).isEqualTo("Bug Report");
    }
}
