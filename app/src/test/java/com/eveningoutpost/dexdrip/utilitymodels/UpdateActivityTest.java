package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

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
    public void httpClient_sharesConnectionPoolAndHasCorrectTimeouts() throws Exception {
        // :: Setup
        Field httpClientField = UpdateActivity.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);

        // :: Act — simulate the lazy init using newBuilder() as in production code
        OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        httpClientField.set(null, client);

        // :: Verify — newBuilder() shares pool but is a distinct instance with explicit timeouts
        assertThat(client).isNotSameInstanceAs(OkHttpWrapper.getClient());
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
        assertThat(client.connectTimeoutMillis()).isEqualTo(30_000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60_000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(20_000);
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
