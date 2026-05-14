package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.messages.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Integration tests for the NightscoutFollow.Nightscout Retrofit interface.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowIntegrationTest extends RobolectricTestWithConfig {

    private static final String ENTRIES_JSON =
            "[{\"_id\":\"69b6b587\",\"sgv\":180,\"date\":1773581680168,\"direction\":\"SingleDown\",\"device\":\"G7\",\"type\":\"sgv\",\"filtered\":0,\"unfiltered\":0},"
                    + "{\"_id\":\"69b6b588\",\"sgv\":204,\"date\":1773581380168,\"direction\":\"Flat\",\"device\":\"Unknown\",\"type\":\"sgv\"}]";

    private static final String TREATMENTS_JSON =
            "[{\"_id\":\"69b6b5b3\",\"eventType\":\"Temp Basal\",\"rate\":0,\"duration\":60,\"date\":1773581725647}]";

    private MockWebServer server;
    private NightscoutFollow.Nightscout api;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(NightscoutFollow.Nightscout.class);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void getEntries_sendsApiSecretAndQueryParams() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));

        // :: Act
        Response<List<Entry>> response = api.getEntries("test-secret", 10, "12345").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).contains("/api/v1/entries.json");
        assertThat(request.getPath()).contains("count=10");
        assertThat(request.getPath()).contains("rr=12345");
        assertThat(request.getHeader("api-secret")).isEqualTo("test-secret");
    }

    @Test
    public void getEntries_parsesEntryArray() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(ENTRIES_JSON)
                .addHeader("Content-Type", "application/json"));

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
        assertThat(second.device).isEqualTo("Unknown");
    }

    @Test
    public void getTreatments_getsWithApiSecret() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse()
                .setBody(TREATMENTS_JSON)
                .addHeader("Content-Type", "application/json"));

        // :: Act
        Response<ResponseBody> response = api.getTreatments("treatment-secret").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/v1/treatments");
        assertThat(request.getHeader("api-secret")).isEqualTo("treatment-secret");
        assertThat(response.body().string()).contains("Temp Basal");
    }
}
