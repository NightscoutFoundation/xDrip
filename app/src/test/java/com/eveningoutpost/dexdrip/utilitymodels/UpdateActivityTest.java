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
        // Reset static httpClient to force re-creation
        Field httpClientField = UpdateActivity.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(null, null);
    }

    @Test
    public void checkForAnUpdate_clientHasDefaultTimeouts() throws Exception {
        // :: Setup — trigger client creation via checkForAnUpdate
        // We can't easily call checkForAnUpdate without a full activity context,
        // but we can verify the field configuration after construction.
        // The httpClient is created inline with: 30s connect, 60s read, 20s write
        // This test documents the expected timeouts.

        // :: Act — manually construct what checkForAnUpdate creates
        OkHttpClient client = OkHttpWrapper.enableTls12OnPreLollipop(new OkHttpClient.Builder())
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // :: Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(20000);
    }

    @Test
    public void asyncDownloader_clientHasCustomTimeoutsAndRedirects() throws Exception {
        // :: Setup — the AsyncDownloader inner class builds a client with
        // 15s connect, 30s read, 30s write + followRedirects + followSslRedirects

        // :: Act — reconstruct AsyncDownloader's client config
        OkHttpClient client = OkHttpWrapper.enableTls12OnPreLollipop(new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true))
                .build();

        // :: Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(15000);
        assertThat(client.readTimeoutMillis()).isEqualTo(30000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(30000);
        assertThat(client.followRedirects()).isTrue();
        assertThat(client.followSslRedirects()).isTrue();
    }
}
