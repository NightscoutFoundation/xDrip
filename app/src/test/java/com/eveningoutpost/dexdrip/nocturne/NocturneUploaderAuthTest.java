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
 * <p>
 * The same route is what makes the recovery tests at the end of this class possible: they let a
 * run fail on the wire and then assert what the next run puts on it.
 *
 * @author Asbjørn Aarrestad
 */
public class NocturneUploaderAuthTest extends RobolectricTestWithConfig {

    private static final String ACCESS_TOKEN = "access-token-for-this-test";
    private static final String INSTANCE_URL_KEY = "nocturne_instance_url";
    private static final String ACCESS_TOKEN_KEY = "nocturne_access_token";
    private static final String REFRESH_TOKEN_KEY = "nocturne_refresh_token";
    private static final String TOKEN_EXPIRY_KEY = "nocturne_token_expiry";
    private static final String CLIENT_ID_KEY = "nocturne_client_id";
    private static final String TOKEN_PATH = "/api/oauth/token";
    private static final String REFRESHED_TOKEN = "access-token-after-refresh";

    /** Bolus, carb intake, note and device event: the resources a treatment uuid may live in. */
    private static final int DELETE_ENDPOINTS_PER_TREATMENT = 4;

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
        // Tests that need a client id seed it; the rest rely on there being none
        PersistentStore.setString(CLIENT_ID_KEY, "");
        Pref.setBoolean("nocturne_upload_sgv", true);
        disableOptionalStreams();
    }

    @After
    public void clearCredentialsAndStopServer() throws IOException {
        // The store is JVM-wide, so it is cleared before the shutdown that may throw, not after.
        Pref.setString(INSTANCE_URL_KEY, "");
        PersistentStore.setString(ACCESS_TOKEN_KEY, "");
        PersistentStore.setString(REFRESH_TOKEN_KEY, "");
        PersistentStore.setLong(TOKEN_EXPIRY_KEY, 0);
        PersistentStore.setString(CLIENT_ID_KEY, "");
        disableOptionalStreams();
        server.shutdown();
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

    /** Registers the client id a refresh needs; without it the service clears the tokens. */
    private static void seedClientId() {
        PersistentStore.setString(CLIENT_ID_KEY, "client-id-for-this-test");
    }

    /** A refresh grant the server accepts: a new access token, valid for an hour. */
    private static MockResponse refreshedTokenResponse() {
        return tokenResponse("{\"access_token\":\"" + REFRESHED_TOKEN
                + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
    }

    /** A 200 from the token endpoint carrying the given body. */
    private static MockResponse tokenResponse(final String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    /** Drives one treatment deletion; the uuid is tried against every resource that may hold it. */
    private boolean deleteOneTreatment() {
        return new NocturneUploader(xdrip.getAppContext())
                .upload(null, null, null, null, Collections.singletonList("treatment-uuid"));
    }

    /** The server no longer accepts the stored token. */
    private static MockResponse unauthorised() {
        return new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid_token\"}");
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

    // ===== Recovery from a token the server has rejected =========================================

    /**
     * After a run in which the server rejected the token, the next run refreshes it before
     * uploading, and the upload carries the refreshed token.
     * <p>
     * Before the fix the second run went straight to the upload with the same rejected token: the
     * stored expiry was still an hour away, so nothing asked for a refresh, and every six-minute
     * retry 401ed until the stored expiry came within a minute of running out.
     */
    @Test
    public void upload_afterTheServerRejectsTheToken_refreshesItOnTheNextRun() throws Exception {
        // :: Setup
        seedClientId();
        server.enqueue(unauthorised());
        server.enqueue(refreshedTokenResponse());
        server.enqueue(jsonResponse());
        assertThat(uploadOneReading()).isFalse();
        nextRequest(); // the rejected upload

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        final RecordedRequest refresh = nextRequest();
        assertThat(refresh.getPath()).isEqualTo(TOKEN_PATH);
        assertThat(refresh.getBody().readUtf8()).contains("grant_type=refresh_token");
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer " + REFRESHED_TOKEN);
        assertThat(uploaded).isTrue();
    }

    /**
     * When the refresh itself is rejected, the next run sends no upload at all and the stored
     * credentials are gone, so the six-minute retry stops putting a doomed request on the wire and
     * the user is told to reconnect.
     * <p>
     * The clearing and the log line are existing behaviour of the refresh path; this pins that a
     * rejected upload now reaches it.
     */
    @Test
    public void upload_afterTheTokenAndTheRefreshAreRejected_stopsUploading() throws Exception {
        // :: Setup
        seedClientId();
        server.enqueue(unauthorised());
        server.enqueue(new MockResponse().setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"invalid_grant\"}"));
        assertThat(uploadOneReading()).isFalse();
        nextRequest(); // the rejected upload

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        assertThat(nextRequest().getPath()).isEqualTo(TOKEN_PATH);
        assertThat(server.getRequestCount()).isEqualTo(2);
        assertThat(NocturneOAuthService.isConnected()).isFalse();
        assertThat(uploaded).isFalse();
    }

    /**
     * Any other failure leaves the token alone: the next run uploads with the same token and no
     * refresh in between.
     * <p>
     * A proxy in front of the instance answers a blocked request with 403 and a body of its own,
     * and an instance that is merely down answers 5xx. Neither says anything about the token, and
     * a refresh on either would be a round-trip spent on nothing.
     */
    @Test
    public void upload_afterAFailureThatIsNotARejection_keepsTheToken() throws Exception {
        // :: Setup
        seedClientId();
        server.enqueue(new MockResponse().setResponseCode(403).setBody("Forbidden"));
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(jsonResponse());
        assertThat(uploadOneReading()).isFalse();
        assertThat(uploadOneReading()).isFalse();
        nextRequest(); // the 403
        nextRequest(); // the 503

        // :: Act
        uploadOneReading();

        // :: Verify
        final RecordedRequest next = nextRequest();
        assertThat(next.getPath()).isNotEqualTo(TOKEN_PATH);
        assertThat(next.getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
    }

    /**
     * With no refresh token stored, a rejection leaves the token as it is: the next run sends it
     * again rather than stopping until the user reconnects.
     * <p>
     * Without a refresh token there is nothing to refresh with, so forgetting the expiry would only
     * make {@code getValidAccessToken()} stop handing the token out.
     */
    @Test
    public void upload_afterARejectionWithNoRefreshToken_keepsSendingTheToken() throws Exception {
        // :: Setup
        seedClientId();
        PersistentStore.setString(REFRESH_TOKEN_KEY, "");
        server.enqueue(unauthorised());
        server.enqueue(jsonResponse());
        assertThat(uploadOneReading()).isFalse();
        nextRequest(); // the rejected upload

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        final RecordedRequest next = nextRequest();
        assertThat(next.getPath()).isNotEqualTo(TOKEN_PATH);
        assertThat(next.getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(uploaded).isTrue();
    }

    /**
     * With no client id stored, a rejection leaves the credentials alone and the next run sends the
     * same token again.
     * <p>
     * A refresh without a client id clears the access and refresh tokens without the reconnect
     * warning a rejected refresh gives. That still happens when the stored expiry runs out, as
     * before; a rejection does not bring it forward.
     */
    @Test
    public void upload_afterARejectionWithNoClientId_keepsTheCredentials() throws Exception {
        // :: Setup
        server.enqueue(unauthorised());
        server.enqueue(jsonResponse());
        assertThat(uploadOneReading()).isFalse();
        nextRequest(); // the rejected upload

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        final RecordedRequest next = nextRequest();
        assertThat(next.getPath()).isNotEqualTo(TOKEN_PATH);
        assertThat(next.getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(uploaded).isTrue();
        assertThat(NocturneOAuthService.isConnected()).isTrue();
    }

    /**
     * A rejected treatment delete arms the refresh too, not only a rejected upload.
     * <p>
     * Deletes report their failures through their own catch rather than the one every upload
     * stream shares, so the two paths have to be pinned separately.
     */
    @Test
    public void deleteTreatment_afterTheServerRejectsTheToken_refreshesItOnTheNextRun()
            throws Exception {
        // :: Setup
        seedClientId();
        Pref.setBoolean("nocturne_upload_treatments", true);
        for (int i = 0; i < DELETE_ENDPOINTS_PER_TREATMENT; i++) {
            server.enqueue(unauthorised());
        }
        server.enqueue(refreshedTokenResponse());
        for (int i = 0; i < DELETE_ENDPOINTS_PER_TREATMENT; i++) {
            server.enqueue(new MockResponse().setResponseCode(404));
        }
        assertThat(deleteOneTreatment()).isFalse();
        for (int i = 0; i < DELETE_ENDPOINTS_PER_TREATMENT; i++) {
            nextRequest(); // the rejected deletes
        }

        // :: Act
        deleteOneTreatment();

        // :: Verify
        assertThat(nextRequest().getPath()).isEqualTo(TOKEN_PATH);
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer " + REFRESHED_TOKEN);
    }

    // ===== A refresh that returns no access token ================================================

    /**
     * A refresh answered without an access token leaves the stored token in place, and the upload
     * goes out with it rather than with an empty bearer header.
     * <p>
     * {@code OAuthTokenResponse} declares no required fields, so such a body deserialises cleanly.
     * Before the fix it was stored as it came: the missing token removed the stored one, and the
     * upload went out as {@code "Bearer "}.
     */
    @Test
    public void upload_whenTheRefreshReturnsNoAccessToken_usesTheStoredToken() throws Exception {
        // :: Setup
        seedClientId();
        PersistentStore.setLong(TOKEN_EXPIRY_KEY, 0); // due for a refresh before the upload
        server.enqueue(tokenResponse("{\"token_type\":\"Bearer\",\"expires_in\":3600}"));
        server.enqueue(jsonResponse());

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        final RecordedRequest refresh = nextRequest();
        assertThat(refresh.getPath()).isEqualTo(TOKEN_PATH);
        assertThat(refresh.getBody().readUtf8()).contains("grant_type=refresh_token");
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(uploaded).isTrue();
    }

    /** An access token that is present but empty is treated the same as a missing one. */
    @Test
    public void upload_whenTheRefreshReturnsAnEmptyAccessToken_usesTheStoredToken()
            throws Exception {
        // :: Setup
        seedClientId();
        PersistentStore.setLong(TOKEN_EXPIRY_KEY, 0); // due for a refresh before the upload
        server.enqueue(tokenResponse(
                "{\"access_token\":\"\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));
        server.enqueue(jsonResponse());

        // :: Act
        final boolean uploaded = uploadOneReading();

        // :: Verify
        assertThat(nextRequest().getPath()).isEqualTo(TOKEN_PATH);
        assertThat(nextRequest().getHeader("Authorization")).isEqualTo("Bearer " + ACCESS_TOKEN);
        assertThat(uploaded).isTrue();
    }
}
