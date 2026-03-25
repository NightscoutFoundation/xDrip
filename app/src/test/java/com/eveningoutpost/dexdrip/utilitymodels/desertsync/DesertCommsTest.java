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
    public void getHttpInstance_hasLanOptimizedTimeouts() throws Exception {
        // :: Setup
        Method getHttpInstance = DesertComms.class.getDeclaredMethod("getHttpInstance");
        getHttpInstance.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) getHttpInstance.invoke(null);

        // :: Verify — short connect timeout (10s) for fast LAN failure detection; 40s read for local sync
        assertThat(client.connectTimeoutMillis()).isEqualTo(10_000);
        assertThat(client.readTimeoutMillis()).isEqualTo(40_000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(20_000);
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
