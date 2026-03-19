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
 * @author Asbjorn Aarrestad
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
    public void gzipInterceptor_addsContentEncodingHeader() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        OkHttpClient client = new OkHttpClient.Builder()
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
        OkHttpClient client = new OkHttpClient.Builder()
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
    public void formBody_containsFeedbackFields() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        FormBody formBody = new FormBody.Builder()
                .add("contact", "test@example.com")
                .add("body", "test feedback")
                .add("rating", "5.0")
                .add("type", "Bug Report")
                .build();
        Request request = new Request.Builder()
                .url(server.url("/joh-feedback"))
                .post(formBody)
                .build();

        // :: Act
        new OkHttpClient().newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();
        String body = recorded.getBody().readUtf8();

        // :: Verify
        assertThat(body).contains("contact=test%40example.com");
        assertThat(body).contains("type=Bug%20Report");
    }
}
