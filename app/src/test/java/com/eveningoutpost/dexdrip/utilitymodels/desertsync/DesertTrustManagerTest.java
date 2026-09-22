package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * The custom TLS trust and hostname verification behind Desert Sync.
 * <p>
 * {@link TrustManager} supplies a deliberately naive trust manager — Desert Sync talks to a peer
 * with a self-signed certificate — and pairs it with a hostname verifier that pins one specific
 * certificate hash. What is asserted here is that trust accepts the untrusted peer, and that the
 * verifier refuses to let an unpinned peer's response through.
 * <p>
 * Neither branch of the hash comparison itself is reachable from a JVM test, and the third case
 * below says why and keeps that from going unnoticed. Serving the pinned certificate would not
 * open the accept branch either: the repository does ship it, with its key, in
 * {@code res/raw/localhost_cert.bks}, but the peer chain the verifier reads it back out of cannot
 * be read at all on this JVM.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class DesertTrustManagerTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpSelfSignedServer() throws IOException {
        HeldCertificate selfSigned = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(selfSigned)
                .build();

        server = new MockWebServer();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.start();
    }

    @After
    public void tearDownSelfSignedServer() throws IOException {
        server.shutdown();
    }

    /** The client shape Desert Sync builds: naive trust, and the verifier under examination. */
    private OkHttpClient desertSyncClient(HostnameVerifier verifier) throws Exception {
        return OkHttpWrapper.getClient().newBuilder()
                .sslSocketFactory(TrustManager.getSSLSocketFactory(),
                        TrustManager.getNaiveTrustManager())
                .hostnameVerifier(verifier)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    // ===== Naive trust manager ===================================================================

    /**
     * The naive trust manager lets a client reach a peer holding an untrusted self-signed cert.
     * <p>
     * Nothing else in the suite exercises a TLS handshake that the default trust manager would
     * reject outright, so this is the only place the naive manager's whole point is visible.
     */
    @Test
    public void naiveTrustManager_acceptsSelfSignedPeer() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("desert-ok"));
        OkHttpClient client = desertSyncClient((hostname, session) -> true);
        Request request = new Request.Builder().url(server.url("/joh")).build();

        // :: Act
        Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().string()).isEqualTo("desert-ok");
    }

    // ===== Certificate pin =======================================================================

    /**
     * The full Desert Sync client shape delivers nothing from a peer the verifier refuses.
     * <p>
     * The refusal is what is asserted, not its reason — see the next test for why the reason is
     * currently the unreadable chain rather than a mismatched hash. The okhttp behaviour this pins
     * is real either way: a verifier returning false must fail the call before any request is
     * transmitted, and {@link SSLPeerUnverifiedException} must be what refused it.
     * <p>
     * That exception is looked for through the suppressed list rather than as the thrown type,
     * because {@code localhost} resolves to both {@code 127.0.0.1} and {@code [::1]} and okhttp 5
     * races them. Its fast-fallback finder keeps the first failure to complete, attaches every later
     * one with {@code addSuppressed}, and throws the first. The verifier only refuses after a full
     * handshake, while the route MockWebServer is not listening on is refused outright — so
     * {@code ConnectException: Connection refused} normally wins and the real refusal ends up
     * suppressed. Asserting the thrown type directly would be asserting which route won a race.
     */
    @Test
    public void xdripHostVerifier_rejectsUnpinnedPeer() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("must-not-be-delivered"));
        OkHttpClient client = desertSyncClient(TrustManager.getXdripHostVerifier());
        Request request = new Request.Builder().url(server.url("/joh")).build();

        // :: Act
        IOException refusal = assertThrows(IOException.class, () -> client.newCall(request).execute());

        // :: Verify — the verifier refused it, and the client transmitted nothing
        assertThat(unverifiedPeerWithin(refusal)).isNotNull();
        assertThat(server.getRequestCount()).isEqualTo(0);
    }

    /**
     * The {@link SSLPeerUnverifiedException} anywhere in {@code failure}: itself, its causes, or
     * anything suppressed onto it or onto those. Null when the call failed for some other reason,
     * which is the case this test needs to stay able to fail on.
     */
    private static SSLPeerUnverifiedException unverifiedPeerWithin(Throwable failure) {
        if (failure == null) {
            return null;
        }
        if (failure instanceof SSLPeerUnverifiedException) {
            return (SSLPeerUnverifiedException) failure;
        }
        for (Throwable suppressed : failure.getSuppressed()) {
            SSLPeerUnverifiedException found = unverifiedPeerWithin(suppressed);
            if (found != null) {
                return found;
            }
        }
        return failure.getCause() == failure ? null : unverifiedPeerWithin(failure.getCause());
    }

    /**
     * The peer certificate chain cannot be read on this JVM, so the pin never compares a hash.
     * <p>
     * Production reads the chain through {@code SSLSession#getPeerCertificateChain()}, which
     * returns the legacy {@code javax.security.cert} type. The JDK this suite runs on no longer
     * ships the implementation behind that type: {@code X509Certificate.getInstance(byte[])} fails
     * with {@code ClassNotFoundException: com/sun/security/cert/internal/x509/X509V1CertImpl}.
     * Conscrypt 2.5.2, which Robolectric installs, then masks that as an
     * {@code IllegalArgumentException: Self-causation not permitted}, because its wrapper passes
     * the new exception as its own cause.
     * <p>
     * The consequence outweighs the cause. Production catches {@code Exception} and returns false,
     * so the rejection above is green whether the hash was compared or the chain was simply
     * unavailable. This states that out loud, and turns red the day the chain becomes readable —
     * which is the day the pin is genuinely under test again.
     */
    @Test
    public void peerCertificateChain_isUnreadableSoTheHashIsNeverCompared() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("desert-ok"));
        final AtomicReference<Throwable> chainFailure = new AtomicReference<>();
        OkHttpClient client = desertSyncClient((hostname, session) -> {
            try {
                session.getPeerCertificateChain();
            } catch (Throwable unreadable) {
                chainFailure.set(unreadable);
            }
            return true;
        });
        Request request = new Request.Builder().url(server.url("/joh")).build();

        // :: Act
        client.newCall(request).execute().close();

        // :: Verify
        assertThat(chainFailure.get()).isNotNull();
    }
}
