package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utils.framework.GzipRequestInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Characterization tests for NightscoutUploader's per-server gzip gating, exercised through the
 * shared {@link GzipRequestInterceptor} wired with {@link NightscoutUploader#NS_GZIP_DECIDER}.
 * Pins that the {@code supportsGzip(host+port)} gate behaves exactly as the original inner-class
 * interceptor did before Phase E consolidation.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutUploaderGzipInterceptorTest extends RobolectricTestWithConfig {

    private static final String MARKER_PREFIX = "ns-end-supports-gzip-";
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

    private OkHttpClient client() {
        return new OkHttpClient.Builder()
                .addInterceptor(new GzipRequestInterceptor(NightscoutUploader.NS_GZIP_DECIDER))
                .build();
    }

    private void markSupportsGzip(HttpUrl url, boolean value) {
        final String id = url.uri().getHost() + url.uri().getPort();
        PersistentStore.setBoolean(MARKER_PREFIX + id, value);
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

    @Test
    public void postWithBody_serverSupportsGzip_isGzippedAndTagged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final HttpUrl url = server.url("/upload");
        markSupportsGzip(url, true);
        final Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        try (Response response = client().newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(gunzip(recorded.getBody().readByteArray())).isEqualTo("{\"a\":1}");
    }

    @Test
    public void postWithBody_serverGzipUnknown_passesThroughUncompressed() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final HttpUrl url = server.url("/upload");
        // no marker set -> supportsGzip returns false
        final Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(JSON, "{\"a\":1}"))
                .build();

        // :: Act
        try (Response response = client().newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isNull();
        assertThat(recorded.getBody().readUtf8()).isEqualTo("{\"a\":1}");
    }

    @Test
    public void getWithoutBody_passesThroughUntouched() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final HttpUrl url = server.url("/ping");
        markSupportsGzip(url, true);
        final Request request = new Request.Builder().url(url).get().build();

        // :: Act
        try (Response response = client().newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isNull();
        assertThat(recorded.getBodySize()).isEqualTo(0);
    }

    @Test
    public void requestAlreadyEncoded_passesThroughUntouched() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final HttpUrl url = server.url("/upload");
        markSupportsGzip(url, true);
        final Request request = new Request.Builder()
                .url(url)
                .header("Content-Encoding", "identity")
                .post(RequestBody.create(JSON, "already"))
                .build();

        // :: Act
        try (Response response = client().newCall(request).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }

        // :: Verify
        final RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("Content-Encoding")).isEqualTo("identity");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("already");
    }
}
