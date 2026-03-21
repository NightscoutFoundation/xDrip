package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.AuthResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Integration tests for the {@link NightscoutFollowV3.NightscoutAuth} Retrofit interface.
 * <p>
 * Verifies that the JWT exchange uses the correct v2 path, passes the access token
 * in the URL, and parses the JWT and expiry from the response.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3AuthTest extends RobolectricTestWithConfig {

    private static final String AUTH_RESPONSE_JSON =
            "{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test\","
                    + "\"sub\":\"test\","
                    + "\"permissionGroups\":[[\"*:*:read\"]],"
                    + "\"iat\":1774173242,"
                    + "\"exp\":1774202042}";

    private MockWebServer server;
    private NightscoutFollowV3.NightscoutAuth api;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(NightscoutFollowV3.NightscoutAuth.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    // ===== Request shape =========================================================================

    @Test
    public void requestJwt_usesV2AuthPath() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE_JSON).addHeader("Content-Type", "application/json"));
        api.requestJwt("test-74c387663745c193").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v2/authorization/request/");
    }

    @Test
    public void requestJwt_includesAccessTokenInPath() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE_JSON).addHeader("Content-Type", "application/json"));
        api.requestJwt("test-74c387663745c193").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("test-74c387663745c193");
    }

    // ===== Response parsing ======================================================================

    @Test
    public void requestJwt_parsesJwtToken() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE_JSON).addHeader("Content-Type", "application/json"));

        Response<AuthResponse> response = api.requestJwt("test-74c387663745c193").execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().token).startsWith("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    }

    @Test
    public void requestJwt_parsesExpiry() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE_JSON).addHeader("Content-Type", "application/json"));

        Response<AuthResponse> response = api.requestJwt("test-74c387663745c193").execute();

        assertThat(response.body().exp).isEqualTo(1774202042L);
    }

    @Test
    public void requestJwt_parsesSubjectName() throws Exception {
        server.enqueue(new MockResponse().setBody(AUTH_RESPONSE_JSON).addHeader("Content-Type", "application/json"));

        Response<AuthResponse> response = api.requestJwt("test-74c387663745c193").execute();

        assertThat(response.body().sub).isEqualTo("test");
    }

    @Test
    public void requestJwt_handlesUnauthorized() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));

        Response<AuthResponse> response = api.requestJwt("bad-token").execute();

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(401);
    }
}
