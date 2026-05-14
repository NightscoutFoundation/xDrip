package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class RetrofitBaseTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        // :: Setup — clear the static instances cache
        Field instancesField = RetrofitBase.class.getDeclaredField("instances");
        instancesField.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) instancesField.get(null)).clear();
        Field urlsField = RetrofitBase.class.getDeclaredField("urls");
        urlsField.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) urlsField.get(null)).clear();
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getRetrofitInstance_clientSharesConnectionPool() throws Exception {
        // :: Setup
        String url = server.url("/").toString();

        // :: Act
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, true);
        OkHttpClient client = extractClient(retrofit);

        // :: Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void getRetrofitInstance_clientHasInfoInterceptor() throws Exception {
        // :: Setup
        String url = server.url("/").toString();

        // :: Act
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, true);
        OkHttpClient client = extractClient(retrofit);

        // :: Verify
        boolean hasInfoInterceptor = client.interceptors().stream()
                .anyMatch(i -> i instanceof InfoInterceptor);
        assertThat(hasInfoInterceptor).isTrue();
    }

    @Test
    public void getRetrofitInstance_clientHasGzipInterceptor() throws Exception {
        // :: Setup
        String url = server.url("/").toString();

        // :: Act
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, true);
        OkHttpClient client = extractClient(retrofit);

        // :: Verify
        boolean hasGzipInterceptor = client.interceptors().stream()
                .anyMatch(i -> i instanceof GzipRequestInterceptor);
        assertThat(hasGzipInterceptor).isTrue();
    }

    @Test
    public void getRetrofitInstance_sendsRequest() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        String url = server.url("/").toString();
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, false);

        // :: Act
        OkHttpClient client = extractClient(retrofit);
        client.newCall(new okhttp3.Request.Builder().url(server.url("/check")).build()).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getPath()).isEqualTo("/check");
    }

    private OkHttpClient extractClient(Retrofit retrofit) throws Exception {
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        return (OkHttpClient) callFactoryField.get(retrofit);
    }
}
