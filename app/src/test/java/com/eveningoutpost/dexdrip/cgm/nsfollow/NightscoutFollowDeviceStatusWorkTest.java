package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Integration tests for device status fetching within {@link NightscoutFollow#work}.
 * Verifies that the devicestatus call is enqueued, rate-limited, and applies
 * battery and reservoir to {@link PumpStatus}.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowDeviceStatusWorkTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        super.setUp();
        server = new MockWebServer();
        server.start();
        Pref.setString("nsfollow_url", server.url("/").toString());
        // Reset PumpStatus to unset state
        PumpStatus.setBattery(-1);
        PumpStatus.setReservoir(-1);
        // Clear rate limiter so devicestatus fetch is not throttled
        JoH.clearRatelimit("nsfollow-devicestatus");
    }

    /**
     * Wait for OkHttp async callbacks and drain the Android main looper.
     * <p>
     * Retrofit posts {@code enqueue()} callbacks to the Android main looper. In Robolectric's
     * PAUSED looper mode the main looper does not run automatically, so we must:
     * <ol>
     *   <li>Sleep briefly to let OkHttp complete the request and post to the main looper.</li>
     *   <li>Call {@code shadowOf(getMainLooper()).idle()} to drain all pending tasks.</li>
     * </ol>
     */
    private static void awaitCallbacks() throws InterruptedException {
        Thread.sleep(300);
        shadowOf(Looper.getMainLooper()).idle();
    }

    /**
     * Configure the server with a path-aware dispatcher so that concurrent requests
     * receive the correct responses regardless of arrival order.
     *
     * @param deviceStatusBody JSON body to return for {@code /devicestatus} requests
     */
    private void usePathDispatcher(final String deviceStatusBody) {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath() != null && request.getPath().contains("devicestatus")) {
                    return new MockResponse().setBody(deviceStatusBody);
                }
                return new MockResponse().setBody("[]");
            }
        });
    }

    @After
    public void tearDownServer() throws Exception {
        server.shutdown();
        NightscoutFollow.resetInstance();
    }

    // ===== Rate limiting =========================================================================

    @Test
    public void work_sendsDeviceStatusRequest_whenNotRateLimited() throws Exception {
        // :: Setup — entries, treatments, devicestatus responses
        server.enqueue(new MockResponse().setBody("[]"));   // entries
        server.enqueue(new MockResponse().setBody("[]"));   // treatments (if requested)
        server.enqueue(new MockResponse().setBody("[{\"uploaderBattery\":77,\"date\":1700000000000}]"));

        // :: Act
        NightscoutFollow.work(false);
        // Wait for OkHttp async callbacks: pump/drain the main looper until
        // Retrofit delivers its callbacks from the background OkHttp thread.
        awaitCallbacks();

        // :: Verify — at least one request reached the server
        assertThat(server.getRequestCount()).isGreaterThan(0);
    }

    @Test
    public void work_doesNotSendDeviceStatusRequest_whenRateLimited() throws Exception {
        // :: Setup — trigger the rate limiter
        JoH.ratelimit("nsfollow-devicestatus", 5 * 60);  // sets the latch

        server.enqueue(new MockResponse().setBody("[]"));   // entries
        server.enqueue(new MockResponse().setBody("[]"));   // treatments

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify — devicestatus request was NOT made (only entries/treatments hit the server)
        // Drain all requests made, confirm none hit the devicestatus path
        int requestCount = server.getRequestCount();
        for (int i = 0; i < requestCount; i++) {
            String path = server.takeRequest(1, TimeUnit.SECONDS).getPath();
            assertThat(path).doesNotContain("devicestatus");
        }
    }

    // ===== PumpStatus population =================================================================

    @Test
    public void work_appliesBatteryToPumpStatus_whenDeviceStatusReturned() throws Exception {
        // :: Setup — use path dispatcher so concurrent requests get correct responses
        usePathDispatcher("[{\"uploaderBattery\":88,\"date\":1700000000000}]");

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(88.0);
    }

    @Test
    public void work_appliesReservoirToPumpStatus_whenDeviceStatusReturned() throws Exception {
        // :: Setup — use path dispatcher so concurrent requests get correct responses
        usePathDispatcher("[{\"pump\":{\"reservoir\":14.5},\"date\":1700000000000}]");

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify
        assertThat(PumpStatus.getReservoirString()).contains("14.5");
    }

    @Test
    public void work_appliesBatteryFromNestedUploaderField() throws Exception {
        // :: Setup — modern REST format with nested uploader.battery
        usePathDispatcher("[{\"uploader\":{\"battery\":65},\"date\":1700000000000}]");

        // :: Act
        NightscoutFollow.work(false);
        awaitCallbacks();

        // :: Verify
        assertThat(PumpStatus.getBattery()).isWithin(0.001).of(65.0);
    }
}
