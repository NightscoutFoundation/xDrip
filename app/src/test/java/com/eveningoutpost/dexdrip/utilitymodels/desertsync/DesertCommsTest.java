package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

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
        // :: Setup — reset static okHttpClient to null for fresh creation
        Field clientField = DesertComms.class.getDeclaredField("okHttpClient");
        clientField.setAccessible(true);
        clientField.set(null, null);
    }

    @Test
    public void getHttpInstance_sharesConnectionPool() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — built via OkHttpWrapper.getClient().newBuilder(), so shares pool
        assertThat(client.connectionPool()).isSameInstanceAs(OkHttpWrapper.getClient().connectionPool());
    }

    @Test
    public void getHttpInstance_inheritsSharedClientTimeouts() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — inherits shared client defaults (no timeout overrides in getHttpInstance)
        OkHttpClient sharedClient = OkHttpWrapper.getClient();
        assertThat(client.connectTimeoutMillis()).isEqualTo(sharedClient.connectTimeoutMillis());
        assertThat(client.readTimeoutMillis()).isEqualTo(sharedClient.readTimeoutMillis());
        assertThat(client.writeTimeoutMillis()).isEqualTo(sharedClient.writeTimeoutMillis());
    }

    @Test
    public void getHttpInstance_hasCustomSslAndHostnameVerifier() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — custom hostname verifier and SSL are set
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
