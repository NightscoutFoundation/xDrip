package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthType;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth.CareLinkAuthentication;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.CountrySettings;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.MonitorData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.Profile;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.RecentData;
import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.Headers;
import okhttp3.HttpUrl;
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
     * Overrides authentication and the core getData(HttpUrl, ...) method so that
     * all requests go to MockWebServer over plain HTTP. Production methods like
     * getConnectDisplayMessage run their actual code — only the transport is swapped.
     */
    private static class TestableCareLinkClient extends CareLinkClient {

        private final MockWebServer mockServer;
        private boolean authReturnsNull = false;

        TestableCareLinkClient(MockWebServer server) {
            super("testuser", "testpass", "eu");
            this.mockServer = server;
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
            return mockServer.getHostName();
        }

        @Override
        protected String cloudServer() {
            return mockServer.getHostName();
        }

        /** Rewrite https→http and fix port so production code hits MockWebServer */
        @Override
        protected <T> T getData(HttpUrl url, RequestBody requestBody, Class<T> dataClass) {
            HttpUrl httpUrl = url.newBuilder()
                    .scheme("http")
                    .host(mockServer.getHostName())
                    .port(mockServer.getPort())
                    .build();
            return super.getData(httpUrl, requestBody, dataClass);
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private TestableCareLinkClient createClient() throws Exception {
        server.start();
        TestableCareLinkClient client = new TestableCareLinkClient(server);
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
        assertThat(request.getPath()).contains(CareLinkClient.API_PATH_DISPLAY_MESSAGE);
        assertThat(result).isNotNull();
    }

    @Test
    public void getData_returnsNullWhenAuthFails() throws Exception {
        // :: Setup
        TestableCareLinkClient client = createClient();
        client.authReturnsNull = true;

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

}
