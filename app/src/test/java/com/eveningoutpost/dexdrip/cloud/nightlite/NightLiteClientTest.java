package com.eveningoutpost.dexdrip.cloud.nightlite;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class NightLiteClientTest extends RobolectricTestWithConfig {

    @Test
    public void client_hasGzipInterceptor() throws Exception {
        // :: Setup
        Field clientField = NightLiteClient.class.getDeclaredField("client");
        clientField.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) clientField.get(null);

        // :: Verify
        assertThat(client.interceptors()).hasSize(1);
        assertThat(client.interceptors().get(0).getClass().getSimpleName())
                .isEqualTo("GzipRequestInterceptor");
    }

    @Test
    public void client_hasAtLeastSharedClientTimeouts() throws Exception {
        // :: Setup
        Field clientField = NightLiteClient.class.getDeclaredField("client");
        clientField.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) clientField.get(null);

        // :: Verify — uses shared client defaults (at least as long as before)
        assertThat(client.connectTimeoutMillis()).isAtLeast(10000);
        assertThat(client.readTimeoutMillis()).isAtLeast(10000);
        assertThat(client.writeTimeoutMillis()).isAtLeast(10000);
    }
}
