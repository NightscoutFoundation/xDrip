package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Contract tests for the single shared okhttp client every xDrip HTTP caller derives from.
 * <p>
 * Plain-HTTP Nightscout servers are a supported and widely used configuration, so the cleartext
 * round trip is asserted here rather than left to incidental coverage.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class OkHttpWrapperSharedClientTest extends RobolectricTestWithConfig {

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

    // ===== Cleartext round trips =================================================================

    /**
     * The shared client reaches a plain-HTTP host, the request arrives as sent, and the response
     * body comes back intact.
     */
    @Test
    public void sharedClient_canPerformSimpleGet() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("hello"));
        Request request = new Request.Builder()
                .url(server.url("/test"))
                .build();

        // :: Act
        Response response = OkHttpWrapper.getClient().newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().string()).isEqualTo("hello");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/test");
        assertThat(recorded.getMethod()).isEqualTo("GET");
    }

    // ===== Clients derived with newBuilder() =====================================================

    /** A derived client overrides a timeout without detaching from the shared connection pool. */
    @Test
    public void sharedClient_newBuilder_canCustomizeTimeouts() throws Exception {
        // :: Setup
        OkHttpClient custom = OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();

        // :: Act & Verify
        assertThat(custom.connectTimeoutMillis()).isEqualTo(5000);
        assertThat(custom.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    /** An interceptor added to a derived client reaches the wire. */
    @Test
    public void sharedClient_newBuilder_canAddInterceptor() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        OkHttpClient custom = OkHttpWrapper.getClient().newBuilder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .header("X-Custom", "test")
                            .build();
                    return chain.proceed(req);
                })
                .build();

        // :: Act
        // The body is not the subject here, but an unclosed response leaks the connection and
        // okhttp then logs the leak against whichever test happens to be running at the next GC.
        custom.newCall(new Request.Builder().url(server.url("/")).build()).execute().close();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("X-Custom")).isEqualTo("test");
    }

    // ===== Defaults every caller inherits ========================================================

    /** The three timeouts on the shared client are the ones every xDrip caller starts from. */
    @Test
    public void sharedClient_hasCorrectDefaultTimeouts() {
        // :: Setup & Act
        OkHttpClient client = OkHttpWrapper.getClient();

        // :: Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(20000);
    }
}
