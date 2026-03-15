package com.eveningoutpost.dexdrip.tidepool;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class TidepoolUploaderTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        // Reset static retrofit to null
        Field retrofitField = TidepoolUploader.class.getDeclaredField("retrofit");
        retrofitField.setAccessible(true);
        retrofitField.set(null, null);
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getRetrofitInstance_createsClientWithInterceptors() throws Exception {
        // :: Setup — use dev server pointing to our mock
        // We can't easily override the URL, but we can verify the client via reset
        // Instead, verify client has interceptors after creation
        Retrofit retrofit = TidepoolUploader.getRetrofitInstance();

        // :: Act
        Field callFactoryField = Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) callFactoryField.get(retrofit);

        // :: Verify — should have HttpLoggingInterceptor + InfoInterceptor
        assertThat(client.interceptors()).hasSize(2);
    }
}
