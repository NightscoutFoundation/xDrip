package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * The cookie jar CareLink follow authenticates through.
 * <p>
 * CareLink issues its session cookie on one host and is then called on another under the same
 * registered domain, so the jar has to store a parent-domain cookie and replay it for a sibling
 * host. Deciding whether {@code Domain=minimed.eu} is a legal scope for {@code carelink.minimed.eu}
 * sends okhttp into its public suffix database, which okhttp 5 relocates into the Android AAR's
 * assets — so this contract is asserted rather than assumed.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class EditableCookieJarTest extends RobolectricTestWithConfig {

    private static final String AUTH_COOKIE = "auth_tmp_token";

    private MockWebServer server;
    private EditableCookieJar cookieJar;

    @Before
    public void setUpServerAndJar() throws IOException {
        server = new MockWebServer();
        server.start();
        cookieJar = new EditableCookieJar();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    /** A client that resolves every hostname to the loopback MockWebServer. */
    private OkHttpClient clientResolvingEverythingToMockServer() {
        return OkHttpWrapper.getClient().newBuilder()
                .cookieJar(cookieJar)
                .dns(hostname -> Collections.singletonList(InetAddress.getByName("127.0.0.1")))
                .build();
    }

    // ===== Session handoff =======================================================================

    /** A cookie set by the server is stored by the jar and replayed on the next request. */
    @Test
    public void jar_replaysServerSetCookieOnTheNextRequest() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .addHeader("Set-Cookie", AUTH_COOKIE + "=abc123; Path=/")
                .setBody("logged-in"));
        server.enqueue(new MockResponse().setBody("data"));
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder().cookieJar(cookieJar).build();

        // :: Act
        client.newCall(new Request.Builder().url(server.url("/login")).build()).execute().close();
        client.newCall(new Request.Builder().url(server.url("/data")).build()).execute().close();

        // :: Verify
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        assertThat(second.getHeader("Cookie")).contains(AUTH_COOKIE + "=abc123");
        assertThat(cookieJar.getCookies(AUTH_COOKIE).get(0).value()).isEqualTo("abc123");
    }

    /**
     * The §13.2 guard. A parent-domain cookie issued on the auth host is replayed on the API host.
     * <p>
     * Scoping {@code Domain=minimed.eu} against the request host {@code carelink.minimed.eu} is the
     * one call on this path that consults okhttp's public suffix database, and it happens inside
     * okhttp's bridge interceptor during {@code execute()} — not in this test's setup.
     */
    @Test
    public void cookiePolicy_replaysParentDomainCookieAcrossHosts() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .addHeader("Set-Cookie", AUTH_COOKIE + "=abc123; Domain=minimed.eu; Path=/")
                .setBody("logged-in"));
        server.enqueue(new MockResponse().setBody("data"));
        OkHttpClient client = clientResolvingEverythingToMockServer();
        int port = server.getPort();

        // :: Act
        client.newCall(new Request.Builder()
                .url("http://carelink.minimed.eu:" + port + "/patient/sso/login")
                .build()).execute().close();
        client.newCall(new Request.Builder()
                .url("http://clcloud.minimed.eu:" + port + "/connect/v2/display/message")
                .build()).execute().close();

        // :: Verify
        server.takeRequest();
        RecordedRequest onApiHost = server.takeRequest();
        assertThat(onApiHost.getHeader("Cookie")).contains(AUTH_COOKIE + "=abc123");
        assertThat(cookieJar.getCookies(AUTH_COOKIE).get(0).domain()).isEqualTo("minimed.eu");
    }

    /**
     * A cookie scoped to a public suffix is refused outright.
     * <p>
     * The other half of the same database lookup, and the half that says the lookup happened at
     * all: {@code Domain=eu} would be a supercookie readable by every host under the registry, so
     * okhttp must reject it where it accepts {@code Domain=minimed.eu}. Measured on 2026-08-22, the
     * two differ exactly this way at 4.12.0 — which is what makes the case above a guard rather
     * than an assumption. If okhttp 5 ever stopped consulting the database, the case above would
     * still be green and this one would go red.
     */
    @Test
    public void cookiePolicy_refusesACookieScopedToAPublicSuffix() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .addHeader("Set-Cookie", AUTH_COOKIE + "=abc123; Domain=eu; Path=/")
                .setBody("logged-in"));
        server.enqueue(new MockResponse().setBody("data"));
        OkHttpClient client = clientResolvingEverythingToMockServer();
        int port = server.getPort();

        // :: Act
        client.newCall(new Request.Builder()
                .url("http://carelink.minimed.eu:" + port + "/patient/sso/login")
                .build()).execute().close();
        client.newCall(new Request.Builder()
                .url("http://clcloud.minimed.eu:" + port + "/connect/v2/display/message")
                .build()).execute().close();

        // :: Verify
        server.takeRequest();
        assertThat(server.takeRequest().getHeader("Cookie")).isNull();
        assertThat(cookieJar.getAllCookies()).isEmpty();
    }

    // ===== Jar bookkeeping =======================================================================

    /** Re-issuing a cookie replaces the stored one instead of accumulating a duplicate. */
    @Test
    public void jar_replacesACookieReissuedUnderTheSameName() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().addHeader("Set-Cookie", AUTH_COOKIE + "=first; Path=/"));
        server.enqueue(new MockResponse().addHeader("Set-Cookie", AUTH_COOKIE + "=second; Path=/"));
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder().cookieJar(cookieJar).build();

        // :: Act
        client.newCall(new Request.Builder().url(server.url("/login")).build()).execute().close();
        client.newCall(new Request.Builder().url(server.url("/login")).build()).execute().close();

        // :: Verify
        assertThat(cookieJar.getCookies(AUTH_COOKIE)).hasSize(1);
        assertThat(cookieJar.getCookies(AUTH_COOKIE).get(0).value()).isEqualTo("second");
    }

    /**
     * Cookies restored from the credential store are sent on the first request.
     * <p>
     * This is how CareLink follow resumes a session across process death:
     * {@code createHttpClient()} seeds the jar via {@code AddCookies} before any call is made.
     */
    @Test
    public void jar_sendsCookiesSeededFromTheCredentialStore() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("data"));
        cookieJar.AddCookies(new Cookie[]{new Cookie.Builder()
                .name(AUTH_COOKIE)
                .value("restored")
                .domain("localhost")
                .path("/")
                .build()});
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder().cookieJar(cookieJar).build();

        // :: Act
        client.newCall(new Request.Builder().url(server.url("/data")).build()).execute().close();

        // :: Verify
        assertThat(server.takeRequest().getHeader("Cookie")).contains(AUTH_COOKIE + "=restored");
    }
}
