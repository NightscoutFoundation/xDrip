package com.eveningoutpost.dexdrip.sharemodels;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.sharemodels.models.ExistingFollower;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Callback;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies ShareRest network interceptor adds required headers (User-Agent, Content-Type, Accept),
 * and that the account-region preference selects which Dexcom Share host every call is addressed to.
 *
 * @author Asbjørn Aarrestad
 */
public class ShareRestTest extends RobolectricTestWithConfig {

    private MockWebServer server;
    private final BlockingQueue<String> requestedHosts = new ArrayBlockingQueue<>(4);

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        storeSessionId("a-session-that-already-exists"); // skips getSessionId's AsyncTask entirely
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getOkHttpClient_addsRequiredHeaders() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("\"ok\""));

        // Create ShareRest with a real context, passing a dummy client to avoid using the private one
        // Then call the private getOkHttpClient to get the interceptor-equipped client
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        OkHttpClient client = (OkHttpClient) method.invoke(shareRest);

        // :: Act
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/test"))
                .post(RequestBody.create("{}", okhttp3.MediaType.parse("application/json")))
                .build();
        client.newCall(request).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getHeader("User-Agent")).contains("CGM-Store-1.2");
        assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void getOkHttpClient_handlesRequestWithNoBody_withoutException() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        OkHttpClient client = (OkHttpClient) method.invoke(shareRest);

        // :: Act — GET request has a null body, exercises the null-body guard in the interceptor
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/test"))
                .get()
                .build();
        okhttp3.Response response = client.newCall(request).execute();

        // :: Verify — required headers are still injected on bodyless requests
        RecordedRequest recorded = server.takeRequest();
        assertThat(response.code()).isEqualTo(200);
        assertThat(recorded.getHeader("User-Agent")).contains("CGM-Store-1.2");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
    }

    @Test
    public void getOkHttpClient_returnsOkHttp3Client() throws Exception {
        // :: Setup & Act
        ShareRest shareRest = new ShareRest(RuntimeEnvironment.application, new OkHttpClient());
        Method method = ShareRest.class.getDeclaredMethod("getOkHttpClient");
        method.setAccessible(true);
        Object client = method.invoke(shareRest);

        // :: Verify
        assertThat(client).isInstanceOf(OkHttpClient.class);
    }

    // ===== Account region selects the Share host =================================================

    /**
     * A US account addresses share2.dexcom.com. Note that US is also the production default, so
     * this half alone would still pass with the read deleted —
     * {@link #nonUsAccountUsesOutsideUsShareHost()} is the one that pins it.
     */
    @Test
    public void usAccountUsesUsShareHost() throws Exception {
        // :: Setup
        storeUsAccount(true);

        // :: Act
        new ShareRest(xdrip.getAppContext(), recordingClient()).getContacts(ignoringCallback());

        // :: Verify
        assertThat(firstRequestedHost()).isEqualTo("share2.dexcom.com");
    }

    /**
     * A non-US account addresses shareous1.dexcom.com. This is the assertion that pins the
     * preference read: the non-US host is the only one the default does not already produce.
     */
    @Test
    public void nonUsAccountUsesOutsideUsShareHost() throws Exception {
        // :: Setup
        storeUsAccount(false);

        // :: Act
        new ShareRest(xdrip.getAppContext(), recordingClient()).getContacts(ignoringCallback());

        // :: Verify
        assertThat(firstRequestedHost()).isEqualTo("shareous1.dexcom.com");
    }

    // ===== Helpers ===============================================================================

    private OkHttpClient recordingClient() {
        return new OkHttpClient.Builder().addInterceptor(chain -> {
            requestedHosts.offer(chain.request().url().host());
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(MediaType.parse("application/json"), "[]"))
                    .build();
        }).build();
    }

    private String firstRequestedHost() throws InterruptedException {
        String host = requestedHosts.poll(10, TimeUnit.SECONDS);
        assertThat(host).isNotNull(); // no request was ever dispatched
        return host;
    }

    private Callback<List<ExistingFollower>> ignoringCallback() {
        return new Callback<List<ExistingFollower>>() {
            @Override
            public void onResponse(Call<List<ExistingFollower>> call, retrofit2.Response<List<ExistingFollower>> response) {
            }

            @Override
            public void onFailure(Call<List<ExistingFollower>> call, Throwable t) {
            }
        };
    }

    private void storeUsAccount(boolean us) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putBoolean("dex_share_us_acct", us).commit();
    }

    private void storeSessionId(String sessionId) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("dexcom_share_session_id", sessionId).commit();
    }
}
