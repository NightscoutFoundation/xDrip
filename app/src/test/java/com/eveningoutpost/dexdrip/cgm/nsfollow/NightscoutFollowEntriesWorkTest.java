package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests for {@link NightscoutFollow#work} entry-fetching strategy.
 * Verifies that date-based filtering is used when a prior reading exists,
 * and that count-only is used on first run (no local readings).
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowEntriesWorkTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        server = new MockWebServer();
        server.start();
        Pref.setString("nsfollow_url", server.url("/").toString());
        JoH.clearRatelimit("nsfollow-devicestatus");
        JoH.clearRatelimit("nsfollow-treatment-download");
        NightscoutFollow.resetInstance();
        BgReading.deleteALL();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        NightscoutFollow.resetInstance();
        BgReading.deleteALL();
    }

    /**
     * Blocks until a request whose path contains {@code needle} arrives, and returns its path.
     * <p>
     * Unlike {@link com.eveningoutpost.dexdrip.Await}, this throws when nothing arrives: these
     * tests assert on the <em>contents</em> of a request, so if it never came there is nothing
     * to assert against and a named failure is the useful outcome.
     */
    private String awaitRequestPathContaining(String needle) throws InterruptedException {
        RecordedRequest r;
        while ((r = server.takeRequest(2, TimeUnit.SECONDS)) != null) {
            if (r.getPath() != null && r.getPath().contains(needle)) {
                return r.getPath();
            }
        }
        throw new AssertionError("No request arrived with a path containing: " + needle);
    }

    private BgReading insertReading(long timestamp) {
        BgReading bg = new BgReading();
        bg.calculated_value = 120.0;
        bg.raw_data = 120.0;
        bg.timestamp = timestamp;
        bg.save();
        return bg;
    }

    // ===== Date filter when prior reading exists =============================================

    @Test
    public void work_usesDateFilter_whenLastReadingExists() throws Exception {
        // :: Setup
        final long lastTs = JoH.tsl() - 5 * Constants.MINUTE_IN_MS;
        insertReading(lastTs);
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);

        // :: Verify — entries request contains find[date][$gt]=<lastTs>
        String decoded = URLDecoder.decode(awaitRequestPathContaining("entries"), "UTF-8");
        assertThat(decoded).contains("find[date][$gt]=" + lastTs);
    }

    @Test
    public void work_includesSafetyCountWithDateFilter() throws Exception {
        // :: Setup
        insertReading(JoH.tsl() - 5 * Constants.MINUTE_IN_MS);
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);

        // :: Verify — count param is present alongside date filter
        assertThat(awaitRequestPathContaining("entries")).contains("count=");
    }

    // ===== Safety limit is fixed 2880 (24h at 1-min × 2) ====================================

    @Test
    public void work_safetyLimitIs2880_whenLastReadingExists() throws Exception {
        // :: Setup
        insertReading(JoH.tsl() - 5 * Constants.MINUTE_IN_MS);
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);

        // :: Verify — safety count is exactly 2880 regardless of sample period
        assertThat(awaitRequestPathContaining("entries")).contains("count=2880");
    }

    // ===== 24-hour time cap on date filter ===================================================

    @Test
    public void work_dateFilterCapsAt24Hours_whenLastReadingIsOlderThan24Hours() throws Exception {
        // :: Setup — last reading 30 hours old
        final long thirtyHoursAgo = JoH.tsl() - 30 * Constants.HOUR_IN_MS;
        final long expectedCutoffFloor = JoH.tsl() - Constants.DAY_IN_MS - 2000L;
        insertReading(thirtyHoursAgo);
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);

        // :: Verify — date filter uses ~now-24h, not the 30h-old reading timestamp
        String decoded = URLDecoder.decode(awaitRequestPathContaining("entries"), "UTF-8");
        String afterGt = decoded.substring(decoded.indexOf("find[date][$gt]=") + "find[date][$gt]=".length());
        long actualCutoff = Long.parseLong(afterGt.split("&")[0]);
        assertThat(actualCutoff).isGreaterThan(thirtyHoursAgo);
        assertThat(actualCutoff).isAtLeast(expectedCutoffFloor);
    }

    // ===== Count-only on first run (no prior readings) =======================================

    @Test
    public void work_usesCountOnly_whenNoLastReading() throws Exception {
        // :: Setup — DB is empty (no BgReadings)
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);

        // :: Verify — entries request has count but no date filter
        String entriesPath = awaitRequestPathContaining("entries");
        assertThat(entriesPath).contains("count=");
        String decoded = URLDecoder.decode(entriesPath, "UTF-8");
        assertThat(decoded).doesNotContain("find[date][$gt]");
    }
}
