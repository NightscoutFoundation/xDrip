package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.NightscoutV3Response;

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

/**
 * Tests for the {@link NightscoutFollowV3.Nightscout#getDeviceStatus} Retrofit method.
 * <p>
 * Verifies request shape (path, auth header, query params) and POJO deserialization
 * for the Nightscout /api/v3/devicestatus endpoint.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3DeviceStatusTest extends RobolectricTestWithConfig {

    private static final String DEVICE_STATUS_JSON =
            "{\"status\":200,\"result\":["
                    + "{\"date\":1774539584869,"
                    + "\"uploaderBattery\":26,"
                    + "\"isCharging\":true,"
                    + "\"pump\":{\"reservoir\":11.0,"
                    + "\"extended\":{\"BaseBasalRate\":0.5}}}"
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

    // ===== Request shape =========================================================================

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
    public void getDeviceStatus_sendsSortDescDate() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("sort$desc=date");
    }

    @Test
    public void getDeviceStatus_sendsLimitOne() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("limit=1");
    }

    @Test
    public void getDeviceStatus_sendsFieldsProjection() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));
        api.getDeviceStatus("Bearer test-jwt").execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("fields=");
        assertThat(request.getPath()).contains("uploaderBattery");
        assertThat(request.getPath()).contains("pump");
    }

    // ===== Response parsing ======================================================================

    @Test
    public void getDeviceStatus_parsesAllTopLevelFields() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        assertThat(response.isSuccessful()).isTrue();
        DeviceStatus ds = response.body().result.get(0);
        assertThat(ds.uploaderBattery).isEqualTo(26);
        assertThat(ds.isCharging).isTrue();
        assertThat(ds.date).isEqualTo(1774539584869L);
    }

    @Test
    public void getDeviceStatus_parsesPumpAndExtendedFields() throws Exception {
        server.enqueue(new MockResponse().setBody(DEVICE_STATUS_JSON).addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        DeviceStatus.Pump pump = response.body().result.get(0).pump;
        assertThat(pump).isNotNull();
        assertThat(pump.reservoir).isWithin(0.001).of(11.0);
        assertThat(pump.extended).isNotNull();
        assertThat(pump.extended.BaseBasalRate).isWithin(0.001).of(0.5);
    }

    @Test
    public void getDeviceStatus_handlesNullPumpWithoutNpe() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[{\"date\":123,\"uploaderBattery\":80}]}")
                .addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        DeviceStatus ds = response.body().result.get(0);
        assertThat(ds.uploaderBattery).isEqualTo(80);
        assertThat(ds.pump).isNull();
    }

    @Test
    public void getDeviceStatus_handlesNullExtendedWithReservoirPresent() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"status\":200,\"result\":[{\"date\":123,\"pump\":{\"reservoir\":5.0}}]}")
                .addHeader("Content-Type", "application/json"));

        Response<NightscoutV3Response<List<DeviceStatus>>> response =
                api.getDeviceStatus("Bearer test-jwt").execute();

        DeviceStatus ds = response.body().result.get(0);
        assertThat(ds.pump.reservoir).isWithin(0.001).of(5.0);
        assertThat(ds.pump.extended).isNull();
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
