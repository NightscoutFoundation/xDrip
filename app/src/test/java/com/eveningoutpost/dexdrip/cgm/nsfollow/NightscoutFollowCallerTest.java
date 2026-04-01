package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;
import com.eveningoutpost.dexdrip.utils.framework.RetrofitService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
}
