package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.security.cert.X509Certificate;

/**
 * Proves the root cause of "Trust anchor for certification path not found" for home-hosted
 * Nightscout users with self-signed or home CA certificates.
 *
 * When targetSdkVersion >= 24 and no network_security_config.xml exists, Android 7.0+ trusts
 * only system CAs by default — user-installed CAs are excluded. This JVM test reproduces that
 * behaviour: the default JVM TrustManager does not know about our test CA, mirroring exactly
 * what Android does with user-installed CAs under the default NSC policy.
 *
 * @see <a href="https://github.com/NightscoutFoundation/xDrip/discussions/4139">Discussion #4139</a>
 */
public class UserCertificateTrustTest {

    private static final BouncyCastleProvider BC = new BouncyCastleProvider();

    /** CA certificate generated fresh for each test run — simulates a home CA. */
    private X509Certificate caCert;
    private MockWebServer server;

    // -----------------------------------------------------------------------
    // Test infrastructure — generates a self-signed CA + server cert pair and
    // starts MockWebServer over HTTPS using those certs.
    // -----------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        Date notBefore = new Date(System.currentTimeMillis() - 86_400_000L);
        Date notAfter  = new Date(System.currentTimeMillis() + 365L * 86_400_000L);

        // Generate home CA key + self-signed certificate
        KeyPair caKeyPair = kpg.generateKeyPair();
        X500Name caName = new X500Name("CN=Home Test CA");
        X509v3CertificateBuilder caBuilder = new X509v3CertificateBuilder(
                caName, BigInteger.ONE, notBefore, notAfter, caName,
                SubjectPublicKeyInfo.getInstance(caKeyPair.getPublic().getEncoded()));
        caBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        ContentSigner caSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC).build(caKeyPair.getPrivate());
        caCert = new JcaX509CertificateConverter().setProvider(BC)
                .getCertificate(caBuilder.build(caSigner));

        // Generate server key + certificate signed by the home CA
        KeyPair serverKeyPair = kpg.generateKeyPair();
        X509v3CertificateBuilder serverBuilder = new X509v3CertificateBuilder(
                caName, BigInteger.valueOf(2), notBefore, notAfter,
                new X500Name("CN=localhost"),
                SubjectPublicKeyInfo.getInstance(serverKeyPair.getPublic().getEncoded()));
        serverBuilder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName[]{
                        new GeneralName(GeneralName.dNSName, "localhost"),
                        new GeneralName(GeneralName.iPAddress, "127.0.0.1")
                }));
        ContentSigner serverSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BC).build(caKeyPair.getPrivate());
        X509Certificate serverCert = new JcaX509CertificateConverter().setProvider(BC)
                .getCertificate(serverBuilder.build(serverSigner));

        // Build server SSLContext
        KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        serverKeyStore.load(null, null);
        serverKeyStore.setKeyEntry("server", serverKeyPair.getPrivate(),
                "".toCharArray(),
                new X509Certificate[]{serverCert, caCert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverKeyStore, "".toCharArray());
        SSLContext serverSsl = SSLContext.getInstance("TLS");
        serverSsl.init(kmf.getKeyManagers(), null, null);

        server = new MockWebServer();
        server.useHttps(serverSsl.getSocketFactory(), false);
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        if (server != null) server.shutdown();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    /**
     * A default OkHttpClient — with no custom trust — rejects a server certificate signed by a
     * home CA. This is what happens on Android 7.0+ when targetSdkVersion >= 24 and no
     * network_security_config.xml adds user CAs to the trust anchors.
     */
    @Test
    public void defaultOkHttpClient_rejectsSelfSignedCaCert() throws IOException {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(server.url("/")).build();

        // :: Act + Verify
        try {
            client.newCall(request).execute();
            fail("Expected SSLHandshakeException: home CA is not in default trust store");
        } catch (SSLHandshakeException expected) {
            // Correct. On the JVM the message is "PKIX path building failed ... unable to find
            // valid certification path"; on Android the equivalent message is
            // "Trust anchor for certification path not found" (discussion #4139). Both are the
            // same SSLHandshakeException — trust-anchor validation rejecting an unknown CA.
        }
    }

    /**
     * When the home CA is added to the trust store, connections succeed. This is the behaviour
     * that network_security_config.xml with {@code <certificates src="user"/>} enables on Android:
     * user-installed CAs are injected into the SSL TrustManager alongside system CAs.
     */
    @Test
    public void okHttpClientWithHomeCaTrusted_acceptsSelfSignedCaCert() throws Exception {
        // :: Setup — trust store containing only our home CA (simulates Android NSC user trust)
        server.enqueue(new MockResponse().setBody("ok"));

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("home-ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new javax.net.ssl.TrustManager[]{trustManager}, null);

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .hostnameVerifier((hostname, session) -> true) // hostname check is irrelevant to this test
                .build();

        Request request = new Request.Builder().url(server.url("/")).build();

        // :: Act
        okhttp3.Response response = client.newCall(request).execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().string()).isEqualTo("ok");
    }
}
