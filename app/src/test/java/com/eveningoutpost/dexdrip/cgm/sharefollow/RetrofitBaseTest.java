package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

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
        // Clear the static instances cache
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
    public void getRetrofitInstance_createsClientWithInterceptors() throws Exception {
        // :: Setup
        String url = server.url("/").toString();

        // :: Act
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, true);

        // :: Verify
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) callFactoryField.get(retrofit);
        // Should have: InfoInterceptor + GzipRequestInterceptor (at minimum)
        assertThat(client.interceptors().size()).isAtLeast(2);
    }

    @Test
    public void getRetrofitInstance_sendsRequest() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("ok"));
        String url = server.url("/").toString();
        Retrofit retrofit = RetrofitBase.getRetrofitInstance("TEST", url, false);

        // :: Act
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) callFactoryField.get(retrofit);
        client.newCall(new okhttp3.Request.Builder().url(server.url("/check")).build()).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getPath()).isEqualTo("/check");
    }
}
