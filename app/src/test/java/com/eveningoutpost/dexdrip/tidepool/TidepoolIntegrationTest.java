package com.eveningoutpost.dexdrip.tidepool;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Integration tests for TidepoolUploader.Tidepool Retrofit interface.
 * Uses MockWebServer to verify HTTP contracts.
 *
 * @author Asbjørn Aarrestad
 */
public class TidepoolIntegrationTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private TidepoolUploader.Tidepool api;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        server = new MockWebServer();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(TidepoolUploader.Tidepool.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getLogin_postsAuthorizationHeader() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"userid\":\"u1\"}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        api.getLogin("Basic dXNlcjpwYXNz").execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/auth/login");
        assertThat(request.getHeader("Authorization")).isEqualTo("Basic dXNlcjpwYXNz");
    }

    @Test
    public void getLogin_extractsSessionTokenFromHeader() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"userid\":\"u1\"}")
                .addHeader("Content-Type", "application/json")
                .addHeader("x-tidepool-session-token", "test-session-token-abc"));

        // :: Act
        Response<MAuthReply> response = api.getLogin("Basic dXNlcjpwYXNz").execute();

        // :: Verify
        String token = response.headers().get("x-tidepool-session-token");
        assertThat(token).isEqualTo("test-session-token-abc");
    }

    @Test
    public void getLogin_parsesAuthReply() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"userid\":\"user123\",\"username\":\"test@example.com\",\"emailVerified\":true,\"emails\":[\"test@example.com\"],\"termsAccepted\":\"2023-01-01\"}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        Response<MAuthReply> response = api.getLogin("Basic dXNlcjpwYXNz").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        MAuthReply body = response.body();
        assertThat(body).isNotNull();
    }

    @Test
    public void openDataSet_postsToUserDataSets() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ds1\",\"uploadId\":\"up1\"}")
                .addHeader("Content-Type", "application/json"));
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), "{\"test\":true}");

        // :: Act
        api.openDataSet("my-token", "user42", body).execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/users/user42/data_sets");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("my-token");
    }

    @Test
    public void doUpload_postsDataToDataset() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"data\":[]}")
                .addHeader("Content-Type", "application/json"));
        String jsonBody = "[{\"type\":\"cbg\",\"value\":120}]";
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), jsonBody);

        // :: Act
        api.doUpload("session-tok", "session99", body).execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/v1/datasets/session99/data");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("session-tok");
        assertThat(request.getBody().readUtf8()).isEqualTo(jsonBody);
    }

    @Test
    public void closeDataSet_putsToDataset() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ds1\"}")
                .addHeader("Content-Type", "application/json"));
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), "{\"status\":\"closed\"}");

        // :: Act
        api.closeDataSet("close-tok", "session77", body).execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getPath()).isEqualTo("/v1/datasets/session77");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("close-tok");
    }

    @Test
    public void getOpenDataSets_sendsQueryParams() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        api.getOpenDataSets("q-token", "user55", "com.eveningoutpost.dexdrip", 10).execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).contains("/v1/users/user55/data_sets");
        assertThat(request.getPath()).contains("client.name=com.eveningoutpost.dexdrip");
        assertThat(request.getPath()).contains("size=10");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("q-token");
    }

    @Test
    public void getDataSet_getsById() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"ds42\"}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        api.getDataSet("get-tok", "ds42").execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/v1/datasets/ds42");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("get-tok");
    }

    @Test
    public void deleteAllData_deletesUserData() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        api.deleteAllData("del-tok", "user88").execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/users/user88/data");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("del-tok");
    }

    @Test
    public void deleteDataSet_deletesById() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        // :: Act
        api.deleteDataSet("del-ds-tok", "ds99").execute();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/v1/datasets/ds99");
        assertThat(request.getHeader("x-tidepool-session-token")).isEqualTo("del-ds-tok");
    }
}
