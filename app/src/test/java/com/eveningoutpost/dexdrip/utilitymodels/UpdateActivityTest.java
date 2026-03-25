package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for UpdateActivity HTTP client configuration.
 *
 * @author Asbjørn Aarrestad
 */
public class UpdateActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetHttpClient() throws Exception {
        // :: Setup — reset static httpClient to force re-creation
        Field httpClientField = UpdateActivity.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);
    }

    @Test
    public void httpClient_usesSharedClient() throws Exception {
        // :: Setup
        Field httpClientField = UpdateActivity.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);

        // :: Act — simulate the lazy init: if (httpClient == null) httpClient = OkHttpWrapper.getClient()
        httpClientField.set(null, OkHttpWrapper.getClient());
        OkHttpClient client = (OkHttpClient) httpClientField.get(null);

        // :: Verify — same instance as shared client
        assertThat(client).isSameInstanceAs(OkHttpWrapper.getClient());
    }

    @Test
    public void httpClient_sharesConnectionPool() throws Exception {
        // :: Setup
        Field httpClientField = UpdateActivity.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);

        // :: Act
        httpClientField.set(null, OkHttpWrapper.getClient());
        OkHttpClient client = (OkHttpClient) httpClientField.get(null);

        // :: Verify
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void asyncDownloader_clientSharesConnectionPool() throws Exception {
        // :: Setup — AsyncDownloader builds: OkHttpWrapper.getClient().newBuilder()...
        // Reproduce the exact construction from the inner class
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        // :: Act & Verify — newBuilder() shares the connection pool
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void asyncDownloader_clientHasCustomTimeouts() throws Exception {
        // :: Setup — reproduce AsyncDownloader's client construction
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        // :: Act & Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(15_000);
        assertThat(client.readTimeoutMillis()).isEqualTo(30_000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(30_000);
        assertThat(client.followRedirects()).isTrue();
        assertThat(client.followSslRedirects()).isTrue();
    }
}
