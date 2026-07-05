package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Tests that {@link NightscoutFollow#work} tolerates a server that rejects the {@code devicestatus}
 * endpoint (Juggluco returns HTTP 400 via wrongpath()): it records the limitation and stops
 * requesting it.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowDeviceStatusFallbackTest extends RobolectricTestWithConfig {

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
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        NsServerCapabilities.reset();
        NightscoutFollow.resetInstance();
    }

    private static void awaitCallbacks() throws InterruptedException {
        Thread.sleep(400);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** entries/treatments respond OK; devicestatus is rejected with 400 like Juggluco's wrongpath(). */
    private void useDeviceStatus400Dispatcher() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                final String path = request.getPath() == null ? "" : request.getPath();
                if (path.contains("devicestatus")) {
                    return new MockResponse().setResponseCode(400)
                            .addHeader("Content-Type", "text/plain").setBody("Bad Request");
                }
                return new MockResponse().setBody("[]");
            }
        });
    }

    // ===== Sticky skip after 400 =================================================================

    @Test
    public void work_marksDeviceStatusUnsupported_afterHttp400() throws Exception {
        // :: Setup
        useDeviceStatus400Dispatcher();

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify — limitation recorded
        assertThat(NsServerCapabilities.supportsDeviceStatus(url)).isFalse();
    }

    @Test
    public void work_skipsDeviceStatusRequest_whenAlreadyUnsupported() throws Exception {
        // :: Setup — limitation already known
        NsServerCapabilities.markDeviceStatusUnsupported(url);
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody("[]");
            }
        });

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify — no devicestatus request was made
        final int count = server.getRequestCount();
        for (int i = 0; i < count; i++) {
            final RecordedRequest r = server.takeRequest(1, TimeUnit.SECONDS);
            if (r != null && r.getPath() != null) {
                assertThat(r.getPath()).doesNotContain("devicestatus");
            }
        }
    }
}
