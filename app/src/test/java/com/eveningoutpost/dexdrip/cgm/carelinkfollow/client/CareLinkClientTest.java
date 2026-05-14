package com.eveningoutpost.dexdrip.cgm.carelinkfollow.client;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class CareLinkClientTest extends RobolectricTestWithConfig {

    @Test
    public void constructor_createsClientWithCookieJar() throws Exception {
        // :: Setup & Act
        CareLinkClient client = new CareLinkClient("user", "pass", "us");
        Field httpClientField = CareLinkClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        OkHttpClient httpClient = (OkHttpClient) httpClientField.get(client);

        // :: Verify
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.cookieJar()).isNotNull();
        // OkHttp3 defaults or better
        assertThat(httpClient.connectTimeoutMillis()).isAtLeast(10000);
        assertThat(httpClient.readTimeoutMillis()).isAtLeast(10000);
        assertThat(httpClient.writeTimeoutMillis()).isAtLeast(10000);
    }
}
