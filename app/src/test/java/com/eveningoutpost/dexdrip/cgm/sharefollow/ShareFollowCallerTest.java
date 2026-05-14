package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.google.common.truth.Truth.assertThat;

/**
 * Caller-level integration tests for Share Follow Retrofit 2 interface.
 * <p>
 * Uses MockWebServer to verify that the Retrofit service correctly sends
 * requests and parses responses, including custom GSON deserialization
 * of {@link Dex_Constants.TREND_ARROW_VALUES} via {@link ShareTrendDeserializer}.
 *
 * @author Asbjørn Aarrestad
 */
public class ShareFollowCallerTest extends RobolectricTestWithConfig {

    private static final String TAG = "ShareFollowCallerTest";
    private static final String VALID_UUID = "12345678-1234-1234-1234-123456789abc";
    private static final String VALID_SESSION_ID = "abcdef01-2345-6789-abcd-ef0123456789";
    private static final String ALL_ZEROS_ID = "00000000-0000-0000-0000-000000000000";

    private MockWebServer server;
    private DexcomShareInterface service;

    @Before
    public void setUp() {
        super.setUp();
        server = new MockWebServer();
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }

        String baseUrl = server.url("/ShareWebServices/Services/").toString();
        service = createService(baseUrl);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        clearRetrofitBaseCache();
    }

    @Test
    public void authenticate_returnsValidAccountId() throws Exception {
        // :: Setup
        String responseUuid = "\"" + VALID_UUID + "\"";
        server.enqueue(new MockResponse()
                .setBody(responseUuid)
                .setHeader("Content-Type", "application/json"));

        ShareAuthenticationBody body = new ShareAuthenticationBody("testPass", "testUser");

        // :: Act
        Call<String> call = service.authenticate(body);
        Response<String> response = call.execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo(VALID_UUID);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/ShareWebServices/Services/General/AuthenticatePublisherAccount");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    public void getSessionId_sendsAccountIdInBody() throws Exception {
        // :: Setup
        String responseSessionId = "\"" + VALID_SESSION_ID + "\"";
        server.enqueue(new MockResponse()
                .setBody(responseSessionId)
                .setHeader("Content-Type", "application/json"));

        ShareLoginBody body = new ShareLoginBody("testPass", VALID_UUID);

        // :: Act
        Call<String> call = service.getSessionId(body);
        Response<String> response = call.execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.body()).isEqualTo(VALID_SESSION_ID);

        RecordedRequest request = server.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"accountId\":\"" + VALID_UUID + "\"");
        assertThat(requestBody).contains("\"password\":\"testPass\"");
        assertThat(requestBody).contains("\"applicationId\":\"" + ShareConstants.APPLICATION_ID + "\"");
    }

    @Test
    public void getGlucoseRecords_parsesRecordsWithTrendEnum() throws Exception {
        // :: Setup
        String jsonResponse = "[{"
                + "\"WT\":\"Date(1638396953000)\","
                + "\"ST\":\"Date(1638396953000)\","
                + "\"DT\":\"Date(1638396953000+0000)\","
                + "\"Value\":167.0,"
                + "\"Trend\":4"
                + "}]";
        server.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json"));

        Map<String, String> options = new HashMap<>();
        options.put("sessionId", VALID_SESSION_ID);
        options.put("minutes", "1440");
        options.put("maxCount", "288");

        // :: Act
        Call<List<ShareGlucoseRecord>> call = service.getGlucoseRecords(options);
        Response<List<ShareGlucoseRecord>> response = call.execute();

        // :: Verify
        assertThat(response.isSuccessful()).isTrue();
        List<ShareGlucoseRecord> records = response.body();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);

        ShareGlucoseRecord record = records.get(0);
        assertThat(record.Value).isWithin(0.01).of(167.0);
        assertThat(record.Trend).isEqualTo(Dex_Constants.TREND_ARROW_VALUES.FLAT);
        assertThat(record.getTimestamp()).isEqualTo(1638396953000L);

        RecordedRequest request = server.takeRequest();
        String path = request.getPath();
        assertThat(path).contains("Publisher/ReadPublisherLatestGlucoseValues");
        assertThat(path).contains("sessionId=" + VALID_SESSION_ID);
        assertThat(path).contains("minutes=1440");
        assertThat(path).contains("maxCount=288");
    }

    @Test
    public void session_populateHandlesIdStates() {
        // :: Setup
        Session session = new Session(TAG);

        // :: Act & Verify — valid UUID for extractAccountId
        session.extractAccountId(VALID_UUID);
        assertThat(session.accountId).isEqualTo(VALID_UUID);

        // :: Act & Verify — invalid UUID (too short) for extractAccountId
        session.extractAccountId("short-id");
        assertThat(session.accountId).isEmpty();

        // :: Act & Verify — valid session ID via populate
        session.populate(VALID_SESSION_ID);
        assertThat(session.sessionId).isEqualTo(VALID_SESSION_ID);

        // :: Act & Verify — all-zeros ID rejected via populate
        session.populate(ALL_ZEROS_ID);
        assertThat(session.sessionId).isEmpty();
    }

    @Test
    public void getGlucoseRecords_handlesServerError() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(500));

        Map<String, String> options = new HashMap<>();
        options.put("sessionId", VALID_SESSION_ID);
        options.put("minutes", "1440");
        options.put("maxCount", "288");

        // :: Act
        Call<List<ShareGlucoseRecord>> call = service.getGlucoseRecords(options);
        Response<List<ShareGlucoseRecord>> response = call.execute();

        // :: Verify
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(500);
    }

    /**
     * Creates a DexcomShareInterface service using a custom Retrofit instance.
     * <p>
     * Note: DexcomShareInterface uses {@code retrofit.http.Headers} (Retrofit 1) instead of
     * {@code retrofit2.http.Headers}. Retrofit 2 ignores these annotations, so headers won't
     * be applied, but the POST/Body/QueryMap annotations work correctly.
     */
    private DexcomShareInterface createService(String baseUrl) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        Dex_Constants.TREND_ARROW_VALUES.class,
                        new ShareTrendDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(DexcomShareInterface.class);
    }

    /**
     * Clears the RetrofitBase static cache so tests don't interfere with each other.
     */
    @SuppressWarnings("unchecked")
    private void clearRetrofitBaseCache() throws Exception {
        Field instancesField = RetrofitBase.class.getDeclaredField("instances");
        instancesField.setAccessible(true);
        ((ConcurrentHashMap<String, Retrofit>) instancesField.get(null)).clear();

        Field urlsField = RetrofitBase.class.getDeclaredField("urls");
        urlsField.setAccessible(true);
        ((ConcurrentHashMap<String, String>) urlsField.get(null)).clear();
    }
}
