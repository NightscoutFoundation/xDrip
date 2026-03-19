package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Caller-level integration tests for Tidepool session management and upload chain.
 * Uses MockWebServer with injected Retrofit to exercise TidepoolUploader and Session.
 *
 * @author Asbjørn Aarrestad
 */
public class TidepoolCallerTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        TidepoolUploader.resetInstance();
        if (server != null) {
            server.shutdown();
        }
    }

    private void injectMockRetrofit() throws Exception {
        TidepoolUploader.resetInstance();
        Field retrofitField = TidepoolUploader.class.getDeclaredField("retrofit");
        retrofitField.setAccessible(true);
        Retrofit mockRetrofit = new Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitField.set(null, mockRetrofit);
    }

    private Session createTestSession() throws Exception {
        injectMockRetrofit();
        Session session = new Session("Bearer test-token", TidepoolUploader.getSESSION_TOKEN_HEADER());
        session.authReply = new MAuthReply("test-user-id");
        session.token = "test-access-token";
        return session;
    }

    @Test
    public void login_extractsSessionTokenFromResponseHeader() throws Exception {
        // :: Setup
        injectMockRetrofit();
        server.enqueue(new MockResponse()
                .setBody("{\"userid\":\"u1\"}")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-tidepool-session-token", "extracted-token-123"));

        TidepoolUploader.Tidepool service = TidepoolUploader.getRetrofitInstance()
                .create(TidepoolUploader.Tidepool.class);

        // :: Act
        Response<MAuthReply> response = service.getLogin("Basic dXNlcjpwYXNz").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        String token = response.headers().get("x-tidepool-session-token");
        assertThat(token).isEqualTo("extracted-token-123");
        assertThat(response.body()).isNotNull();
        assertThat(response.body().userid).isEqualTo("u1");
    }

    @Test
    public void session_usesInjectedMockRetrofit() throws Exception {
        // :: Setup
        Session session = createTestSession();
        server.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        Response<List<MDatasetReply>> response = session.service.getOpenDataSets(
                session.token, session.authReply.userid,
                "com.eveningoutpost.dexdrip", 1).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("/v1/users/test-user-id/data_sets");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("test-access-token");
    }

    @Test
    public void startSession_queriesOpenDataSets() throws Exception {
        // :: Setup
        Session session = createTestSession();

        // Clear rate limiter so startSession proceeds
        clearRateLimit("tidepool-start-session");

        // Enqueue response for getOpenDataSets (empty list triggers openDataSet)
        server.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // Enqueue response for openDataSet
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ds1\",\"uploadId\":\"up1\"}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        TidepoolUploader.startSession(session, false);

        // :: Verify
        RecordedRequest firstRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(firstRequest).isNotNull();
        assertThat(firstRequest.getPath()).contains("/v1/users/test-user-id/data_sets");
        assertThat(firstRequest.getHeader("x-tidepool-session-token")).isEqualTo("test-access-token");
    }

    /**
     * Clears the JoH rate limiter for a given name so that rate-limited methods proceed.
     */
    private void clearRateLimit(String name) throws Exception {
        Field rateLimitsField = com.eveningoutpost.dexdrip.models.JoH.class.getDeclaredField("rateLimits");
        rateLimitsField.setAccessible(true);
        Object map = rateLimitsField.get(null);
        if (map instanceof java.util.Map) {
            ((java.util.Map<?, ?>) map).remove(name);
        }
    }
}
