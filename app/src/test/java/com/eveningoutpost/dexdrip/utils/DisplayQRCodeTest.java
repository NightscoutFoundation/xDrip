package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.FormBody;
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
public class DisplayQRCodeTest extends RobolectricTestWithConfig {

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
    public void formPost_containsDataField() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ID:12345678901234567890123456789012"));
        String testData = "dGVzdA==";
        FormBody formBody = new FormBody.Builder()
                .add("data", testData)
                .build();
        Request request = new Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (jamorham)")
                .header("Connection", "close")
                .url(server.url("/joh-setsw"))
                .post(formBody)
                .build();

        // :: Act
        new OkHttpClient().newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getBody().readUtf8()).contains("data=dGVzdA%3D%3D");
    }

    @Test
    public void formPost_successWithIdResponse_parsesId() throws Exception {
        // :: Setup
        String idResponse = "ID:12345678901234567890123456789012";
        server.enqueue(new MockResponse().setBody(idResponse));
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .post(new FormBody.Builder().add("data", "test").build())
                .build();

        // :: Act
        Response response = new OkHttpClient().newCall(request).execute();
        String reply = response.body().string();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(reply).startsWith("ID:");
        assertThat(reply.length()).isEqualTo(35);
    }
}
