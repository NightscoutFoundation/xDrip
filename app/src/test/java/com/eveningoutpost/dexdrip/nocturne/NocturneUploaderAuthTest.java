package com.eveningoutpost.dexdrip.nocturne;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * How {@link NocturneUploader} authenticates against a Nocturne instance.
 * <p>
 * These tests go through the real {@code (Context)} constructor rather than the package-private
 * test seam, because the seam leaves {@code apiClient} null and never executes the authentication
 * wiring. That is exactly how the constructor came to throw on every authenticated upload without
 * any test noticing (issue #4700): {@code ApiClient.setAccessToken} is an unconditional throw in
 * nocturne-java 0.2.4, so the token has to be sent as a default header instead.
 *
 * @author Asbjørn Aarrestad
 */
public class NocturneUploaderAuthTest extends RobolectricTestWithConfig {

    private static final String ACCESS_TOKEN = "access-token-for-this-test";
    private static final String INSTANCE_URL_KEY = "nocturne_instance_url";
    private static final String ACCESS_TOKEN_KEY = "nocturne_access_token";
    private static final String REFRESH_TOKEN_KEY = "nocturne_refresh_token";
    private static final String TOKEN_EXPIRY_KEY = "nocturne_token_expiry";

    /** Every opt-in upload stream. Each one is an extra request if it is left switched on. */
    private static final String[] OPTIONAL_STREAM_KEYS = {
            "nocturne_upload_calibrations",
            "nocturne_upload_bloodtests",
            "nocturne_upload_treatments",
            "nocturne_upload_heartrate",
            "nocturne_upload_stepcount",
            "nocturne_upload_devicestatus",
            "nocturne_upload_motion",
    };

    private MockWebServer server;

    @Before
    public void startServerAndSeedCredentials() throws IOException {
        server = new MockWebServer();
        server.start();
        // Loopback keeps getBaseUrl() from rewriting http:// to https://
        Pref.setString(INSTANCE_URL_KEY, "http://127.0.0.1:" + server.getPort());
        seedAccessToken(ACCESS_TOKEN);
        disableOptionalStreams();
    }

    @After
    public void stopServerAndClearCredentials() throws IOException {
        server.shutdown();
        Pref.setString(INSTANCE_URL_KEY, "");
        PersistentStore.setString(ACCESS_TOKEN_KEY, "");
        PersistentStore.setString(REFRESH_TOKEN_KEY, "");
        PersistentStore.setLong(TOKEN_EXPIRY_KEY, 0);
        disableOptionalStreams();
    }

    /**
     * Forces every opt-in stream off, rather than trusting the default.
     * <p>
     * {@code Pref} caches its {@code SharedPreferences} for the life of the JVM and
     * {@code NocturneUploaderTest} switches four of these on without ever switching them back, so
     * a sibling class earlier in the run can leave them true. That matters here and nowhere else:
     * {@code uploadHeartRates}, {@code uploadStepCounts} and {@code uploadMotionTracking} read the
     * database directly and {@code uploadDeviceStatus} has no data gate at all, so a leaked flag
     * puts an extra request on the wire and every {@code takeRequest()} below then reads the wrong
     * one. The queue-driven streams are harmless by comparison — they null-check their list first.
     */
    private void disableOptionalStreams() {
        for (final String key : OPTIONAL_STREAM_KEYS) {
            Pref.setBoolean(key, false);
        }
    }

    /** Stores a token that {@code getValidAccessToken()} will hand back without refreshing. */
    private void seedAccessToken(final String token) {
        PersistentStore.setString(ACCESS_TOKEN_KEY, token);
        PersistentStore.setString(REFRESH_TOKEN_KEY, "refresh-token");
        PersistentStore.setLong(TOKEN_EXPIRY_KEY, JoH.tsl() + Constants.HOUR_IN_MS);
    }

    /** A reading with enough set for {@code mapBgReading} to build a request. */
    private static BgReading aReading() {
        final BgReading reading = new BgReading();
        reading.timestamp = JoH.tsl();
        reading.calculated_value = 120.5;
        reading.calculated_value_slope = 0.0005;
        reading.raw_data = 100;
        reading.age_adjusted_raw_value = 100;
        reading.filtered_data = 98;
        reading.noise = "3";
        return reading;
    }

    /** Drives one SGV upload; every other stream is off. */
    private boolean uploadOneReading() {
        return new NocturneUploader(xdrip.getAppContext())
                .upload(Collections.singletonList(aReading()), null, null, null, null);
    }

    /**
     * An empty, well-formed success response.
     * <p>
     * {@code sensorGlucoseCreateSensorGlucoseBulk} returns {@code List<SensorGlucose>}, so the body
     * has to be a JSON array. An object body makes gson throw, {@code uploadSgv} catch it and
     * report failure, and the test fail for a reason that has nothing to do with the header.
     */
    private static MockResponse jsonResponse() {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]");
    }

    /**
     * The next request to reach the server, or a failure if none arrives.
     * <p>
     * The no-argument {@code takeRequest()} blocks for ever, which turns any miscount of requests
     * into a hung build instead of a legible failure.
     */
    private RecordedRequest nextRequest() throws InterruptedException {
        final RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        return request;
    }

    // ===== Bearer token on the wire ==============================================================

    /**
     * The stored access token reaches the server as an {@code Authorization: Bearer} header.
     * <p>
     * This is the regression guard for #4700. Before the fix the constructor threw before any
     * request was built, so the throw came out of the act step and no request ever arrived.
     */
    @Test
    public void upload_sendsTheAccessTokenAsABearerHeader() throws Exception {
        // :: Setup
        server.enqueue(jsonResponse());

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(uploaded).isTrue();
    }

    /**
     * The {@code Origin} header the Cloudflare-fronted instances need is still sent alongside it.
     * <p>
     * Both headers go on the same builder, so a change to one is the likely way to lose the other.
     */
    @Test
    public void upload_stillSendsTheOriginHeader() throws Exception {
        // :: Setup
        server.enqueue(jsonResponse());

        // :: Act
        uploadOneReading();

        // :: Verify
        assertThat(nextRequest().getHeader("Origin"))
                .isEqualTo("http://127.0.0.1:" + server.getPort());
    }

    // ===== Token currency across runs ============================================================

    /**
     * A second upload run sends the token that is stored at that moment, not the earlier one.
     * <p>
     * The header is baked in at construction, so it is only ever as fresh as the object. This pins
     * the half of that which is testable from outside: each new uploader re-reads the store. The
     * other half — that {@code UploaderTask} really does build a new one per run — is a property of
     * the caller and is not observable here, so it is not what this test claims.
     */
    @Test
    public void upload_afterATokenChange_sendsTheNewToken() throws Exception {
        // :: Setup
        server.enqueue(jsonResponse());
        server.enqueue(jsonResponse());
        uploadOneReading();
        nextRequest();

        // :: Act
        seedAccessToken("a-refreshed-token");
        uploadOneReading();

        // :: Verify
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer a-refreshed-token");
    }
}
