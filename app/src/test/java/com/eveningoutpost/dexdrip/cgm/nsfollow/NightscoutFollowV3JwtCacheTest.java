package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Verifies that {@link NightscoutFollowV3#getJwt()} caches the JWT and avoids
 * a network round-trip on every poll cycle.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3JwtCacheTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        RetrofitService.clear();
        NightscoutFollowV3.resetInstance();
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        RetrofitService.clear();
        NightscoutFollowV3.resetInstance();
        if (server != null) {
            server.shutdown();
        }
    }

    /** Returns the server base URL with the token embedded as RFC 3986 userinfo. */
    private String urlWithToken(String token) {
        return server.url("/").toString().replace("://", "://" + token + "@");
    }

    @Test
    public void getJwt_cachesResultAndMakesOnlyOneExchangeRequest() throws Exception {
        // :: Setup
        Pref.setString("nsfollow_url", urlWithToken("test-74c387663745c193"));
        long futureExpirySec = System.currentTimeMillis() / 1000 + 7200;
        server.enqueue(new MockResponse()
                .setBody("{\"token\":\"my-cached-jwt\",\"sub\":\"test\",\"exp\":" + futureExpirySec + "}")
                .addHeader("Content-Type", "application/json"));

        // :: Act — call getJwt() twice
        String first = NightscoutFollowV3.getJwt();
        String second = NightscoutFollowV3.getJwt();

        // :: Verify — only one request was made, both calls return the same JWT
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(first).isEqualTo("my-cached-jwt");
        assertThat(second).isEqualTo("my-cached-jwt");
    }

    @Test
    public void getJwt_refetchesAfterResetInstance() throws Exception {
        // :: Setup
        Pref.setString("nsfollow_url", urlWithToken("test-74c387663745c193"));
        long futureExpirySec = System.currentTimeMillis() / 1000 + 7200;
        server.enqueue(new MockResponse()
                .setBody("{\"token\":\"first-jwt\",\"sub\":\"test\",\"exp\":" + futureExpirySec + "}")
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("{\"token\":\"second-jwt\",\"sub\":\"test\",\"exp\":" + futureExpirySec + "}")
                .addHeader("Content-Type", "application/json"));

        // :: Act — first call, then reset, then second call
        String first = NightscoutFollowV3.getJwt();
        NightscoutFollowV3.resetInstance();
        String second = NightscoutFollowV3.getJwt();

        // :: Verify — cache cleared by reset, second call triggers a new exchange
        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(first).isEqualTo("first-jwt");
        assertThat(second).isEqualTo("second-jwt");
    }

    @Test
    public void getJwt_returnsNullWhenNoTokenConfigured() {
        // :: Setup — URL has no userinfo (open instance)
        Pref.setString("nsfollow_url", server.url("/").toString());

        // :: Act + Verify — no network request, returns null
        assertThat(NightscoutFollowV3.getJwt()).isNull();
        assertThat(server.getRequestCount()).isEqualTo(0);
    }
}
