package com.eveningoutpost.dexdrip.sharemodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.eveningoutpost.dexdrip.sharemodels.models.ShareUploadPayload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies DexcomShare Retrofit 2 interface annotations produce correct HTTP requests.
 *
 * @author Asbjørn Aarrestad
 */
public class DexcomShareTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private DexcomShare api;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/").toString())
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(DexcomShare.class);
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getSessionId_postsToCorrectPathWithBody() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"test-session-id\""));
        Map<String, String> body = new HashMap<>();
        body.put("accountName", "user");
        body.put("password", "pass");
        body.put("applicationId", "app-id");

        // :: Act
        String result = api.getSessionId(body).execute().body();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/General/LoginPublisherAccountByName");
        assertThat(request.getMethod()).isEqualTo("POST");
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"accountName\":\"user\"");
        assertThat(requestBody).contains("\"password\":\"pass\"");
        assertThat(result).isEqualTo("test-session-id");
    }

    @Test
    public void checkSessionActive_sendsSessionIdAsQueryParam() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("true"));

        // :: Act
        Boolean result = api.checkSessionActive("my-session").execute().body();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/IsRemoteMonitoringSessionActive?sessionId=my-session");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(result).isTrue();
    }

    @Test
    public void startRemoteMonitoringSession_sendsSessionIdAndSerial() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));

        // :: Act
        api.StartRemoteMonitoringSession("session-123", "SN456").execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/StartRemoteMonitoringSession?sessionId=session-123&serialNumber=SN456");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    public void deleteContact_sendsSessionIdAndContactId() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));

        // :: Act
        api.deleteContact("session-123", "contact-456").execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/DeleteContact?sessionId=session-123&contactId=contact-456");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Length")).isEqualTo("0");
    }

    @Test
    public void uploadBGRecords_sendsSessionIdAndSerializedPayload() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));
        ShareUploadableBg bg = new ShareUploadableBg() {
            @Override public int getMgdlValue() { return 120; }
            @Override public long getEpochTimestamp() { return 0L; }
            @Override public int getSlopeOrdinal() { return 0; }
        };
        ShareUploadPayload payload = new ShareUploadPayload("SN-123", bg);

        // :: Act
        api.uploadBGRecords("session-abc", payload).execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/PostReceiverEgvRecords?sessionId=session-abc");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8()).contains("SN-123");
    }

    @Test
    public void getContacts_sendsSessionIdAsQueryParam_withNoBody() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("[]"));

        // :: Act
        api.getContacts("session-abc").execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify — bodyless POST: exercises the null-body guard in the network interceptor
        assertThat(request.getPath()).isEqualTo("/Publisher/ListPublisherAccountSubscriptions?sessionId=session-abc");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEmpty();
    }

    @Test
    public void checkMonitorAssignment_sendsSessionIdAndSerial() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"AssignedToYou\""));

        // :: Act
        String result = api.checkMonitorAssignment("session-abc", "SN-123").execute().body();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/CheckMonitoredReceiverAssignmentStatus?sessionId=session-abc&serialNumber=SN-123");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(result).isEqualTo("AssignedToYou");
    }

    @Test
    public void updateMonitorAssignment_sendsSessionIdAndSerial() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setResponseCode(200));

        // :: Act
        api.updateMonitorAssignment("session-abc", "SN-123").execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/Publisher/ReplacePublisherAccountMonitoredReceiver?sessionId=session-abc&serialNumber=SN-123");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    public void authenticatePublisherAccount_sendsSessionIdSerialAndBody() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"auth-result\""));
        Map<String, String> body = new HashMap<>();
        body.put("accountName", "user");

        // :: Act
        api.authenticatePublisherAccount("session-abc", "SN-123", body).execute();
        RecordedRequest request = server.takeRequest();

        // :: Verify
        assertThat(request.getPath()).isEqualTo("/General/AuthenticatePublisherAccount?sessionId=session-abc&serialNumber=SN-123");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).contains("\"accountName\":\"user\"");
    }
}
