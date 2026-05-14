package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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

        // Create ShareRest with a real context, passing a dummy client to avoid using the private one
        // Then call the private getOkHttpClient to get the interceptor-equipped client
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        OkHttpClient client = (OkHttpClient) method.invoke(shareRest);

        // :: Act
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/test"))
                .post(RequestBody.create(okhttp3.MediaType.parse("application/json"), "{}"))
                .build();
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).contains("CGM-Store-1.2");
        assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void getOkHttpClient_handlesRequestWithNoBody_withoutException() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        OkHttpClient client = (OkHttpClient) method.invoke(shareRest);

        // :: Act — GET request has a null body, exercises the null-body guard in the interceptor
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/test"))
                .get()
                .build();
        okhttp3.Response response = client.newCall(request).execute();

        // :: Verify — required headers are still injected on bodyless requests
        RecordedRequest recorded = server.takeRequest();
        assertThat(response.code()).isEqualTo(200);
        assertThat(recorded.getHeader("User-Agent")).contains("CGM-Store-1.2");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void getOkHttpClient_returnsOkHttp3Client() throws Exception {
        // :: Setup & Act
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        Object client = method.invoke(shareRest);

        // :: Verify
        assertThat(client).isInstanceOf(OkHttpClient.class);
    }
}
