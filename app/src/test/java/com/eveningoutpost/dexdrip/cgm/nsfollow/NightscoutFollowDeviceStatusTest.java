package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.DeviceStatus;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Verifies that {@link NightscoutFollow.Nightscout#getDeviceStatus} sends the correct
 * HTTP request and correctly deserialises the response.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowDeviceStatusTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private NightscoutFollow.Nightscout api;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        server = new MockWebServer();
        server.start();
        api = RetrofitService.getRetrofitInstance(server.url("/").toString(), "test", false)
                .create(NightscoutFollow.Nightscout.class);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        RetrofitService.remove(server.url("/").toString(), "test", false);
    }

    // ===== Request shape =========================================================================

    @Test
    public void getDeviceStatus_requestsCorrectPath() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[{}]"));

        // :: Act
        api.getDeviceStatus(null).execute();

        // :: Verify
        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req.getPath()).startsWith("/api/v1/devicestatus.json");
    }

    @Test
    public void getDeviceStatus_requestsCountOne() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[{}]"));

        // :: Act
        api.getDeviceStatus(null).execute();

        // :: Verify
        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req.getPath()).contains("count=1");
    }

    @Test
    public void getDeviceStatus_sendsApiSecretHeader_whenProvided() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[{}]"));

        // :: Act
        api.getDeviceStatus("mysecret").execute();

        // :: Verify
        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req.getHeader("api-secret")).isEqualTo("mysecret");
    }

    @Test
    public void getDeviceStatus_omitsApiSecretHeader_whenNull() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[{}]"));

        // :: Act
        api.getDeviceStatus(null).execute();

        // :: Verify
        RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req.getHeader("api-secret")).isNull();
    }

    // ===== Response parsing ======================================================================

    @Test
    public void getDeviceStatus_parsesUploaderBattery() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("[{\"uploaderBattery\":84,\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).uploaderBattery).isEqualTo(84);
    }

    @Test
    public void getDeviceStatus_parsesPumpReservoir() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("[{\"pump\":{\"reservoir\":11.5},\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).pump.reservoir).isWithin(0.001).of(11.5);
    }

    @Test
    public void getDeviceStatus_returnsEmptyList_forEmptyJsonArray() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).isEmpty();
    }

    @Test
    public void getDeviceStatus_handlesNullUploaderBattery() throws Exception {
        // :: Setup — field absent, should deserialise as null (not crash)
        server.enqueue(new MockResponse().setBody("[{\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).uploaderBattery).isNull();
    }

    @Test
    public void getDeviceStatus_parsesNestedUploaderBattery() throws Exception {
        // :: Setup — modern REST upload format
        server.enqueue(new MockResponse()
                .setBody("[{\"uploader\":{\"battery\":72},\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).uploader).isNotNull();
        assertThat(result.get(0).uploader.battery).isEqualTo(72);
    }

    @Test
    public void getDeviceStatus_parsesIsCharging() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("[{\"uploaderBattery\":80,\"isCharging\":true,\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCharging).isTrue();
    }

    @Test
    public void getDeviceStatus_parsesPumpBatteryPercent() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody("[{\"pump\":{\"battery\":{\"percent\":87}},\"date\":1700000000000}]"));

        // :: Act
        List<DeviceStatus> result = api.getDeviceStatus(null).execute().body();

        // :: Verify
        assertThat(result).hasSize(1);
        assertThat(result.get(0).pump).isNotNull();
        assertThat(result.get(0).pump.battery).isNotNull();
        assertThat(result.get(0).pump.battery.percent).isEqualTo(87);
    }
}
