package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

/**
 * Caller-level integration tests for NightscoutFollow.Nightscout via RetrofitService.
 * <p>
 * Exercises the full production stack: RetrofitService.getRetrofitInstance() with its
 * interceptors (HttpLogging, InfoInterceptor, GzipRequestInterceptor) and custom GSON
 * (UNRELIABLE_INTEGER_FACTORY), verifying that the Nightscout client works end-to-end.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowCallerTest extends RobolectricTestWithConfig {

    private static final String ENTRIES_JSON =
            "[{\"_id\":\"69b6b587\",\"sgv\":180,\"date\":1773581680168,\"direction\":\"SingleDown\",\"device\":\"G7\",\"type\":\"sgv\",\"filtered\":0,\"unfiltered\":0},"
                    + "{\"_id\":\"69b6b588\",\"sgv\":204,\"date\":1773581380168,\"direction\":\"Flat\",\"device\":\"Unknown\",\"type\":\"sgv\"}]";

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

    @Test
    public void retrofitService_createsWorkingNightscoutClient() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));

        String baseUrl = server.url("/").toString();
        NightscoutFollow.Nightscout api = RetrofitService
                .getRetrofitInstance(baseUrl, "NightscoutFollow", true)
                .create(NightscoutFollow.Nightscout.class);

        // :: Act
        Response<List<Entry>> response = api.getEntries("test-secret", 10, "12345").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();

        List<Entry> entries = response.body();
        assertThat(entries).hasSize(2);

        Entry first = entries.get(0);
        assertThat(first.sgv).isEqualTo(180);
        assertThat(first.direction).isEqualTo("SingleDown");
        assertThat(first.device).isEqualTo("G7");
        assertThat(first.date).isWithin(0.1).of(1773581680168.0);

        Entry second = entries.get(1);
        assertThat(second.sgv).isEqualTo(204);
        assertThat(second.direction).isEqualTo("Flat");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("api-secret")).isEqualTo("test-secret");
        assertThat(request.getPath()).contains("/api/v1/entries.json");
        assertThat(request.getPath()).contains("count=10");
    }

    @Test
    public void retrofitService_handlesServerErrorGracefully() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500));

        String baseUrl = server.url("/").toString();
        NightscoutFollow.Nightscout api = RetrofitService
                .getRetrofitInstance(baseUrl, "NightscoutFollow", true)
                .create(NightscoutFollow.Nightscout.class);

        // :: Act
        Response<List<Entry>> response = api.getEntries("test-secret", 10, "12345").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(500);
    }

    // ===== Device status ==========================================================================

    @Test
    public void work_requestsDeviceStatusPath() throws Exception {
        // :: Setup — clear rate limiter, configure path-aware dispatcher
        JoH.clearRatelimit("nsfollow-devicestatus");
        Pref.setString("nsfollow_url", server.url("/").toString());
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("devicestatus")) {
                    return new MockResponse().setBody("[{\"uploaderBattery\":55,\"date\":1700000000000}]");
                }
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);
        Thread.sleep(300);
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify — at least one request hit the devicestatus path
        boolean found = false;
        int count = server.getRequestCount();
        for (int i = 0; i < count; i++) {
            String path = server.takeRequest(1, TimeUnit.SECONDS).getPath();
            if (path != null && path.contains("devicestatus")) {
                found = true;
            }
        }
        assertThat(found).isTrue();
        NightscoutFollow.resetInstance();
    }

    @Test
    public void work_deviceStatusAppliesBatteryToPumpStatus() throws Exception {
        // :: Setup
        JoH.clearRatelimit("nsfollow-devicestatus");
        PumpStatus.setBattery(-1);
        Pref.setString("nsfollow_url", server.url("/").toString());
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("devicestatus")) {
                    return new MockResponse().setBody("[{\"uploaderBattery\":62,\"date\":1700000000000}]");
                }
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);
        Thread.sleep(300);
        shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(62.0);
        NightscoutFollow.resetInstance();
    }
}
