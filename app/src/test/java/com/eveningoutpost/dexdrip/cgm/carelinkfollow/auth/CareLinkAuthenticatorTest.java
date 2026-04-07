package com.eveningoutpost.dexdrip.cgm.carelinkfollow.auth;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Method;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class CareLinkAuthenticatorTest extends RobolectricTestWithConfig {

    @Test
    public void getHttpClient_returnsClientWithAtLeastDefaultTimeouts() throws Exception {
        // :: Setup
        CareLinkCredentialStore credStore = CareLinkCredentialStore.getInstance();
        CareLinkAuthenticator auth = new CareLinkAuthenticator("us", credStore);
        Method getHttpClient = CareLinkAuthenticator.class.getDeclaredMethod("getHttpClient");
        getHttpClient.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpClient.invoke(auth);

        // :: Verify
        assertThat(client).isNotNull();
        assertThat(client.connectTimeoutMillis()).isAtLeast(10000);
        assertThat(client.readTimeoutMillis()).isAtLeast(10000);
        assertThat(client.writeTimeoutMillis()).isAtLeast(10000);
    }
}
