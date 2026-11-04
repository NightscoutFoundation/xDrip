package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

/**
 * Tests for the activity (heart rate, steps) upload of {@link NightscoutUploader}.
 */
public class NightscoutUploaderActivityTest extends RobolectricTestWithConfig {

    private static final String SECRET = "secret";

    private MockWebServer server;
    private NightscoutUploader uploader;
    private NightscoutUploader.NightscoutService service;

    @Before
    public void setUpServer() throws IOException {
        super.setUp();
        server = new MockWebServer();
        server.start();
        uploader = new NightscoutUploader(RuntimeEnvironment.application);
        service = new Retrofit.Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(new OkHttpClient())
                .build()
                .create(NightscoutUploader.NightscoutService.class);
        PersistentStore.setLong("nightscout-rest-heartrate-synced-time", 0);
        PersistentStore.setLong("nightscout-rest-steps-synced-time", 0);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void heartRateUpload_doesNotSendTheLastUploadedRecordAgain() throws Exception {
        final long now = JoH.tsl();
        HeartRate.create(now - 120_000, 70, 1);
        HeartRate.create(now - 60_000, 72, 1);

        post("postHeartRate");
        assertThat(uploadedRecords()).isEqualTo(2);

        post("postHeartRate");
        assertThat(server.getRequestCount()).isEqualTo(1);

        HeartRate.create(now - 30_000, 74, 1);
        post("postHeartRate");
        assertThat(uploadedRecords()).isEqualTo(1);
    }

    @Test
    public void stepsUpload_doesNotSendTheLastUploadedRecordAgain() throws Exception {
        final long now = JoH.tsl();
        StepCounter.createUniqueRecord(now - 120_000, 100, true);
        StepCounter.createUniqueRecord(now - 60_000, 200, true);

        post("postStepsCount");
        assertThat(uploadedRecords()).isEqualTo(2);

        post("postStepsCount");
        assertThat(server.getRequestCount()).isEqualTo(1);

        StepCounter.createUniqueRecord(now - 30_000, 300, true);
        post("postStepsCount");
        assertThat(uploadedRecords()).isEqualTo(1);
    }

    private void post(final String methodName) throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        final Method method = NightscoutUploader.class.getDeclaredMethod(methodName,
                NightscoutUploader.NightscoutService.class, String.class);
        method.setAccessible(true);
        try {
            method.invoke(uploader, service, SECRET);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    // Number of records in the next activity request.
    private int uploadedRecords() throws Exception {
        final RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/activity");
        return new JSONArray(request.getBody().readUtf8()).length();
    }
}
