package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.NightscoutV3Response;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;

/**
 * Integration tests for the {@link NightscoutFollowV3.Nightscout} Retrofit interface.
 * <p>
 * Verifies that requests use API v3 paths, Bearer auth header, field projection,
 * sort ordering, type filter, and incremental date filtering.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3IntegrationTest extends RobolectricTestWithConfig {

    // Real v3 response shape from k-ns.aarrestad.com (projected fields only)
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
    private NightscoutFollowV3.Nightscout api;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(NightscoutFollowV3.Nightscout.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    // ===== getEntries — request shape ============================================================

    @Test
    public void getEntries_usesV3Path() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/entries");
    }

    @Test
    public void getEntries_sendsBearerAuthHeader() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer my-jwt-token", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer my-jwt-token");
    }

    @Test
    public void getEntries_omitsAuthHeaderWhenNull() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries(null, 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isNull();
    }

    @Test
    public void getEntries_doesNotSendApiSecretHeader() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("api-secret")).isNull();
    }

    @Test
    public void getEntries_doesNotSendTokenQueryParam() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).doesNotContain("token=");
    }

    @Test
    public void getEntries_sendsSortDescDate() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("sort$desc=date");
    }

    @Test
    public void getEntries_sendsTypeFilter() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("type=sgv");
    }

    @Test
    public void getEntries_sendsFieldsProjection() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1000000L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("fields=");
        assertThat(request.getPath()).contains("sgv");
        assertThat(request.getPath()).contains("identifier");
    }

    @Test
    public void getEntries_sendsDateGtWithLiteralDollar() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 1773944260838L, 288).execute();

        // encoded=true preserves literal $
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("date$gt=1773944260838");
    }

    @Test
    public void getEntries_sendsLimitParam() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));
        api.getEntries("Bearer test-jwt", 0L, 288).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("limit=288");
    }

    // ===== getEntries — response parsing =========================================================

    @Test
    public void getEntries_unwrapsResultArray() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<Entry>>> response = api.getEntries("Bearer test-jwt", 0L, 288).execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().result).hasSize(2);
    }

    @Test
    public void getEntries_parsesEntryFields() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_ENTRIES_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<Entry>>> response = api.getEntries("Bearer test-jwt", 0L, 288).execute();

        Entry first = response.body().result.get(0);
        assertThat(first.sgv).isEqualTo(181);
        assertThat(first.date).isWithin(0.1).of(1773944260838.0);
        assertThat(first.noise).isEqualTo(1);
        assertThat(first.identifier).isEqualTo("805201b8");
    }

    @Test
    public void getEntries_handlesEmptyResultArray() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[]}")
                .addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<Entry>>> response = api.getEntries("Bearer test-jwt", 0L, 288).execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().result).isEmpty();
    }

    @Test
    public void getEntries_handlesUnauthorized() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));

        Response<NightscoutV3Response<List<Entry>>> response = api.getEntries(null, 0L, 288).execute();

        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(401);
    }

    // ===== getTreatments =========================================================================

    @Test
    public void getTreatments_usesV3Path() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/treatments");
    }

    @Test
    public void getTreatments_sendsBearerAuthHeader() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer my-jwt-token", 0L, 100).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer my-jwt-token");
    }

    @Test
    public void getTreatments_sendsSortDescDate() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("sort$desc=date");
    }

    @Test
    public void getTreatments_sendsDateGtWithLiteralDollar() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 1773944260838L, 100).execute();

        // encoded=true preserves literal $
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("date$gt=1773944260838");
    }

    @Test
    public void getTreatments_sendsLimitParam() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 0L, 50).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("limit=50");
    }

    @Test
    public void getTreatments_sendsFieldsProjection() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("fields=");
        assertThat(request.getPath()).contains("eventType");
        assertThat(request.getPath()).contains("identifier");
        assertThat(request.getPath()).contains("created_at");
    }

    @Test
    public void getTreatments_bootstrapWithSinceMsZeroFetchesUpToLimit() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));
        api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        // sinceMs=0 means no prior local treatment — NS returns up to limit records
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("date$gt=0");
        assertThat(request.getPath()).contains("limit=100");
    }

    @Test
    public void getTreatments_handlesEmptyResultArray() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[]}")
                .addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<JsonObject>>> response =
                api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().result).isEmpty();
    }

    @Test
    public void getTreatments_unwrapsResultArray() throws Exception {
        server.enqueue(new MockResponse().setBody(V3_TREATMENTS_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<JsonObject>>> response =
                api.getTreatments("Bearer test-jwt", 0L, 100).execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().result).hasSize(1);
        assertThat(response.body().result.get(0).get("eventType").getAsString())
                .isEqualTo("Correction Bolus");
    }

    // ===== getDeviceStatus — request shape =======================================================

    private static final String DEVICE_STATUS_JSON =
            "{\"status\":200,\"result\":["
                    + "{\"date\":1774539584869,\"uploaderBattery\":26,\"isCharging\":true,"
                    + "\"pump\":{\"reservoir\":11.0,\"extended\":{\"BaseBasalRate\":0.5}}}"
                    + "]}";

    @Test
    public void getDeviceStatus_usesV3DeviceStatusPath() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/api/v3/devicestatus");
    }

    @Test
    public void getDeviceStatus_sendsBearerAuthHeader() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer my-jwt-token").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer my-jwt-token");
    }

    @Test
    public void getDeviceStatus_sendsSortDescDateAndLimitOne() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("sort$desc=date");
        assertThat(request.getPath()).contains("limit=1");
    }

    @Test
    public void getDeviceStatus_sendsFieldsProjectionWithRequiredFields() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("fields=");
        assertThat(request.getPath()).contains("uploaderBattery");
        assertThat(request.getPath()).contains("pump");
    }

    // ===== getDeviceStatus — response parsing ====================================================

    @Test
    public void getDeviceStatus_parsesAllFields() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        assertThat(response.isSuccessful()).isTrue();
        DeviceStatus ds = response.body().result.get(0);
        assertThat(ds.uploaderBattery).isEqualTo(26);
        assertThat(ds.isCharging).isTrue();
        assertThat(ds.pump.reservoir).isWithin(0.001).of(11.0);
        assertThat(ds.pump.extended.BaseBasalRate).isWithin(0.001).of(0.5);
    }

    @Test
    public void getDeviceStatus_handlesEmptyResultList() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[]}")
                .addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body().result).isEmpty();
    }
}
