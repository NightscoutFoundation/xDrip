package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Executable documentation of the Juggluco Nightscout-emulator contract, driven end-to-end through
 * {@link NightscoutFollow#work}. Mirrors watchserver.cpp behaviour (verified 2026-07-02):
 * <ul>
 *   <li>entries with find[date][$gt] and no new data → HTTP 200 body {@code {}} (givenothing()).</li>
 *   <li>entries with data → HTTP 200 JSON array.</li>
 *   <li>devicestatus.json → HTTP 400 Bad Request text/plain (wrongpath(), route commented out).</li>
 * </ul>
 * The follower must import readings when present, treat {@code {}} as an empty poll (no error), and
 * stop polling devicestatus after the 400.
 *
 * @author Asbjørn Aarrestad
 */
public class JugglucoEmulatorContractTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private String url;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        server = new MockWebServer();
        server.start();
        url = server.url("/").toString();
        Pref.setString("nsfollow_url", url);
        JoH.clearRatelimit("nsfollow-devicestatus");
        JoH.clearRatelimit("nsfollow-treatment-download");
        NsServerCapabilities.reset();
        NightscoutFollow.resetInstance();
        BgReading.deleteALL();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        NsServerCapabilities.reset();
        NightscoutFollow.resetInstance();
        BgReading.deleteALL();
    }

    private static void awaitCallbacks() throws InterruptedException {
        Thread.sleep(400);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** A Juggluco-shaped dispatcher: {@code entriesBody} for entries, 400 for devicestatus. */
    private void useJugglucoDispatcher(final String entriesBody) {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath() == null ? "" : request.getPath();
                if (path.contains("devicestatus")) {
                    return new MockResponse().setResponseCode(400)
                            .addHeader("Content-Type", "text/plain")
                            .setBody("400 Bad Request: " + path);
                }
                if (path.contains("entries")) {
                    return new MockResponse().setResponseCode(200)
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody(entriesBody);
                }
                return new MockResponse().setBody("[]"); // treatments etc.
            }
        });
    }

    // ===== Empty poll: {} tolerated, no data, no crash ===========================================

    @Test
    public void emptyEntriesObject_isToleratedAndDeviceStatusDisabled() throws Exception {
        // :: Setup — prior reading forces the find[date][$gt] path; Juggluco answers {} (no new data)
        final BgReading prior = new BgReading();
        prior.calculated_value = 120.0;
        prior.raw_data = 120.0;
        prior.timestamp = JoH.tsl() - 60_000L;
        prior.save();
        useJugglucoDispatcher("{}\n");

        // :: Act
        NightscoutFollow.work(true);
        awaitCallbacks();

        // :: Verify — the follower reached the server (no parse crash), and devicestatus is disabled
        assertThat(server.getRequestCount()).isGreaterThan(0);
        assertThat(NsServerCapabilities.supportsDeviceStatus(url)).isFalse();
    }

    // ===== Entries with data are imported ========================================================

    @Test
    public void entriesArray_isImported() throws Exception {
        // :: Setup — fresh DB → 24h date-filtered query; Juggluco returns a real array
        final long ts = JoH.tsl() - 120_000L;
        useJugglucoDispatcher("[{\"sgv\":142,\"date\":" + ts + ",\"direction\":\"Flat\",\"type\":\"sgv\"}]");

        // :: Act
        NightscoutFollow.work(true);
        awaitCallbacks();

        // :: Verify — the reading landed in the DB
        assertThat(BgReading.getForPreciseTimestamp(ts, 10_000)).isNotNull();
    }
}
