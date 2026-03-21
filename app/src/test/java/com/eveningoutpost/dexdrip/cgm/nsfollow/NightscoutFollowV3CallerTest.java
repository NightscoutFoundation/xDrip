package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.NightscoutV3Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

/**
 * Caller-level integration tests for {@link NightscoutFollowV3.Nightscout} via
 * {@link RetrofitService}.
 * <p>
 * Exercises the full production stack with interceptors and custom Gson,
 * verifying end-to-end v3 deserialization.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3CallerTest extends RobolectricTestWithConfig {

    private static final String V3_ENTRIES_JSON =
            "{\"status\":200,\"result\":["
                    + "{\"identifier\":\"805201b8\",\"sgv\":181,\"date\":1773944260838,"
                    + "\"unfiltered\":0,\"filtered\":0,\"noise\":1},"
                    + "{\"identifier\":\"8d3f57ad\",\"sgv\":175,\"date\":1773943960874,"
                    + "\"unfiltered\":0,\"filtered\":0,\"noise\":0}"
                    + "]}";

    private static final String V3_TREATMENTS_JSON =
            "{\"status\":200,\"result\":["
                    + "{\"_id\":\"507f1f77bcf86cd799439011\","
                    + "\"eventType\":\"Correction Bolus\",\"insulin\":0.4,\"date\":1773944301015}"
                    + "]}";

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        RetrofitService.clear();
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        RetrofitService.clear();
        if (server != null) {
            server.shutdown();
        }
    }

    // ===== Entries — full production stack =======================================================

    @Test
    public void retrofitService_createsWorkingV3EntriesClient() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(V3_ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));

        String baseUrl = server.url("/").toString();
        NightscoutFollowV3.Nightscout api = RetrofitService
                .getRetrofitInstance(baseUrl, "NightscoutFollowV3", true)
                .create(NightscoutFollowV3.Nightscout.class);

        // :: Act
        Response<NightscoutV3Response<List<Entry>>> response =
                api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        // :: Verify response
        assertThat(response.isSuccessful()).isTrue();
        List<Entry> entries = response.body().result;
        assertThat(entries).hasSize(2);

        Entry first = entries.get(0);
        assertThat(first.sgv).isEqualTo(181);
        assertThat(first.date).isWithin(0.1).of(1773944260838.0);
        assertThat(first.noise).isEqualTo(1);
        assertThat(first.identifier).isEqualTo("805201b8");

        // :: Verify request shape
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/entries");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-jwt");
        assertThat(request.getPath()).doesNotContain("token=");
        assertThat(request.getPath()).contains("sort$desc=date");
        assertThat(request.getPath()).contains("type=sgv");
        assertThat(request.getPath()).contains("date$gt=1000000");
        assertThat(request.getPath()).contains("limit=288");
        assertThat(request.getHeader("User-Agent")).contains("xDrip");
        assertThat(request.getHeader("api-secret")).isNull();
    }

    // ===== Treatments — full production stack ====================================================

    @Test
    public void retrofitService_createsWorkingV3TreatmentsClient() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(V3_TREATMENTS_JSON)
                .addHeader("Content-Type", "application/json"));

        String baseUrl = server.url("/").toString();
        NightscoutFollowV3.Nightscout api = RetrofitService
                .getRetrofitInstance(baseUrl, "NightscoutFollowV3", true)
                .create(NightscoutFollowV3.Nightscout.class);

        // :: Act
        Response<NightscoutV3Response<List<JsonObject>>> response =
                api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        // :: Verify response
        assertThat(response.isSuccessful()).isTrue();
        List<JsonObject> result = response.body().result;
        assertThat(result).hasSize(1);

        // Verify re-serialization round-trip for processTreatmentResponse
        String jsonArray = new Gson().toJson(result);
        JSONArray parsed = new JSONArray(jsonArray);
        assertThat(parsed.length()).isEqualTo(1);
        assertThat(parsed.getJSONObject(0).getString("eventType")).isEqualTo("Correction Bolus");
        assertThat(parsed.getJSONObject(0).getString("_id")).isEqualTo("507f1f77bcf86cd799439011");

        // :: Verify request shape
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/treatments");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-jwt");
        assertThat(request.getPath()).doesNotContain("token=");
    }

    // ===== URL userinfo — Retrofit/OkHttp compatibility ==========================================

    @Test
    public void retrofitService_acceptsBaseUrlWithUserinfo() throws Exception {
        // :: Setup — embed token as userinfo (https://token@host), same as URL pref convention
        server.enqueue(new MockResponse()
                .setBody(V3_ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));
        String urlWithToken = server.url("/").toString().replace("://", "://mytoken@");

        NightscoutFollowV3.Nightscout api = RetrofitService
                .getRetrofitInstance(urlWithToken, "NightscoutFollowV3", false)
                .create(NightscoutFollowV3.Nightscout.class);

        // :: Act — must not throw; OkHttp strips userinfo from actual request
        Response<NightscoutV3Response<List<Entry>>> response =
                api.getEntries("Bearer test-jwt", 0L, 10).execute();

        // :: Verify — request reached the mock server with no token leakage
        assertThat(response.isSuccessful()).isTrue();
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/entries");
        assertThat(request.getPath()).doesNotContain("mytoken");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-jwt");
    }
}
