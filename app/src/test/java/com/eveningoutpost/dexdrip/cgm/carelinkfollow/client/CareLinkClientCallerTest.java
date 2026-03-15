package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthType;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthentication;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.CountrySettings;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.DisplayMessage;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.MonitorData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Profile;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.User;
import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Caller-level integration tests for {@link CareLinkClient}.
 * <p>
 * Uses a testable subclass to bypass authentication and redirect
 * HTTPS requests to a local MockWebServer over HTTP.
 *
 * @author Asbjørn Aarrestad
 */
public class CareLinkClientCallerTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private boolean authReturnsNull = false;

    @Before
    public void setUp() {
        super.setUp();
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    // ---------------------------------------------------------------
    // Testable subclass
    // ---------------------------------------------------------------

    /**
     * Overrides authentication and URL building so that requests go
     * to MockWebServer over plain HTTP.
     */
    private class TestableCareLinkClient extends CareLinkClient {

        TestableCareLinkClient() {
            super("testuser", "testpass", "eu");
        }

        @Override
        protected CareLinkAuthentication getAuthentication() {
            if (authReturnsNull) {
                return null;
            }
            Headers headers = new Headers.Builder()
                    .add("Authorization", "Bearer test-token")
                    .build();
            return new CareLinkAuthentication(headers, CareLinkAuthType.Browser);
        }

        @Override
        protected String careLinkServer() {
            return server.getHostName() + ":" + server.getPort();
        }

        @Override
        protected String cloudServer() {
            return server.getHostName() + ":" + server.getPort();
        }

        @Override
        protected <T> T getData(String host, String path, RequestBody requestBody, Class<T> dataClass) {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(server.getHostName())
                    .port(server.getPort())
                    .addPathSegments(path)
                    .build();
            return getData(url, requestBody, dataClass);
        }

        @Override
        protected <T> T getData(String host, String path, Map<String, String> queryParams,
                                RequestBody requestBody, Class<T> dataClass) {
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(server.getHostName())
                    .port(server.getPort())
                    .addPathSegments(path);
            if (queryParams != null) {
                for (Map.Entry<String, String> param : queryParams.entrySet()) {
                    urlBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
            }
            return getData(urlBuilder.build(), requestBody, dataClass);
        }

        @Override
        public RecentData getConnectDisplayMessage(String username, String role,
                                                   String patientUsername, String endpointUrl) {
            JsonObject userJson = new JsonObject();
            userJson.addProperty("username", username);
            userJson.addProperty("role", role);
            if (!JoH.emptyString(patientUsername)) {
                userJson.addProperty("patientId", patientUsername);
            }
            userJson.addProperty("appVersion", "3.6.0");

            RequestBody requestBody = RequestBody.create(
                    MediaType.get("application/json; charset=utf-8"),
                    new GsonBuilder().create().toJson(userJson));

            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(server.getHostName())
                    .port(server.getPort())
                    .addPathSegments(API_PATH_DISPLAY_MESSAGE)
                    .build();

            DisplayMessage displayMessage = getData(url, requestBody, DisplayMessage.class);
            if (displayMessage != null && displayMessage.patientData != null) {
                return displayMessage.patientData;
            }
            return null;
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private TestableCareLinkClient createClient() throws Exception {
        server.start();
        TestableCareLinkClient client = new TestableCareLinkClient();
        client.sessionInfosLoaded = true;
        client.sessionUser = new User();
        client.sessionUser.role = "patient";
        client.sessionProfile = new Profile();
        client.sessionProfile.username = "testuser";
        client.sessionCountrySettings = new CountrySettings();
        client.sessionMonitorData = new MonitorData();
        return client;
    }

    // ---------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------

    @Test
    public void getLast24Hours_sendsCorrectRequestAndParsesResponse() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"medicalDeviceFamily\":\"NGP\"}"));

        // :: Act
        RecentData result = client.getLast24Hours();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("patient/connect/data");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-token");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(result).isNotNull();
    }

    @Test
    public void getConnectDisplayMessage_sendsPostWithUserJsonBody() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        String responseJson = "{\"patientData\":{\"medicalDeviceFamily\":\"NGP\"}}";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson));

        // :: Act
        RecentData result = client.getConnectDisplayMessage(
                "testuser", "patient", "patient123", "https://example.com/endpoint");

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"username\":\"testuser\"");
        assertThat(body).contains("\"role\":\"patient\"");
        assertThat(body).contains("\"patientId\":\"patient123\"");
        assertThat(body).contains("\"appVersion\":\"3.6.0\"");
        assertThat(request.getPath()).contains(API_PATH_DISPLAY_MESSAGE);
        assertThat(result).isNotNull();
    }

    @Test
    public void getData_returnsNullWhenAuthFails() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        authReturnsNull = true;

        // :: Act
        User result = client.getMyUser();

        // :: Verify
        assertThat(result).isNull();
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    public void getData_returnsNullOnServerError() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        server.enqueue(new MockResponse().setResponseCode(500));

        // :: Act
        User result = client.getMyUser();

        // :: Verify
        assertThat(result).isNull();
        assertThat(client.getLastResponseCode()).isEqualTo(500);
    }

    @Test
    public void getData_includesBrowserHeaders() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"medicalDeviceFamily\":\"NGP\"}"));

        // :: Act
        client.getLast24Hours();

        // :: Verify
        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("User-Agent")).contains("Mozilla/5.0");
        assertThat(request.getHeader("Accept")).contains("application/json");
    }

    private static final String API_PATH_DISPLAY_MESSAGE = "connect/carepartner/v13/display/message";
}
