package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import static com.google.common.truth.Truth.assertThat;

/**
 * The transport {@link ShareRest} talks to Dexcom Share over: a server whose certificate no trusted
 * authority vouches for must be refused, and the account password, session id and response
 * payload must never reach the log.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class ShareRestTransportTest extends RobolectricTestWithConfig {

    private static final String PASSWORD = "hunter2-share-password";
    private static final String SESSION_ID = "0f1e2d3c-session-id-under-test";
    private static final String RESPONSE_PAYLOAD = "\"response-payload-under-test\"";

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        ShadowLog.clear();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    private OkHttpClient shareClient() {
        return new ShareRest(RuntimeEnvironment.getApplication(), new OkHttpClient()).getOkHttpClient();
    }

    private List<String> shareRestLogMessages() {
        return ShadowLog.getLogsForTag(ShareRest.TAG).stream()
                .map(log -> log.msg)
                .collect(Collectors.toList());
    }

    // ===== Certificate trust ==================================================================================

    /**
     * A man-in-the-middle presents a certificate it made itself. The Share client must refuse to talk to it
     * rather than hand over the Dexcom credentials.
     */
    @Test
    public void selfSignedServerCertificate_isRefused() throws Exception {
        // :: Setup
        HeldCertificate selfSigned = new HeldCertificate.Builder().build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(selfSigned)
                .build();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("\"session\""));
        server.start();
        // An IP literal gives OkHttp one route; "localhost" adds ::1, whose refused connect can mask the TLS failure.
        Request request = new Request.Builder()
                .url(server.url("/General/LoginPublisherAccountByName").newBuilder().host("127.0.0.1").build())
                .post(RequestBody.create("{\"password\":\"" + PASSWORD + "\"}", null))
                .build();

        // :: Act
        IOException failure = null;
        try {
            shareClient().newCall(request).execute().close();
        } catch (IOException e) {
            failure = e;
        }

        // :: Verify
        assertThat(failure).isInstanceOf(SSLException.class);
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    // ===== Logging ============================================================================================

    /** The login request body carries the account password in clear text. */
    @Test
    public void requestBody_isNotLogged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"session\""));
        server.start();
        Request request = new Request.Builder()
                .url(server.url("/General/LoginPublisherAccountByName"))
                .post(RequestBody.create("{\"password\":\"" + PASSWORD + "\"}", null))
                .build();

        // :: Act
        shareClient().newCall(request).execute().close();

        // :: Verify
        assertThat(server.takeRequest().getBody().readUtf8()).contains(PASSWORD);
        assertThat(String.join("\n", shareRestLogMessages())).doesNotContain(PASSWORD);
    }

    /** Every authenticated Share call puts the session id in the query string. */
    @Test
    public void sessionIdInUrl_isNotLogged() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("true"));
        server.start();
        Request request = new Request.Builder()
                .url(server.url("/Publisher/IsRemoteMonitoringSessionActive?sessionId=" + SESSION_ID))
                .post(RequestBody.create("", null))
                .build();

        // :: Act
        shareClient().newCall(request).execute().close();

        // :: Verify
        assertThat(server.takeRequest().getPath()).contains(SESSION_ID);
        assertThat(String.join("\n", shareRestLogMessages())).doesNotContain(SESSION_ID);
    }

    /** The login response body is the session id itself. */
    @Test
    public void responseBody_isNotLogged_butStillReachesTheCaller() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody(RESPONSE_PAYLOAD));
        server.start();
        Request request = new Request.Builder()
                .url(server.url("/General/LoginPublisherAccountByName"))
                .post(RequestBody.create("{}", null))
                .build();

        // :: Act
        String body;
        try (Response response = shareClient().newCall(request).execute()) {
            body = response.body().string();
        }

        // :: Verify
        assertThat(body).isEqualTo(RESPONSE_PAYLOAD);
        assertThat(String.join("\n", shareRestLogMessages())).doesNotContain(RESPONSE_PAYLOAD);
    }
}
