package com.eveningoutpost.dexdrip.utils.framework;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class RetrofitServiceTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws IOException {
        server = new MockWebServer();
        server.start();
        RetrofitService.clear();
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
        Retrofit retrofit = RetrofitService.getRetrofitInstance(url, "TEST", false);

        // :: Verify
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) callFactoryField.get(retrofit);
        assertThat(client.interceptors()).hasSize(3);
        assertThat(client.interceptors().stream().anyMatch(i -> i instanceof HttpLoggingInterceptor)).isTrue();
        assertThat(client.interceptors().stream().anyMatch(i -> i instanceof InfoInterceptor)).isTrue();
        assertThat(client.interceptors().stream().anyMatch(i -> i instanceof GzipRequestInterceptor)).isTrue();
    }

    @Test
    public void getRetrofitInstance_sendsRequestWithGzip() throws Exception {
        // :: Setup
        server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        String url = server.url("/").toString();
        Retrofit retrofit = RetrofitService.getRetrofitInstance(url, "TEST", false);

        // :: Act — make a real request through the client
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) callFactoryField.get(retrofit);
        client.newCall(new okhttp3.Request.Builder().url(server.url("/test")).build()).execute();
        RecordedRequest recorded = server.takeRequest();

        // :: Verify
        assertThat(recorded.getPath()).isEqualTo("/test");
    }

    @Test
    public void getRetrofitInstance_cachesInstances() throws Exception {
        // :: Setup
        String url = server.url("/").toString();

        // :: Act
        Retrofit first = RetrofitService.getRetrofitInstance(url, "TEST", false);
        Retrofit second = RetrofitService.getRetrofitInstance(url, "TEST", false);

        // :: Verify
        assertThat(first).isSameInstanceAs(second);
    }
}
