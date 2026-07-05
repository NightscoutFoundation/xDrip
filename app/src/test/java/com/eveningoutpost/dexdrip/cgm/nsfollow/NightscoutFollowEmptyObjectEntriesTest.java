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
import retrofit2.Response;

/**
 * Verifies the production Retrofit/Gson stack (built by {@link RetrofitService}) tolerates the
 * Juggluco emulator returning an empty JSON object {@code {}} for an empty entries result set,
 * instead of throwing {@code Expected BEGIN_ARRAY but was BEGIN_OBJECT}. Encodes the Juggluco
 * contract documented in the plan (watchserver.cpp givenothing()).
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowEmptyObjectEntriesTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private String url;
    private NightscoutFollow.Nightscout api;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        url = server.url("/").toString();
        api = RetrofitService.getRetrofitInstance(url, "TEST", false)
                .create(NightscoutFollow.Nightscout.class);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        RetrofitService.remove(url, "TEST", false);
    }

    // ===== Juggluco {} empty result ==============================================================

    @Test
    public void getEntriesSince_emptyObjectBody_parsesAsEmptyList() throws Exception {
        // :: Setup — Juggluco returns {} (HTTP 200) when find[date][$gt] has no new entries
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}\n"));

        // :: Act
        final Response<List<Entry>> response =
                api.getEntriesSince("s", 2880, 1773581680000L, "12345").execute();

        // :: Verify — successful, empty list, no exception
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEmpty();
    }

    // ===== Normal array still parses =============================================================

    @Test
    public void getEntries_arrayBody_stillParses() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("[{\"sgv\":123,\"date\":1773581680168}]"));

        // :: Act
        final Response<List<Entry>> response = api.getEntries("s", 10, "12345").execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).hasSize(1);
        assertThat(response.body().get(0).sgv).isEqualTo(123);
    }
}
