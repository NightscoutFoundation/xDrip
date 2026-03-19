package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Integration tests for {@link NightscoutUploader.NightscoutService} using MockWebServer.
 * Verifies that the Retrofit 2 interface contract matches Nightscout API expectations.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutServiceIntegrationTest extends RobolectricTestWithConfig {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String API_SECRET = "test-api-secret-hash";

    private MockWebServer server;
    private NightscoutUploader.NightscoutService service;

    @Before
    public void setUpService() throws IOException {
        // :: Setup
        server = new MockWebServer();
        server.start();

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(new OkHttpClient())
                .build();

        service = retrofit.create(NightscoutUploader.NightscoutService.class);
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void upload_withApiSecret_postsToEntries() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "[{\"sgv\":120}]");

        // :: Act
        final Response<ResponseBody> response = service.upload(API_SECRET, body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/entries");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
        assertThat(request.getBody().readUtf8()).isEqualTo("[{\"sgv\":120}]");
    }

    @Test
    public void upload_withoutApiSecret_postsToEntries() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "[{\"sgv\":120}]");

        // :: Act
        final Response<ResponseBody> response = service.upload(body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/entries");
        assertThat(request.getHeader("api-secret")).isNull();
        assertThat(request.getBody().readUtf8()).isEqualTo("[{\"sgv\":120}]");
    }

    @Test
    public void uploadDeviceStatus_withoutApiSecret_postsToDevicestatus() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "{\"device\":\"xDrip\"}");

        // :: Act
        final Response<ResponseBody> response = service.uploadDeviceStatus(body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/devicestatus");
        assertThat(request.getHeader("api-secret")).isNull();
    }

    @Test
    public void uploadDeviceStatus_withApiSecret_postsToDevicestatus() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "{\"device\":\"xDrip\"}");

        // :: Act
        final Response<ResponseBody> response = service.uploadDeviceStatus(API_SECRET, body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/devicestatus");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void getStatus_sendsGetToStatusJson() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"ok\"}"));

        // :: Act
        final Response<ResponseBody> response = service.getStatus(API_SECRET).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/v1/status.json");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void uploadTreatments_postsToTreatments() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "{\"eventType\":\"Correction Bolus\"}");

        // :: Act
        final Response<ResponseBody> response = service.uploadTreatments(API_SECRET, body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void upsertTreatments_putsToTreatments() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "{\"eventType\":\"Correction Bolus\"}");

        // :: Act
        final Response<ResponseBody> response = service.upsertTreatments(API_SECRET, body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void downloadTreatments_sendsGetWithIfModifiedSinceHeader() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));
        final String ifModified = "2024-01-01T00:00:00Z";

        // :: Act
        final Response<ResponseBody> response = service.downloadTreatments(API_SECRET, ifModified).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
        assertThat(request.getHeader("BROKEN-If-Modified-Since")).isEqualTo(ifModified);
    }

    @Test
    public void findTreatmentByUUID_sendsGetWithQueryParam() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200).setBody("[]"));
        final String uuid = "abc-123-def";

        // :: Act
        final Response<ResponseBody> response = service.findTreatmentByUUID(API_SECRET, uuid).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments.json?find%5Buuid%5D=abc-123-def");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void deleteTreatment_sendsDeleteWithPathParam() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final String treatmentId = "5f1234567890abcdef123456";

        // :: Act
        final Response<ResponseBody> response = service.deleteTreatment(API_SECRET, treatmentId).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments/5f1234567890abcdef123456");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
    }

    @Test
    public void uploadActivity_postsToActivity() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        final RequestBody body = RequestBody.create(JSON, "{\"mills\":1234567890}");

        // :: Act
        final Response<ResponseBody> response = service.uploadActivity(API_SECRET, body).execute();
        final RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/activity");
        assertThat(request.getHeader("api-secret")).isEqualTo(API_SECRET);
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"mills\":1234567890}");
    }

    @Test
    public void upload_withGzipInterceptor_compressesRequestBody() throws Exception {
        // :: Setup
        final MockWebServer gzipServer = new MockWebServer();
        gzipServer.start();
        gzipServer.enqueue(new MockResponse().setResponseCode(200));

        final OkHttpClient gzipClient = new OkHttpClient.Builder()
                .addInterceptor(new GzipRequestInterceptor())
                .build();
        final Retrofit gzipRetrofit = new Retrofit.Builder()
                .baseUrl(gzipServer.url("/api/v1/"))
                .client(gzipClient)
                .build();
        final NightscoutUploader.NightscoutService gzipService =
                gzipRetrofit.create(NightscoutUploader.NightscoutService.class);

        final String payload = "[{\"sgv\":120,\"type\":\"sgv\",\"direction\":\"Flat\"}]";
        final RequestBody body = RequestBody.create(JSON, payload);

        // :: Act
        final Response<ResponseBody> response = gzipService.upload(API_SECRET, body).execute();
        final RecordedRequest request = gzipServer.takeRequest();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
        assertThat(decompress(request.getBody().readByteArray())).isEqualTo(payload);

        gzipServer.shutdown();
    }

    /**
     * Decompresses a GZIP-compressed byte array to a String.
     */
    private static String decompress(byte[] compressed) throws IOException {
        try (final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
             final Reader reader = new InputStreamReader(gis, StandardCharsets.UTF_8)) {
            final StringBuilder sb = new StringBuilder();
            final char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }
}
