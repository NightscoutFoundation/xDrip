package com.eveningoutpost.dexdrip.influxdb;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class InfluxDBUploaderTest extends RobolectricTestWithConfig {

    @Test
    public void constructor_createsClientWithCorrectTimeouts() throws Exception {
        // :: Setup
        InfluxDBUploader uploader = new InfluxDBUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        Field clientField = InfluxDBUploader.class.getDeclaredField("client");
        clientField.setAccessible(true);
        OkHttpClient.Builder builder = (OkHttpClient.Builder) clientField.get(uploader);
        OkHttpClient client = builder.build();

        // :: Verify
        assertThat(client.connectTimeoutMillis()).isEqualTo(30000);
        assertThat(client.readTimeoutMillis()).isEqualTo(60000);
        assertThat(client.writeTimeoutMillis()).isEqualTo(60000);
    }

    @Test
    public void constructor_hasNetworkInterceptor() throws Exception {
        // :: Setup
        InfluxDBUploader uploader = new InfluxDBUploader(
                org.robolectric.RuntimeEnvironment.application);

        // :: Act
        Field clientField = InfluxDBUploader.class.getDeclaredField("client");
        clientField.setAccessible(true);
        OkHttpClient.Builder builder = (OkHttpClient.Builder) clientField.get(uploader);
        OkHttpClient client = builder.build();

        // :: Verify
        assertThat(client.networkInterceptors()).hasSize(1);
    }
}
