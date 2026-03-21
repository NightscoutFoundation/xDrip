package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Verifies that {@link NightscoutFollow#work(boolean)} routes to the correct API version
 * based on the {@code nsfollow_use_v3} preference, and that
 * {@link NightscoutFollow#resetInstance()} propagates the reset to {@link NightscoutFollowV3}.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowDelegationTest extends RobolectricTestWithConfig {

    private static final String V1_ENTRIES_JSON =
            "[{\"_id\":\"abc123\",\"sgv\":180,\"date\":1773581680168,\"type\":\"sgv\"}]";

    private static final String V3_ENTRIES_JSON =
            "{\"status\":200,\"result\":["
                    + "{\"identifier\":\"805201b8\",\"sgv\":181,\"date\":1773944260838,"
                    + "\"unfiltered\":0,\"filtered\":0,\"noise\":1}"
                    + "]}";

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        RetrofitService.clear();
        NightscoutFollow.resetInstance();
        NightscoutFollowV3.resetInstance();
        server = new MockWebServer();
        server.start();
        Pref.setString("nsfollow_url", server.url("/").toString());
        Pref.setBoolean("nsfollow_use_v3", false);
        JoH.clearRatelimit("nsfollow-v3-devicestatus");
    }

    @After
    public void tearDown() throws Exception {
        RetrofitService.clear();
        NightscoutFollow.resetInstance();
        NightscoutFollowV3.resetInstance();
        if (server != null) {
            server.shutdown();
        }
    }

    // ===== Routing — v3 pref on ===================================================================

    @Test
    public void work_routesToV3Entries_whenPrefEnabled() throws Exception {
        // :: Setup — pre-inject JWT so no auth network call is needed;
        //    enqueue 2 responses since work() makes entries + devicestatus requests
        NightscoutFollowV3.setJwtForTest("test-jwt", System.currentTimeMillis() + 3_600_000L);
        server.enqueue(new MockResponse()
                .setBody(V3_ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[]}")
                .addHeader("Content-Type", "application/json"));
        Pref.setBoolean("nsfollow_use_v3", true);

        // :: Execute
        NightscoutFollow.work(false);

        // :: Verify — one of the two async requests goes to /api/v3/entries
        RecordedRequest first = server.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest second = server.takeRequest(2, TimeUnit.SECONDS);
        boolean hasEntries = false;
        for (RecordedRequest req : new RecordedRequest[]{first, second}) {
            if (req != null && req.getPath().startsWith("/api/v3/entries")) {
                hasEntries = true;
            }
        }
        assertThat(hasEntries).isTrue();
    }

    // ===== Routing — v3 pref off ==================================================================

    @Test
    public void work_routesToV1Entries_whenPrefDisabled() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(V1_ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));
        Pref.setBoolean("nsfollow_use_v3", false);

        // :: Execute
        NightscoutFollow.work(false);

        // :: Verify
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).startsWith("/api/v1/entries.json");
    }

    // ===== DeviceStatus — work() makes a devicestatus request ====================================

    @Test
    public void work_makesDeviceStatusRequest_whenV3Enabled() throws Exception {
        // :: Setup — pre-inject JWT; enqueue responses for entries and devicestatus
        NightscoutFollowV3.setJwtForTest("test-jwt", System.currentTimeMillis() + 3_600_000L);
        server.enqueue(new MockResponse()
                .setBody(V3_ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[]}")
                .addHeader("Content-Type", "application/json"));
        Pref.setBoolean("nsfollow_use_v3", true);

        // :: Execute
        NightscoutFollow.work(false);

        // :: Verify — take both async requests, check one goes to /api/v3/devicestatus
        RecordedRequest first = server.takeRequest(2, TimeUnit.SECONDS);
        RecordedRequest second = server.takeRequest(2, TimeUnit.SECONDS);
        boolean hasDeviceStatus = false;
        for (RecordedRequest req : new RecordedRequest[]{first, second}) {
            if (req != null && req.getPath().startsWith("/api/v3/devicestatus")) {
                hasDeviceStatus = true;
            }
        }
        assertThat(hasDeviceStatus).isTrue();
    }

    // ===== Reset propagation ======================================================================

    @Test
    public void resetInstance_clearsV3JwtCache_whenJwtWasCached() {
        // :: Setup — token in URL so jwtStatusText() gets past its null-token guard
        Pref.setString("nsfollow_url", "https://some-token@my.nightscout.com");
        NightscoutFollowV3.setJwtForTest("test-jwt", System.currentTimeMillis() + 3_600_000L);
        assertThat(NightscoutFollowV3.jwtStatusText()).startsWith("Active");

        // :: Execute
        NightscoutFollow.resetInstance();

        // :: Verify — JWT cache cleared; token pref still set so "Not fetched" is returned
        assertThat(NightscoutFollowV3.jwtStatusText()).isEqualTo("Not fetched");
    }
}
