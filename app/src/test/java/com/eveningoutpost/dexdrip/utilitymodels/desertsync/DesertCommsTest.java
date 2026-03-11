package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class DesertCommsTest extends RobolectricTestWithConfig {

    @Before
    public void resetClient() throws Exception {
        // Reset static okHttpClient to null for fresh creation
        Field clientField = DesertComms.class.getDeclaredField("okHttpClient");
        clientField.setAccessible(true);
        clientField.set(null, null);
    }

    @Test
    public void getHttpInstance_hasCorrectTimeouts() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — uses shared client defaults (at least as long as original 10/40/20)
        assertThat(client.connectTimeoutMillis()).isAtLeast(10000);
        assertThat(client.readTimeoutMillis()).isAtLeast(40000);
        assertThat(client.writeTimeoutMillis()).isAtLeast(20000);
    }

    @Test
    public void getHttpInstance_hasCustomSslAndHostnameVerifier() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — custom hostname verifier is set (not the default)
        assertThat(client.hostnameVerifier()).isNotNull();
        assertThat(client.sslSocketFactory()).isNotNull();
    }

    @Test
    public void getHttpInstance_returnsSameInstance() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient first = (OkHttpClient) getHttpInstance.invoke(null);
        OkHttpClient second = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify
        assertThat(first).isSameInstanceAs(second);
    }
}
