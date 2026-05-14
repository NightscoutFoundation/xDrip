package com.eveningoutpost.dexdrip.deposit;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class WebDepositTest extends RobolectricTestWithConfig {

    private MockWebServer server;

    @Before
    public void setUpServer() throws Exception {
        server = new MockWebServer();
        server.start();
        // Reset static retrofit field to force re-creation
        Field retrofitField = WebDeposit.class.getDeclaredField("retrofit");
        retrofitField.setAccessible(true);
        retrofitField.set(null, null);
    }

    @After
    public void tearDownServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void getRetrofitInstance_createsClientWithLongReadTimeout() throws Exception {
        // :: Setup
        Pref.setString("web_deposit_url", server.url("/").toString());

        // :: Act
        retrofit2.Retrofit retrofit = WebDeposit.getRetrofitInstance();

        // :: Verify — extract OkHttpClient from Retrofit
        Field callFactoryField = retrofit2.Retrofit.class.getDeclaredField("callFactory");
        callFactoryField.setAccessible(true);
        okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) callFactoryField.get(retrofit);
        assertThat(client.readTimeoutMillis()).isEqualTo(600000);
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
    }
}
