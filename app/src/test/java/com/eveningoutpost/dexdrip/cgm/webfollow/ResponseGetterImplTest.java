package com.eveningoutpost.dexdrip.cgm.webfollow;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class ResponseGetterImplTest extends RobolectricTestWithConfig {

    @Test
    public void client_hasAtLeastSharedClientTimeouts() throws Exception {
        // :: Setup
        ResponseGetterImpl impl = new ResponseGetterImpl();
        Field clientField = ResponseGetterImpl.class.getDeclaredField("client");
        clientField.setAccessible(true);

        // :: Act
        OkHttpClient client = (OkHttpClient) clientField.get(impl);

        // :: Verify — uses shared client defaults (at least as long as before)
        assertThat(client.connectTimeoutMillis()).isAtLeast(10000);
        assertThat(client.readTimeoutMillis()).isAtLeast(10000);
        assertThat(client.writeTimeoutMillis()).isAtLeast(10000);
    }
}
