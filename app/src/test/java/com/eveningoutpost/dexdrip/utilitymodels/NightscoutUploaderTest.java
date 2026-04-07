package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class NightscoutUploaderTest extends RobolectricTestWithConfig {

    @Test
    public void constructor_createsClientWithCorrectTimeouts() throws Exception {
        // :: Setup & Act
        NightscoutUploader uploader = new NightscoutUploader(RuntimeEnvironment.application);
        Field clientField = NightscoutUploader.class.getDeclaredField("client");
        clientField.setAccessible(true);
        OkHttpClient client = (OkHttpClient) clientField.get(uploader);

        // :: Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(60000);
    }
}
