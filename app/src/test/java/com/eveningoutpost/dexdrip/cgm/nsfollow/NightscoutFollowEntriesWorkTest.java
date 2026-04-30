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
        final long lastTs = 1773581680168L;
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
        insertReading(1773581680168L);
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
