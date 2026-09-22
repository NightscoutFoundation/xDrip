package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * A real request driven through the third-party Nocturne SDK on xDrip's shared okhttp client.
 * <p>
 * {@code nocturne-java} is the one okhttp consumer in the tree that xDrip does not compile: it is
 * handed {@link com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper#getClient()} and builds its
 * own requests with it. The other Nocturne tests cover request mapping and treatment routing, and
 * no HTTP happens in either — so nothing else here would notice if the SDK linked against a new
 * okhttp but behaved differently on the wire.
 * <p>
 * Client registration is the subject because it is the shortest production path that reaches the
 * SDK's own request building: one call, one request, no session to fake. Cleartext only; the SDK's
 * progress-reporting bodies are not covered.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class NocturneApiClientTest extends RobolectricTestWithConfig {

    private static final String REGISTER_PATH = "/api/oauth/register";

    private MockWebServer server;
    private String basePath;

    @Before
    public void setUpServerAndInstanceUrl() throws IOException {
        server = new MockWebServer();
        server.start();

        // MockWebServer serves localhost, which getBaseUrl() leaves on http:// untouched
        Pref.setString("nocturne_instance_url", server.url("/").toString());
        PersistentStore.setString("nocturne_client_id", "");

        basePath = "http://" + server.getHostName() + ":" + server.getPort();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
        PersistentStore.setString("nocturne_client_id", "");
    }

    // ===== The SDK on the shared client ==========================================================

    /**
     * Registering leaves the phone as a POST the Nocturne API would recognise.
     * <p>
     * The assertions are read off the request the server received rather than off the return value
     * alone: every failure inside {@code registerClient} is caught and reported as null, so a
     * service that never sent anything looks the same from the outside as a rejected one.
     */
    @Test
    public void registerClient_reachesTheServerThroughTheSharedClient() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"client_id\":\"granted-client-id\"}"));
        NocturneOAuthService service = new NocturneOAuthService();

        // :: Act
        String clientId = service.registerClient();

        // :: Verify
        assertThat(clientId).isEqualTo("granted-client-id");
        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo(REGISTER_PATH);
        assertThat(recorded.getHeader("Origin")).isEqualTo(basePath);
        assertThat(recorded.getBody().readUtf8()).contains("xDrip+");
    }

    /** The registered client id is persisted, so a second call answers without a second request. */
    @Test
    public void registerClient_persistsTheClientIdAndDoesNotAskTwice() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"client_id\":\"granted-client-id\"}"));
        NocturneOAuthService service = new NocturneOAuthService();

        // :: Act
        service.registerClient();
        String second = service.registerClient();

        // :: Verify
        assertThat(second).isEqualTo("granted-client-id");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    /**
     * A server error is reported as a failure, and the request was really made.
     * <p>
     * The recorded request is what separates this from a service that returned null because no
     * instance URL was configured in the first place.
     */
    @Test
    public void registerClient_reportsFailureOnServerError() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        NocturneOAuthService service = new NocturneOAuthService();

        // :: Act
        String clientId = service.registerClient();

        // :: Verify
        assertThat(clientId).isNull();
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).isEqualTo(REGISTER_PATH);
    }
}
