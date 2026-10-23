package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import static com.google.common.truth.Truth.assertThat;

/**
 * TLS contract for the shared okhttp client.
 * <p>
 * Every HTTPS caller in xDrip — Nightscout, Dexcom, Tidepool, Desert Sync — derives its client from
 * {@link OkHttpWrapper#getClient()} with {@code newBuilder()}, which keeps the production
 * interceptors, timeouts and connection pool. This asserts that such a derived client completes a
 * handshake and delivers a body, which is the only unit-level exercise of the platform TLS stack.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class OkHttpWrapperTlsTest extends RobolectricTestWithConfig {

    private HeldCertificate localhostCertificate;
    private HandshakeCertificates clientCertificates;
    private MockWebServer server;

    @Before
    public void setUpHttpsServer() throws IOException {
        localhostCertificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(localhostCertificate)
                .build();
        clientCertificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate(localhostCertificate.certificate())
                .build();

        server = new MockWebServer();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.start();
    }

    @After
    public void tearDownHttpsServer() throws IOException {
        server.shutdown();
    }

    /** A client derived from the shared client completes a TLS handshake and delivers the body. */
    @Test
    public void derivedClient_completesTlsRoundTrip() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("tls-ok"));
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .sslSocketFactory(clientCertificates.sslSocketFactory(),
                        clientCertificates.trustManager())
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(server.url("/api/v1/entries")).build();

        // :: Act
        Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().string()).isEqualTo("tls-ok");
        assertThat(response.handshake()).isNotNull();
        assertThat(response.handshake().peerCertificates())
                .contains(localhostCertificate.certificate());
    }
}
