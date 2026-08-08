package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

/**
 * Covers {@code ShareRest}'s network interceptor rebuilding the response body via
 * {@code ResponseBody.create(bodyString, contentType)}, both when the server sends no
 * {@code Content-Type} header (the null-media-type case the okhttp 4 argument-order swap
 * makes interesting) and when it sends one. The interceptor consumes the original body to
 * log it, so a test that successfully reads the returned body's content proves the rebuild
 * happened and preserved the payload.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class ShareRestNullContentTypeTest extends RobolectricTestWithConfig {

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

    private OkHttpClient getInterceptorEquippedClient() throws Exception {
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        return (OkHttpClient) method.invoke(shareRest);
    }

    @Test
    public void noContentTypeHeader_rebuiltBodyHasNullContentTypeAndOriginalString() throws Exception {
        // :: Setup — a plain setBody() call sets no Content-Type header
        server.enqueue(new MockResponse().setBody("\"session-abc\""));
        OkHttpClient client = getInterceptorEquippedClient();

        // :: Act
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .get()
                .build();
        Response response = client.newCall(request).execute();

        // :: Verify — the header assertion confirms the no-Content-Type setup actually held
        assertThat(response.header("Content-Type")).isNull();
        assertThat(response.body().contentType()).isNull();
        assertThat(response.body().string()).isEqualTo("\"session-abc\"");
    }

    @Test
    public void jsonContentTypeHeader_rebuiltBodyPreservesTypeSubtypeAndPayload() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"a\":1}")
                .setHeader("Content-Type", "application/json"));
        OkHttpClient client = getInterceptorEquippedClient();

        // :: Act
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .get()
                .build();
        Response response = client.newCall(request).execute();
        MediaType contentType = response.body().contentType();

        // :: Verify — assert the stable parts only: okhttp appends a default charset=utf-8
        // to a media type that doesn't carry one, so a whole-value comparison is brittle.
        assertThat(contentType).isNotNull();
        assertThat(contentType.type()).isEqualTo("application");
        assertThat(contentType.subtype()).isEqualTo("json");
        assertThat(response.body().string()).isEqualTo("{\"a\":1}");
    }
}
