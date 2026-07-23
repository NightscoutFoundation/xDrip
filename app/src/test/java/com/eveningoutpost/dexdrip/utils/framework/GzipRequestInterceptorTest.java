package com.eveningoutpost.dexdrip.utils.framework;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Characterization tests for the shared {@link GzipRequestInterceptor} on okhttp 3.12.13,
 * covering both the no-arg (always-gzip) path and the {@link GzipDecider} predicate path.
 *
 * @author Asbjørn Aarrestad
 */
public class GzipRequestInterceptorTest extends RobolectricTestWithConfig {

    private static final MediaType JSON = MediaType.parse("application/json");

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) server.shutdown();
    }

    private OkHttpClient clientWith(GzipRequestInterceptor interceptor) {
        return new OkHttpClient.Builder().addInterceptor(interceptor).build();
    }

    private static String gunzip(byte[] gzipped) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped))) {
            final byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
        return out.toString("UTF-8");
    }

    // -- no-arg (always gzip) path --------------------------------------------

    @Test
    public void noArg_postWithBody_isGzippedAndTagged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final Request request = new Request.Builder()
                .url(server.url("/upload"))
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        try (Response response = clientWith(new GzipRequestInterceptor()).newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(gunzip(recorded.getBody().readByteArray())).isEqualTo("{\"a\":1}");
    }

    @Test
    public void noArg_getWithoutBody_passesThroughUntouched() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final Request request = new Request.Builder().url(server.url("/ping")).get().build();

        // :: Act
        try (Response response = clientWith(new GzipRequestInterceptor()).newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isNull();
        assertThat(recorded.getBodySize()).isEqualTo(0);
    }

    @Test
    public void noArg_requestAlreadyEncoded_passesThroughUntouched() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final Request request = new Request.Builder()
                .url(server.url("/upload"))
                .header("Content-Encoding", "identity")
                .post(RequestBody.create(JSON, "already"))
                .build();

        // :: Act
        try (Response response = clientWith(new GzipRequestInterceptor()).newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("identity");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("already");
    }

    // -- decider (predicate) path ---------------------------------------------

    @Test
    public void decider_returnsFalse_passesThroughUncompressed() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final Request request = new Request.Builder()
                .url(server.url("/upload"))
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        try (Response response = clientWith(new GzipRequestInterceptor(req -> false)).newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isNull();
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"a\":1}");
    }

    @Test
    public void decider_returnsTrue_isGzippedAndTagged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final Request request = new Request.Builder()
                .url(server.url("/upload"))
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        try (Response response = clientWith(new GzipRequestInterceptor(req -> true)).newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(gunzip(recorded.getBody().readByteArray())).isEqualTo("{\"a\":1}");
    }
}
