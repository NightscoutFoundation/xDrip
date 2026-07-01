package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
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

    private static void awaitCallbacks() throws InterruptedException {
        Thread.sleep(300);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** Collects all requests the server received during the test. */
    private List<String> drainRequestPaths() throws InterruptedException {
        List<String> paths = new ArrayList<>();
        RecordedRequest r;
        while ((r = server.takeRequest(500, TimeUnit.MILLISECONDS)) != null) {
            paths.add(r.getPath());
        }
        return paths;
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
        awaitCallbacks();

        // :: Verify — entries request contains find[date][$gt]=<lastTs>
        List<String> paths = drainRequestPaths();
        String entriesPath = paths.stream()
                .filter(p -> p.contains("entries"))
                .findFirst()
                .orElse("");
        String decoded = URLDecoder.decode(entriesPath, "UTF-8");
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
        awaitCallbacks();

        // :: Verify — count param is present alongside date filter
        List<String> paths = drainRequestPaths();
        String entriesPath = paths.stream()
                .filter(p -> p.contains("entries"))
                .findFirst()
                .orElse("");
        assertThat(entriesPath).contains("count=");
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
        awaitCallbacks();

        // :: Verify — safety count is exactly 2880 regardless of sample period
        List<String> paths = drainRequestPaths();
        String entriesPath = paths.stream()
                .filter(p -> p.contains("entries"))
                .findFirst()
                .orElse("");
        assertThat(entriesPath).contains("count=2880");
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
        awaitCallbacks();

        // :: Verify — date filter uses ~now-24h, not the 30h-old reading timestamp
        List<String> paths = drainRequestPaths();
        String entriesPath = paths.stream()
                .filter(p -> p.contains("entries"))
                .findFirst()
                .orElse("");
        String decoded = URLDecoder.decode(entriesPath, "UTF-8");
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
        awaitCallbacks();

        // :: Verify — entries request has count but no date filter
        List<String> paths = drainRequestPaths();
        String entriesPath = paths.stream()
                .filter(p -> p.contains("entries"))
                .findFirst()
                .orElse("");
        assertThat(entriesPath).contains("count=");
        String decoded = URLDecoder.decode(entriesPath, "UTF-8");
        assertThat(decoded).doesNotContain("find[date][$gt]");
    }
}
